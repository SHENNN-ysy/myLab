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

@Repository
public class JdbcContentReleaseRepository implements ContentReleaseRepository {
    private static final ObjectMapper OM = JacksonObjectMapper.get();
    private static final RowMapper<ContentRelease> RELEASE_MAPPER = JdbcContentReleaseRepository::mapRelease;

    private final JdbcTemplate jdbc;

    public JdbcContentReleaseRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public ContentRelease findDraft(String moduleKey) {
        return one("SELECT * FROM content_releases WHERE module_key = ? AND state = 'DRAFT' AND deleted_at IS NULL", moduleKey);
    }

    @Override
    public ContentRelease findCurrent(String moduleKey) {
        return one("SELECT * FROM content_releases WHERE module_key = ? AND state IN ('PUBLISHED','OFFLINE') AND deleted_at IS NULL", moduleKey);
    }

    @Override
    public ContentRelease findPublished(String moduleKey) {
        return one("SELECT * FROM content_releases WHERE module_key = ? AND state = 'PUBLISHED' AND deleted_at IS NULL", moduleKey);
    }

    @Override
    public ContentRelease findVersion(String moduleKey, int versionNo) {
        return one("SELECT * FROM content_releases WHERE module_key = ? AND version_no = ? AND deleted_at IS NULL", moduleKey, versionNo);
    }

    @Override
    public List<ContentRelease> findVersions(String moduleKey) {
        return jdbc.query("SELECT * FROM content_releases WHERE module_key = ? AND state <> 'DRAFT' AND deleted_at IS NULL ORDER BY version_no DESC",
                RELEASE_MAPPER, moduleKey);
    }

    @Override
    public void lockModule(String moduleKey) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))",
                preparedStatement -> preparedStatement.setString(1, moduleKey),
                resultSet -> null);
    }

    @Override
    public int nextVersion(String moduleKey) {
        Integer result = jdbc.queryForObject("SELECT COALESCE(MAX(version_no), 0) + 1 FROM content_releases WHERE module_key = ?",
                Integer.class, moduleKey);
        return result == null ? 1 : result;
    }

    @Override
    public void add(ContentRelease release) {
        jdbc.update("""
                INSERT INTO content_releases
                    (id, module_key, version_no, state, source_release_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, release.getId(), release.getModuleKey(), release.getVersionNo(), release.getState(),
                release.getSourceReleaseId(), release.getCreatedAt(), release.getUpdatedAt());
    }

    @Override
    public boolean touchDraft(UUID releaseId, OffsetDateTime expectedUpdatedAt, OffsetDateTime nextUpdatedAt) {
        if (expectedUpdatedAt == null) {
            return jdbc.update("UPDATE content_releases SET updated_at = ? WHERE id = ? AND state = 'DRAFT' AND deleted_at IS NULL",
                    nextUpdatedAt, releaseId) == 1;
        }
        return jdbc.update("UPDATE content_releases SET updated_at = ? WHERE id = ? AND state = 'DRAFT' AND updated_at = ? AND deleted_at IS NULL",
                nextUpdatedAt, releaseId, expectedUpdatedAt) == 1;
    }

    @Override
    public void replaceData(ContentRelease release, Object data) {
        JsonNode root = OM.valueToTree(data);
        deleteReleaseData(release);
        switch (release.getModuleKey()) {
            case "skills" -> writeSkills(release.getId(), root.path("items"));
            case "footprints" -> writeFootprints(release.getId(), array(root, "details", "items"));
            case "hobbies" -> writeHobbies(release.getId(), root.path("cards"));
            case "vibe" -> writeVibeTools(release.getId(), root.path("tools"));
            case "mylab" -> writeMylabCards(release.getId(), array(root, "cards", "posts"));
            default -> throw new IllegalArgumentException("unknown module: " + release.getModuleKey());
        }
    }

    @Override
    public Object readData(ContentRelease release) {
        if (release == null) return null;
        return switch (release.getModuleKey()) {
            case "skills" -> Map.of("items", readSkills(release.getId()));
            case "footprints" -> Map.of("details", readFootprints(release.getId()));
            case "hobbies" -> Map.of("cards", readHobbies(release.getId()));
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

    @Override
    public void offline(ContentRelease current, OffsetDateTime now) {
        jdbc.update("UPDATE content_releases SET state = 'OFFLINE', updated_at = ? WHERE id = ? AND state = 'PUBLISHED'",
                now, current.getId());
    }

    @Override
    public void deleteDraft(ContentRelease draft) {
        jdbc.update("DELETE FROM content_releases WHERE id = ? AND state = 'DRAFT'", draft.getId());
    }

    private void deleteReleaseData(ContentRelease release) {
        String table = switch (release.getModuleKey()) {
            case "skills" -> "skills";
            case "footprints" -> "footprints";
            case "hobbies" -> "hobbies";
            case "vibe" -> "vibe_tools";
            case "mylab" -> "mylab_cards";
            default -> throw new IllegalArgumentException("unknown module: " + release.getModuleKey());
        };
        jdbc.update("DELETE FROM " + table + " WHERE release_id = ?", release.getId());
    }

    private void writeSkills(UUID releaseId, JsonNode items) {
        int order = 0;
        for (JsonNode item : iterable(items)) {
            jdbc.update("""
                    INSERT INTO skills
                        (id, release_id, skill_key, name, percentage, level_code, level_text, icon, bar_style, is_new, enabled, sort_order)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, uuid(item, "row_id"), releaseId, key(item, "skill_key", "id"), text(item, "name"),
                    integer(item, "percentage", 0), firstText(item, "level_code", "level"), text(item, "level_text"),
                    text(item, "icon"), text(item, "bar_style"), bool(item, "is_new", false),
                    bool(item, "enabled", true), integer(item, "sort_order", order++));
        }
    }

    private void writeFootprints(UUID releaseId, JsonNode items) {
        int order = 0;
        for (JsonNode item : iterable(items)) {
            UUID id = uuid(item, "row_id");
            String contents = text(item, "contents");
            if (contents == null && item.path("paragraphs").isArray()) {
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

    private void writeHobbies(UUID releaseId, JsonNode items) {
        int order = 0;
        for (JsonNode item : iterable(items)) {
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

    private List<Map<String, Object>> readSkills(UUID releaseId) {
        return jdbc.query("""
                SELECT id, skill_key, name, percentage, level_code, level_text, icon, bar_style, is_new, enabled, sort_order
                FROM skills WHERE release_id = ? AND deleted_at IS NULL ORDER BY sort_order, skill_key
                """, (rs, n) -> mapOf(
                "row_id", rs.getObject("id"), "id", rs.getString("skill_key"), "skill_key", rs.getString("skill_key"),
                "name", rs.getString("name"), "percentage", rs.getInt("percentage"),
                "level", rs.getString("level_code"), "level_code", rs.getString("level_code"),
                "level_text", rs.getString("level_text"), "icon", rs.getString("icon"),
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
                "resource_id", rs.getObject("resource_id"), "resource_object_key", rs.getString("resource_object_key")), releaseId);
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

    private static JsonNode array(JsonNode root, String preferred, String fallback) {
        JsonNode result = root.path(preferred);
        return result.isArray() ? result : root.path(fallback);
    }

    private static Iterable<JsonNode> iterable(JsonNode node) {
        return node != null && node.isArray() ? node : List.of();
    }

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

    private static List<String> splitParagraphs(String contents) {
        if (contents == null || contents.isBlank()) return List.of();
        return Arrays.stream(contents.split("(?:\\r?\\n){2,}"))
                .map(String::trim).filter(value -> !value.isEmpty()).toList();
    }

    private static Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            if (pairs[index + 1] != null) result.put(String.valueOf(pairs[index]), pairs[index + 1]);
        }
        return result;
    }
}
