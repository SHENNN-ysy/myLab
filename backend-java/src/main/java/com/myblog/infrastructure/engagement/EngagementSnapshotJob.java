package com.myblog.infrastructure.engagement;

import com.myblog.application.model.dto.EngagementDtos;
import com.myblog.application.repository.EngagementStatsRepository;
import com.myblog.application.service.engagement.EngagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 互动聚合快照任务：每分钟把 Redis 中的绝对计数按 dirty→processing→ack 认领模式
 * 整体覆盖写入 PostgreSQL（见 {@link RedisEngagementStore} 的一致性模型），
 * 并在应用启动时把 PG 快照回填 Redis，作为 Redis 数据丢失后的兜底恢复。
 */
@Slf4j
@Component
public class EngagementSnapshotJob implements ApplicationRunner {
    private final RedisEngagementStore store;
    private final EngagementStatsRepository repository;

    public EngagementSnapshotJob(RedisEngagementStore store, EngagementStatsRepository repository) {
        this.store = store;
        this.repository = repository;
    }

    /**
     * 启动恢复：先把上次异常退出遗留的 processing 标记搬回 dirty，
     * 再从 PG 快照回填 Redis（近 120 天按日数据，与日键 TTL 口径一致）。
     * 失败只告警不阻断启动，后续请求与快照任务会继续重试。
     */
    @Override
    public void run(ApplicationArguments args) {
        try {
            store.recoverProcessing();
            repository.findAllEngagement().forEach(store::restoreEngagement);
            store.restoreSite(repository.findSiteStatistics());
            LocalDate today = LocalDate.now(EngagementService.BUSINESS_ZONE);
            repository.findDailyStatistics(today.minusDays(119), today).forEach(store::restoreDaily);
        } catch (RuntimeException exception) {
            log.warn("互动聚合数据启动恢复失败，将在后续请求或快照任务中重试", exception);
        }
    }

    /**
     * 每 60 秒执行一轮：抢分布式锁防多实例并发 → 认领脏标记 → 读 Redis 绝对值
     * 落 PG → ack。任何环节失败都不 ack，processing 标记留给下轮 recover 重试；
     * 锁在 finally 中释放，避免异常导致 50 秒内快照停摆。
     */
    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void synchronize() {
        String lockToken = null;
        try {
            lockToken = store.trySnapshotLock();
            if (lockToken == null) return;

            RedisEngagementStore.ClaimedSnapshot claimed = store.claimSnapshot();
            if (claimed.empty()) return;

            List<EngagementDtos.EngagementSummary> engagement = claimed.postKeys().isEmpty()
                    ? List.of() : store.engagement(List.copyOf(claimed.postKeys()));
            EngagementDtos.SiteStatisticsView site = claimed.site() ? store.siteStatistics() : null;
            List<EngagementDtos.DailyStatisticsView> daily = new ArrayList<>();
            for (String day : claimed.days()) daily.add(store.dailyStatistics(LocalDate.parse(day)));

            repository.saveSnapshot(engagement, site, daily);
            store.acknowledge(claimed);
        } catch (RuntimeException exception) {
            log.error("互动聚合快照同步失败，processing 标记将保留以便重试", exception);
        } finally {
            if (lockToken != null) {
                try {
                    store.releaseSnapshotLock(lockToken);
                } catch (RuntimeException exception) {
                    log.warn("互动聚合快照锁释放失败", exception);
                }
            }
        }
    }
}
