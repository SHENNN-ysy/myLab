package com.myblog.application.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

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
}
