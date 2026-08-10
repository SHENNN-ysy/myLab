package com.myblog.application.model.vo;

import java.util.UUID;

/** 用户的公开信息视图，只保留可对外暴露的最小字段集。 */
public record UserPublicVO(UUID id, String username, String role) { }
