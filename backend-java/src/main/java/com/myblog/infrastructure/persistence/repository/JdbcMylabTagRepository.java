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

@Repository
public class JdbcMylabTagRepository implements MylabTagRepository {
    private static final RowMapper<MylabTag> MAPPER = JdbcMylabTagRepository::map;
    private final JdbcTemplate jdbc;

    public JdbcMylabTagRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<MylabTag> findAll(boolean includeDisabled) {
        String filter = includeDisabled ? "" : " AND enabled = TRUE";
        return jdbc.query("SELECT * FROM mylab_tags WHERE deleted_at IS NULL" + filter + " ORDER BY sort_order, tag_key", MAPPER);
    }

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
