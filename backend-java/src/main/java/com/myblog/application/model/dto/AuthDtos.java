package com.myblog.application.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record Login(
            @Size(min = 3, max = 64) String username,
            @Size(min = 8, max = 64) String password) {
    }

    public record Refresh(@NotBlank String refreshToken) {
    }

    public record PasswordChange(
            @Size(min = 8, max = 64) String oldPassword,
            @Size(min = 8, max = 64) String newPassword) {
    }
}
