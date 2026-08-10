package com.myblog.infrastructure.security;

import com.myblog.common.properties.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * 纯 JWT 签名/解析工具类：不依赖 Redis，也不做用户查询，
 * 黑名单等有状态校验由 {@link JwtService} 负责。
 */
public final class JwtUtil {

    private JwtUtil() {
    }

    /** 由配置中的密钥构建 HMAC-SHA 签名密钥 */
    private static SecretKey key(AppProperties props) {
        return Keys.hmacShaKeyFor(props.jwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 签发 JWT。
     *
     * @param subject 令牌主体（用户 id）
     * @param type    令牌类型（access / refresh），解析时会校验
     * @param ttl     有效期
     */
    public static String issue(AppProperties props, String subject, String role,
                               String type, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .claim("type", type)
                .id(UUID.randomUUID().toString().replace("-", ""))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key(props))
                .compact();
    }

    /**
     * 验签并解析 JWT 负载。
     *
     * @throws ExpiredJwtException 令牌已过期
     * @throws io.jsonwebtoken.JwtException 签名非法或格式错误
     */
    public static Claims parse(AppProperties props, String token) {
        return Jwts.parser()
                .verifyWith(key(props))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** 判断异常是否由令牌过期引起，供调用方转换为业务异常 */
    public static boolean isExpired(Throwable error) {
        return error instanceof ExpiredJwtException;
    }
}
