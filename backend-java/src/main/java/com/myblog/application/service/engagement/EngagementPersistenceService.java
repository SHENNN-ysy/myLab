package com.myblog.application.service.engagement;

import com.myblog.application.model.dto.EngagementDtos;
import com.myblog.application.port.EngagementEventStream;
import com.myblog.application.port.EngagementPersistenceLease;
import com.myblog.application.port.EngagementStore;
import com.myblog.application.repository.EngagementStatsRepository;
import com.myblog.common.constant.ContentConstant;
import com.myblog.common.properties.EngagementStreamProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Redis Stream 互动脏通知消费服务：批量读取最新绝对值，提交 PG 后再确认消息。 */
@Slf4j
@Service
public class EngagementPersistenceService {
    private static final Set<String> EVENT_TYPES = Set.of("VIEW", "LIKE", "UNLIKE", "VISIT");

    private final EngagementEventStream stream;
    private final EngagementPersistenceLease lease;
    private final EngagementStore store;
    private final EngagementStatsRepository repository;
    private final EngagementStreamProperties properties;

    public EngagementPersistenceService(EngagementEventStream stream,
                                        EngagementPersistenceLease lease,
                                        EngagementStore store,
                                        EngagementStatsRepository repository,
                                        EngagementStreamProperties properties) {
        this.stream = stream;
        this.lease = lease;
        this.store = store;
        this.repository = repository;
        this.properties = properties;
    }

    /** 应用启动时幂等创建 Stream Consumer Group。 */
    public void initialize() {
        if (properties.enabled()) stream.ensureGroup();
    }

    /**
     * 执行一个单活消费批次：优先重试 Pending，再读取新消息；PG 提交成功后才 XACK。
     *
     * @return 本轮成功确认的有效消息数量，未获得租约或没有消息时返回 0
     */
    public int synchronize(String consumer) {
        if (!properties.enabled()) return 0;
        String token = lease.tryAcquire(properties.lockTtl());
        if (token == null) return 0;
        try {
            stream.ensureGroup();
            List<EngagementEventStream.Message> messages = stream.claimStale(
                    consumer, properties.batchSize(), properties.claimIdle());
            if (messages.isEmpty()) {
                messages = stream.readNew(consumer, properties.batchSize(), properties.blockTimeout());
            }
            return persist(messages);
        } finally {
            releaseLease(token);
        }
    }

    /** 按安全水位清理已经确认且超过保留期的 Stream 历史。 */
    public void trimAcknowledged() {
        if (properties.enabled()) stream.trimAcknowledged(properties.retention());
    }

    /** 提供给调度适配器更新 Micrometer Gauge 的积压概览。 */
    public EngagementEventStream.Status status() {
        return properties.enabled() ? stream.status() : new EngagementEventStream.Status(0, 0);
    }

    private int persist(List<EngagementEventStream.Message> messages) {
        if (messages.isEmpty()) return 0;
        List<ValidMessage> valid = new ArrayList<>(messages.size());
        List<String> invalidIds = new ArrayList<>();
        for (EngagementEventStream.Message message : messages) {
            try {
                valid.add(parse(message));
            } catch (IllegalArgumentException exception) {
                stream.deadLetter(message, exception.getMessage());
                invalidIds.add(message.id());
            }
        }
        if (!invalidIds.isEmpty()) stream.acknowledge(invalidIds);
        if (valid.isEmpty()) return 0;

        LinkedHashSet<String> postKeys = new LinkedHashSet<>();
        LinkedHashSet<LocalDate> dates = new LinkedHashSet<>();
        boolean siteDirty = false;
        List<String> validIds = new ArrayList<>(valid.size());
        for (ValidMessage message : valid) {
            if (message.postKey() != null) postKeys.add(message.postKey());
            if (message.date() != null) dates.add(message.date());
            siteDirty = siteDirty || message.siteDirty();
            validIds.add(message.id());
        }

        List<EngagementDtos.EngagementSummary> engagement = postKeys.isEmpty()
                ? List.of() : store.engagement(List.copyOf(postKeys));
        EngagementDtos.SiteStatisticsView site = siteDirty ? store.siteStatistics() : null;
        List<EngagementDtos.DailyStatisticsView> daily = new ArrayList<>(dates.size());
        for (LocalDate date : dates) daily.add(store.dailyStatistics(date));

        repository.saveSnapshot(engagement, site, daily);
        stream.acknowledge(validIds);
        return valid.size();
    }

    private ValidMessage parse(EngagementEventStream.Message message) {
        Map<String, String> fields = message.fields();
        String eventType = fields.get("event_type");
        if (!EVENT_TYPES.contains(eventType)) throw invalid("event_type 非法");
        boolean postDirty = flag(fields, "post_dirty");
        boolean siteDirty = flag(fields, "site_dirty");
        boolean dailyDirty = flag(fields, "daily_dirty");
        validateDimensions(eventType, postDirty, siteDirty, dailyDirty);

        String postKey = null;
        if (postDirty) {
            postKey = fields.get("post_key");
            if (postKey == null || !ContentConstant.POST_KEY_PATTERN.matcher(postKey).matches()) {
                throw invalid("post_key 非法");
            }
        }
        LocalDate date = null;
        if (dailyDirty) {
            try {
                date = LocalDate.parse(fields.get("stat_date"));
            } catch (RuntimeException exception) {
                throw invalid("stat_date 非法");
            }
        }
        return new ValidMessage(message.id(), postKey, date, siteDirty);
    }

    private void validateDimensions(String eventType, boolean postDirty, boolean siteDirty, boolean dailyDirty) {
        boolean valid = switch (eventType) {
            case "VIEW", "LIKE" -> postDirty && siteDirty && dailyDirty;
            case "UNLIKE" -> postDirty && siteDirty && !dailyDirty;
            case "VISIT" -> !postDirty && siteDirty && dailyDirty;
            default -> false;
        };
        if (!valid) throw invalid("聚合维度与 event_type 不匹配");
    }

    private boolean flag(Map<String, String> fields, String name) {
        String value = fields.get(name);
        if (!"0".equals(value) && !"1".equals(value)) throw invalid(name + " 非法");
        return "1".equals(value);
    }

    private IllegalArgumentException invalid(String reason) {
        return new IllegalArgumentException(reason);
    }

    private void releaseLease(String token) {
        try {
            lease.release(token);
        } catch (RuntimeException exception) {
            log.warn("互动 Stream 消费租约释放失败", exception);
        }
    }

    private record ValidMessage(String id, String postKey, LocalDate date, boolean siteDirty) {
    }
}
