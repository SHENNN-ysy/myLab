package com.myblog.application.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 认证相关的请求 DTO 集合，覆盖登录、刷新令牌与当前账号信息修改场景。
 * <p>
 * 各字段的校验规则与文档说明见字段上的注解。
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    @Schema(name = "LoginRequest", description = "管理员登录请求")
    public record Login(
            @NotBlank @Size(min = 3, max = 64)
            @Schema(description = "管理员用户名", example = "admin") String username,
            @NotBlank @Size(min = 8, max = 64)
            @Schema(description = "管理员密码", example = "Admin@123456", format = "password") String password) {
    }

    @Schema(name = "RefreshTokenRequest", description = "刷新访问令牌请求")
    public record Refresh(
            @NotBlank @Schema(description = "刷新令牌", example = "eyJhbGciOiJIUzI1NiJ9...") String refreshToken) {
    }

    @Schema(name = "PasswordChangeRequest", description = "修改密码请求")
    public record PasswordChange(
            @NotBlank @Size(min = 8, max = 64)
            @Schema(description = "原密码", format = "password") String oldPassword,
            @NotBlank @Size(min = 8, max = 64)
            @Schema(description = "新密码", format = "password") String newPassword) {
    }

    @Schema(name = "AccountUpdateRequest", description = "修改当前账号信息请求")
    public record AccountUpdate(
            @NotBlank @Size(min = 3, max = 64)
            @Schema(description = "新账号名称", example = "admin") String username,
            @NotBlank @Size(min = 8, max = 64)
            @Schema(description = "当前密码", format = "password") String oldPassword,
            @Size(min = 8, max = 64)
            @Schema(description = "新密码；不修改时省略", format = "password") String newPassword) {
    }
}
