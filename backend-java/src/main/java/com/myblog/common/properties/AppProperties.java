package com.myblog.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 应用自定义配置聚合体，从 {@code app.*} 命名空间绑定。
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String jwtSecret, // JWT 签名密钥
        long accessExpireMinutes, // 访问令牌有效期（分钟）
        long refreshExpireDays, // 刷新令牌有效期（天）
        String corsOrigins, // 允许的跨域来源，逗号分隔
        int rateLimitPerMinute, // 全局限流阈值（次/分钟）
        int loginRateLimitPerMinute, // 登录接口限流阈值（次/分钟）
        String ossEndpoint, // OSS 访问域名
        String ossAccessKeyId,
        String ossAccessKeySecret,
        String ossBucket,
        String ossCdnDomain, // OSS 文件对外访问的 CDN 域名
        String ossObjectPrefix, // OSS 对象键的统一前缀
        int ossMaxFileSizeMb, // 上传文件大小上限（MB）
        String initialAdminUsername, // 首次启动时创建的初始管理员账号
        String initialAdminPassword) {
}
