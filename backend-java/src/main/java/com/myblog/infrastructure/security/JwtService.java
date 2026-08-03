package com.myblog.infrastructure.security;

import com.myblog.application.model.entity.User;
import com.myblog.common.exception.TokenExpiredException;
import com.myblog.common.exception.TokenRevokedException;
import com.myblog.common.exception.UnauthorizedException;
import com.myblog.application.port.TokenClaims;
import com.myblog.application.port.TokenService;
import com.myblog.common.properties.AppProperties;
import com.myblog.application.model.vo.TokenPairVO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * JWT service: issues, parses, and revokes tokens. Pure signing lives in {@link JwtUtil};
 * this class adds Redis-backed blacklist lookups.
 */
@Service
public class JwtService implements TokenService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final AppProperties props;
    private final StringRedisTemplate redis;

    public JwtService(AppProperties props, StringRedisTemplate redis) {
        this.props = props;
        this.redis = redis;
    }

    @Override
    public TokenPairVO pair(User user) {
        String access = JwtUtil.issue(props, user.getId().toString(), user.getRole(), "access",
                Duration.ofMinutes(props.accessExpireMinutes()));
        String refresh = JwtUtil.issue(props, user.getId().toString(), user.getRole(), "refresh",
                Duration.ofDays(props.refreshExpireDays()));
        return new TokenPairVO(access, refresh, "bearer", props.accessExpireMinutes() * 60);
    }

    @Override
    public TokenClaims parse(String token, String expectedType) {
        Claims claims = parseClaims(token, expectedType);
        return new TokenClaims(java.util.UUID.fromString(claims.getSubject()),
                claims.get("role", String.class));
    }

    private Claims parseClaims(String token, String expectedType) {
        Claims claims;
        try {
            claims = JwtUtil.parse(props, token);
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException();
        } catch (Exception e) {
            throw new UnauthorizedException("Access token expired or invalid");
        }
        String actualType = claims.get("type", String.class);
        if (!expectedType.equals(actualType)) {
            throw new UnauthorizedException("Invalid token type");
        }
        if (Boolean.TRUE.equals(redis.hasKey(BLACKLIST_PREFIX + claims.getId()))) {
            throw new TokenRevokedException();
        }
        return claims;
    }

    @Override
    public void revoke(String token) {
        Claims claims = parseClaims(token, "access");
        long ttl = Math.max(1L,
                claims.getExpiration().toInstant().getEpochSecond() - Instant.now().getEpochSecond());
        redis.opsForValue().set(BLACKLIST_PREFIX + claims.getId(), "1", ttl, TimeUnit.SECONDS);
    }
}
