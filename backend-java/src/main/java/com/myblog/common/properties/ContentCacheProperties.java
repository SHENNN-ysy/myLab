package com.myblog.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 博客前台公开内容缓存配置。
 */
@ConfigurationProperties(prefix = "app.content-cache")
public record ContentCacheProperties(
        boolean enabled,
        Duration ttl,
        Duration lockTtl,
        Duration waitTimeout) {
}
