package com.myblog.common.constant;

import java.util.List;

/**
 * Path prefixes that bypass authentication and authorization.
 */
public final class SecurityConstant {

    public static final String HEALTH_PREFIX = "/actuator/health";
    public static final String HEALTH_API = "/api/v1/health";
    public static final String AUTH_LOGIN = "/api/v1/auth/login";
    public static final String AUTH_REFRESH = "/api/v1/auth/refresh";
    public static final String VISIT_TRACK = "/api/v1/visits/logs/track";

    public static final List<String> PUBLIC_GET_PREFIXES = List.of(
            "/api/v1/skills",
            "/api/v1/projects",
            "/api/v1/footprints",
            "/api/v1/about-bubbles"
    );

    private SecurityConstant() {
    }
}
