package com.myblog.infrastructure.persistence.repository;

import com.myblog.application.model.entity.MylabTag;
import com.myblog.application.repository.MylabTagRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * 实验室（mylab）标签仓储：基于 {@link JdbcTemplate} 实现应用层 {@link MylabTagRepository} 端口，
 * 负责 mylab_tags 表的查询与维护，删除采用软删除（写 deleted_at）。
 */
@Repository
public class JdbcMylabTagRepository implements MylabTagRepository {
    private static final RowMapper<MylabTag> MAPPER = JdbcMylabTagRepository::map; // 结果集 → 实体
    private final JdbcTemplate jdbc;

    public JdbcMylabTagRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 查询全部未删除标签。
     *
     * @param includeDisabled 为 true 时包含已禁用标签，否则只返回启用中的
     */
    @Override
    public List<MylabTag> findAll(boolean includeDisabled) {
        String filter = includeDisabled ? "" : " AND enabled = TRUE";
        return jdbc.query("SELECT * FROM mylab_tags WHERE deleted_at IS NULL" + filter + " ORDER BY sort_order, tag_key", MAPPER);
    }

    /** 按 id 批量查询启用中的标签，入参为空时直接返回空列表 */
    @Override
    public List<MylabTag> findActiveByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) return List.of();
        String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        return jdbc.query("SELECT * FROM mylab_tags WHERE id IN (" + placeholders + ") AND enabled = TRUE AND deleted_at IS NULL",
                MAPPER, ids.toArray());
    }

    @Override
    public MylabTag findById(UUID id) {
        List<MylabTag> rows = jdbc.query("SELECT * FROM mylab_tags WHERE id = ? AND deleted_at IS NULL", MAPPER, id);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    /**
     * 校验 tag_key 或 name 是否已被占用（新增/编辑前的唯一性校验）。
     *
     * @param excludedId 编辑场景下需排除的自身 id，新增时传 null
     */
    @Override
    public boolean keyOrNameExists(String key, String name, UUID excludedId) {
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM mylab_tags
                WHERE deleted_at IS NULL AND (tag_key = ? OR name = ?) AND (?::uuid IS NULL OR id <> ?::uuid)
                """, Long.class, key, name, excludedId, excludedId);
        return count != null && count > 0;
    }

    @Override
    public void add(MylabTag tag) {
        jdbc.update("""
                INSERT INTO mylab_tags (id, tag_key, name, enabled, sort_order, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, tag.getId(), tag.getTagKey(), tag.getName(), tag.getEnabled(), tag.getSortOrder(),
                tag.getCreatedAt(), tag.getUpdatedAt());
    }

    @Override
    public void save(MylabTag tag) {
        jdbc.update("UPDATE mylab_tags SET tag_key = ?, name = ?, enabled = ?, sort_order = ?, updated_at = ? WHERE id = ? AND deleted_at IS NULL",
                tag.getTagKey(), tag.getName(), tag.getEnabled(), tag.getSortOrder(), tag.getUpdatedAt(), tag.getId());
    }

    /** 软删除标签；仅当记录存在且未删除时返回 true */
    @Override
    public boolean remove(UUID id) {
        return jdbc.update("UPDATE mylab_tags SET deleted_at = NOW(), updated_at = NOW() WHERE id = ? AND deleted_at IS NULL", id) == 1;
    }

    private static MylabTag map(ResultSet rs, int rowNum) throws SQLException {
        MylabTag tag = new MylabTag();
        tag.setId((UUID) rs.getObject("id"));
        tag.setTagKey(rs.getString("tag_key"));
        tag.setName(rs.getString("name"));
        tag.setEnabled(rs.getBoolean("enabled"));
        tag.setSortOrder(rs.getInt("sort_order"));
        tag.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        tag.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
        tag.setDeletedAt(rs.getObject("deleted_at", OffsetDateTime.class));
        return tag;
    }
}
