package com.myblog.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Redis 管理会话配置。 */
@ConfigurationProperties(prefix = "app.session")
public record SessionProperties(Duration idleTimeout) {

    public SessionProperties {
        if (idleTimeout == null || idleTimeout.isZero() || idleTimeout.isNegative()) {
            throw new IllegalArgumentException("app.session.idle-timeout 必须大于 0");
        }
    }

    /** 返回 Redis EXPIRE 使用的空闲超时秒数。 */
    public long idleTimeoutSeconds() {
        return idleTimeout.toSeconds();
    }
}
