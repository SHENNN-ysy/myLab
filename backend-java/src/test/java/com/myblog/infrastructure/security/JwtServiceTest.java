package com.myblog.infrastructure.security;

import com.myblog.common.exception.TokenRevokedException;
import com.myblog.common.exception.UnauthorizedException;
import com.myblog.common.properties.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JwtService 吊销链路测试：重点覆盖 refresh 令牌可吊销（退出登录生效的前提）。
 */
@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private static final AppProperties PROPS = new AppProperties(
            "test-jwt-secret-key-that-is-long-enough-for-hmac-sha-256", 30, 7,
            "", 0, 0, "", "", "", "", "", "", 0, "", "");

    @Mock
    StringRedisTemplate redis;
    @Mock
    ValueOperations<String, String> valueOps;

    private JwtService service;

    @BeforeEach
    void setUp() {
        service = new JwtService(PROPS, redis);
    }

    private String issue(String type) {
        return JwtUtil.issue(PROPS, UUID.randomUUID().toString(), "admin", type, Duration.ofMinutes(10));
    }

    @Test
    void revokeAccessTokenBlacklistsJti() {
        String token = issue("access");
        when(redis.hasKey(anyString())).thenReturn(false);
        when(redis.opsForValue()).thenReturn(valueOps);

        service.revoke(token);

        verify(valueOps).set(startsWith("jwt:blacklist:"), eq("1"), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    void revokeRefreshTokenIsAcceptedAndBlocksFurtherParsing() {
        String token = issue("refresh");
        when(redis.hasKey(anyString())).thenReturn(false);
        when(redis.opsForValue()).thenReturn(valueOps);

        // refresh 令牌可吊销，不再抛"令牌类型错误"
        assertDoesNotThrow(() -> service.revoke(token));

        // 黑名单命中后，该 refresh 令牌无法再用于换发新令牌
        when(redis.hasKey(anyString())).thenReturn(true);
        assertThrows(TokenRevokedException.class, () -> service.parse(token, "refresh"));
    }

    @Test
    void revokeRejectsUnknownTokenType() {
        String token = issue("other");
        when(redis.hasKey(anyString())).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> service.revoke(token));
        verify(redis, never()).opsForValue();
    }
}
