package com.myblog.application.model.command.user;

public final class UserCommands {

    private UserCommands() {
    }

    public record Create(
            String username,
            String role,
            String password) {
    }

    public record Update(
            String role,
            Boolean isActive,
            String password) {
    }
}
