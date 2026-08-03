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
 * Pure JWT signing / parsing utility. Does not depend on Redis or user lookup.
 */
public final class JwtUtil {

    private JwtUtil() {
    }

    private static SecretKey key(AppProperties props) {
        return Keys.hmacShaKeyFor(props.jwtSecret().getBytes(StandardCharsets.UTF_8));
    }

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

    public static Claims parse(AppProperties props, String token) {
        return Jwts.parser()
                .verifyWith(key(props))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Returns true if the token is expired. Used by callers that want to translate exceptions. */
    public static boolean isExpired(Throwable error) {
        return error instanceof ExpiredJwtException;
    }
}
