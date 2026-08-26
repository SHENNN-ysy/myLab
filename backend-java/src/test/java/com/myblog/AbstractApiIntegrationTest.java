package com.myblog;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API 集成测试基类：Testcontainers 启动真实 PostgreSQL（Flyway 完整迁移）与 Redis，
 * 通过 {@link TestRestTemplate} 走完整 HTTP 链路，让 API 测试同时充当集成测试。
 *
 * <p>镜像版本与生产 docker-compose 对齐；jwt-secret / 初始管理员密码等无默认值的环境变量
 * 在此固定为测试专用值，限流阈值调大避免误伤测试请求。</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.jwt-secret=api-it-jwt-secret-0123456789abcdef0123456789abcdef",
        "app.initial-admin-username=api-it-initial-admin",
        "app.initial-admin-password=api-it-initial-admin-password",
        // yml 中该值默认引用环境变量 ${JWT_SECRET}，属性覆盖 app.jwt-secret 对它不生效，需单独固定
        "app.engagement-hash-secret=api-it-engagement-hash-secret",
        "app.rate-limit-per-minute=100000",
        "app.login-rate-limit-per-minute=100000",
})
public abstract class AbstractApiIntegrationTest {

    // 不用 @Testcontainers 按类启停：多个 IT 类共享同一 Spring 缓存上下文，
    // 容器随类重启会换映射端口，缓存上下文里的连接坐标随即失效（命令挂起直至超时）。
    // 改为静态块一次性启动，整个测试 JVM 生命周期内有效，退出时由 Ryuk 回收。
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"));

    static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");
    }

    // 哈希强度无需与线上一致，BCrypt 校验与编码强度无关
    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    @Autowired
    protected TestRestTemplate rest;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected StringRedisTemplate redis;

    /** 每条用例前清空 Redis：限流计数与令牌黑名单不带入下一条用例 */
    @BeforeEach
    void flushRedis() {
        try (RedisConnection connection = redis.getRequiredConnectionFactory().getConnection()) {
            connection.serverCommands().flushDb();
        }
    }

    /**
     * 直接落库创建一个启用状态的测试用户（绕过接口，保证前置数据可控），返回用户 id。
     */
    protected UUID ensureUser(String username, String rawPassword, String role) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, username, password_hash, role, is_active)"
                        + " VALUES (?, ?, ?, ?, true)",
                id, username, BCRYPT.encode(rawPassword), role);
        return id;
    }

    /**
     * 生成带随机片段的唯一键（用户名、post_key 等），避免与基线数据及其他用例冲突，无需清表。
     */
    protected String uniqueKey(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 在当前已发布的 mylab 版本中插入一张测试卡片（基线没有已发布版本时先自建一个），
     * 供公开内容与互动接口用例做前置数据；post_key 由调用方用 {@link #uniqueKey} 保证唯一。
     */
    protected void ensurePublishedMylabCard(String postKey, String title, boolean enabled) {
        UUID releaseId = publishedMylabReleaseId();
        jdbc.update("INSERT INTO mylab_cards (id, release_id, post_key, card_title, card_summary,"
                        + " post_date, enabled, sort_order, card_type)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ARTICLE')",
                UUID.randomUUID(), releaseId, postKey, title, "API IT 自建卡片",
                LocalDate.now(), enabled, 9000);
    }

    /** 取当前已发布 mylab 版本的 id；不存在时按发布约束（published_by/published_at 非空）自建一版 */
    private UUID publishedMylabReleaseId() {
        List<UUID> ids = jdbc.query(
                "SELECT id FROM content_releases"
                        + " WHERE module_key = 'mylab' AND state = 'PUBLISHED' AND deleted_at IS NULL",
                (rs, rowNum) -> rs.getObject(1, UUID.class));
        if (!ids.isEmpty()) {
            return ids.getFirst();
        }
        UUID publisherId = ensureUser(uniqueKey("apitest-publisher-"), "It-passw0rd!", "admin");
        UUID releaseId = UUID.randomUUID();
        jdbc.update("INSERT INTO content_releases (id, module_key, version_no, state, published_by, published_at)"
                        + " VALUES (?, 'mylab',"
                        + " (SELECT COALESCE(MAX(version_no), 0) + 1 FROM content_releases WHERE module_key = 'mylab'),"
                        + " 'PUBLISHED', ?, now())",
                releaseId, publisherId);
        return releaseId;
    }

    /**
     * 走登录接口换取 access token；登录失败时直接断言失败，避免用例在错误状态下继续。
     */
    protected String login(String username, String password) {
        JsonNode body = loginRaw(username, password).getBody();
        assertThat(body).isNotNull();
        assertThat(body.path("code").asInt()).as("登录应成功").isEqualTo(0);
        return body.path("data").path("tokens").path("access_token").asText();
    }

    /**
     * 自建一个指定角色的用户并登录，直接返回其 access token。
     */
    protected String loginAs(String role) {
        String username = uniqueKey("apitest-" + role + "-");
        String password = "It-passw0rd!";
        ensureUser(username, password, role);
        return login(username, password);
    }

    /**
     * 调用登录接口并返回原始响应，供需要断言失败场景的用例使用。
     */
    protected ResponseEntity<JsonNode> loginRaw(String username, String password) {
        return rest.postForEntity("/api/v1/auth/login",
                Map.of("username", username, "password", password), JsonNode.class);
    }

    /**
     * 构造携带 Bearer 令牌的请求头。
     */
    protected HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    /**
     * 断言响应为指定 HTTP 状态且业务码符合预期，返回响应体便于继续断言 data。
     */
    protected JsonNode assertStatusAndCode(ResponseEntity<JsonNode> response, HttpStatus status, int code) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        JsonNode body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.path("code").asInt()).isEqualTo(code);
        return body;
    }
}
