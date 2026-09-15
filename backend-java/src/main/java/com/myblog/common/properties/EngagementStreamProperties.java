package com.myblog.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Redis Stream 互动统计落库链路的批处理、重试与保留配置。 */
@ConfigurationProperties(prefix = "app.engagement-stream")
public record EngagementStreamProperties(
        boolean enabled,
        int batchSize,
        Duration blockTimeout,
        Duration claimIdle,
        Duration retention,
        Duration lockTtl,
        Duration pollDelay,
        Duration trimInterval) {
}
