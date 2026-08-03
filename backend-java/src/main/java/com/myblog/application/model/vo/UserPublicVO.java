package com.myblog.application.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/** Public-facing user projection returned by auth and self endpoints. */
public record UserPublicVO(
        UUID id,
        String username,
        String email,
        String nickname,
        String role,
        @JsonProperty("avatar_url") String avatarUrl) {
}
