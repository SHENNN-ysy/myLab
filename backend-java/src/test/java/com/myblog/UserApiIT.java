package com.myblog;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 管理员账号管理接口集成测试：分页查询、创建、更新、删除及其权限边界。
 * 创建/删除仅 superadmin，列表/更新 admin 即可，viewer 一律 403。
 */
class UserApiIT extends AbstractApiIntegrationTest {

    private static final String USERS_URL = "/api/v1/users";

    @Test
    void superadminCreatesUserAndItCanLogin() {
        String superadmin = loginAs("superadmin");
        String username = uniqueKey("apitest-user-");
        String password = "It-passw0rd!";

        // 创建：响应不含密码哈希，库里角色/启用状态正确
        JsonNode created = assertStatusAndCode(
                exchange(USERS_URL, HttpMethod.POST, superadmin,
                        Map.of("username", username, "role", "editor", "password", password)),
                HttpStatus.OK, 0);
        assertThat(created.path("data").path("username").asText()).isEqualTo(username);
        assertThat(created.path("data").path("role").asText()).isEqualTo("editor");
        assertThat(created.path("data").has("password")).isFalse();
        assertThat(created.path("data").has("password_hash")).isFalse();
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT role, is_active FROM users WHERE username = ?", username);
        assertThat(row.get("role")).isEqualTo("editor");
        assertThat(row.get("is_active")).isEqualTo(true);

        // 落库的 BCrypt 哈希可直接通过登录接口验证
        assertStatusAndCode(loginRaw(username, password), HttpStatus.OK, 0);
    }

    @Test
    void adminCanListAndUpdateUsers() {
        String admin = loginAs("admin");

        // 分页列表：返回结构与分页参数生效
        JsonNode page = assertStatusAndCode(
                exchange(USERS_URL + "?page=1&page_size=5", HttpMethod.GET, admin, null),
                HttpStatus.OK, 0);
        assertThat(page.path("data").path("records").size()).isLessThanOrEqualTo(5);
        assertThat(page.path("data").path("total").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(page.path("data").path("page_size").asInt()).isEqualTo(5);

        // 更新：角色变更落库
        UUID targetId = ensureUser(uniqueKey("apitest-target-"), "It-passw0rd!", "viewer");
        JsonNode updated = assertStatusAndCode(
                exchange(USERS_URL + "/" + targetId, HttpMethod.PUT, admin,
                        Map.of("role", "editor")),
                HttpStatus.OK, 0);
        assertThat(updated.path("data").path("role").asText()).isEqualTo("editor");
        assertThat(jdbc.queryForObject("SELECT role FROM users WHERE id = ?",
                String.class, targetId)).isEqualTo("editor");
    }

    @Test
    void viewerAndNonSuperadminForbidden() {
        String viewer = loginAs("viewer");
        String admin = loginAs("admin");
        Map<String, String> createBody = Map.of(
                "username", uniqueKey("apitest-user-"), "password", "It-passw0rd!");

        // viewer 越权一律 403；创建/删除仅 superadmin，普通 admin 同样 403
        assertStatusAndCode(exchange(USERS_URL, HttpMethod.GET, viewer, null),
                HttpStatus.FORBIDDEN, 10004);
        assertStatusAndCode(exchange(USERS_URL, HttpMethod.POST, viewer, createBody),
                HttpStatus.FORBIDDEN, 10004);
        assertStatusAndCode(exchange(USERS_URL, HttpMethod.POST, admin, createBody),
                HttpStatus.FORBIDDEN, 10004);
        assertStatusAndCode(exchange(USERS_URL + "/" + UUID.randomUUID(), HttpMethod.DELETE, admin, null),
                HttpStatus.FORBIDDEN, 10004);
        assertStatusAndCode(rest.getForEntity(USERS_URL, JsonNode.class),
                HttpStatus.UNAUTHORIZED, 10001);
    }

    @Test
    void createUserValidationFails() {
        String superadmin = loginAs("superadmin");
        String username = uniqueKey("apitest-user-");

        // 密码过短、缺少必填字段走参数校验（10007）
        assertStatusAndCode(exchange(USERS_URL, HttpMethod.POST, superadmin,
                        Map.of("username", username, "password", "short")),
                HttpStatus.UNPROCESSABLE_ENTITY, 10007);
        assertStatusAndCode(exchange(USERS_URL, HttpMethod.POST, superadmin,
                        Map.of("username", username)),
                HttpStatus.UNPROCESSABLE_ENTITY, 10007);

        // 非法角色在服务层被拦截（10007），不会撞 users 表 CHECK 约束冒泡成 500
        assertStatusAndCode(exchange(USERS_URL, HttpMethod.POST, superadmin,
                        Map.of("username", uniqueKey("apitest-user-"), "password", "It-passw0rd!", "role", "root")),
                HttpStatus.UNPROCESSABLE_ENTITY, 10007);

        // 同名用户冲突（11005）
        assertStatusAndCode(exchange(USERS_URL, HttpMethod.POST, superadmin,
                        Map.of("username", username, "password", "It-passw0rd!")),
                HttpStatus.OK, 0);
        assertStatusAndCode(exchange(USERS_URL, HttpMethod.POST, superadmin,
                        Map.of("username", username, "password", "It-passw0rd!")),
                HttpStatus.CONFLICT, 11005);
    }

    @Test
    void updateDeleteEdgeCases() {
        String superadmin = loginAs("superadmin");

        // 不存在的用户：更新与删除均 404（11004）
        assertStatusAndCode(exchange(USERS_URL + "/" + UUID.randomUUID(), HttpMethod.PUT, superadmin,
                        Map.of("role", "editor")),
                HttpStatus.NOT_FOUND, 11004);
        assertStatusAndCode(exchange(USERS_URL + "/" + UUID.randomUUID(), HttpMethod.DELETE, superadmin, null),
                HttpStatus.NOT_FOUND, 11004);

        // 删除为软删除：deleted_at 落库，被删用户无法再登录
        String username = uniqueKey("apitest-user-");
        String password = "It-passw0rd!";
        UUID userId = ensureUser(username, password, "viewer");

        // 更新为非法角色：服务层校验拦截（10007），角色保持原值
        assertStatusAndCode(exchange(USERS_URL + "/" + userId, HttpMethod.PUT, superadmin,
                        Map.of("role", "root")),
                HttpStatus.UNPROCESSABLE_ENTITY, 10007);
        String role = jdbc.queryForObject("SELECT role FROM users WHERE id = ?", String.class, userId);
        assertThat(role).as("非法角色更新被拒后角色应保持不变").isEqualTo("viewer");

        assertStatusAndCode(exchange(USERS_URL + "/" + userId, HttpMethod.DELETE, superadmin, null),
                HttpStatus.OK, 0);
        Timestamp deletedAt = jdbc.queryForObject(
                "SELECT deleted_at FROM users WHERE id = ?", Timestamp.class, userId);
        assertThat(deletedAt).as("删除应为软删除").isNotNull();
        assertStatusAndCode(loginRaw(username, password), HttpStatus.UNAUTHORIZED, 11001);
    }

    /** 携带 Bearer 令牌与 JSON 请求体发起调用（body 可为 null） */
    private ResponseEntity<JsonNode> exchange(String url, HttpMethod method, String token, Object body) {
        HttpHeaders headers = authHeaders(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(url, method, new HttpEntity<>(body, headers), JsonNode.class);
    }
}
