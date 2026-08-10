package com.myblog.infrastructure.security;

import com.myblog.application.model.entity.User;
import com.myblog.common.exception.TokenExpiredException;
import com.myblog.common.exception.TokenRevokedException;
import com.myblog.common.exception.UnauthorizedException;
import com.myblog.common.enumeration.ErrorCode;
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
 * JWT 服务：负责令牌的签发、解析与吊销。纯签名/验签逻辑在 {@link JwtUtil}，
 * 本类在其上叠加基于 Redis 的黑名单校验，实现应用层 {@link TokenService} 端口。
 */
@Service
public class JwtService implements TokenService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:"; // Redis 吊销黑名单键前缀（键为令牌 jti）

    private final AppProperties props; // JWT 密钥与有效期等配置
    private final StringRedisTemplate redis; // 黑名单读写

    public JwtService(AppProperties props, StringRedisTemplate redis) {
        this.props = props;
        this.redis = redis;
    }

    /** 为用户签发访问/刷新令牌对，有效期取自配置 */
    @Override
    public TokenPairVO pair(User user) {
        String access = JwtUtil.issue(props, user.getId().toString(), user.getRole(), "access",
                Duration.ofMinutes(props.accessExpireMinutes()));
        String refresh = JwtUtil.issue(props, user.getId().toString(), user.getRole(), "refresh",
                Duration.ofDays(props.refreshExpireDays()));
        return new TokenPairVO(access, refresh, "bearer", props.accessExpireMinutes() * 60);
    }

    /**
     * 解析并校验令牌，返回其中的用户身份声明。
     *
     * @param expectedType 期望的令牌类型（access / refresh），类型不符视为无效
     */
    @Override
    public TokenClaims parse(String token, String expectedType) {
        Claims claims = parseClaims(token, expectedType);
        return new TokenClaims(java.util.UUID.fromString(claims.getSubject()),
                claims.get("role", String.class));
    }

    /** 解析并做完整校验：过期、无效、类型错误、已吊销分别抛出对应业务异常 */
    private Claims parseClaims(String token, String expectedType) {
        Claims claims;
        try {
            claims = JwtUtil.parse(props, token);
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException();
        } catch (Exception e) {
            throw new UnauthorizedException(ErrorCode.AUTHENTICATION_FAILED, "访问令牌无效");
        }
        String actualType = claims.get("type", String.class);
        if (!expectedType.equals(actualType)) {
            throw new UnauthorizedException(ErrorCode.AUTHENTICATION_FAILED, "令牌类型错误");
        }
        if (Boolean.TRUE.equals(redis.hasKey(BLACKLIST_PREFIX + claims.getId()))) {
            throw new TokenRevokedException();
        }
        return claims;
    }

    /** 吊销访问令牌：把令牌 jti 写入 Redis 黑名单，TTL 取令牌剩余有效期 */
    @Override
    public void revoke(String token) {
        Claims claims = parseClaims(token, "access");
        long ttl = Math.max(1L,
                claims.getExpiration().toInstant().getEpochSecond() - Instant.now().getEpochSecond());
        redis.opsForValue().set(BLACKLIST_PREFIX + claims.getId(), "1", ttl, TimeUnit.SECONDS);
    }
}
