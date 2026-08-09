package com.myblog.application.model.vo;

import java.util.UUID;

public record UserPublicVO(UUID id, String username, String role) { }
