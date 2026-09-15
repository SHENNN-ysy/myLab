package com.myblog.infrastructure.engagement;

import com.myblog.application.repository.EngagementStatsRepository;
import com.myblog.application.service.engagement.EngagementPersistenceService;
import com.myblog.application.service.engagement.EngagementService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/** Redis Stream 消费任务：恢复聚合快照、持续落库，并定期清理已确认历史。 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.engagement-stream", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class EngagementStreamJob implements ApplicationRunner {
    private static final int DAILY_RETENTION_DAYS = 120;
    private static final int CONSUMER_ID_LENGTH = 8;
    private final EngagementPersistenceService persistence;
    private final RedisEngagementStore store;
    private final EngagementStatsRepository repository;
    private final String consumerName;
    private final Counter processedCounter;
    private final Counter failureCounter;
    private final Timer batchTimer;
    private final AtomicLong lastSuccessEpochMillis = new AtomicLong();
    private final AtomicLong streamLength = new AtomicLong();
    private final AtomicLong pendingMessages = new AtomicLong();
    private volatile boolean snapshotsRestored;

    public EngagementStreamJob(EngagementPersistenceService persistence,
                               RedisEngagementStore store,
                               EngagementStatsRepository repository,
                               MeterRegistry registry) {
        this.persistence = persistence;
        this.store = store;
        this.repository = repository;
        this.consumerName = consumerName();
        this.processedCounter = registry.counter("engagement.stream.messages.processed");
        this.failureCounter = registry.counter("engagement.stream.consume.failures");
        this.batchTimer = registry.timer("engagement.stream.batch.duration");
        Gauge.builder("engagement.stream.last.success.epoch.millis", lastSuccessEpochMillis, AtomicLong::get)
                .register(registry);
        Gauge.builder("engagement.stream.length", streamLength, AtomicLong::get).register(registry);
        Gauge.builder("engagement.stream.pending", pendingMessages, AtomicLong::get).register(registry);
    }

    /** 创建消费组并以 PG 最近快照补齐缺失的 Redis 聚合键，不覆盖 Redis 已有实时更新。 */
    @Override
    public void run(ApplicationArguments args) {
        try {
            initializeAndRestore();
        } catch (RuntimeException exception) {
            log.warn("互动 Stream 启动初始化失败，消费任务将在后续轮次重试", exception);
        }
    }

    /** 短周期批量消费；异常不确认消息，由后续 Pending 接管重试。 */
    @Scheduled(fixedDelayString = "${app.engagement-stream.poll-delay:1s}",
            initialDelayString = "${app.engagement-stream.poll-delay:1s}")
    public void consume() {
        Timer.Sample sample = Timer.start();
        try {
            initializeAndRestore();
            int processed = persistence.synchronize(consumerName);
            if (processed > 0) {
                processedCounter.increment(processed);
                lastSuccessEpochMillis.set(System.currentTimeMillis());
            }
            updateBacklogMetrics();
        } catch (RuntimeException exception) {
            failureCounter.increment();
            log.error("互动 Stream 消费失败，消息将保留等待重试", exception);
        } finally {
            sample.stop(batchTimer);
        }
    }

    /** 定期按 Consumer Group 安全水位清理超过保留期的已消费消息。 */
    @Scheduled(fixedDelayString = "${app.engagement-stream.trim-interval:1h}",
            initialDelayString = "${app.engagement-stream.trim-interval:1h}")
    public void trim() {
        try {
            persistence.trimAcknowledged();
            updateBacklogMetrics();
        } catch (RuntimeException exception) {
            log.warn("互动 Stream 历史清理失败，将在下一周期重试", exception);
        }
    }

    private String consumerName() {
        String hostname = System.getenv().getOrDefault("HOSTNAME", "local");
        return hostname + ":" + UUID.randomUUID().toString().substring(0, CONSUMER_ID_LENGTH);
    }

    /** 初始化失败不阻断应用；调度轮次持续重试，成功后仅执行一次 PG 快照回填。 */
    private synchronized void initializeAndRestore() {
        persistence.initialize();
        if (snapshotsRestored) return;
        repository.findAllEngagement().forEach(store::restoreEngagement);
        store.restoreSite(repository.findSiteStatistics());
        LocalDate today = LocalDate.now(EngagementService.BUSINESS_ZONE);
        repository.findDailyStatistics(today.minusDays(DAILY_RETENTION_DAYS - 1L), today)
                .forEach(store::restoreDaily);
        snapshotsRestored = true;
    }

    private void updateBacklogMetrics() {
        com.myblog.application.port.EngagementEventStream.Status status = persistence.status();
        streamLength.set(status.length());
        pendingMessages.set(status.pending());
    }

}
