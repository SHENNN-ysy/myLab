package com.myblog.application.model.command.user;

public final class UserCommands {

    private UserCommands() {
    }

    public record Create(
            String username,
            String email,
            String nickname,
            String role,
            String password) {
    }

    public record Update(
            String email,
            String nickname,
            String role,
            Boolean isActive,
            String avatarUrl,
            String website,
            String bio,
            String password) {
    }
}
