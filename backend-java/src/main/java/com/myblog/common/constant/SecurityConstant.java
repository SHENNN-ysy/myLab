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
    public static final String OPENAPI_JSON = "/v3/api-docs/**";
    public static final String OPENAPI_YAML = "/v3/api-docs.yaml";
    public static final String SWAGGER_UI = "/swagger-ui.html";
    public static final String SWAGGER_UI_RESOURCES = "/swagger-ui/**";

    public static final List<String> PUBLIC_GET_PREFIXES = List.of("/api/v1/public");

    private SecurityConstant() {
    }
}
