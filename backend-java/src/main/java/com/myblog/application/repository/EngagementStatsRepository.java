package com.myblog.application.repository;

import com.myblog.application.model.dto.EngagementDtos;

import java.time.LocalDate;
import java.util.List;

/**
 * PostgreSQL 互动聚合快照仓储：只存浏览/点赞的聚合绝对值快照（文章维度、站点总计、按日），
 * 由快照任务从 Redis 整体覆盖（upsert），可幂等重放。
 * 禁止在此接口中引入任何访客或点赞关系数据——访客明细只存在于 Redis 实时层。
 */
public interface EngagementStatsRepository {
    boolean publishedPostExists(String postKey);

    List<EngagementDtos.EngagementSummary> findEngagement(List<String> postKeys);

    List<EngagementDtos.EngagementSummary> findAllEngagement();

    EngagementDtos.SiteStatisticsView findSiteStatistics();

    List<EngagementDtos.DailyStatisticsView> findDailyStatistics(LocalDate from, LocalDate to);

    void upsertEngagement(List<EngagementDtos.EngagementSummary> values);

    void upsertSiteStatistics(EngagementDtos.SiteStatisticsView value);

    void upsertDailyStatistics(List<EngagementDtos.DailyStatisticsView> values);

    void saveSnapshot(
            List<EngagementDtos.EngagementSummary> engagement,
            EngagementDtos.SiteStatisticsView site,
            List<EngagementDtos.DailyStatisticsView> daily);
}
