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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 内容管理接口集成测试：草稿保存/编辑、发布/下线的状态流转、历史版本操作与 MyLab 标签管理。
 * 发布流转选用 vibe 模块做隔离（发布态校验不需要文件资源，且其他 IT 不依赖 vibe 的公开内容），
 * mylab 的已发布版本不被触碰，保证 PublicContentApiIT/PublicEngagementApiIT 与之互不干扰。
 */
class AdminContentApiIT extends AbstractApiIntegrationTest {

    private static final String CONTENT_URL = "/api/v1/admin/content";
    private static final String VIBE_URL = CONTENT_URL + "/vibe";
    private static final String TAGS_URL = "/api/v1/admin/mylab/tags";

    @Test
    void draftPublishOfflineLifecycle() {
        String admin = loginAs("admin");
        String toolKey = uniqueKey("apitest-tool-");
        // 基线可能残留 vibe 草稿，先放弃以保证从"无草稿"开始（没有草稿时 404，忽略结果）
        exchange(VIBE_URL + "/draft", HttpMethod.DELETE, admin, null);

        // 存草稿：落库为 DRAFT 版本，工具数据拆进 vibe_tools 表
        JsonNode saved = assertStatusAndCode(
                exchange(VIBE_URL, HttpMethod.PUT, admin,
                        Map.of("data", vibeData(toolKey, "第一版描述"))),
                HttpStatus.OK, 0);
        assertThat(countRows("content_releases",
                "module_key = 'vibe' AND state = 'DRAFT' AND deleted_at IS NULL")).isEqualTo(1);
        assertThat(countRows("vibe_tools t JOIN content_releases r ON r.id = t.release_id",
                "r.state = 'DRAFT' AND t.tool_key = '" + toolKey + "'")).isEqualTo(1);

        // 编辑草稿：携带 updated_at 做乐观并发校验，改动落库
        String updatedAt = saved.path("data").path("updated_at").asText();
        assertStatusAndCode(
                exchange(VIBE_URL, HttpMethod.PUT, admin,
                        Map.of("expected_updated_at", updatedAt, "data", vibeData(toolKey, "第二版描述"))),
                HttpStatus.OK, 0);
        String description = jdbc.queryForObject(
                "SELECT t.description FROM vibe_tools t JOIN content_releases r ON r.id = t.release_id"
                        + " WHERE r.state = 'DRAFT' AND t.tool_key = ?",
                String.class, toolKey);
        assertThat(description).isEqualTo("第二版描述");

        // 乐观锁：沿用旧的 expected_updated_at 再改应被拒绝
        assertStatusAndCode(
                exchange(VIBE_URL, HttpMethod.PUT, admin,
                        Map.of("expected_updated_at", updatedAt, "data", vibeData(toolKey, "第三版描述"))),
                HttpStatus.CONFLICT, 12005);

        // 发布：DRAFT 转 PUBLISHED 并记录发布人/时间，旧线上版本（若有）归档
        UUID previousCurrent = currentVibeReleaseId();
        JsonNode published = assertStatusAndCode(
                exchange(VIBE_URL + "/publish", HttpMethod.POST, admin, null), HttpStatus.OK, 0);
        Integer publishedVersion = published.path("data").path("published_version").intValue();
        assertThat(publishedVersion).isNotNull();
        Map<String, Object> release = jdbc.queryForMap(
                "SELECT state, published_by, published_at FROM content_releases"
                        + " WHERE module_key = 'vibe' AND version_no = ?",
                publishedVersion);
        assertThat(release.get("state")).isEqualTo("PUBLISHED");
        assertThat(release.get("published_by")).isNotNull();
        assertThat(release.get("published_at")).isNotNull();
        if (previousCurrent != null) {
            assertThat(jdbc.queryForObject("SELECT state FROM content_releases WHERE id = ?",
                    String.class, previousCurrent)).isEqualTo("ARCHIVED");
        }

        // 发布后经公开接口可见
        JsonNode publicVibe = assertStatusAndCode(
                rest.getForEntity("/api/v1/public/content/vibe", JsonNode.class), HttpStatus.OK, 0);
        assertThat(publicVibe.path("data").path("tools").toString()).contains(toolKey);

        // 下线：状态转 OFFLINE，公开接口随之 404
        assertStatusAndCode(exchange(VIBE_URL + "/offline", HttpMethod.POST, admin, null),
                HttpStatus.OK, 0);
        assertThat(jdbc.queryForObject(
                        "SELECT state FROM content_releases WHERE module_key = 'vibe' AND version_no = ?",
                        String.class, publishedVersion))
                .isEqualTo("OFFLINE");
        assertStatusAndCode(rest.getForEntity("/api/v1/public/content/vibe", JsonNode.class),
                HttpStatus.NOT_FOUND, 12002);
    }

    @Test
    void versionOperations() {
        String admin = loginAs("admin");
        // 自建两个已发布版本：v1 归档、v2 线上
        exchange(VIBE_URL + "/draft", HttpMethod.DELETE, admin, null);
        assertStatusAndCode(exchange(VIBE_URL, HttpMethod.PUT, admin,
                Map.of("data", vibeData(uniqueKey("apitest-v1-"), "版本一"))), HttpStatus.OK, 0);
        JsonNode first = assertStatusAndCode(exchange(VIBE_URL + "/publish", HttpMethod.POST, admin, null),
                HttpStatus.OK, 0);
        int v1 = first.path("data").path("published_version").intValue();
        assertStatusAndCode(exchange(VIBE_URL, HttpMethod.PUT, admin,
                Map.of("data", vibeData(uniqueKey("apitest-v2-"), "版本二"))), HttpStatus.OK, 0);
        JsonNode second = assertStatusAndCode(exchange(VIBE_URL + "/publish", HttpMethod.POST, admin, null),
                HttpStatus.OK, 0);
        int v2 = second.path("data").path("published_version").intValue();

        // 版本列表包含两个版本；线上版本不可删除
        JsonNode versions = assertStatusAndCode(
                exchange(VIBE_URL + "/versions", HttpMethod.GET, admin, null), HttpStatus.OK, 0);
        assertThat(versions.path("data").size()).isGreaterThanOrEqualTo(2);
        assertStatusAndCode(exchange(VIBE_URL + "/versions/" + v2, HttpMethod.DELETE, admin, null),
                HttpStatus.CONFLICT, 12005);

        // 归档版本可删除：软删除后按版本号查询返回 404
        JsonNode v1View = assertStatusAndCode(
                exchange(VIBE_URL + "/versions/" + v1, HttpMethod.GET, admin, null), HttpStatus.OK, 0);
        UUID v1Id = UUID.fromString(v1View.path("data").path("id").asText());
        assertStatusAndCode(exchange(VIBE_URL + "/versions/" + v1, HttpMethod.DELETE, admin, null),
                HttpStatus.OK, 0);
        Timestamp deletedAt = jdbc.queryForObject(
                "SELECT deleted_at FROM content_releases WHERE id = ?", Timestamp.class, v1Id);
        assertThat(deletedAt).as("删除版本应软标记 deleted_at").isNotNull();
        assertStatusAndCode(exchange(VIBE_URL + "/versions/" + v1, HttpMethod.GET, admin, null),
                HttpStatus.NOT_FOUND, 12003);
        assertStatusAndCode(exchange(VIBE_URL + "/versions/99999", HttpMethod.GET, admin, null),
                HttpStatus.NOT_FOUND, 12003);

        // 草稿可放弃：物理删除草稿记录，重复放弃返回 404
        assertStatusAndCode(exchange(VIBE_URL, HttpMethod.PUT, admin,
                Map.of("data", vibeData(uniqueKey("apitest-v3-"), "版本三"))), HttpStatus.OK, 0);
        assertStatusAndCode(exchange(VIBE_URL + "/draft", HttpMethod.DELETE, admin, null),
                HttpStatus.OK, 0);
        assertThat(countRows("content_releases",
                "module_key = 'vibe' AND state = 'DRAFT' AND deleted_at IS NULL")).isZero();
        assertStatusAndCode(exchange(VIBE_URL + "/draft", HttpMethod.DELETE, admin, null),
                HttpStatus.NOT_FOUND, 12003);
    }

    @Test
    void viewerGets403AndAnonymousGets401() {
        String viewer = loginAs("viewer");

        assertStatusAndCode(exchange(CONTENT_URL, HttpMethod.GET, viewer, null),
                HttpStatus.FORBIDDEN, 10004);
        assertStatusAndCode(exchange(VIBE_URL, HttpMethod.PUT, viewer,
                        Map.of("data", vibeData(uniqueKey("apitest-tool-"), "越权"))),
                HttpStatus.FORBIDDEN, 10004);
        assertStatusAndCode(exchange(TAGS_URL, HttpMethod.POST, viewer,
                        Map.of("tag_key", uniqueKey("apitest-tag-"), "name", "越权标签")),
                HttpStatus.FORBIDDEN, 10004);
        assertStatusAndCode(rest.getForEntity(CONTENT_URL, JsonNode.class),
                HttpStatus.UNAUTHORIZED, 10001);
    }

    @Test
    void saveDraftValidationFails() {
        String admin = loginAs("admin");

        // data 不是 JSON 对象、字段值越界均走内容校验（12004）；未知模块按模块不存在处理
        assertStatusAndCode(exchange(VIBE_URL, HttpMethod.PUT, admin, Map.of("data", List.of())),
                HttpStatus.UNPROCESSABLE_ENTITY, 12004);
        assertStatusAndCode(exchange(VIBE_URL, HttpMethod.PUT, admin,
                        Map.of("data", Map.of("tools", List.of(Map.of(
                                "tool_key", uniqueKey("apitest-tool-"), "percentage", 150))))),
                HttpStatus.UNPROCESSABLE_ENTITY, 12004);
        assertStatusAndCode(exchange(CONTENT_URL + "/notamodule", HttpMethod.PUT, admin,
                        Map.of("data", Map.of())),
                HttpStatus.NOT_FOUND, 12001);
    }

    @Test
    void mylabTagCrud() {
        String admin = loginAs("admin");
        String tagKey = uniqueKey("apitest-tag-");

        // 创建：落库可查
        JsonNode created = assertStatusAndCode(exchange(TAGS_URL, HttpMethod.POST, admin,
                        Map.of("tag_key", tagKey, "name", "API IT 标签", "sort_order", 0)),
                HttpStatus.OK, 0);
        UUID tagId = UUID.fromString(created.path("data").path("id").asText());
        assertThat(countRows("mylab_tags", "id = '" + tagId + "' AND deleted_at IS NULL")).isEqualTo(1);

        // 重复标识冲突、缺少必填名称
        assertStatusAndCode(exchange(TAGS_URL, HttpMethod.POST, admin,
                        Map.of("tag_key", tagKey, "name", "另一个名字")),
                HttpStatus.CONFLICT, 10006);
        assertStatusAndCode(exchange(TAGS_URL, HttpMethod.POST, admin,
                        Map.of("tag_key", uniqueKey("apitest-tag-"))),
                HttpStatus.UNPROCESSABLE_ENTITY, 12004);

        // 更新：名称落库
        assertStatusAndCode(exchange(TAGS_URL + "/" + tagId, HttpMethod.PUT, admin,
                        Map.of("tag_key", tagKey, "name", "API IT 标签（改）")),
                HttpStatus.OK, 0);
        assertThat(jdbc.queryForObject("SELECT name FROM mylab_tags WHERE id = ?",
                String.class, tagId)).isEqualTo("API IT 标签（改）");

        // 删除：软标记 deleted_at；重复删除与删除不存在 id 均 404
        assertStatusAndCode(exchange(TAGS_URL + "/" + tagId, HttpMethod.DELETE, admin, null),
                HttpStatus.OK, 0);
        Timestamp deletedAt = jdbc.queryForObject(
                "SELECT deleted_at FROM mylab_tags WHERE id = ?", Timestamp.class, tagId);
        assertThat(deletedAt).as("标签删除应为软删除").isNotNull();
        assertStatusAndCode(exchange(TAGS_URL + "/" + tagId, HttpMethod.DELETE, admin, null),
                HttpStatus.NOT_FOUND, 10005);
        assertStatusAndCode(exchange(TAGS_URL + "/" + UUID.randomUUID(), HttpMethod.DELETE, admin, null),
                HttpStatus.NOT_FOUND, 10005);
    }

    /** 构造可通过发布态校验的 vibe 模块数据（启用的工具必须有 name 和 description） */
    private Map<String, Object> vibeData(String toolKey, String description) {
        return Map.of("tools", List.of(Map.of(
                "tool_key", toolKey,
                "name", "API IT 工具",
                "description", description,
                "percentage", 50,
                "enabled", true,
                "sort_order", 0)));
    }

    /** 当前 vibe 线上版本（PUBLISHED/OFFLINE）的 id，没有则返回 null */
    private UUID currentVibeReleaseId() {
        List<UUID> ids = jdbc.query(
                "SELECT id FROM content_releases"
                        + " WHERE module_key = 'vibe' AND state IN ('PUBLISHED','OFFLINE') AND deleted_at IS NULL",
                (rs, rowNum) -> rs.getObject(1, UUID.class));
        return ids.isEmpty() ? null : ids.getFirst();
    }

    private int countRows(String from, String where) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM " + from + " WHERE " + where, Integer.class);
        return count == null ? 0 : count;
    }

    /** 携带 Bearer 令牌与 JSON 请求体发起调用（body 可为 null） */
    private ResponseEntity<JsonNode> exchange(String url, HttpMethod method, String token, Object body) {
        HttpHeaders headers = authHeaders(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(url, method, new HttpEntity<>(body, headers), JsonNode.class);
    }
}
