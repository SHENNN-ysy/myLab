package com.myblog.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Redis Stream 互动统计落库链路的批处理、重试与保留配置。 */
@ConfigurationProperties(prefix = "app.engagement-stream")
public record EngagementStreamProperties(
        /** 单批最大消息数：限制每轮 Pending 接管与新消息读取的规模。 */
        int batchSize,
        /** 读新消息（XREADGROUP BLOCK）的阻塞时长；超时返回空批，进入下一调度轮次。 */
        Duration blockTimeout,
        /** Pending 消息空闲超过该时长才被接管（XAUTOCLAIM），避免抢占仍在处理中的消息。 */
        Duration claimIdle,
        /** 已确认消息在 Stream 中的保留时长，超期后按安全水位裁剪。 */
        Duration retention,
        /** 单活租约 TTL：多实例部署时同一时刻只允许一个实例执行落库批次。 */
        Duration lockTtl,
        /** 消费调度间隔（poll-delay）：上一轮结束后等待该时长再消费下一批。 */
        Duration pollDelay,
        /** 历史清理调度间隔（trim-interval）：按 retention 定期裁剪 Stream。 */
        Duration trimInterval) {
}
