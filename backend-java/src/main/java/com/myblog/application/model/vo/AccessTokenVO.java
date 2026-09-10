package com.myblog.application.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 管理后台单会话令牌响应。 */
public record AccessTokenVO(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        // 滑动空闲超时时间，单位为秒
        @JsonProperty("expires_in") long expiresIn) {
}
