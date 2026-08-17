package com.myblog.infrastructure.persistence.repository;

import com.myblog.application.model.dto.EngagementDtos;
import com.myblog.application.repository.EngagementStatsRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * PostgreSQL 互动聚合统计仓储：快照落库的实现侧，不写入任何访客级数据。
 * 写入语义是"整体覆盖"——ON CONFLICT 用 Redis 的绝对值直接替换旧值而非增量累加，
 * 因此快照可幂等重放（认领模式允许"至少一次"落库，重复写同一份绝对值无害）。
 */
@Repository
public class JdbcEngagementStatsRepository implements EngagementStatsRepository {
    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;

    public JdbcEngagementStatsRepository(JdbcTemplate jdbc, NamedParameterJdbcTemplate namedJdbc) {
        this.jdbc = jdbc;
        this.namedJdbc = namedJdbc;
    }

    /** 校验 postKey 对应的是已发布且启用的 mylab 卡片，防止对未上线内容记录互动 */
    @Override
    public boolean publishedPostExists(String postKey) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM mylab_cards card
                JOIN content_releases release ON release.id = card.release_id
                WHERE release.module_key = 'mylab'
                  AND release.state = 'PUBLISHED'
                  AND release.deleted_at IS NULL
                  AND card.post_key = ?
                  AND card.enabled = TRUE
                  AND card.deleted_at IS NULL
                """, Integer.class, postKey);
        return count != null && count > 0;
    }

    @Override
    public List<EngagementDtos.EngagementSummary> findEngagement(List<String> postKeys) {
        if (postKeys.isEmpty()) return List.of();
        return namedJdbc.query("""
                SELECT post_key, view_count, like_count
                FROM mylab_engagement_stats
                WHERE post_key IN (:postKeys)
                """, new MapSqlParameterSource("postKeys", postKeys),
                (rs, rowNum) -> new EngagementDtos.EngagementSummary(
                        rs.getString("post_key"), rs.getLong("view_count"), rs.getLong("like_count")));
    }

    @Override
    public List<EngagementDtos.EngagementSummary> findAllEngagement() {
        return jdbc.query("""
                SELECT post_key, view_count, like_count
                FROM mylab_engagement_stats
                ORDER BY post_key
                """, (rs, rowNum) -> new EngagementDtos.EngagementSummary(
                rs.getString("post_key"), rs.getLong("view_count"), rs.getLong("like_count")));
    }

    @Override
    public EngagementDtos.SiteStatisticsView findSiteStatistics() {
        List<EngagementDtos.SiteStatisticsView> values = jdbc.query("""
                SELECT visit_count, total_view_count, total_like_count, updated_at
                FROM site_traffic_stats WHERE id = 1
                """, (rs, rowNum) -> new EngagementDtos.SiteStatisticsView(
                rs.getLong("visit_count"), rs.getLong("total_view_count"), rs.getLong("total_like_count"),
                rs.getObject("updated_at", OffsetDateTime.class)));
        return values.isEmpty()
                ? new EngagementDtos.SiteStatisticsView(0, 0, 0, null)
                : values.getFirst();
    }

    @Override
    public List<EngagementDtos.DailyStatisticsView> findDailyStatistics(LocalDate from, LocalDate to) {
        return jdbc.query("""
                SELECT stat_date, visit_count, view_count, like_count
                FROM site_daily_stats
                WHERE stat_date BETWEEN ? AND ?
                ORDER BY stat_date
                """, (rs, rowNum) -> new EngagementDtos.DailyStatisticsView(
                rs.getObject("stat_date", LocalDate.class), rs.getLong("visit_count"),
                rs.getLong("view_count"), rs.getLong("like_count")), from, to);
    }

    @Override
    @Transactional
    public void upsertEngagement(List<EngagementDtos.EngagementSummary> values) {
        if (values.isEmpty()) return;
        jdbc.batchUpdate("""
                INSERT INTO mylab_engagement_stats (post_key, view_count, like_count)
                VALUES (?, ?, ?)
                ON CONFLICT (post_key) DO UPDATE SET
                    view_count = EXCLUDED.view_count,
                    like_count = EXCLUDED.like_count,
                    updated_at = NOW()
                """, values, values.size(), (statement, value) -> {
            statement.setString(1, value.postKey());
            statement.setLong(2, value.viewCount());
            statement.setLong(3, value.likeCount());
        });
    }

    @Override
    public void upsertSiteStatistics(EngagementDtos.SiteStatisticsView value) {
        jdbc.update("""
                INSERT INTO site_traffic_stats (id, visit_count, total_view_count, total_like_count)
                VALUES (1, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    visit_count = EXCLUDED.visit_count,
                    total_view_count = EXCLUDED.total_view_count,
                    total_like_count = EXCLUDED.total_like_count,
                    updated_at = NOW()
                """, value.visitCount(), value.totalViewCount(), value.totalLikeCount());
    }

    @Override
    @Transactional
    public void upsertDailyStatistics(List<EngagementDtos.DailyStatisticsView> values) {
        if (values.isEmpty()) return;
        jdbc.batchUpdate("""
                INSERT INTO site_daily_stats (stat_date, visit_count, view_count, like_count)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (stat_date) DO UPDATE SET
                    visit_count = EXCLUDED.visit_count,
                    view_count = EXCLUDED.view_count,
                    like_count = EXCLUDED.like_count,
                    updated_at = NOW()
                """, values, values.size(), (statement, value) -> {
            statement.setObject(1, value.date());
            statement.setLong(2, value.visitCount());
            statement.setLong(3, value.viewCount());
            statement.setLong(4, value.likeCount());
        });
    }

    /**
     * 一次快照的整体落库：三类 upsert 包在同一事务里，避免 PG 中不同维度的
     * 快照时间不一致（文章、站点、按日要么同时更新，要么都不更新等待重试）。
     */
    @Override
    @Transactional
    public void saveSnapshot(List<EngagementDtos.EngagementSummary> engagement,
                             EngagementDtos.SiteStatisticsView site,
                             List<EngagementDtos.DailyStatisticsView> daily) {
        upsertEngagement(engagement);
        if (site != null) upsertSiteStatistics(site);
        upsertDailyStatistics(daily);
    }
}
