package com.myblog.application.port;

import com.myblog.application.model.dto.EngagementDtos;

import java.time.LocalDate;
import java.util.List;

/**
 * Redis 实时互动状态端口：隔离"高并发、允许最终一致"的互动计数能力。
 * 匿名访客状态只允许存在于该端口对应的缓存实现中（不落库、不入 PG）；
 * 持久化通过快照任务周期性把 Redis 绝对值写入 {@code EngagementStatsRepository}，
 * 因此本端口只承诺实时视图，不承诺强持久。
 * <p>
 * 页面浏览、点赞和取消点赞属于复合写操作，实现必须原子完成访客去重、聚合计数、
 * 滑动过期刷新和落库通知，避免并发请求造成计数与脏标记不一致。
 */
public interface EngagementStore {
    /** 判断访客 Hash 是否仍处于有效期内；该查询本身不刷新滑动 TTL。 */
    boolean visitorExists(String visitorHash);

    /** 创建访客状态，仅登记身份元数据，不计入访问量或浏览量。 */
    void createVisitor(String visitorHash);

    /** 按输入顺序批量读取文章或项目的实时浏览量、点赞量，缺失计数按 0 返回。 */
    List<EngagementDtos.EngagementSummary> engagement(List<String> postKeys);

    /**
     * 登记页面浏览并刷新访客有效期。
     * 同一访客对同一页面目标只计数一次；首次有效页面行为同时登记一次站点访问。
     */
    EngagementDtos.PageViewResult registerPageView(
            String visitorHash, EngagementDtos.PageType pageType, String postKey, LocalDate date);

    /**
     * 幂等登记点赞并刷新访客有效期；尚未登记详情浏览时，同时补记访问和详情浏览。
     */
    EngagementDtos.EngagementView like(String visitorHash, String postKey, LocalDate date);

    /**
     * 幂等取消点赞并刷新访客有效期；尚未登记详情浏览时，同时补记访问和详情浏览。
     */
    EngagementDtos.EngagementView unlike(String visitorHash, String postKey, LocalDate date);

    /** 读取站点访问量、总浏览量和总点赞量的 Redis 实时绝对值。 */
    EngagementDtos.SiteStatisticsView siteStatistics();

    /** 读取指定业务日期的访问量、浏览量和点赞量实时绝对值。 */
    EngagementDtos.DailyStatisticsView dailyStatistics(LocalDate date);
}
