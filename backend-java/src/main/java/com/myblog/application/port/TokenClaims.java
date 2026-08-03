package com.myblog.application.port;

import java.util.UUID;

public record TokenClaims(UUID userId, String role) {
}
