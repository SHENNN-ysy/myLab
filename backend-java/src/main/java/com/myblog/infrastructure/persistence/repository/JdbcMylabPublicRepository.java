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

    /** 首页项目区：仅取参与展示（project_show_order 非空）的 PROJECT 卡片，按展示位次排序，标签已在仓储层展开为名称。 */
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

    /** MyLab 全量列表：卡片只带 tag_ids，另附全局启用标签字典，由服务层按字典展开为标签名。 */
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

    /**
     * 按开关组合查询卡片：includeMarkdown 决定是否投影 Markdown 正文，projectsOnly 限定首页展示项目，
     * includeTagIds 决定是否批量回填 tag_ids，enabledOnly 与 limit 配合实现"最新 N 条启用卡片"。
     * SQL 片段均为硬编码常量，仅 releaseId/postKey/limit 走占位符，参数顺序必须与占位符出现顺序一致。
     */
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

    /** 批量回填每张卡片引用的 tag_id 列表（保持卡片内标签顺序），供全量摘要按标签字典展开为名称。 */
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

    /** 全局启用标签字典：供列表页把卡片上的 tag_ids 解析为标签名。 */
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

    /**
     * 行映射：row_id 是行级主键，用于关联标签与行级标识，随草稿落库前由服务层统一剥离；
     * 标题、摘要、日期同时输出两套键名（如 title 与 card_title），兼容消费方的双键回退读取。
     */
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
