package com.myblog.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Aggregated application properties. Bound from the {@code app.*} configuration namespace.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String jwtSecret,
        long accessExpireMinutes,
        long refreshExpireDays,
        String corsOrigins,
        int rateLimitPerMinute,
        int loginRateLimitPerMinute,
        String ossEndpoint,
        String ossAccessKeyId,
        String ossAccessKeySecret,
        String ossBucket,
        String ossCdnDomain,
        String ossObjectPrefix,
        int ossMaxFileSizeMb,
        String initialAdminUsername,
        String initialAdminPassword) {
}
