package com.myblog.infrastructure.engagement;

import com.myblog.application.port.EngagementEventStream;
import com.myblog.common.constant.RedisKeyPrefix;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamInfo;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Spring Data Redis Stream 适配器：Consumer Group、Pending 接管、死信与安全清理。 */
@Component
public class RedisEngagementEventStream implements EngagementEventStream {
    public static final String STREAM_KEY = RedisKeyPrefix.BLOG + "stream:engagement:v1";
    public static final String GROUP_NAME = "engagement-persistence-v1";
    public static final String DEAD_LETTER_KEY = RedisKeyPrefix.BLOG + "stream:engagement:dlq:v1";
    // Stream 起始 ID；消费组从未投递过消息时 lastDeliveredId 即为此值
    private static final String ZERO_ID = "0-0";
    private static final int STREAM_ID_PARTS = 2;

    private final StringRedisTemplate redis;

    public RedisEngagementEventStream(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** 使用 MKSTREAM 从 0-0 创建消费组；BUSYGROUP 表示已存在，按幂等成功处理。 */
    @Override
    public void ensureGroup() {
        try {
            redis.execute((RedisCallback<String>) connection -> connection.streamCommands()
                    .xGroupCreate(bytes(STREAM_KEY), GROUP_NAME, ReadOffset.from(ZERO_ID), true));
        } catch (DataAccessException exception) {
            if (!containsMessage(exception, "BUSYGROUP")) throw exception;
        }
    }

    /**
     * 以消费组 lastConsumed（">"）偏移读取从未投递的新消息，长轮询至多等待 blockTimeout。
     *
     * @return 读到的新消息；超时仍无消息时返回空列表
     */
    @Override
    public List<Message> readNew(String consumer, int count, Duration blockTimeout) {
        StreamReadOptions options = StreamReadOptions.empty().count(count).block(blockTimeout);
        List<MapRecord<String, String, String>> records = operations().read(
                Consumer.from(GROUP_NAME, consumer), options,
                StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()));
        return messages(records);
    }

    /** Spring Data Redis 通过 XPENDING + XCLAIM 接管超时消息，语义等价于本场景的 XAUTOCLAIM。 */
    @Override
    public List<Message> claimStale(String consumer, int count, Duration minIdle) {
        PendingMessages pending = operations().pending(STREAM_KEY, GROUP_NAME, Range.unbounded(), count);
        List<RecordId> staleIds = new ArrayList<>();
        for (PendingMessage message : pending) {
            if (message.getElapsedTimeSinceLastDelivery().compareTo(minIdle) >= 0) {
                staleIds.add(message.getId());
            }
        }
        if (staleIds.isEmpty()) return List.of();
        List<MapRecord<String, String, String>> claimed = operations().claim(
                STREAM_KEY, GROUP_NAME, consumer, minIdle, staleIds.toArray(RecordId[]::new));
        return messages(claimed);
    }

    /**
     * XACK 批量确认：仅在落库成功或已转死信后调用，确认后消息离开 Pending 不再重投，
     * 因此落库失败时绝不能确认（配合 claimStale 实现至少一次处理）。
     */
    @Override
    public void acknowledge(Collection<String> messageIds) {
        if (!messageIds.isEmpty()) {
            operations().acknowledge(STREAM_KEY, GROUP_NAME, messageIds.toArray(String[]::new));
        }
    }

    /** 把消息拷贝进死信 Stream（保留原 ID、原因、失败时间与原始字段供排障）；本方法不确认原消息，调用方需另行 acknowledge。 */
    @Override
    public void deadLetter(Message message, String reason) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("original_id", message.id());
        fields.put("reason", reason);
        fields.put("failed_at", Instant.now().toString());
        fields.put("payload", message.fields().toString());
        operations().add(DEAD_LETTER_KEY, fields);
    }

    /** 只清理保留期、最后投递位置和最老 Pending 三者共同允许的历史区间。 */
    @Override
    public void trimAcknowledged(Duration retention) {
        StreamInfo.XInfoGroup group = operations().groups(STREAM_KEY).stream()
                .filter(value -> GROUP_NAME.equals(value.groupName()))
                .findFirst().orElse(null);
        String lastDeliveredId = group == null ? null : group.lastDeliveredId();
        if (lastDeliveredId == null || ZERO_ID.equals(lastDeliveredId)) return;

        String threshold = minimumId(cutoffId(retention), lastDeliveredId);
        PendingMessagesSummary pending = operations().pending(STREAM_KEY, GROUP_NAME);
        if (pending != null && pending.getTotalPendingMessages() > 0) {
            threshold = minimumId(threshold, pending.minMessageId());
        }
        if (ZERO_ID.equals(threshold)) return;
        String trimId = threshold;
        redis.execute((RedisCallback<Object>) connection -> connection.execute(
                "XTRIM", bytes(STREAM_KEY), bytes("MINID"), bytes(trimId)));
    }

    /** 返回 Stream 总长度与本消费组 Pending 数量；Stream 不存在时长度按 0 计。 */
    @Override
    public Status status() {
        Long length = operations().size(STREAM_KEY);
        PendingMessagesSummary pending = operations().pending(STREAM_KEY, GROUP_NAME);
        return new Status(length == null ? 0 : length,
                pending == null ? 0 : pending.getTotalPendingMessages());
    }

    @SuppressWarnings("unchecked")
    private StreamOperations<String, String, String> operations() {
        return redis.opsForStream();
    }

    private List<Message> messages(List<MapRecord<String, String, String>> records) {
        if (records == null || records.isEmpty()) return List.of();
        return records.stream().map(record -> {
            Map<String, String> fields = new LinkedHashMap<>();
            fields.putAll(record.getValue());
            return new Message(record.getId().getValue(), fields);
        }).toList();
    }

    /** 按保留期换算 Stream ID 阈值：Stream ID 首段即毫秒时间戳，早于该时间的消息已超出保留期。 */
    private static String cutoffId(Duration retention) {
        long timestamp = Instant.now().minus(retention).toEpochMilli();
        return Math.max(0, timestamp) + "-0";
    }

    private static String minimumId(String left, String right) {
        return compareIds(left, right) <= 0 ? left : right;
    }

    /** Stream ID 各段位数不固定，字典序不可靠，必须拆成 时间戳/序号 两段按数值比较。 */
    private static int compareIds(String left, String right) {
        String[] leftParts = left.split("-", STREAM_ID_PARTS);
        String[] rightParts = right.split("-", STREAM_ID_PARTS);
        int timestamp = Long.compare(Long.parseLong(leftParts[0]), Long.parseLong(rightParts[0]));
        if (timestamp != 0) return timestamp;
        return Long.compare(Long.parseLong(leftParts[1]), Long.parseLong(rightParts[1]));
    }

    private static boolean containsMessage(Throwable throwable, String expected) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (current.getMessage() != null && current.getMessage().contains(expected)) return true;
        }
        return false;
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
