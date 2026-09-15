package com.myblog.application.port;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** 互动聚合脏通知队列端口；消息只描述受影响聚合维度，不承载访客身份。 */
public interface EngagementEventStream {

    /** 幂等创建 Stream 与持久化 Consumer Group。 */
    void ensureGroup();

    /** 读取尚未投递给当前消费组的新消息。 */
    List<Message> readNew(String consumer, int count, Duration blockTimeout);

    /** 接管超过最小空闲时间仍未确认的消息。 */
    List<Message> claimStale(String consumer, int count, Duration minIdle);

    /** 确认已经成功持久化或已转入死信的消息。 */
    void acknowledge(Collection<String> messageIds);

    /** 把无法解析的内部消息写入死信 Stream，保留原始字段用于排障。 */
    void deadLetter(Message message, String reason);

    /** 按消费组安全水位清理超过保留期且不再需要重放的历史消息。 */
    void trimAcknowledged(Duration retention);

    /** 返回运维监控使用的 Stream 长度与 Pending 数量。 */
    Status status();

    /** Stream 原始消息。 */
    record Message(String id, Map<String, String> fields) {
        public Message {
            fields = Map.copyOf(fields);
        }
    }

    /** Consumer Group 当前积压概览。 */
    record Status(long length, long pending) {
    }

}
