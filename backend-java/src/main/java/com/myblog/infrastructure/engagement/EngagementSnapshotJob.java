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

/** 每分钟把 Redis 绝对计数写入 PostgreSQL，并在启动时恢复聚合数据。 */
@Slf4j
@Component
public class EngagementSnapshotJob implements ApplicationRunner {
    private final RedisEngagementStore store;
    private final EngagementStatsRepository repository;

    public EngagementSnapshotJob(RedisEngagementStore store, EngagementStatsRepository repository) {
        this.store = store;
        this.repository = repository;
    }

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
