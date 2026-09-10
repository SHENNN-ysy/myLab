package com.myblog;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** 管理后台 Redis 会话认证的端到端集成测试。 */
class AuthApiIT extends AbstractApiIntegrationTest {

    private static final String ME_URL = "/api/v1/auth/me";
    private static final String LOGOUT_URL = "/api/v1/auth/logout";
    private static final String ACCOUNT_URL = "/api/v1/auth/account";
    // 会话空闲 8 小时滑动过期；SHORT_TTL 用于把 TTL 压短以观察续期
    private static final long SESSION_TTL_SECONDS = Duration.ofHours(8).toSeconds();
    private static final long SHORT_TTL_SECONDS = 60L;
    // 续期后 TTL 断言允许的时钟误差窗口
    private static final long TTL_TOLERANCE_SECONDS = 10L;

    /** 登录-查询-续期-登出全流程：会话以摘要键存 Redis 并按用户索引，/me 滑动续期，登出后令牌失效。 */
    @Test
    void loginMeSlidingExpirationAndLogoutFullFlow() {
        String username = uniqueUsername();
        String password = "It-passw0rd!";
        UUID userId = ensureUser(username, password, "admin");

        JsonNode login = assertStatusAndCode(loginRaw(username, password), HttpStatus.OK, 0);
        JsonNode tokens = login.path("data").path("tokens");
        String accessToken = tokens.path("access_token").asText();
        // 访问令牌应为随机 UUID，且不签发 refresh_token
        assertThat(UUID.fromString(accessToken).toString()).isEqualTo(accessToken);
        assertThat(tokens.path("expires_in").asLong()).isEqualTo(SESSION_TTL_SECONDS);
        assertThat(tokens.has("refresh_token")).isFalse();
        assertThat(login.path("data").path("user").path("username").asText()).isEqualTo(username);
        Timestamp lastLoginAt = jdbc.queryForObject(
                "SELECT last_login_at FROM users WHERE id = ?", Timestamp.class, userId);
        assertThat(lastLoginAt).as("登录成功后 last_login_at 应落库").isNotNull();

        // Redis 会话以令牌的 SHA-256 摘要为键（不落明文令牌），并按用户 ZSet 反向索引
        Set<String> sessionKeys = redis.keys("auth:session:*");
        assertThat(sessionKeys).hasSize(1);
        String sessionKey = sessionKeys.iterator().next();
        assertThat(sessionKey).doesNotContain(accessToken);
        assertThat(sessionKey).matches("auth:session:[0-9a-f]{64}");
        Map<Object, Object> session = redis.opsForHash().entries(sessionKey);
        assertThat(session).containsKeys("user_id", "created_at", "last_seen_at");
        assertThat(session.get("user_id")).isEqualTo(userId.toString());
        assertThat(redis.opsForZSet().size("auth:user-sessions:" + userId)).isEqualTo(1L);
        String digest = sessionKey.substring("auth:session:".length());
        assertThat(redis.opsForZSet().range("auth:user-sessions:" + userId, 0, -1))
                .containsExactly(digest);

        // 先把 TTL 压短，再访问 /me 验证滑动续期会重置回完整 8 小时
        Boolean shortened = redis.expire(sessionKey, SHORT_TTL_SECONDS, TimeUnit.SECONDS);
        assertThat(shortened).isTrue();
        JsonNode me = assertStatusAndCode(
                rest.exchange(ME_URL, HttpMethod.GET, new HttpEntity<>(authHeaders(accessToken)),
                        JsonNode.class),
                HttpStatus.OK, 0);
        assertThat(me.path("data").path("username").asText()).isEqualTo(username);
        Long renewedTtl = redis.getExpire(sessionKey, TimeUnit.SECONDS);
        assertThat(renewedTtl).isGreaterThan(SESSION_TTL_SECONDS - TTL_TOLERANCE_SECONDS);

        assertStatusAndCode(rest.exchange(LOGOUT_URL, HttpMethod.POST,
                        new HttpEntity<>(authHeaders(accessToken)), JsonNode.class),
                HttpStatus.OK, 0);
        assertThat(redis.hasKey(sessionKey)).isFalse();
        assertThat(redis.hasKey("auth:user-sessions:" + userId)).isFalse();
        assertUnauthorized(accessToken);
    }

    /** 修改用户名/密码成功后吊销该用户全部会话，旧凭证不可再登录，新凭证可登录。 */
    @Test
    void accountUpdateRevokesEverySession() {
        String username = uniqueUsername();
        String updatedUsername = uniqueUsername();
        String password = "It-passw0rd!";
        String updatedPassword = "Updated-passw0rd!";
        ensureUser(username, password, "admin");
        String firstToken = login(username, password);
        String secondToken = login(username, password);

        JsonNode updated = assertStatusAndCode(
                rest.exchange(ACCOUNT_URL, HttpMethod.PUT,
                        new HttpEntity<>(Map.of(
                                "username", updatedUsername,
                                "old_password", password,
                                "new_password", updatedPassword), authHeaders(firstToken)),
                        JsonNode.class),
                HttpStatus.OK, 0);

        assertThat(updated.path("data").path("username").asText()).isEqualTo(updatedUsername);
        assertUnauthorized(firstToken);
        assertUnauthorized(secondToken);
        assertStatusAndCode(loginRaw(username, password), HttpStatus.UNAUTHORIZED, 11001);
        assertStatusAndCode(loginRaw(updatedUsername, updatedPassword), HttpStatus.OK, 0);
    }

    /** 登出仅吊销当前会话，同一用户的其他会话不受影响。 */
    @Test
    void logoutRevokesOnlyCurrentSession() {
        String username = uniqueUsername();
        String password = "It-passw0rd!";
        ensureUser(username, password, "admin");
        String firstToken = login(username, password);
        String secondToken = login(username, password);

        assertStatusAndCode(rest.exchange(LOGOUT_URL, HttpMethod.POST,
                        new HttpEntity<>(authHeaders(firstToken)), JsonNode.class),
                HttpStatus.OK, 0);

        assertUnauthorized(firstToken);
        assertStatusAndCode(rest.exchange(ME_URL, HttpMethod.GET,
                        new HttpEntity<>(authHeaders(secondToken)), JsonNode.class),
                HttpStatus.OK, 0);
    }

    /** 登录与改密并发竞争：改密成功后不允许残留用旧凭证签发的可用会话。 */
    @Test
    void concurrentLoginAndPasswordChangeCannotLeaveOldCredentialSession() throws Exception {
        String username = uniqueUsername();
        String password = "It-passw0rd!";
        String updatedPassword = "Updated-passw0rd!";
        ensureUser(username, password, "admin");
        String existingToken = login(username, password);
        // 双闸门栅栏：ready 等两个工作线程就绪，start 保证两个请求真正并发发出
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<ResponseEntity<JsonNode>> loginFuture = executor.submit(() -> {
                awaitStart(ready, start);
                return loginRaw(username, password);
            });
            Future<ResponseEntity<JsonNode>> updateFuture = executor.submit(() -> {
                awaitStart(ready, start);
                return rest.exchange(ACCOUNT_URL, HttpMethod.PUT,
                        new HttpEntity<>(Map.of(
                                "username", username,
                                "old_password", password,
                                "new_password", updatedPassword), authHeaders(existingToken)),
                        JsonNode.class);
            });
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            ResponseEntity<JsonNode> concurrentLogin = loginFuture.get();
            assertStatusAndCode(updateFuture.get(), HttpStatus.OK, 0);
            // 竞争结果两种都合法：并发登录成功则新令牌须随后被改密吊销，或直接因旧密码被拒
            if (concurrentLogin.getStatusCode() == HttpStatus.OK) {
                String issuedToken = concurrentLogin.getBody().path("data")
                        .path("tokens").path("access_token").asText();
                assertUnauthorized(issuedToken);
            } else {
                assertStatusAndCode(concurrentLogin, HttpStatus.UNAUTHORIZED, 11001);
            }
        } finally {
            executor.shutdownNow();
        }

        assertUnauthorized(existingToken);
        assertStatusAndCode(loginRaw(username, password), HttpStatus.UNAUTHORIZED, 11001);
        assertStatusAndCode(loginRaw(username, updatedPassword), HttpStatus.OK, 0);
    }

    /** 密码错误登录返回 401 与凭证无效错误码。 */
    @Test
    void loginWithWrongPasswordFails() {
        String username = uniqueUsername();
        ensureUser(username, "It-passw0rd!", "admin");

        ResponseEntity<JsonNode> response = loginRaw(username, "wrong-password");

        assertStatusAndCode(response, HttpStatus.UNAUTHORIZED, 11001);
    }

    /** 未携带令牌访问受保护接口返回 401。 */
    @Test
    void meWithoutTokenReturns401() {
        assertStatusAndCode(rest.getForEntity(ME_URL, JsonNode.class),
                HttpStatus.UNAUTHORIZED, 10001);
    }

    /** 携带不存在的令牌访问 /me 同样按未认证处理。 */
    @Test
    void meWithInvalidTokenReturns401() {
        assertUnauthorized("not-a-real-token");
    }

    /** 被注销、过期或格式非法的令牌均按未认证处理。 */
    private void assertUnauthorized(String token) {
        assertStatusAndCode(rest.exchange(ME_URL, HttpMethod.GET,
                        new HttpEntity<>(authHeaders(token)), JsonNode.class),
                HttpStatus.UNAUTHORIZED, 10001);
    }

    /** 让两个并发请求都就绪后再同时进入 HTTP 调用。 */
    private void awaitStart(CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
    }

    /** 用户名带随机片段，避免与基线数据及其他用例冲突。 */
    private String uniqueUsername() {
        return uniqueKey("apitest-");
    }
}
