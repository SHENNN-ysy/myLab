package com.myblog.infrastructure.persistence.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myblog.application.model.entity.ContentRelease;
import com.myblog.application.repository.ContentReleaseRepository;
import com.myblog.common.json.JacksonObjectMapper;
import com.myblog.infrastructure.persistence.codec.ModuleCodec;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.mapOf;

/**
 * 版本化内容发布仓储：实现应用层 {@link ContentReleaseRepository} 端口。
 * 本类只保留两部分职责：
 * 1. content_releases（发布记录）的草稿/发布/下线状态机、版本号分配与咨询锁；
 * 2. 模块分发——各内容模块（home、about、skills、footprints、hobbies、vibe、mylab）数据表的
 *    整包读写由 codec 包按模块承载（{@link ModuleCodec} 实现注册表，按 moduleKey 分发），
 *    即"JSON 快照 ⇄ 关系表"双向装配器已下沉到 codec；本类只负责删旧数据 + 调用 codec、
 *    以及把 mylab 的全局标签（不属于版本快照）装配到读取结果中。
 * <p>
 * 与校验层的分工：业务校验（必填字段、结构合法性）由应用层在调用前完成；
 * codec 层只做字段级的新旧命名兼容与默认值填充，不做业务规则判断。
 * <p>
 * 持久化方式：模块数据表读写已全面 MyBatis-Plus 化（实体 + Mapper，软删除过滤由
 * {@code @TableLogic} 自动接管，见 codec 包）。保留 {@link JdbcTemplate} 的场景：
 * 本类的 content_releases 状态机、readActiveTags（mylab_tags 表归属 JdbcMylabTagRepository，
 * 不属于版本快照）、咨询锁（lockModule）与版本号分配（nextVersion）——这两处是 ORM 表达不了的
 * 数据库特性；codec 侧的级联软标记（softDeleteData）与物理清空（deleteData）同样保留原生 SQL，
 * 语义不能由 @TableLogic 接管。
 */
@Repository
public class JdbcContentReleaseRepository implements ContentReleaseRepository {
    private static final ObjectMapper OM = JacksonObjectMapper.get(); // 全局共享的 Jackson 实例
    private static final RowMapper<ContentRelease> RELEASE_MAPPER = JdbcContentReleaseRepository::mapRelease; // 发布记录行映射器

    private final JdbcTemplate jdbc;
    private final Map<String, ModuleCodec> codecs; // 模块编解码器注册表（按 moduleKey 分发）

    public JdbcContentReleaseRepository(JdbcTemplate jdbc, List<ModuleCodec> codecList) {
        this.jdbc = jdbc;
        Map<String, ModuleCodec> registry = new HashMap<>();
        for (ModuleCodec codec : codecList) {
            registry.put(codec.moduleKey(), codec);
        }
        this.codecs = Map.copyOf(registry);
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

    /** 查询模块全部未删除版本，按最近发布时间或更新时间倒序 */
    @Override
    public List<ContentRelease> findVersions(String moduleKey) {
        return jdbc.query("""
                SELECT * FROM content_releases
                WHERE module_key = ? AND deleted_at IS NULL
                ORDER BY COALESCE(published_at, updated_at, created_at) DESC, version_no DESC
                """,
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
                    (id, module_key, version_no, version_name, version_description, state,
                     source_release_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, release.getId(), release.getModuleKey(), release.getVersionNo(),
                release.getVersionName(), release.getVersionDescription(), release.getState(), release.getSourceReleaseId(),
                release.getCreatedAt(), release.getUpdatedAt());
    }

    /**
     * 更新草稿的 updated_at 时间戳；传入 expectedUpdatedAt 时作为乐观锁条件，
     * 防止并发编辑互相覆盖。
     *
     * @return 更新成功返回 true；期望时间戳不匹配（草稿已被他人改动）返回 false
     */
    @Override
    public boolean updateDraft(UUID releaseId, OffsetDateTime expectedUpdatedAt, OffsetDateTime nextUpdatedAt,
                               String versionName, String versionDescription) {
        if (expectedUpdatedAt == null) {
            return jdbc.update("""
                    UPDATE content_releases
                    SET version_name = ?, version_description = ?, updated_at = ?
                    WHERE id = ? AND state = 'DRAFT' AND deleted_at IS NULL
                    """, versionName, versionDescription, nextUpdatedAt, releaseId) == 1;
        }
        return jdbc.update("""
                UPDATE content_releases
                SET version_name = ?, version_description = ?, updated_at = ?
                WHERE id = ? AND state = 'DRAFT' AND updated_at = ? AND deleted_at IS NULL
                """, versionName, versionDescription, nextUpdatedAt, releaseId, expectedUpdatedAt) == 1;
    }

    /**
     * 用 JSON 快照整体替换某次发布的模块数据：先清空该 release 的旧数据，再由模块 codec 拆表写入。
     *
     * @throws IllegalArgumentException 模块名无法识别时抛出
     */
    @Override
    public void replaceData(ContentRelease release, Object data) {
        JsonNode root = OM.valueToTree(data);
        ModuleCodec codec = codec(release.getModuleKey());
        codec.deleteData(release.getId());
        codec.write(release.getId(), root);
    }

    /** 读取某次发布的模块数据并聚合为 JSON 结构（与 replaceData 的写入结构互逆） */
    @Override
    public Object readData(ContentRelease release) {
        if (release == null) return null;
        ModuleCodec codec = codecs.get(release.getModuleKey());
        if (codec == null) return Map.of();
        Object versioned = codec.read(release.getId());
        if ("mylab".equals(release.getModuleKey()) && versioned instanceof Map<?, ?> versionedMap) {
            // mylab 的全局标签不属于版本快照，由本类装配在版本数据之前（保持 tags 在前的键序）
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("tags", readActiveTags());
            versionedMap.forEach((key, value) -> result.put(String.valueOf(key), value));
            return result;
        }
        return versioned;
    }

    /**
     * 发布草稿：把当前生效版本归档，再将草稿置为已发布。
     *
     * @throws IllegalStateException 草稿状态在发布期间被并发修改时抛出
     */
    @Override
    public void publish(ContentRelease draft, ContentRelease current, UUID actorId, OffsetDateTime now) {
        if (current != null) {
            // 先把旧当前版本归档；同一模块至多一个 PUBLISHED/OFFLINE 由部分唯一索引
            // uq_content_release_current（state IN ('PUBLISHED','OFFLINE')）在数据库层兜底
            jdbc.update("UPDATE content_releases SET state = 'ARCHIVED', updated_at = ? WHERE id = ? AND state IN ('PUBLISHED','OFFLINE')",
                    now, current.getId());
        }
        // WHERE 中的 state = 'DRAFT' 即乐观并发控制（CAS）：草稿在发布期间被改动则影响行数为 0
        int updated = jdbc.update("""
                UPDATE content_releases
                SET state = 'PUBLISHED', published_by = ?, published_at = ?, updated_at = ?
                WHERE id = ? AND state = 'DRAFT' AND deleted_at IS NULL
                """, actorId, now, now, draft.getId());
        if (updated != 1) throw new IllegalStateException("draft state changed while publishing");
    }

    /** 将历史记录原地恢复为草稿；原草稿保留为归档记录，不复制任何模块数据。 */
    @Override
    public void restoreAsDraft(ContentRelease source, ContentRelease currentDraft, OffsetDateTime now) {
        if (currentDraft != null) {
            int archived = jdbc.update("""
                    UPDATE content_releases SET state = 'ARCHIVED', updated_at = ?
                    WHERE id = ? AND state = 'DRAFT' AND deleted_at IS NULL
                    """, now, currentDraft.getId());
            if (archived != 1) throw new IllegalStateException("draft state changed while restoring");
        }
        int restored = jdbc.update("""
                UPDATE content_releases SET state = 'DRAFT', updated_at = ?
                WHERE id = ? AND state IN ('ARCHIVED','OFFLINE') AND deleted_at IS NULL
                """, now, source.getId());
        if (restored != 1) throw new IllegalStateException("source state changed while restoring");
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
     * 软删除历史版本：模块数据行由 codec 级联打 deleted_at 标记（解除资源引用），
     * 最后标记 content_releases 本体。所有 UPDATE 带 deleted_at IS NULL 条件，保证幂等。
     */
    @Override
    public void softDeleteVersion(ContentRelease release, OffsetDateTime now) {
        UUID releaseId = release.getId();
        codec(release.getModuleKey()).softDeleteData(releaseId, now);
        jdbc.update("UPDATE content_releases SET deleted_at = ?, updated_at = ? WHERE id = ? AND deleted_at IS NULL",
                now, now, releaseId);
    }

    /**
     * 读取全局启用的 mylab 标签（不属于版本快照，各版本共享）。
     * 保留 JdbcTemplate：mylab_tags 表的归属仓储是 JdbcMylabTagRepository（仍为 JDBC），
     * 此处另建 MP Mapper 会造成同一张表的双轨归属，留待该仓储迁移时一并处理。
     */
    private List<Map<String, Object>> readActiveTags() {
        return jdbc.query("""
                SELECT id, tag_key, name, enabled, sort_order
                FROM mylab_tags WHERE enabled = TRUE AND deleted_at IS NULL ORDER BY sort_order, tag_key
                """, (rs, n) -> mapOf("id", rs.getObject("id"), "tag_key", rs.getString("tag_key"),
                "name", rs.getString("name"), "enabled", true, "sort_order", rs.getInt("sort_order")));
    }

    /** 按模块名取编解码器，无法识别时抛异常（与原 switch default 分支语义一致） */
    private ModuleCodec codec(String moduleKey) {
        ModuleCodec codec = codecs.get(moduleKey);
        if (codec == null) throw new IllegalArgumentException("unknown module: " + moduleKey);
        return codec;
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
        release.setVersionName(rs.getString("version_name"));
        release.setVersionDescription(rs.getString("version_description"));
        release.setState(rs.getString("state"));
        release.setPublishedBy((UUID) rs.getObject("published_by"));
        release.setSourceReleaseId((UUID) rs.getObject("source_release_id"));
        release.setPublishedAt(rs.getObject("published_at", OffsetDateTime.class));
        release.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        release.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
        release.setDeletedAt(rs.getObject("deleted_at", OffsetDateTime.class));
        return release;
    }
}
