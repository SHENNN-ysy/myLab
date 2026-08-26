package com.myblog;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 认证 API 集成测试打样：验证 Testcontainers（PG 迁移 + Redis）、登录签发令牌、
 * 鉴权过滤器与令牌吊销在真实 HTTP 链路上端到端可用。
 */
class AuthApiIT extends AbstractApiIntegrationTest {

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String ME_URL = "/api/v1/auth/me";
    private static final String REFRESH_URL = "/api/v1/auth/refresh";
    private static final String LOGOUT_URL = "/api/v1/auth/logout";
    private static final String ACCOUNT_URL = "/api/v1/auth/account";

    @Test
    void loginMeRefreshLogoutFullFlow() {
        String username = uniqueUsername();
        String password = "It-passw0rd!";
        UUID userId = ensureUser(username, password, "admin");

        // 登录：签发令牌对，且最后登录时间落库
        JsonNode login = assertStatusAndCode(loginRaw(username, password), HttpStatus.OK, 0);
        String accessToken = login.path("data").path("tokens").path("access_token").asText();
        String refreshToken = login.path("data").path("tokens").path("refresh_token").asText();
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();
        assertThat(login.path("data").path("user").path("username").asText()).isEqualTo(username);
        Timestamp lastLoginAt = jdbc.queryForObject(
                "SELECT last_login_at FROM users WHERE id = ?", Timestamp.class, userId);
        assertThat(lastLoginAt).as("登录成功后 last_login_at 应落库").isNotNull();

        // /me：携带 access token 返回当前用户
        JsonNode me = assertStatusAndCode(
                rest.exchange(ME_URL, HttpMethod.GET, new HttpEntity<>(authHeaders(accessToken)),
                        JsonNode.class),
                HttpStatus.OK, 0);
        assertThat(me.path("data").path("username").asText()).isEqualTo(username);
        assertThat(me.path("data").path("role").asText()).isEqualTo("admin");

        // refresh：旧 refresh token 换发新令牌对，新 access token 可直接使用
        JsonNode refreshed = assertStatusAndCode(
                rest.postForEntity(REFRESH_URL, Map.of("refresh_token", refreshToken), JsonNode.class),
                HttpStatus.OK, 0);
        String newAccessToken = refreshed.path("data").path("access_token").asText();
        String newRefreshToken = refreshed.path("data").path("refresh_token").asText();
        assertThat(newAccessToken).isNotBlank();
        assertStatusAndCode(
                rest.exchange(ME_URL, HttpMethod.GET, new HttpEntity<>(authHeaders(newAccessToken)),
                        JsonNode.class),
                HttpStatus.OK, 0);

        // logout：吊销请求头中的 access token 与请求体中的 refresh token
        assertStatusAndCode(
                rest.exchange(LOGOUT_URL, HttpMethod.POST,
                        new HttpEntity<>(Map.of("refresh_token", newRefreshToken),
                                authHeaders(newAccessToken)),
                        JsonNode.class),
                HttpStatus.OK, 0);

        // 吊销写路径落在 Redis 黑名单（jwt:blacklist:<jti>），且旧令牌真的失效
        Set<String> blacklistKeys = redis.keys("jwt:blacklist:*");
        assertThat(blacklistKeys).as("退出后 access 与 refresh 令牌都应进入黑名单")
                .hasSizeGreaterThanOrEqualTo(2);
        assertStatusAndCode(
                rest.postForEntity(REFRESH_URL, Map.of("refresh_token", newRefreshToken), JsonNode.class),
                HttpStatus.UNAUTHORIZED, 10003);
    }

    @Test
    void loginWithWrongPasswordFails() {
        String username = uniqueUsername();
        ensureUser(username, "It-passw0rd!", "admin");

        ResponseEntity<JsonNode> response = loginRaw(username, "wrong-password");

        assertStatusAndCode(response, HttpStatus.UNAUTHORIZED, 11001);
    }

    @Test
    void currentUserCanChangeUsernameAndPassword() {
        String username = uniqueUsername();
        String updatedUsername = uniqueUsername();
        String password = "It-passw0rd!";
        String updatedPassword = "Updated-passw0rd!";
        ensureUser(username, password, "admin");
        JsonNode login = assertStatusAndCode(loginRaw(username, password), HttpStatus.OK, 0);
        String accessToken = login.path("data").path("tokens").path("access_token").asText();

        JsonNode updated = assertStatusAndCode(
                rest.exchange(ACCOUNT_URL, HttpMethod.PUT,
                        new HttpEntity<>(Map.of(
                                "username", updatedUsername,
                                "old_password", password,
                                "new_password", updatedPassword),
                                authHeaders(accessToken)),
                        JsonNode.class),
                HttpStatus.OK, 0);

        assertThat(updated.path("data").path("username").asText()).isEqualTo(updatedUsername);
        assertStatusAndCode(loginRaw(username, password), HttpStatus.UNAUTHORIZED, 11001);
        assertStatusAndCode(loginRaw(updatedUsername, updatedPassword), HttpStatus.OK, 0);
    }

    @Test
    void meWithoutTokenReturns401() {
        ResponseEntity<JsonNode> response = rest.getForEntity(ME_URL, JsonNode.class);

        assertStatusAndCode(response, HttpStatus.UNAUTHORIZED, 10001);
    }

    @Test
    void meWithInvalidTokenReturns401() {
        // 非法令牌按匿名放行，最终被 /me 的认证规则拒绝
        ResponseEntity<JsonNode> response = rest.exchange(ME_URL, HttpMethod.GET,
                new HttpEntity<>(authHeaders("not-a-real-token")), JsonNode.class);

        assertStatusAndCode(response, HttpStatus.UNAUTHORIZED, 10001);
    }

    /** 用户名带随机片段，避免与基线数据及其他用例冲突，无需清表 */
    private String uniqueUsername() {
        return "apitest-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
