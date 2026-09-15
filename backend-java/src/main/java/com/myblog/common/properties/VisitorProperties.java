package com.myblog.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** 匿名访客凭证的滑动有效期与 Cookie 安全配置。 */
@ConfigurationProperties(prefix = "app")
public record VisitorProperties(
        Duration visitorIdentityTtl,
        boolean visitorCookieSecure) {
    public VisitorProperties {
        if (visitorIdentityTtl == null || visitorIdentityTtl.isZero() || visitorIdentityTtl.isNegative()) {
            throw new IllegalArgumentException("app.visitor-identity-ttl 必须大于 0");
        }
    }
}
