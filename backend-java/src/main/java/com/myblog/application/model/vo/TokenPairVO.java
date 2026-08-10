package com.myblog.application.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 认证成功后返回的令牌对（访问令牌 + 刷新令牌）。 */
public record TokenPairVO(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("token_type") String tokenType,
        // 访问令牌的有效期，单位：秒
        @JsonProperty("expires_in") long expiresIn) {
}
