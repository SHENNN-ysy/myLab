package com.myblog.infrastructure.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myblog.application.port.SessionService;
import com.myblog.common.exception.AuthenticationUnavailableException;
import com.myblog.infrastructure.persistence.mapper.user.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 会话过滤器的故障隔离与公开接口跳过规则测试。 */
@ExtendWith(MockitoExtension.class)
class SessionAuthenticationFilterTest {

    @Mock SessionService sessions;
    @Mock UserMapper users;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /** 公开接口即使带了 Authorization 头也不查会话，直接放行且不触碰 Redis。 */
    @Test
    void publicApiNeverTouchesAdminSessionRedis() throws Exception {
        SessionAuthenticationFilter filter = filter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/public/content");
        request.addHeader("Authorization", "Bearer ignored-on-public-api");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
        verify(sessions, never()).authenticate(org.mockito.ArgumentMatchers.anyString());
    }

    /** 会话服务（Redis）故障时返回 503 与专属错误码，而非当作未认证的 401。 */
    @Test
    void redisFailureReturns503WithoutClearingItAsUnauthorized() throws Exception {
        String token = "7ae523a4-7df8-49b5-ac44-4f02f46e65f4";
        when(sessions.authenticate(token)).thenThrow(new AuthenticationUnavailableException());
        SessionAuthenticationFilter filter = filter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        JsonNode body = new ObjectMapper().readTree(response.getContentAsByteArray());
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(body.path("code").asInt()).isEqualTo(10014);
        assertThat(body.path("message").asText()).isEqualTo("认证服务暂不可用");
    }

    private SessionAuthenticationFilter filter() {
        return new SessionAuthenticationFilter(sessions, users, new ObjectMapper());
    }
}
