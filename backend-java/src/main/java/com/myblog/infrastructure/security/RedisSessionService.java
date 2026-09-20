package com.myblog.infrastructure.security;

import com.myblog.application.model.entity.User;
import com.myblog.application.model.vo.AccessTokenVO;
import com.myblog.application.port.SessionIdentity;
import com.myblog.application.port.SessionService;
import com.myblog.common.constant.RedisKeyPrefix;
import com.myblog.common.enumeration.ErrorCode;
import com.myblog.common.exception.AuthenticationUnavailableException;
import com.myblog.common.exception.UnauthorizedException;
import com.myblog.common.properties.SessionProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Redis 会话实现：客户端持有 UUID，Redis 仅保存 SHA-256 摘要和最小用户身份。
 */
@Slf4j
@Service
public class RedisSessionService implements SessionService {

    private static final String SESSION_PREFIX = RedisKeyPrefix.AUTH + "session:";
    // 用户会话反向索引 ZSet（member=令牌摘要，score=过期毫秒），支撑按用户批量吊销
    private static final String USER_SESSIONS_PREFIX = RedisKeyPrefix.AUTH + "user-sessions:";
    private static final long MILLIS_PER_SECOND = 1000L;

    /** 签发脚本：写会话 Hash 并在用户索引登记（顺带清除已过期项，防止索引无限增长），两步原子完成。 */
    private static final DefaultRedisScript<Long> ISSUE_SCRIPT = new DefaultRedisScript<>("""
            redis.call('HSET', KEYS[1],
                'user_id', ARGV[1],
                'created_at', ARGV[2],
                'last_seen_at', ARGV[2])
            redis.call('EXPIRE', KEYS[1], ARGV[3])
            redis.call('ZREMRANGEBYSCORE', KEYS[2], '-inf', ARGV[2])
            redis.call('ZADD', KEYS[2], ARGV[4], ARGV[5])
            redis.call('EXPIRE', KEYS[2], ARGV[3])
            return 1
            """, Long.class);

    /** 认证脚本：命中会话则滑动续期（刷新 last_seen_at 与 TTL）并同步用户索引，未命中返回 nil。 */
    private static final DefaultRedisScript<String> AUTHENTICATE_SCRIPT = new DefaultRedisScript<>("""
            local userId = redis.call('HGET', KEYS[1], 'user_id')
            if not userId then
                return nil
            end
            redis.call('HSET', KEYS[1], 'last_seen_at', ARGV[1])
            redis.call('EXPIRE', KEYS[1], ARGV[2])
            local indexKey = ARGV[5] .. userId
            redis.call('ZREMRANGEBYSCORE', indexKey, '-inf', ARGV[1])
            redis.call('ZADD', indexKey, ARGV[3], ARGV[4])
            redis.call('EXPIRE', indexKey, ARGV[2])
            return userId
            """, String.class);

    /** 注销脚本：删会话 Hash 并从用户索引移除；索引清空时连索引键一并删除，会话不存在返回 0 保持幂等。 */
    private static final DefaultRedisScript<Long> REVOKE_SCRIPT = new DefaultRedisScript<>("""
            local userId = redis.call('HGET', KEYS[1], 'user_id')
            if not userId then
                return 0
            end
            redis.call('DEL', KEYS[1])
            local indexKey = ARGV[2] .. userId
            redis.call('ZREM', indexKey, ARGV[1])
            if redis.call('ZCARD', indexKey) == 0 then
                redis.call('DEL', indexKey)
            end
            return 1
            """, Long.class);

    /** 吊销全部会话脚本：按用户索引逐个删除会话 Hash，最后删除索引本身，返回删除的会话数。 */
    private static final DefaultRedisScript<Long> REVOKE_ALL_SCRIPT = new DefaultRedisScript<>("""
            local tokens = redis.call('ZRANGE', KEYS[1], 0, -1)
            for _, digest in ipairs(tokens) do
                redis.call('DEL', ARGV[1] .. digest)
            end
            redis.call('DEL', KEYS[1])
            return #tokens
            """, Long.class);

    private final StringRedisTemplate redis;
    private final long idleTimeoutSeconds;

    public RedisSessionService(StringRedisTemplate redis, SessionProperties properties) {
        this.redis = redis;
        this.idleTimeoutSeconds = properties.idleTimeoutSeconds();
    }

    @Override
    public AccessTokenVO issue(User user) {
        String token = UUID.randomUUID().toString();
        String digest = digest(token);
        long now = Instant.now().toEpochMilli();
        long expiresAt = now + idleTimeoutSeconds * MILLIS_PER_SECOND;
        execute(ISSUE_SCRIPT,
                List.of(sessionKey(digest), userSessionsKey(user.getId())),
                user.getId().toString(), Long.toString(now), Long.toString(idleTimeoutSeconds),
                Long.toString(expiresAt), digest);
        return new AccessTokenVO(token, "bearer", idleTimeoutSeconds);
    }

    @Override
    public SessionIdentity authenticate(String token) {
        String digest = digest(token);
        long now = Instant.now().toEpochMilli();
        long expiresAt = now + idleTimeoutSeconds * MILLIS_PER_SECOND;
        String userId = execute(AUTHENTICATE_SCRIPT, List.of(sessionKey(digest)),
                Long.toString(now), Long.toString(idleTimeoutSeconds), Long.toString(expiresAt),
                digest, USER_SESSIONS_PREFIX);
        if (userId == null) {
            throw new UnauthorizedException(ErrorCode.AUTHENTICATION_FAILED, "会话不存在或已过期");
        }
        try {
            return new SessionIdentity(UUID.fromString(userId));
        } catch (IllegalArgumentException exception) {
            // user_id 由本服务写入，出现非法值说明 Redis 数据已损坏，降级为认证失败而非系统错误
            log.error("Redis 会话包含非法 user_id，session_digest={}", digest);
            throw new UnauthorizedException(ErrorCode.AUTHENTICATION_FAILED, "会话身份无效");
        }
    }

    @Override
    public void revoke(String token) {
        String digest = digest(token);
        execute(REVOKE_SCRIPT, List.of(sessionKey(digest)), digest, USER_SESSIONS_PREFIX);
    }

    @Override
    public void revokeAll(UUID userId) {
        execute(REVOKE_ALL_SCRIPT, List.of(userSessionsKey(userId)), SESSION_PREFIX);
    }

    /** 校验 UUID 规范格式后计算摘要，避免 Redis 中保存可直接复用的 Bearer Token。 */
    private String digest(String token) {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException(ErrorCode.AUTHENTICATION_FAILED, "会话令牌缺失");
        }
        final String canonical;
        try {
            canonical = UUID.fromString(token).toString();
        } catch (IllegalArgumentException exception) {
            throw new UnauthorizedException(ErrorCode.AUTHENTICATION_FAILED, "会话令牌格式错误");
        }
        if (!canonical.equals(token)) {
            throw new UnauthorizedException(ErrorCode.AUTHENTICATION_FAILED, "会话令牌格式错误");
        }
        try {
            byte[] value = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(value);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JVM 不支持 SHA-256", exception);
        }
    }

    /** Redis 故障统一转换为 503，避免前端把基础设施故障误判为登录过期。 */
    private <T> T execute(DefaultRedisScript<T> script, List<String> keys, String... args) {
        try {
            return redis.execute(script, keys, (Object[]) args);
        } catch (DataAccessException exception) {
            log.error("Redis 会话操作失败", exception);
            throw new AuthenticationUnavailableException();
        }
    }

    private String sessionKey(String digest) {
        return SESSION_PREFIX + digest;
    }

    private String userSessionsKey(UUID userId) {
        return USER_SESSIONS_PREFIX + userId;
    }
}
