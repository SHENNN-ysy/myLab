package com.myblog.application.model.vo;

/** 认证结果的组合出参：令牌对 + 当前用户的公开信息。 */
public record AuthResultVO(TokenPairVO tokens, UserPublicVO user) {
}
