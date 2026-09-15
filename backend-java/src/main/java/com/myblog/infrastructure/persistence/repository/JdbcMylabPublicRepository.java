package com.myblog.infrastructure.persistence.repository;

import com.myblog.application.repository.MylabPublicRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** MyLab 公开读取仓储：列表使用字段投影，避免读取全部 Markdown 正文。 */
@Repository
public class JdbcMylabPublicRepository implements MylabPublicRepository {

    private final JdbcTemplate jdbc;

    public JdbcMylabPublicRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<String, Object> readProjects(UUID releaseId) {
        List<Map<String, Object>> cards = cards(releaseId, null, false, true, false, false, null);
        attachTagNames(cards);
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("cards", cards);
        return root;
    }

    @Override
    public Map<String, Object> readLatest(UUID releaseId, int limit) {
        // 仅启用卡片参与 LIMIT 截取，避免最新 5 条被停用卡片占位后再被公开化过滤
        List<Map<String, Object>> cards = cards(releaseId, null, false, false, false, true, limit);
        attachTagNames(cards);
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("cards", cards);
        return root;
    }

    @Override
    public Map<String, Object> readSummary(UUID releaseId) {
        return root(cards(releaseId, null, false, false, true, false, null));
    }

    @Override
    public Map<String, Object> readDetail(UUID releaseId, String postKey) {
        List<Map<String, Object>> cards = cards(releaseId, postKey, true, false, true, false, null);
        // 详情只缓存卡片本体：标签字典仅列表页解析 tag_ids 需要，避免每个 post_key 冗余一份全量标签
        if (cards.isEmpty()) return null;
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("cards", cards);
        return detail;
    }

    private Map<String, Object> root(List<Map<String, Object>> cards) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("tags", activeTags());
        root.put("cards", cards);
        return root;
    }

    private List<Map<String, Object>> cards(UUID releaseId, String postKey, boolean includeMarkdown,
                                             boolean projectsOnly, boolean includeTagIds,
                                             boolean enabledOnly, Integer limit) {
        String markdownColumn = includeMarkdown ? ", mc.markdown_content\n" : "\n";
        String postFilter = postKey == null ? "" : " AND mc.post_key = ?";
        String projectFilter = projectsOnly
                ? " AND mc.card_type = 'PROJECT' AND mc.project_show_order IS NOT NULL" : "";
        String enabledFilter = enabledOnly ? " AND mc.enabled = TRUE" : "";
        String orderBy = projectsOnly
                ? " ORDER BY mc.project_show_order, mc.post_date DESC NULLS LAST, mc.post_key"
                : " ORDER BY mc.post_date DESC NULLS LAST, mc.post_key";
        String limitClause = limit == null ? "" : " LIMIT ?";
        List<Object> arguments = new ArrayList<>();
        arguments.add(releaseId);
        if (postKey != null) arguments.add(postKey);
        if (limit != null) arguments.add(limit);
        List<Map<String, Object>> cards = jdbc.query("""
                SELECT mc.id, mc.post_key, mc.card_title, mc.card_summary, mc.post_date,
                       mc.enabled, mc.card_type, mc.project_show_order,
                       mc.project_contents, mr.image_resource_id, image.object_key AS image_object_key
                """ + markdownColumn + """
                FROM mylab_cards mc
                LEFT JOIN mylab_resources mr ON mr.card_id = mc.id AND mr.deleted_at IS NULL
                LEFT JOIN resources image ON image.id = mr.image_resource_id AND image.deleted_at IS NULL
                WHERE mc.release_id = ? AND mc.deleted_at IS NULL
                """ + postFilter + projectFilter + enabledFilter + orderBy + limitClause,
                (rs, rowNum) -> card(rs, includeMarkdown), arguments.toArray());
        if (includeTagIds) attachTagIds(cards);
        return cards;
    }

    private void attachTagIds(List<Map<String, Object>> cards) {
        if (cards.isEmpty()) return;
        List<UUID> cardIds = cards.stream().map(card -> (UUID) card.get("row_id")).toList();
        String placeholders = String.join(",", java.util.Collections.nCopies(cardIds.size(), "?"));
        Map<UUID, List<UUID>> tagIds = new HashMap<>();
        RowCallbackHandler collectTag = rs -> tagIds
                .computeIfAbsent(rs.getObject("card_id", UUID.class), ignored -> new ArrayList<>())
                .add(rs.getObject("tag_id", UUID.class));
        jdbc.query("SELECT card_id, tag_id FROM mylab_card_tags WHERE deleted_at IS NULL AND card_id IN ("
                        + placeholders + ") ORDER BY card_id, sort_order",
                collectTag, cardIds.toArray());
        cards.forEach(card -> card.put("tag_ids", tagIds.getOrDefault(card.get("row_id"), List.of())));
    }

    /** 只查询所选项目实际引用的有效标签，并按卡片内标签顺序直接展开为名称数组。 */
    private void attachTagNames(List<Map<String, Object>> cards) {
        if (cards.isEmpty()) return;
        List<UUID> cardIds = cards.stream().map(card -> (UUID) card.get("row_id")).toList();
        String placeholders = String.join(",", java.util.Collections.nCopies(cardIds.size(), "?"));
        Map<UUID, List<String>> tagNames = new HashMap<>();
        RowCallbackHandler collectTag = rs -> tagNames
                .computeIfAbsent(rs.getObject("card_id", UUID.class), ignored -> new ArrayList<>())
                .add(rs.getString("name"));
        jdbc.query("""
                SELECT link.card_id, tag.name
                FROM mylab_card_tags link
                JOIN mylab_tags tag ON tag.id = link.tag_id
                WHERE link.deleted_at IS NULL
                  AND tag.deleted_at IS NULL
                  AND tag.enabled = TRUE
                  AND link.card_id IN (
                """ + placeholders + ") ORDER BY link.card_id, link.sort_order",
                collectTag, cardIds.toArray());
        cards.forEach(card -> card.put("tags", tagNames.getOrDefault(card.get("row_id"), List.of())));
    }

    private List<Map<String, Object>> activeTags() {
        return jdbc.query("""
                SELECT id, tag_key, name, enabled
                FROM mylab_tags
                WHERE enabled = TRUE AND deleted_at IS NULL
                ORDER BY name, tag_key
                """, (rs, rowNum) -> {
            Map<String, Object> tag = new LinkedHashMap<>();
            tag.put("id", rs.getObject("id", UUID.class));
            tag.put("tag_key", rs.getString("tag_key"));
            tag.put("name", rs.getString("name"));
            tag.put("enabled", true);
            return tag;
        });
    }

    private static Map<String, Object> card(ResultSet rs, boolean includeMarkdown) throws SQLException {
        Map<String, Object> card = new LinkedHashMap<>();
        String postKey = rs.getString("post_key");
        card.put("row_id", rs.getObject("id", UUID.class));
        card.put("id", postKey);
        card.put("post_key", postKey);
        card.put("title", rs.getString("card_title"));
        card.put("card_title", rs.getString("card_title"));
        card.put("summary", rs.getString("card_summary"));
        card.put("card_summary", rs.getString("card_summary"));
        card.put("date", rs.getObject("post_date"));
        card.put("post_date", rs.getObject("post_date"));
        card.put("enabled", rs.getBoolean("enabled"));
        card.put("card_type", rs.getString("card_type"));
        card.put("project_show_order", rs.getObject("project_show_order"));
        card.put("project_contents", rs.getString("project_contents"));
        card.put("image_resource_id", rs.getObject("image_resource_id", UUID.class));
        card.put("image_object_key", rs.getString("image_object_key"));
        if (includeMarkdown) card.put("markdown_content", rs.getString("markdown_content"));
        return card;
    }
}
