package com.myblog.application.service.engagement;

import com.myblog.application.model.dto.EngagementDtos;
import com.myblog.application.port.EngagementEventStream;
import com.myblog.application.port.EngagementPersistenceLease;
import com.myblog.application.port.EngagementStore;
import com.myblog.application.repository.EngagementStatsRepository;
import com.myblog.common.properties.EngagementStreamProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** EngagementPersistenceService 单测：覆盖批量聚合、Pending 重试、死信、确认顺序和单活租约。 */
@ExtendWith(MockitoExtension.class)
class EngagementPersistenceServiceTest {
    private static final Duration RETENTION = Duration.ofHours(72);
    private static final LocalDate DATE = LocalDate.of(2026, 9, 14);

    @Mock EngagementEventStream stream;
    @Mock EngagementPersistenceLease lease;
    @Mock EngagementStore store;
    @Mock EngagementStatsRepository repository;

    private EngagementPersistenceService service;

    @BeforeEach
    void setUp() {
        service = service(true);
    }

    /** 新消息按文章、站点和日期去重读取绝对值，事务保存成功后批量确认。 */
    @Test
    @SuppressWarnings("unchecked")
    void newMessagesAreCoalescedPersistedAndAcknowledged() {
        acquireLease();
        when(stream.claimStale("consumer", 500, Duration.ofSeconds(30))).thenReturn(List.of());
        when(stream.readNew("consumer", 500, Duration.ofSeconds(2))).thenReturn(List.of(
                message("1-0", "VIEW", "post-a", DATE, true, true, true),
                message("2-0", "LIKE", "post-a", DATE, true, true, true),
                message("3-0", "VISIT", "", DATE, false, true, true),
                message("4-0", "UNLIKE", "post-b", null, true, true, false)));
        when(store.engagement(List.of("post-a", "post-b"))).thenReturn(List.of(
                new EngagementDtos.EngagementSummary("post-a", 8, 2),
                new EngagementDtos.EngagementSummary("post-b", 3, 0)));
        EngagementDtos.SiteStatisticsView site = new EngagementDtos.SiteStatisticsView(
                5, 11, 2, OffsetDateTime.now());
        when(store.siteStatistics()).thenReturn(site);
        EngagementDtos.DailyStatisticsView daily = new EngagementDtos.DailyStatisticsView(DATE, 5, 8, 2);
        when(store.dailyStatistics(DATE)).thenReturn(daily);

        assertThat(service.synchronize("consumer")).isEqualTo(4);

        ArgumentCaptor<List<EngagementDtos.EngagementSummary>> engagement = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<EngagementDtos.DailyStatisticsView>> days = ArgumentCaptor.forClass(List.class);
        verify(repository).saveSnapshot(engagement.capture(), eq(site), days.capture());
        assertThat(engagement.getValue()).hasSize(2);
        assertThat(days.getValue()).containsExactly(daily);
        verify(stream).acknowledge(List.of("1-0", "2-0", "3-0", "4-0"));
        verify(lease).release("lease-token");
    }

    /** Pending 消息优先于新消息处理，避免死亡消费者遗留消息长期饥饿。 */
    @Test
    void stalePendingMessagesAreProcessedFirst() {
        acquireLease();
        EngagementEventStream.Message pending = message(
                "5-0", "UNLIKE", "post-a", null, true, true, false);
        when(stream.claimStale("consumer", 500, Duration.ofSeconds(30))).thenReturn(List.of(pending));
        when(store.engagement(List.of("post-a"))).thenReturn(List.of(
                new EngagementDtos.EngagementSummary("post-a", 1, 0)));
        when(store.siteStatistics()).thenReturn(new EngagementDtos.SiteStatisticsView(0, 1, 0, null));

        assertThat(service.synchronize("consumer")).isOne();

        verify(stream, never()).readNew(any(), eq(500), any());
        verify(stream).acknowledge(List.of("5-0"));
    }

    /** 无法解析的内部消息全部进入 DLQ 后确认，不阻塞后续 Pending。 */
    @Test
    void malformedMessagesMoveToDeadLetterAndAreAcknowledged() {
        acquireLease();
        when(stream.claimStale(any(), eq(500), any())).thenReturn(List.of(
                raw("1-0", Map.of("event_type", "UNKNOWN")),
                raw("2-0", Map.of("event_type", "VIEW", "post_dirty", "x",
                        "site_dirty", "1", "daily_dirty", "1")),
                message("3-0", "VIEW", "post-a", DATE, false, true, true),
                message("4-0", "VIEW", "bad key", DATE, true, true, true),
                message("5-0", "VISIT", "", null, false, true, true)));

        assertThat(service.synchronize("consumer")).isZero();

        verify(stream).acknowledge(List.of("1-0", "2-0", "3-0", "4-0", "5-0"));
        verify(stream).deadLetter(eq(raw("1-0", Map.of("event_type", "UNKNOWN"))), any());
        verify(repository, never()).saveSnapshot(any(), any(), any());
    }

    /** PostgreSQL 提交失败时不确认有效消息，租约仍在 finally 中释放。 */
    @Test
    void databaseFailureLeavesMessagePending() {
        acquireLease();
        when(stream.claimStale(any(), eq(500), any())).thenReturn(List.of(
                message("1-0", "VISIT", "", DATE, false, true, true)));
        when(store.siteStatistics()).thenReturn(new EngagementDtos.SiteStatisticsView(1, 0, 0, null));
        when(store.dailyStatistics(DATE)).thenReturn(new EngagementDtos.DailyStatisticsView(DATE, 1, 0, 0));
        RuntimeException failure = new RuntimeException("database unavailable");
        org.mockito.Mockito.doThrow(failure).when(repository).saveSnapshot(any(), any(), any());

        assertThatThrownBy(() -> service.synchronize("consumer")).isSameAs(failure);

        verify(stream, never()).acknowledge(any());
        verify(lease).release("lease-token");
    }

    /** 关闭 Stream 或未抢到租约时不读取队列。 */
    @Test
    void disabledOrLeaseContentionSkipsConsumption() {
        EngagementPersistenceService disabled = service(false);
        assertThat(disabled.synchronize("consumer")).isZero();
        disabled.initialize();
        disabled.trimAcknowledged();
        assertThat(disabled.status()).isEqualTo(new EngagementEventStream.Status(0, 0));
        verify(stream, never()).ensureGroup();

        when(lease.tryAcquire(Duration.ofSeconds(30))).thenReturn(null);
        assertThat(service.synchronize("consumer")).isZero();
        verify(stream, never()).readNew(any(), eq(500), any());
    }

    /** 初始化和历史清理使用配置中的消费组与保留期。 */
    @Test
    void initializationAndTrimDelegateToStream() {
        EngagementEventStream.Status status = new EngagementEventStream.Status(12, 3);
        when(stream.status()).thenReturn(status);
        service.initialize();
        service.trimAcknowledged();

        verify(stream).ensureGroup();
        verify(stream).trimAcknowledged(RETENTION);
        assertThat(service.status()).isEqualTo(status);
    }

    /** 没有消息时正常释放租约；释放异常只告警，不破坏消费结果。 */
    @Test
    void emptyBatchAndReleaseFailureAreHarmless() {
        acquireLease();
        when(stream.claimStale(any(), eq(500), any())).thenReturn(List.of());
        when(stream.readNew(any(), eq(500), any())).thenReturn(List.of());
        org.mockito.Mockito.doThrow(new RuntimeException("release failed")).when(lease).release("lease-token");

        assertThat(service.synchronize("consumer")).isZero();
    }

    private void acquireLease() {
        when(lease.tryAcquire(Duration.ofSeconds(30))).thenReturn("lease-token");
    }

    private EngagementPersistenceService service(boolean enabled) {
        EngagementStreamProperties properties = new EngagementStreamProperties(
                enabled, 500, Duration.ofSeconds(2), Duration.ofSeconds(30), RETENTION,
                Duration.ofSeconds(30), Duration.ofSeconds(1), Duration.ofHours(1));
        return new EngagementPersistenceService(stream, lease, store, repository, properties);
    }

    private EngagementEventStream.Message message(String id, String type, String postKey, LocalDate date,
                                                   boolean postDirty, boolean siteDirty, boolean dailyDirty) {
        return raw(id, Map.of(
                "event_type", type,
                "post_key", postKey,
                "stat_date", date == null ? "" : date.toString(),
                "post_dirty", postDirty ? "1" : "0",
                "site_dirty", siteDirty ? "1" : "0",
                "daily_dirty", dailyDirty ? "1" : "0"));
    }

    private EngagementEventStream.Message raw(String id, Map<String, String> fields) {
        return new EngagementEventStream.Message(id, fields);
    }
}
