package com.myblog.application.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Access + refresh token pair returned after successful authentication. */
public record TokenPairVO(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn) {
}
