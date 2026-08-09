package com.myblog.application.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(name = "AdminUserResponse", description = "管理员账号响应，不包含密码哈希")
public record UserOutVO(
        UUID id,
        String username,
        String role,
        @JsonProperty("is_active") Boolean isActive,
        @JsonProperty("last_login_at") OffsetDateTime lastLoginAt,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        @JsonProperty("updated_at") OffsetDateTime updatedAt) {
}
