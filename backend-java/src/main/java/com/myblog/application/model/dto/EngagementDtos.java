package com.myblog.application.model.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** 内容互动、全站流量和管理端趋势相关的稳定 API 数据结构。 */
public final class EngagementDtos {
    private EngagementDtos() { }

    /** 单篇文章的互动计数摘要（浏览数 + 点赞数）。 */
    public record EngagementSummary(String postKey, long viewCount, long likeCount) { }

    /** 全站累计统计视图；snapshotAt 为数据来源（实时或快照）的生成时间。 */
    public record SiteStatisticsView(
            long visitCount,
            long totalViewCount,
            long totalLikeCount,
            OffsetDateTime snapshotAt) { }

    /** 互动操作（浏览/点赞/取消点赞）后的完整视图：文章计数、当前访客是否已赞，以及全站累计统计。 */
    public record EngagementView(
            String postKey,
            long viewCount,
            long likeCount,
            boolean liked,
            SiteStatisticsView siteStatistics) { }

    /** 单日统计视图，日期按业务时区（Asia/Shanghai）划分。 */
    public record DailyStatisticsView(
            LocalDate date,
            long visitCount,
            long viewCount,
            long likeCount) { }

    /** 管理端近 N 日趋势视图，timezone 标明 items 中日期的划分口径。 */
    public record AnalyticsTrendView(
            int days,
            String timezone,
            List<DailyStatisticsView> items) { }

    /** 匿名身份解析结果；issued=true 表示控制器需要回写新 Cookie。 */
    public record VisitorIdentity(String token, String visitorHash, boolean issued) { }
}
