package com.myblog.application.model.vo;

/** Combined auth response (tokens + public user). */
public record AuthResultVO(TokenPairVO tokens, UserPublicVO user) {
}
