package com.myblog.application.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 管理员账号的出参视图，供后台用户管理接口返回。
 */
@Schema(name = "AdminUserResponse", description = "管理员账号响应，不包含密码哈希")
public record UserOutVO(
        UUID id,
        String username,
        String role,
        // 账号是否启用，禁用的账号无法登录
        @JsonProperty("is_active") Boolean isActive,
        @JsonProperty("last_login_at") OffsetDateTime lastLoginAt,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        @JsonProperty("updated_at") OffsetDateTime updatedAt) {
}
