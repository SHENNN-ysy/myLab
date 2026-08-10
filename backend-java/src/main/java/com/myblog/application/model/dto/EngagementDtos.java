package com.myblog.application.model.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** 内容互动、全站流量和管理端趋势相关的稳定 API 数据结构。 */
public final class EngagementDtos {
    private EngagementDtos() { }

    public record EngagementSummary(String postKey, long viewCount, long likeCount) { }

    public record SiteStatisticsView(
            long visitCount,
            long totalViewCount,
            long totalLikeCount,
            OffsetDateTime snapshotAt) { }

    public record EngagementView(
            String postKey,
            long viewCount,
            long likeCount,
            boolean liked,
            SiteStatisticsView siteStatistics) { }

    public record DailyStatisticsView(
            LocalDate date,
            long visitCount,
            long viewCount,
            long likeCount) { }

    public record AnalyticsTrendView(
            int days,
            String timezone,
            List<DailyStatisticsView> items) { }

    /** 匿名身份解析结果；issued=true 表示控制器需要回写新 Cookie。 */
    public record VisitorIdentity(String token, String visitorHash, boolean issued) { }
}
