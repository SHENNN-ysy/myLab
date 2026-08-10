package com.myblog.application.port;

import com.myblog.application.model.dto.EngagementDtos;

import java.time.LocalDate;
import java.util.List;

/** Redis 实时互动状态端口。匿名访客状态只允许存在于该端口对应的缓存实现中。 */
public interface EngagementStore {
    boolean visitorExists(String visitorHash);

    void createVisitor(String visitorHash);

    List<EngagementDtos.EngagementSummary> engagement(List<String> postKeys);

    EngagementDtos.EngagementView registerView(String visitorHash, String postKey, LocalDate date);

    EngagementDtos.EngagementView like(String visitorHash, String postKey, LocalDate date);

    EngagementDtos.EngagementView unlike(String visitorHash, String postKey);

    EngagementDtos.SiteStatisticsView registerVisit(String visitorHash, LocalDate date);

    EngagementDtos.SiteStatisticsView siteStatistics();

    EngagementDtos.DailyStatisticsView dailyStatistics(LocalDate date);
}
