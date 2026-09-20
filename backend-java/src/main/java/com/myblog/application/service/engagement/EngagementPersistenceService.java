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

/**
 * Redis Stream 互动脏通知消费服务。
 * Stream 消息只表示哪些聚合维度发生变化，不携带计数增量；消费者合并同批消息后，
 * 从 Redis 读取最新绝对值并覆盖 PostgreSQL 快照，因此消息重放不会重复累加。
 * 只有数据库事务成功后才确认消息，保证处理失败时消息仍可由 Pending 重试。
 */
@Slf4j
@Service
public class EngagementPersistenceService {
    /** 当前消费者理解的内部事件类型；未知类型进入死信，避免阻塞整个消费组。 */
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
        stream.ensureGroup();
    }

    /**
     * 执行一个单活消费批次：优先重试 Pending，再读取新消息；PG 提交成功后才 XACK。
     *
     * @return 本轮成功确认的有效消息数量，未获得租约或没有消息时返回 0
     */
    public int synchronize(String consumer) {
        // 多实例部署时只允许一个实例执行落库批次，避免不同批次的绝对值快照交叉覆盖。
        String token = lease.tryAcquire(properties.lockTtl());
        if (token == null) return 0;
        try {
            stream.ensureGroup();
            // 先接管超时 Pending，确保实例异常退出后遗留的消息最终能够继续处理。
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
        stream.trimAcknowledged(properties.retention());
    }

    /** 提供给调度适配器更新 Micrometer Gauge 的积压概览。 */
    public EngagementEventStream.Status status() {
        return stream.status();
    }

    /**
     * 校验并归并一个消息批次，然后按受影响维度读取 Redis 最新值落库。
     * 非法消息先转入死信再确认；有效消息必须等 PostgreSQL 快照提交成功后才确认。
     */
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

        // 同批事件按文章和日期去重，避免热点内容产生大量重复 Redis 查询与 PG upsert。
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

    /** 解析并校验内部消息，只保留后续读取快照所需的聚合维度。 */
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

    /** 校验事件类型与脏维度组合，拒绝可能导致部分聚合遗漏的畸形消息。 */
    private void validateDimensions(String eventType, boolean postDirty, boolean siteDirty, boolean dailyDirty) {
        boolean valid = switch (eventType) {
            case "VIEW", "LIKE" -> postDirty && siteDirty && dailyDirty;
            case "UNLIKE" -> postDirty && siteDirty && !dailyDirty;
            case "VISIT" -> !postDirty && siteDirty && dailyDirty;
            default -> false;
        };
        if (!valid) throw invalid("聚合维度与 event_type 不匹配");
    }

    /** 读取只允许为 0 或 1 的 Stream 标志字段。 */
    private boolean flag(Map<String, String> fields, String name) {
        String value = fields.get(name);
        if (!"0".equals(value) && !"1".equals(value)) throw invalid(name + " 非法");
        return "1".equals(value);
    }

    private IllegalArgumentException invalid(String reason) {
        return new IllegalArgumentException(reason);
    }

    /**
     * 尽力释放单活租约；租约带 TTL，即使释放失败也不覆盖本轮持久化结果，
     * 后续实例可在租约自然过期后继续消费。
     */
    private void releaseLease(String token) {
        try {
            lease.release(token);
        } catch (RuntimeException exception) {
            log.warn("互动 Stream 消费租约释放失败", exception);
        }
    }

    /** 已通过协议校验、可安全参与本批维度归并的消息。 */
    private record ValidMessage(String id, String postKey, LocalDate date, boolean siteDirty) {
    }
}
