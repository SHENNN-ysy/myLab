package com.myblog.application.port;

import java.util.UUID;

/**
 * 令牌声明：解析令牌后得到的用户身份信息。
 *
 * @param userId 用户 ID
 * @param role 用户角色
 */
public record TokenClaims(UUID userId, String role) {
}
