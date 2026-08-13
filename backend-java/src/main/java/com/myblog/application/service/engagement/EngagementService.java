package com.myblog.application.service.engagement;

import com.myblog.application.model.dto.EngagementDtos;
import com.myblog.application.port.EngagementStore;
import com.myblog.application.repository.EngagementStatsRepository;
import com.myblog.common.exception.EngagementUnavailableException;
import com.myblog.common.exception.NotFoundException;
import com.myblog.common.exception.ValidationException;
import com.myblog.common.security.Authorization;
import com.myblog.common.security.CurrentUser;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 内容互动（浏览/点赞）与站点统计应用服务。
 * 实时计数走 Redis（EngagementStore），Redis 不可用时降级读取 PostgreSQL 聚合快照，
 * 保证公开读接口不随缓存故障整体不可用；写入路径不做降级，直接失败。
 * 隐私边界：服务内只接触经 HMAC 哈希的 visitorHash，不保存访客原始标识。
 */
@Service
public class EngagementService {
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai"); // 统计口径统一按业务时区（东八区）划日
    private static final Pattern POST_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,95}$");
    private static final Set<Integer> TREND_DAYS = Set.of(7, 30, 90);

    private final EngagementStore store;
    private final EngagementStatsRepository repository;

    public EngagementService(EngagementStore store, EngagementStatsRepository repository) {
        this.store = store;
        this.repository = repository;
    }

    /**
     * 批量查询文章的浏览/点赞计数。
     * 优先读 Redis 实时值；Redis 不可用时降级为 PostgreSQL 最近一次聚合快照，
     * 快照同样缺失的文章按 0 兜底，保证返回数量与入参一一对应。
     */
    public List<EngagementDtos.EngagementSummary> engagement(List<String> rawPostKeys) {
        List<String> postKeys = normalizePostKeys(rawPostKeys);
        try {
            return store.engagement(postKeys);
        } catch (EngagementUnavailableException exception) {
            Map<String, EngagementDtos.EngagementSummary> snapshots = new LinkedHashMap<>();
            repository.findEngagement(postKeys).forEach(value -> snapshots.put(value.postKey(), value));
            return postKeys.stream()
                    .map(key -> snapshots.getOrDefault(key, new EngagementDtos.EngagementSummary(key, 0, 0)))
                    .toList();
        }
    }

    /**
     * 登记一次文章浏览：先确认文章已对外可见再写入 Redis（同一访客当日去重由存储层保证）。
     */
    public EngagementDtos.EngagementView registerView(String visitorHash, String postKey) {
        validatePublishedPost(postKey);
        return store.registerView(visitorHash, postKey, today());
    }

    /**
     * 点赞：先确认文章已对外可见再写入 Redis。
     */
    public EngagementDtos.EngagementView like(String visitorHash, String postKey) {
        validatePublishedPost(postKey);
        return store.like(visitorHash, postKey, today());
    }

    /**
     * 取消点赞：先确认文章已对外可见再写入 Redis。
     */
    public EngagementDtos.EngagementView unlike(String visitorHash, String postKey) {
        validatePublishedPost(postKey);
        return store.unlike(visitorHash, postKey);
    }

    /** 登记一次站点访问（按访客当日去重，由存储层保证）。 */
    public EngagementDtos.SiteStatisticsView registerVisit(String visitorHash) {
        return store.registerVisit(visitorHash, today());
    }

    /**
     * 站点累计统计：优先读 Redis 实时值，不可用时降级为 PostgreSQL 最近一次快照。
     */
    public EngagementDtos.SiteStatisticsView siteStatistics() {
        try {
            return store.siteStatistics();
        } catch (EngagementUnavailableException exception) {
            return repository.findSiteStatistics();
        }
    }

    public EngagementDtos.SiteStatisticsView adminSummary(CurrentUser actor) {
        Authorization.requireAdmin(actor);
        return siteStatistics();
    }

    /**
     * 近 N 日趋势（仅管理员）：历史日期取 PostgreSQL 每日快照，当天用 Redis 实时值覆盖，
     * 避免快照尚未落库导致当天数据恒为 0；Redis 不可用时保留快照值。缺数据的日期补 0。
     */
    public EngagementDtos.AnalyticsTrendView trends(CurrentUser actor, int days) {
        Authorization.requireAdmin(actor);
        if (!TREND_DAYS.contains(days)) throw new ValidationException("days 只允许 7、30 或 90");

        LocalDate today = today();
        LocalDate from = today.minusDays(days - 1L);
        Map<LocalDate, EngagementDtos.DailyStatisticsView> values = new LinkedHashMap<>();
        repository.findDailyStatistics(from, today).forEach(value -> values.put(value.date(), value));
        try {
            values.put(today, store.dailyStatistics(today));
        } catch (EngagementUnavailableException ignored) {
            // Redis 不可用时保留数据库最后一次快照。
        }

        List<EngagementDtos.DailyStatisticsView> items = new ArrayList<>(days);
        for (LocalDate date = from; !date.isAfter(today); date = date.plusDays(1)) {
            items.add(values.getOrDefault(date, new EngagementDtos.DailyStatisticsView(date, 0, 0, 0)));
        }
        return new EngagementDtos.AnalyticsTrendView(days, BUSINESS_ZONE.getId(), items);
    }

    /**
     * 写入前的双层校验：先校验 post_key 格式，再确认文章存在且属于已发布内容，
     * 防止对未公开或不存在的内容刷互动计数。
     */
    private void validatePublishedPost(String postKey) {
        validatePostKey(postKey);
        if (!repository.publishedPostExists(postKey)) {
            throw new NotFoundException("文章或项目不存在、未启用或尚未发布");
        }
    }

    /** 归一化批量查询的 post_key：去空白、去重、校验格式，并限制单次最多 100 个以防滥用。 */
    private List<String> normalizePostKeys(List<String> rawPostKeys) {
        if (rawPostKeys == null || rawPostKeys.isEmpty()) throw new ValidationException("post_keys 不能为空");
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        rawPostKeys.forEach(value -> {
            String postKey = value == null ? "" : value.trim();
            validatePostKey(postKey);
            unique.add(postKey);
        });
        if (unique.size() > 100) throw new ValidationException("post_keys 最多允许 100 个");
        return List.copyOf(unique);
    }

    private void validatePostKey(String postKey) {
        if (postKey == null || !POST_KEY_PATTERN.matcher(postKey).matches()) {
            throw new ValidationException("post_key 格式不正确");
        }
    }

    private LocalDate today() {
        return ZonedDateTime.now(BUSINESS_ZONE).toLocalDate();
    }
}
