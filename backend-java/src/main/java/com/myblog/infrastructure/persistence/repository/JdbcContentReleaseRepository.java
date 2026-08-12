package com.myblog.infrastructure.persistence.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myblog.application.model.entity.ContentRelease;
import com.myblog.application.repository.ContentReleaseRepository;
import com.myblog.common.json.JacksonObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 版本化内容发布仓储：基于 {@link JdbcTemplate} 实现应用层 {@link ContentReleaseRepository} 端口。
 * 负责 content_releases（发布记录）的草稿/发布/下线状态流转，以及各内容模块
 * （home、about、skills、footprints、hobbies、vibe、mylab）数据表的整包读写：
 * 写入时按 JSON 快照拆表落库，读取时再聚合回 JSON 结构。
 */
@Repository
public class JdbcContentReleaseRepository implements ContentReleaseRepository {
    private static final ObjectMapper OM = JacksonObjectMapper.get(); // 全局共享的 Jackson 实例
    private static final RowMapper<ContentRelease> RELEASE_MAPPER = JdbcContentReleaseRepository::mapRelease; // 发布记录行映射器

    private final JdbcTemplate jdbc;

    public JdbcContentReleaseRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 查询模块当前的草稿版本，无草稿返回 null */
    @Override
    public ContentRelease findDraft(String moduleKey) {
        return one("SELECT * FROM content_releases WHERE module_key = ? AND state = 'DRAFT' AND deleted_at IS NULL", moduleKey);
    }

    /** 查询模块的当前版本（已发布或已下线，即最新一个非草稿、非归档版本） */
    @Override
    public ContentRelease findCurrent(String moduleKey) {
        return one("SELECT * FROM content_releases WHERE module_key = ? AND state IN ('PUBLISHED','OFFLINE') AND deleted_at IS NULL", moduleKey);
    }

    /** 查询模块处于已发布状态的版本 */
    @Override
    public ContentRelease findPublished(String moduleKey) {
        return one("SELECT * FROM content_releases WHERE module_key = ? AND state = 'PUBLISHED' AND deleted_at IS NULL", moduleKey);
    }

    /** 按版本号查询模块的历史版本 */
    @Override
    public ContentRelease findVersion(String moduleKey, int versionNo) {
        return one("SELECT * FROM content_releases WHERE module_key = ? AND version_no = ? AND deleted_at IS NULL", moduleKey, versionNo);
    }

    /** 查询模块的全部历史版本（不含草稿），按版本号倒序 */
    @Override
    public List<ContentRelease> findVersions(String moduleKey) {
        return jdbc.query("SELECT * FROM content_releases WHERE module_key = ? AND state <> 'DRAFT' AND deleted_at IS NULL ORDER BY version_no DESC",
                RELEASE_MAPPER, moduleKey);
    }

    /** 对模块加 PostgreSQL 事务级咨询锁，串行化同一模块的发布操作，须在外层事务内调用 */
    @Override
    public void lockModule(String moduleKey) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))",
                preparedStatement -> preparedStatement.setString(1, moduleKey),
                resultSet -> null);
    }

    /** 计算模块的下一个版本号（当前最大版本号 + 1，从 1 开始） */
    @Override
    public int nextVersion(String moduleKey) {
        Integer result = jdbc.queryForObject("SELECT COALESCE(MAX(version_no), 0) + 1 FROM content_releases WHERE module_key = ?",
                Integer.class, moduleKey);
        return result == null ? 1 : result;
    }

    /** 插入一条新的发布记录（初始为草稿状态） */
    @Override
    public void add(ContentRelease release) {
        jdbc.update("""
                INSERT INTO content_releases
                    (id, module_key, version_no, state, source_release_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, release.getId(), release.getModuleKey(), release.getVersionNo(), release.getState(),
                release.getSourceReleaseId(), release.getCreatedAt(), release.getUpdatedAt());
    }

    /**
     * 更新草稿的 updated_at 时间戳；传入 expectedUpdatedAt 时作为乐观锁条件，
     * 防止并发编辑互相覆盖。
     *
     * @return 更新成功返回 true；期望时间戳不匹配（草稿已被他人改动）返回 false
     */
    @Override
    public boolean touchDraft(UUID releaseId, OffsetDateTime expectedUpdatedAt, OffsetDateTime nextUpdatedAt) {
        if (expectedUpdatedAt == null) {
            return jdbc.update("UPDATE content_releases SET updated_at = ? WHERE id = ? AND state = 'DRAFT' AND deleted_at IS NULL",
                    nextUpdatedAt, releaseId) == 1;
        }
        return jdbc.update("UPDATE content_releases SET updated_at = ? WHERE id = ? AND state = 'DRAFT' AND updated_at = ? AND deleted_at IS NULL",
                nextUpdatedAt, releaseId, expectedUpdatedAt) == 1;
    }

    /**
     * 用 JSON 快照整体替换某次发布的模块数据：先清空该 release 的旧数据，再按模块拆表写入。
     *
     * @throws IllegalArgumentException 模块名无法识别时抛出
     */
    @Override
    public void replaceData(ContentRelease release, Object data) {
        JsonNode root = OM.valueToTree(data);
        deleteReleaseData(release);
        switch (release.getModuleKey()) {
            case "home" -> writeHomeImages(release.getId(), root.path("images"));
            case "about" -> writeAbout(release.getId(), root);
            case "skills" -> writeSkills(release.getId(), root.path("items"));
            case "footprints" -> writeFootprints(release.getId(), array(root, "details", "items"));
            case "hobbies" -> writeHobbies(release.getId(), root);
            case "vibe" -> writeVibeTools(release.getId(), root.path("tools"));
            case "mylab" -> writeMylabCards(release.getId(), array(root, "cards", "posts"));
            default -> throw new IllegalArgumentException("unknown module: " + release.getModuleKey());
        }
    }

    /** 读取某次发布的模块数据并聚合为 JSON 结构（与 replaceData 的写入结构互逆） */
    @Override
    public Object readData(ContentRelease release) {
        if (release == null) return null;
        return switch (release.getModuleKey()) {
            case "home" -> Map.of("images", readHomeImages(release.getId()));
            case "about" -> readAbout(release.getId());
            case "skills" -> Map.of("items", readSkills(release.getId()));
            case "footprints" -> Map.of("details", readFootprints(release.getId()));
            case "hobbies" -> {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("cards", readHobbies(release.getId()));
                result.put("time_tags", readHobbyTimeTags(release.getId()));
                result.put("time_points", readHobbyTimePoints(release.getId()));
                yield result;
            }
            case "vibe" -> Map.of("tools", readVibeTools(release.getId()));
            case "mylab" -> {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("tags", readActiveTags());
                result.put("cards", readMylabCards(release.getId()));
                yield result;
            }
            default -> Map.of();
        };
    }

    /**
     * 发布草稿：把当前生效版本归档，再将草稿置为已发布。
     *
     * @throws IllegalStateException 草稿状态在发布期间被并发修改时抛出
     */
    @Override
    public void publish(ContentRelease draft, ContentRelease current, UUID actorId, OffsetDateTime now) {
        if (current != null) {
            jdbc.update("UPDATE content_releases SET state = 'ARCHIVED', updated_at = ? WHERE id = ? AND state IN ('PUBLISHED','OFFLINE')",
                    now, current.getId());
        }
        int updated = jdbc.update("""
                UPDATE content_releases
                SET state = 'PUBLISHED', published_by = ?, published_at = ?, updated_at = ?
                WHERE id = ? AND state = 'DRAFT' AND deleted_at IS NULL
                """, actorId, now, now, draft.getId());
        if (updated != 1) throw new IllegalStateException("draft state changed while publishing");
    }

    /** 下线当前已发布版本（状态置为 OFFLINE） */
    @Override
    public void offline(ContentRelease current, OffsetDateTime now) {
        jdbc.update("UPDATE content_releases SET state = 'OFFLINE', updated_at = ? WHERE id = ? AND state = 'PUBLISHED'",
                now, current.getId());
    }

    /** 物理删除草稿发布记录（仅草稿可删） */
    @Override
    public void deleteDraft(ContentRelease draft) {
        jdbc.update("DELETE FROM content_releases WHERE id = ? AND state = 'DRAFT'", draft.getId());
    }

    /**
     * 软删除历史版本：按模块级联为子表/孙表数据行打 deleted_at 标记（解除资源引用），
     * 最后标记 content_releases 本体。所有 UPDATE 带 deleted_at IS NULL 条件，保证幂等。
     */
    @Override
    public void softDeleteVersion(ContentRelease release, OffsetDateTime now) {
        UUID releaseId = release.getId();
        switch (release.getModuleKey()) {
            case "home" -> softDelete("home_images", "release_id = ?", now, releaseId);
            case "about" -> {
                String parent = "about_content_id IN (SELECT id FROM about_contents WHERE release_id = ?)";
                softDelete("about_profile_bullets", parent, now, releaseId);
                softDelete("about_bubbles", parent, now, releaseId);
                softDelete("about_contents", "release_id = ?", now, releaseId);
            }
            case "skills" -> softDelete("skills", "release_id = ?", now, releaseId);
            case "footprints" -> {
                softDelete("footprint_resources",
                        "footprint_id IN (SELECT id FROM footprints WHERE release_id = ?)", now, releaseId);
                softDelete("footprints", "release_id = ?", now, releaseId);
            }
            case "hobbies" -> {
                softDelete("hobby_resources",
                        "hobby_id IN (SELECT id FROM hobbies WHERE release_id = ?)", now, releaseId);
                softDelete("hobbies", "release_id = ?", now, releaseId);
                softDelete("hobby_time_tags", "release_id = ?", now, releaseId);
                softDelete("hobby_time_points", "release_id = ?", now, releaseId);
            }
            case "vibe" -> softDelete("vibe_tools", "release_id = ?", now, releaseId);
            case "mylab" -> {
                String parent = "card_id IN (SELECT id FROM mylab_cards WHERE release_id = ?)";
                softDelete("mylab_card_tags", parent, now, releaseId);
                softDelete("mylab_resources", parent, now, releaseId);
                softDelete("mylab_cards", "release_id = ?", now, releaseId);
            }
            default -> throw new IllegalArgumentException("unknown module: " + release.getModuleKey());
        }
        jdbc.update("UPDATE content_releases SET deleted_at = ?, updated_at = ? WHERE id = ? AND deleted_at IS NULL",
                now, now, releaseId);
    }

    /** 按条件为指定表的数据行打 deleted_at 标记（幂等） */
    private void softDelete(String table, String condition, OffsetDateTime now, UUID releaseId) {
        jdbc.update("UPDATE " + table + " SET deleted_at = ? WHERE " + condition + " AND deleted_at IS NULL",
                now, releaseId);
    }

    /** 清空某次发布在各模块数据表中的数据（replaceData 前置步骤） */
    private void deleteReleaseData(ContentRelease release) {
        UUID releaseId = release.getId();
        switch (release.getModuleKey()) {
            case "home" -> jdbc.update("DELETE FROM home_images WHERE release_id = ?", releaseId);
            case "about" -> jdbc.update("DELETE FROM about_contents WHERE release_id = ?", releaseId);
            case "skills" -> jdbc.update("DELETE FROM skills WHERE release_id = ?", releaseId);
            case "footprints" -> jdbc.update("DELETE FROM footprints WHERE release_id = ?", releaseId);
            case "hobbies" -> {
                jdbc.update("DELETE FROM hobby_time_points WHERE release_id = ?", releaseId);
                jdbc.update("DELETE FROM hobby_time_tags WHERE release_id = ?", releaseId);
                jdbc.update("DELETE FROM hobbies WHERE release_id = ?", releaseId);
            }
            case "vibe" -> jdbc.update("DELETE FROM vibe_tools WHERE release_id = ?", releaseId);
            case "mylab" -> jdbc.update("DELETE FROM mylab_cards WHERE release_id = ?", releaseId);
            default -> throw new IllegalArgumentException("unknown module: " + release.getModuleKey());
        }
    }

    private void writeHomeImages(UUID releaseId, JsonNode images) {
        int order = 0;
        for (JsonNode image : iterable(images)) {
            jdbc.update("""
                    INSERT INTO home_images
                        (id, release_id, image_resource_id, alt_text, object_position, sort_order)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, uuid(image, "row_id"), releaseId, nullableUuid(image, "image_resource_id"),
                    text(image, "alt"), Objects.requireNonNullElse(text(image, "object_position"), "50% 50%"),
                    order++);
        }
    }

    private void writeAbout(UUID releaseId, JsonNode root) {
        JsonNode profile = root.path("profile");
        JsonNode ingredients = root.path("ingredients");
        UUID aboutId = uuid(root, "row_id");
        jdbc.update("""
                INSERT INTO about_contents
                    (id, release_id, profile_title, avatar_resource_id, avatar_alt, intro, outro,
                     ingredients_title, ingredients_description)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, aboutId, releaseId, text(profile, "title"), nullableUuid(profile, "avatar_resource_id"),
                text(profile, "avatar_alt"), text(profile, "intro"), text(profile, "outro"),
                text(ingredients, "title"), text(ingredients, "description"));
        int bulletOrder = 0;
        for (JsonNode bullet : iterable(profile.path("bullets"))) {
            jdbc.update("INSERT INTO about_profile_bullets (id, about_content_id, contents, sort_order) VALUES (?, ?, ?, ?)",
                    UUID.randomUUID(), aboutId, bullet.asText(), bulletOrder++);
        }
        int bubbleOrder = 0;
        for (JsonNode bubble : iterable(root.path("bubbles"))) {
            jdbc.update("""
                    INSERT INTO about_bubbles
                        (id, about_content_id, bubble_text, bubble_size, background_color, text_color, glow_color, sort_order)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, uuid(bubble, "row_id"), aboutId, text(bubble, "text"), text(bubble, "size"),
                    text(bubble, "background_color"), text(bubble, "text_color"), text(bubble, "glow_color"),
                    bubbleOrder++);
        }
    }

    private void writeSkills(UUID releaseId, JsonNode items) {
        int order = 0;
        for (JsonNode item : iterable(items)) {
            jdbc.update("""
                    INSERT INTO skills
                        (id, release_id, skill_key, name, percentage, level_code, level_text, icon_resource_id, bar_style, is_new, enabled, sort_order)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, uuid(item, "row_id"), releaseId, key(item, "skill_key", "id"), text(item, "name"),
                    integer(item, "percentage", 0), firstText(item, "level_code", "level"), text(item, "level_text"),
                    nullableUuid(item, "icon_resource_id"), text(item, "bar_style"), bool(item, "is_new", false),
                    bool(item, "enabled", true), integer(item, "sort_order", order++));
        }
    }

    private void writeFootprints(UUID releaseId, JsonNode items) {
        int order = 0;
        for (JsonNode item : iterable(items)) {
            UUID id = uuid(item, "row_id");
            String contents = text(item, "contents");
            if (contents == null && item.path("paragraphs").isArray()) {
                // 兼容旧结构：没有 contents 时把段落数组用空行拼接为正文
                List<String> paragraphs = new ArrayList<>();
                item.path("paragraphs").forEach(node -> paragraphs.add(node.asText()));
                contents = String.join("\n\n", paragraphs);
            }
            jdbc.update("""
                    INSERT INTO footprints
                        (id, release_id, city_key, title, summary, contents, enabled, sort_order)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, id, releaseId, key(item, "city_key", "id"), text(item, "title"), text(item, "summary"), contents,
                    bool(item, "enabled", true), integer(item, "sort_order", order++));
            writeManyResources("footprint_resources", "footprint_id", id, item.path("resource_ids"));
        }
    }

    private void writeHobbies(UUID releaseId, JsonNode root) {
        int order = 0;
        for (JsonNode item : iterable(root.path("cards"))) {
            UUID id = uuid(item, "row_id");
            jdbc.update("""
                    INSERT INTO hobbies
                        (id, release_id, hobby_key, title, description, enabled, sort_order)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, id, releaseId, key(item, "hobby_key", "id"), text(item, "title"), text(item, "description"),
                    bool(item, "enabled", true), integer(item, "sort_order", order++));
            UUID resourceId = nullableUuid(item, "resource_id", "image_resource_id");
            if (resourceId != null) {
                jdbc.update("INSERT INTO hobby_resources (id, hobby_id, resource_id) VALUES (?, ?, ?)",
                        UUID.randomUUID(), id, resourceId);
            }
        }
        order = 0;
        for (JsonNode tag : iterable(root.path("time_tags"))) {
            jdbc.update("""
                    INSERT INTO hobby_time_tags
                        (id, release_id, data_key, name, color, label_x, label_y, label_scale, enabled, sort_order)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, uuid(tag, "row_id"), releaseId, text(tag, "data_key"), text(tag, "name"), text(tag, "color"),
                    integer(tag, "label_x", 0), integer(tag, "label_y", 0), decimal(tag, "label_scale", 1.0),
                    bool(tag, "enabled", true), order++);
        }
        for (JsonNode point : iterable(root.path("time_points"))) {
            JsonNode values = point.path("values");
            jdbc.update("""
                    INSERT INTO hobby_time_points
                        (id, release_id, age, "爱好1", "爱好2", "爱好3", "爱好4", "爱好5")
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, uuid(point, "row_id"), releaseId, integer(point, "age", -2),
                    decimal(values, "爱好1", 0), decimal(values, "爱好2", 0), decimal(values, "爱好3", 0),
                    decimal(values, "爱好4", 0), decimal(values, "爱好5", 0));
        }
    }

    private void writeVibeTools(UUID releaseId, JsonNode items) {
        int order = 0;
        for (JsonNode item : iterable(items)) {
            jdbc.update("""
                    INSERT INTO vibe_tools
                        (id, release_id, tool_key, name, percentage, description, enabled, sort_order)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, uuid(item, "row_id"), releaseId, key(item, "tool_key", "id"), text(item, "name"),
                    integer(item, "percentage", 0), text(item, "description"), bool(item, "enabled", true),
                    integer(item, "sort_order", order++));
        }
    }

    private void writeMylabCards(UUID releaseId, JsonNode items) {
        int order = 0;
        for (JsonNode item : iterable(items)) {
            UUID id = uuid(item, "row_id");
            // 未显式给出 card_type 时按 post_key 前缀推断：project- 开头视为项目卡片
            String type = Objects.requireNonNullElse(firstText(item, "card_type"),
                    key(item, "post_key", "id").startsWith("project-") ? "PROJECT" : "ARTICLE").toUpperCase();
            Integer projectOrder = "PROJECT".equals(type)
                    ? integer(item, "project_show_order", integer(item, "sort_order", order)) : null;
            jdbc.update("""
                    INSERT INTO mylab_cards
                        (id, release_id, post_key, card_title, card_summary, post_date, enabled,
                         sort_order, card_type, project_show_order, project_contents)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, id, releaseId, key(item, "post_key", "id"), firstText(item, "card_title", "title"),
                    firstText(item, "card_summary", "summary"), localDate(item, "post_date", "date"),
                    bool(item, "enabled", true), integer(item, "sort_order", order++), type, projectOrder,
                    firstText(item, "project_contents", "project_content"));
            int tagOrder = 0;
            for (JsonNode tagId : iterable(item.path("tag_ids"))) {
                jdbc.update("INSERT INTO mylab_card_tags (id, card_id, tag_id, sort_order) VALUES (?, ?, ?, ?)",
                        UUID.randomUUID(), id, UUID.fromString(tagId.asText()), tagOrder++);
            }
            UUID imageId = nullableUuid(item, "image_resource_id");
            UUID contentId = nullableUuid(item, "content_resource_id");
            if (imageId != null || contentId != null) {
                jdbc.update("""
                        INSERT INTO mylab_resources (id, card_id, image_resource_id, content_resource_id)
                        VALUES (?, ?, ?, ?)
                        """, UUID.randomUUID(), id, imageId, contentId);
            }
        }
    }

    private void writeManyResources(String table, String ownerColumn, UUID ownerId, JsonNode ids) {
        int order = 0;
        for (JsonNode node : iterable(ids)) {
            UUID resourceId = UUID.fromString(node.asText());
            jdbc.update("INSERT INTO " + table + " (id, " + ownerColumn + ", resource_id, sort_order) VALUES (?, ?, ?, ?)",
                    UUID.randomUUID(), ownerId, resourceId, order++);
        }
    }

    private List<Map<String, Object>> readHomeImages(UUID releaseId) {
        return jdbc.query("""
                SELECT hi.id, hi.image_resource_id, hi.alt_text, hi.object_position, hi.sort_order,
                       r.object_key AS image_object_key
                FROM home_images hi
                LEFT JOIN resources r ON r.id = hi.image_resource_id
                WHERE hi.release_id = ? AND hi.deleted_at IS NULL
                ORDER BY hi.sort_order
                """, (rs, n) -> mapOf(
                "row_id", rs.getObject("id"), "image_resource_id", rs.getObject("image_resource_id"),
                "image_object_key", rs.getString("image_object_key"), "alt", rs.getString("alt_text"),
                "object_position", rs.getString("object_position"), "sort_order", rs.getInt("sort_order")), releaseId);
    }

    private Map<String, Object> readAbout(UUID releaseId) {
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT ac.id, ac.profile_title, ac.avatar_resource_id, avatar.object_key AS avatar_object_key,
                       ac.avatar_alt, ac.intro, ac.outro, ac.ingredients_title, ac.ingredients_description
                FROM about_contents ac
                LEFT JOIN resources avatar ON avatar.id = ac.avatar_resource_id
                WHERE ac.release_id = ? AND ac.deleted_at IS NULL
                """, (rs, n) -> mapOf(
                "row_id", rs.getObject("id"), "title", rs.getString("profile_title"),
                "avatar_resource_id", rs.getObject("avatar_resource_id"),
                "avatar_object_key", rs.getString("avatar_object_key"), "avatar_alt", rs.getString("avatar_alt"),
                "intro", rs.getString("intro"), "outro", rs.getString("outro"),
                "ingredients_title", rs.getString("ingredients_title"),
                "ingredients_description", rs.getString("ingredients_description")), releaseId);
        if (rows.isEmpty()) return Map.of("profile", Map.of("bullets", List.of()), "ingredients", Map.of(), "bubbles", List.of());
        Map<String, Object> row = rows.getFirst();
        UUID aboutId = (UUID) row.get("row_id");
        Map<String, Object> profile = mapOf(
                "title", row.get("title"), "avatar_resource_id", row.get("avatar_resource_id"),
                "avatar_object_key", row.get("avatar_object_key"), "avatar_alt", row.get("avatar_alt"),
                "intro", row.get("intro"), "bullets", readAboutBullets(aboutId), "outro", row.get("outro"));
        Map<String, Object> ingredients = mapOf(
                "title", row.get("ingredients_title"), "description", row.get("ingredients_description"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("row_id", aboutId);
        result.put("profile", profile);
        result.put("ingredients", ingredients);
        result.put("bubbles", readAboutBubbles(aboutId));
        return result;
    }

    private List<String> readAboutBullets(UUID aboutId) {
        return jdbc.query("""
                SELECT contents FROM about_profile_bullets
                WHERE about_content_id = ? AND deleted_at IS NULL ORDER BY sort_order
                """, (rs, n) -> rs.getString("contents"), aboutId);
    }

    private List<Map<String, Object>> readAboutBubbles(UUID aboutId) {
        return jdbc.query("""
                SELECT id, bubble_text, bubble_size, background_color, text_color, glow_color, sort_order
                FROM about_bubbles WHERE about_content_id = ? AND deleted_at IS NULL ORDER BY sort_order
                """, (rs, n) -> mapOf(
                "row_id", rs.getObject("id"), "text", rs.getString("bubble_text"),
                "size", rs.getString("bubble_size"), "background_color", rs.getString("background_color"),
                "text_color", rs.getString("text_color"), "glow_color", rs.getString("glow_color"),
                "sort_order", rs.getInt("sort_order")), aboutId);
    }

    private List<Map<String, Object>> readSkills(UUID releaseId) {
        return jdbc.query("""
                SELECT s.id, s.skill_key, s.name, s.percentage, s.level_code, s.level_text,
                       s.icon_resource_id, icon.object_key AS icon_object_key,
                       s.bar_style, s.is_new, s.enabled, s.sort_order
                FROM skills s
                LEFT JOIN resources icon ON icon.id = s.icon_resource_id
                WHERE s.release_id = ? AND s.deleted_at IS NULL ORDER BY s.sort_order, s.skill_key
                """, (rs, n) -> mapOf(
                "row_id", rs.getObject("id"), "id", rs.getString("skill_key"), "skill_key", rs.getString("skill_key"),
                "name", rs.getString("name"), "percentage", rs.getInt("percentage"),
                "level", rs.getString("level_code"), "level_code", rs.getString("level_code"),
                "level_text", rs.getString("level_text"), "icon_resource_id", rs.getObject("icon_resource_id"),
                "icon_object_key", rs.getString("icon_object_key"),
                "bar_style", rs.getString("bar_style"), "is_new", rs.getBoolean("is_new"),
                "enabled", rs.getBoolean("enabled"), "sort_order", rs.getInt("sort_order")), releaseId);
    }

    private List<Map<String, Object>> readFootprints(UUID releaseId) {
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT id, city_key, title, summary, contents, enabled, sort_order
                FROM footprints WHERE release_id = ? AND deleted_at IS NULL ORDER BY sort_order, city_key
                """, (rs, n) -> {
            String contents = rs.getString("contents");
            Map<String, Object> result = mapOf(
                    "row_id", rs.getObject("id"), "id", rs.getString("city_key"), "city_key", rs.getString("city_key"),
                    "title", rs.getString("title"), "summary", rs.getString("summary"), "contents", contents,
                    "paragraphs", splitParagraphs(contents), "enabled", rs.getBoolean("enabled"),
                    "sort_order", rs.getInt("sort_order"));
            result.put("resources", readFootprintResources((UUID) rs.getObject("id")));
            return result;
        }, releaseId);
        return rows;
    }

    private List<Map<String, Object>> readHobbies(UUID releaseId) {
        return jdbc.query("""
                SELECT h.id, h.hobby_key, h.title, h.description, h.enabled, h.sort_order,
                       r.id AS resource_id, r.object_key AS resource_object_key
                FROM hobbies h
                LEFT JOIN hobby_resources hr ON hr.hobby_id = h.id AND hr.deleted_at IS NULL
                LEFT JOIN resources r ON r.id = hr.resource_id
                WHERE h.release_id = ? AND h.deleted_at IS NULL
                ORDER BY h.sort_order, h.hobby_key
                """, (rs, n) -> mapOf(
                "row_id", rs.getObject("id"), "id", rs.getString("hobby_key"), "hobby_key", rs.getString("hobby_key"),
                "title", rs.getString("title"), "description", rs.getString("description"),
                "enabled", rs.getBoolean("enabled"), "sort_order", rs.getInt("sort_order"),
                "image_resource_id", rs.getObject("resource_id"), "image_object_key", rs.getString("resource_object_key")), releaseId);
    }

    private List<Map<String, Object>> readHobbyTimeTags(UUID releaseId) {
        return jdbc.query("""
                SELECT id, data_key, name, color, label_x, label_y, label_scale, enabled, sort_order
                FROM hobby_time_tags WHERE release_id = ? AND deleted_at IS NULL ORDER BY sort_order, data_key
                """, (rs, n) -> mapOf(
                "row_id", rs.getObject("id"), "data_key", rs.getString("data_key"), "name", rs.getString("name"),
                "color", rs.getString("color"), "label_x", rs.getInt("label_x"), "label_y", rs.getInt("label_y"),
                "label_scale", rs.getBigDecimal("label_scale"), "enabled", rs.getBoolean("enabled"),
                "sort_order", rs.getInt("sort_order")), releaseId);
    }

    private List<Map<String, Object>> readHobbyTimePoints(UUID releaseId) {
        return jdbc.query("""
                SELECT id, age, "爱好1", "爱好2", "爱好3", "爱好4", "爱好5"
                FROM hobby_time_points WHERE release_id = ? AND deleted_at IS NULL ORDER BY age
                """, (rs, n) -> {
            Map<String, Object> values = mapOf(
                    "爱好1", rs.getBigDecimal("爱好1"), "爱好2", rs.getBigDecimal("爱好2"),
                    "爱好3", rs.getBigDecimal("爱好3"), "爱好4", rs.getBigDecimal("爱好4"),
                    "爱好5", rs.getBigDecimal("爱好5"));
            return mapOf("row_id", rs.getObject("id"), "age", rs.getInt("age"), "values", values);
        }, releaseId);
    }

    private List<Map<String, Object>> readVibeTools(UUID releaseId) {
        return jdbc.query("""
                SELECT id, tool_key, name, percentage, description, enabled, sort_order
                FROM vibe_tools WHERE release_id = ? AND deleted_at IS NULL ORDER BY sort_order, tool_key
                """, (rs, n) -> mapOf(
                "row_id", rs.getObject("id"), "id", rs.getString("tool_key"), "tool_key", rs.getString("tool_key"),
                "name", rs.getString("name"), "percentage", rs.getInt("percentage"),
                "description", rs.getString("description"), "enabled", rs.getBoolean("enabled"),
                "sort_order", rs.getInt("sort_order")), releaseId);
    }

    private List<Map<String, Object>> readMylabCards(UUID releaseId) {
        return jdbc.query("""
                SELECT c.id, c.post_key, c.card_title, c.card_summary, c.post_date,
                       ARRAY(SELECT card_tag.tag_id
                             FROM mylab_card_tags card_tag
                             WHERE card_tag.card_id = c.id AND card_tag.deleted_at IS NULL
                             ORDER BY card_tag.sort_order) AS tag_ids,
                       c.enabled,
                       c.sort_order, c.card_type, c.project_show_order, c.project_contents,
                       mr.image_resource_id, image.object_key AS image_object_key,
                       mr.content_resource_id, content.object_key AS content_object_key
                FROM mylab_cards c
                LEFT JOIN mylab_resources mr ON mr.card_id = c.id AND mr.deleted_at IS NULL
                LEFT JOIN resources image ON image.id = mr.image_resource_id
                LEFT JOIN resources content ON content.id = mr.content_resource_id
                WHERE c.release_id = ? AND c.deleted_at IS NULL
                ORDER BY c.sort_order, c.post_key
                """, (rs, n) -> mapOf(
                "row_id", rs.getObject("id"), "id", rs.getString("post_key"), "post_key", rs.getString("post_key"),
                "title", rs.getString("card_title"), "card_title", rs.getString("card_title"),
                "summary", rs.getString("card_summary"), "card_summary", rs.getString("card_summary"),
                "date", rs.getObject("post_date"), "post_date", rs.getObject("post_date"),
                "tag_ids", uuidArray(rs, "tag_ids"), "enabled", rs.getBoolean("enabled"),
                "sort_order", rs.getInt("sort_order"), "card_type", rs.getString("card_type"),
                "project_show_order", rs.getObject("project_show_order"),
                "project_contents", rs.getString("project_contents"),
                "image_resource_id", rs.getObject("image_resource_id"), "image_object_key", rs.getString("image_object_key"),
                "content_resource_id", rs.getObject("content_resource_id"), "content_object_key", rs.getString("content_object_key")), releaseId);
    }

    private List<Map<String, Object>> readActiveTags() {
        return jdbc.query("""
                SELECT id, tag_key, name, enabled, sort_order
                FROM mylab_tags WHERE enabled = TRUE AND deleted_at IS NULL ORDER BY sort_order, tag_key
                """, (rs, n) -> mapOf("id", rs.getObject("id"), "tag_key", rs.getString("tag_key"),
                "name", rs.getString("name"), "enabled", true, "sort_order", rs.getInt("sort_order")));
    }

    private List<Map<String, Object>> readFootprintResources(UUID footprintId) {
        return jdbc.query("""
                SELECT r.id, r.object_key, r.mime_type, fr.sort_order
                FROM footprint_resources fr JOIN resources r ON r.id = fr.resource_id
                WHERE fr.footprint_id = ? AND fr.deleted_at IS NULL
                ORDER BY fr.sort_order
                """, (rs, n) -> mapOf("id", rs.getObject("id"), "object_key", rs.getString("object_key"),
                "mime_type", rs.getString("mime_type"), "sort_order", rs.getInt("sort_order")), footprintId);
    }

    /** 执行查询并取第一条，无结果返回 null */
    private ContentRelease one(String sql, Object... args) {
        List<ContentRelease> rows = jdbc.query(sql, RELEASE_MAPPER, args);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private static ContentRelease mapRelease(ResultSet rs, int rowNum) throws SQLException {
        ContentRelease release = new ContentRelease();
        release.setId((UUID) rs.getObject("id"));
        release.setModuleKey(rs.getString("module_key"));
        release.setVersionNo(rs.getInt("version_no"));
        release.setState(rs.getString("state"));
        release.setPublishedBy((UUID) rs.getObject("published_by"));
        release.setSourceReleaseId((UUID) rs.getObject("source_release_id"));
        release.setPublishedAt(rs.getObject("published_at", OffsetDateTime.class));
        release.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        release.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
        release.setDeletedAt(rs.getObject("deleted_at", OffsetDateTime.class));
        return release;
    }

    /** 取 JSON 数组字段，优先 preferred 名，不是数组则回退到 fallback 名（兼容新旧字段命名） */
    private static JsonNode array(JsonNode root, String preferred, String fallback) {
        JsonNode result = root.path(preferred);
        return result.isArray() ? result : root.path(fallback);
    }

    private static Iterable<JsonNode> iterable(JsonNode node) {
        return node != null && node.isArray() ? node : List.of();
    }

    /** 读取行 id；快照中缺失（新增行）时生成随机 UUID */
    private static UUID uuid(JsonNode node, String field) {
        UUID value = nullableUuid(node, field);
        return value == null ? UUID.randomUUID() : value;
    }

    private static UUID nullableUuid(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = node.path(field).asText("").trim();
            if (!value.isEmpty()) return UUID.fromString(value);
        }
        return null;
    }

    /** 读取必填文本字段（优先 preferred 名，回退 fallback 名），缺失或空白时抛异常 */
    private static String key(JsonNode node, String preferred, String fallback) {
        String result = firstText(node, preferred, fallback);
        if (result == null || result.isBlank()) throw new IllegalArgumentException(preferred + " is required");
        return result;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private static String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private static int integer(JsonNode node, String field, int fallback) {
        return node.has(field) && node.path(field).canConvertToInt() ? node.path(field).asInt() : fallback;
    }

    private static double decimal(JsonNode node, String field, double fallback) {
        return node.has(field) && node.path(field).isNumber() ? node.path(field).asDouble() : fallback;
    }

    private static boolean bool(JsonNode node, String field, boolean fallback) {
        return node.has(field) ? node.path(field).asBoolean(fallback) : fallback;
    }

    private static LocalDate localDate(JsonNode node, String... fields) {
        String value = firstText(node, fields);
        return value == null ? null : LocalDate.parse(value);
    }

    private static List<UUID> uuidArray(ResultSet rs, String column) throws SQLException {
        java.sql.Array value = rs.getArray(column);
        if (value == null) return List.of();
        Object[] raw = (Object[]) value.getArray();
        return Arrays.stream(raw).map(item -> item instanceof UUID uuid ? uuid : UUID.fromString(item.toString())).toList();
    }

    /** 按空行把正文切分为段落列表 */
    private static List<String> splitParagraphs(String contents) {
        if (contents == null || contents.isBlank()) return List.of();
        return Arrays.stream(contents.split("(?:\\r?\\n){2,}"))
                .map(String::trim).filter(value -> !value.isEmpty()).toList();
    }

    /** 以键值对构建有序 Map，值为 null 的键跳过（保持输出 JSON 字段顺序稳定） */
    private static Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            if (pairs[index + 1] != null) result.put(String.valueOf(pairs[index]), pairs[index + 1]);
        }
        return result;
    }
}
