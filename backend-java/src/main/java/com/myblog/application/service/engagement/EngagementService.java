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

/** 内容互动与站点统计应用服务。实时读取 Redis，读取失败时使用 PostgreSQL 聚合快照。 */
@Service
public class EngagementService {
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Pattern POST_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,95}$");
    private static final Set<Integer> TREND_DAYS = Set.of(7, 30, 90);

    private final EngagementStore store;
    private final EngagementStatsRepository repository;

    public EngagementService(EngagementStore store, EngagementStatsRepository repository) {
        this.store = store;
        this.repository = repository;
    }

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

    public EngagementDtos.EngagementView registerView(String visitorHash, String postKey) {
        validatePublishedPost(postKey);
        return store.registerView(visitorHash, postKey, today());
    }

    public EngagementDtos.EngagementView like(String visitorHash, String postKey) {
        validatePublishedPost(postKey);
        return store.like(visitorHash, postKey, today());
    }

    public EngagementDtos.EngagementView unlike(String visitorHash, String postKey) {
        validatePublishedPost(postKey);
        return store.unlike(visitorHash, postKey);
    }

    public EngagementDtos.SiteStatisticsView registerVisit(String visitorHash) {
        return store.registerVisit(visitorHash, today());
    }

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

    private void validatePublishedPost(String postKey) {
        validatePostKey(postKey);
        if (!repository.publishedPostExists(postKey)) {
            throw new NotFoundException("文章或项目不存在、未启用或尚未发布");
        }
    }

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
