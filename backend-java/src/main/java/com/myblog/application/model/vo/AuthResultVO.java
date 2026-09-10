package com.myblog.application.model.vo;

/** 认证结果的组合出参：单会话令牌 + 当前用户的公开信息。 */
public record AuthResultVO(AccessTokenVO tokens, UserPublicVO user) {
}
