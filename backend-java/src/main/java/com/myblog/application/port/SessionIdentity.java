package com.myblog.application.port;

import java.util.UUID;

/** Redis 会话中保存的最小身份，只信任用户 ID，权限信息始终从数据库读取。 */
public record SessionIdentity(UUID userId) {
}
