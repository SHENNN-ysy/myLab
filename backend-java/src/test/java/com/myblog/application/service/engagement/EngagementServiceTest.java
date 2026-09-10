package com.myblog.application.service.engagement;

import com.myblog.application.model.dto.EngagementDtos;
import com.myblog.application.port.EngagementStore;
import com.myblog.application.repository.EngagementStatsRepository;
import com.myblog.common.exception.EngagementUnavailableException;
import com.myblog.common.exception.NotFoundException;
import com.myblog.common.exception.ValidationException;
import com.myblog.common.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * EngagementService 单测：覆盖浏览/点赞/访问计数读写、Redis 故障时降级读取 PG 快照、
 * 趋势统计的时区处理与当日实时值合并，以及批量查询参数校验。
 */
@ExtendWith(MockitoExtension.class)
class EngagementServiceTest {
    @Mock EngagementStore store;
    @Mock EngagementStatsRepository repository;

    private EngagementService service;
    private CurrentUser admin;

    @BeforeEach
    void setUp() {
        service = new EngagementService(store, repository);
        admin = new CurrentUser(UUID.randomUUID(), "admin", "admin");
    }

    /** Redis 不可用时站点统计降级为 PG 快照。 */
    @Test
    void summaryFallsBackToPostgresSnapshotWhenRedisIsUnavailable() {
        EngagementDtos.SiteStatisticsView snapshot =
                new EngagementDtos.SiteStatisticsView(12, 34, 5, OffsetDateTime.now());
        when(store.siteStatistics()).thenThrow(new EngagementUnavailableException());
        when(repository.findSiteStatistics()).thenReturn(snapshot);

        assertThat(service.siteStatistics()).isEqualTo(snapshot);
    }

    /** 批量降级按请求顺序返回，快照缺失的键计数补零。 */
    @Test
    void batchFallbackPreservesRequestedOrderAndFillsMissingCounters() {
        when(store.engagement(any())).thenThrow(new EngagementUnavailableException());
        when(repository.findEngagement(List.of("post-b", "post-a")))
                .thenReturn(List.of(new EngagementDtos.EngagementSummary("post-a", 8, 2)));

        List<EngagementDtos.EngagementSummary> result = service.engagement(List.of("post-b", "post-a"));

        assertThat(result).extracting(EngagementDtos.EngagementSummary::postKey)
                .containsExactly("post-b", "post-a");
        assertThat(result.getFirst().viewCount()).isZero();
        assertThat(result.get(1).likeCount()).isEqualTo(2);
    }

    /** 趋势补齐区间内每一天，当日数据取 Redis 实时值。 */
    @Test
    void trendAlwaysContainsEveryDateAndUsesRealtimeValueForToday() {
        LocalDate today = LocalDate.now(EngagementService.BUSINESS_ZONE);
        when(repository.findDailyStatistics(today.minusDays(6), today)).thenReturn(List.of(
                new EngagementDtos.DailyStatisticsView(today.minusDays(1), 2, 3, 1)));
        when(store.dailyStatistics(today)).thenReturn(
                new EngagementDtos.DailyStatisticsView(today, 4, 8, 2));

        EngagementDtos.AnalyticsTrendView result = service.trends(admin, 7);

        assertThat(result.items()).hasSize(7);
        assertThat(result.items().getFirst().date()).isEqualTo(today.minusDays(6));
        assertThat(result.items().getLast().visitCount()).isEqualTo(4);
        assertThat(result.timezone()).isEqualTo("Asia/Shanghai");
    }

    /** 不支持的趋势天数范围报参数错误。 */
    @Test
    void unsupportedTrendRangeIsRejected() {
        assertThatThrownBy(() -> service.trends(admin, 14))
                .isInstanceOf(ValidationException.class);
    }

    /** 未发布或未启用的文章不允许互动，且不触达 Redis。 */
    @Test
    void interactionRejectsPostThatIsNotPublishedAndEnabled() {
        when(repository.publishedPostExists("draft-post")).thenReturn(false);

        assertThatThrownBy(() -> service.like("visitor", "draft-post"))
                .isInstanceOf(NotFoundException.class);
        verify(store, never()).like(eq("visitor"), eq("draft-post"), any());
    }

    /** 批量查询前对重复的 postKey 去重。 */
    @Test
    void duplicatePostKeysAreMergedBeforeBatchRead() {
        when(store.engagement(List.of("post-a"))).thenReturn(
                List.of(new EngagementDtos.EngagementSummary("post-a", 1, 1)));

        assertThat(service.engagement(List.of("post-a", "post-a"))).hasSize(1);
        verify(store).engagement(List.of("post-a"));
    }

    /** 空键列表报参数错误。 */
    @Test
    void batchRejectsEmptyKeyList() {
        assertThatThrownBy(() -> service.engagement(List.of()))
                .isInstanceOf(ValidationException.class);
    }

    /** 含 null 或非法字符的键报参数错误。 */
    @Test
    void batchRejectsNullOrMalformedKey() {
        // List.of 不允许 null，这里用 Arrays.asList 构造含 null 的入参
        assertThatThrownBy(() -> service.engagement(java.util.Arrays.asList("post-a", null)))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.engagement(List.of("bad key!")))
                .isInstanceOf(ValidationException.class);
    }

    /** 单次批量键数超过 100 时报参数错误。 */
    @Test
    void batchRejectsMoreThanHundredKeys() {
        // 构造 101 个键，超出单次批量上限 100
        List<String> keys = java.util.stream.IntStream.range(0, 101)
                .mapToObj(index -> "post-" + index)
                .toList();

        assertThatThrownBy(() -> service.engagement(keys))
                .isInstanceOf(ValidationException.class);
    }

    /** 已发布文章的浏览登记委托给 Redis 存储。 */
    @Test
    void registerViewDelegatesToStoreForPublishedPost() {
        EngagementDtos.EngagementView view = view("post-a", 5, 1, false);
        when(repository.publishedPostExists("post-a")).thenReturn(true);
        when(store.registerView(eq("visitor"), eq("post-a"), any())).thenReturn(view);

        assertThat(service.registerView("visitor", "post-a")).isEqualTo(view);
    }

    /** 未发布文章登记浏览报不存在，且不触达 Redis。 */
    @Test
    void registerViewRejectsUnpublishedPost() {
        when(repository.publishedPostExists("draft-post")).thenReturn(false);

        assertThatThrownBy(() -> service.registerView("visitor", "draft-post"))
                .isInstanceOf(NotFoundException.class);
        verify(store, never()).registerView(eq("visitor"), eq("draft-post"), any());
    }

    /** 已发布文章的点赞委托给 Redis 存储。 */
    @Test
    void likeDelegatesToStoreForPublishedPost() {
        EngagementDtos.EngagementView view = view("post-a", 5, 2, true);
        when(repository.publishedPostExists("post-a")).thenReturn(true);
        when(store.like(eq("visitor"), eq("post-a"), any())).thenReturn(view);

        assertThat(service.like("visitor", "post-a")).isEqualTo(view);
    }

    /** 非法 postKey 点赞报参数错误。 */
    @Test
    void likeRejectsMalformedPostKey() {
        assertThatThrownBy(() -> service.like("visitor", "bad key!"))
                .isInstanceOf(ValidationException.class);
    }

    /** 取消点赞委托给 Redis 存储。 */
    @Test
    void unlikeDelegatesToStoreForPublishedPost() {
        EngagementDtos.EngagementView view = view("post-a", 5, 1, false);
        when(repository.publishedPostExists("post-a")).thenReturn(true);
        when(store.unlike("visitor", "post-a")).thenReturn(view);

        assertThat(service.unlike("visitor", "post-a")).isEqualTo(view);
    }

    /** 站点访问登记直接委托给 Redis 存储。 */
    @Test
    void registerVisitDelegatesToStore() {
        EngagementDtos.SiteStatisticsView statistics =
                new EngagementDtos.SiteStatisticsView(10, 20, 3, OffsetDateTime.now());
        when(store.registerVisit(eq("visitor"), any())).thenReturn(statistics);

        assertThat(service.registerVisit("visitor")).isEqualTo(statistics);
    }

    /** Redis 可用时站点统计取实时值，不查 PG 快照。 */
    @Test
    void siteStatisticsPrefersRedisRealtimeValue() {
        EngagementDtos.SiteStatisticsView realtime =
                new EngagementDtos.SiteStatisticsView(10, 20, 3, OffsetDateTime.now());
        when(store.siteStatistics()).thenReturn(realtime);

        assertThat(service.siteStatistics()).isEqualTo(realtime);
        verify(repository, never()).findSiteStatistics();
    }

    /** 非管理员查看后台统计摘要被拒绝。 */
    @Test
    void adminSummaryRequiresAdmin() {
        CurrentUser viewer = new CurrentUser(UUID.randomUUID(), "guest", "viewer");

        assertThatThrownBy(() -> service.adminSummary(viewer))
                .isInstanceOf(com.myblog.common.exception.ForbiddenException.class);
    }

    /** 管理员统计摘要返回站点实时统计。 */
    @Test
    void adminSummaryReturnsSiteStatistics() {
        EngagementDtos.SiteStatisticsView realtime =
                new EngagementDtos.SiteStatisticsView(10, 20, 3, OffsetDateTime.now());
        when(store.siteStatistics()).thenReturn(realtime);

        assertThat(service.adminSummary(admin)).isEqualTo(realtime);
    }

    /** Redis 不可用时当日趋势沿用 PG 快照。 */
    @Test
    void trendKeepsDatabaseSnapshotForTodayWhenRedisIsUnavailable() {
        LocalDate today = LocalDate.now(EngagementService.BUSINESS_ZONE);
        EngagementDtos.DailyStatisticsView todaySnapshot =
                new EngagementDtos.DailyStatisticsView(today, 7, 9, 1);
        when(repository.findDailyStatistics(today.minusDays(6), today)).thenReturn(List.of(todaySnapshot));
        when(store.dailyStatistics(today)).thenThrow(new EngagementUnavailableException());

        EngagementDtos.AnalyticsTrendView result = service.trends(admin, 7);

        assertThat(result.items()).hasSize(7);
        assertThat(result.items().getLast()).isEqualTo(todaySnapshot);
    }

    private EngagementDtos.EngagementView view(String postKey, long views, long likes, boolean liked) {
        return new EngagementDtos.EngagementView(postKey, views, likes, liked,
                new EngagementDtos.SiteStatisticsView(10, 20, 3, OffsetDateTime.now()));
    }
}
