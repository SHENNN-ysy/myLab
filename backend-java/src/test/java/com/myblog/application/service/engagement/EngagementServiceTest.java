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

    @Test
    void summaryFallsBackToPostgresSnapshotWhenRedisIsUnavailable() {
        EngagementDtos.SiteStatisticsView snapshot =
                new EngagementDtos.SiteStatisticsView(12, 34, 5, OffsetDateTime.now());
        when(store.siteStatistics()).thenThrow(new EngagementUnavailableException());
        when(repository.findSiteStatistics()).thenReturn(snapshot);

        assertThat(service.siteStatistics()).isEqualTo(snapshot);
    }

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

    @Test
    void unsupportedTrendRangeIsRejected() {
        assertThatThrownBy(() -> service.trends(admin, 14))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void interactionRejectsPostThatIsNotPublishedAndEnabled() {
        when(repository.publishedPostExists("draft-post")).thenReturn(false);

        assertThatThrownBy(() -> service.like("visitor", "draft-post"))
                .isInstanceOf(NotFoundException.class);
        verify(store, never()).like(eq("visitor"), eq("draft-post"), any());
    }

    @Test
    void duplicatePostKeysAreMergedBeforeBatchRead() {
        when(store.engagement(List.of("post-a"))).thenReturn(
                List.of(new EngagementDtos.EngagementSummary("post-a", 1, 1)));

        assertThat(service.engagement(List.of("post-a", "post-a"))).hasSize(1);
        verify(store).engagement(List.of("post-a"));
    }
}
