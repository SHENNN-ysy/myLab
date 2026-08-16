package com.myblog.infrastructure.persistence.codec;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.myblog.application.model.entity.FileRecord;
import com.myblog.application.model.entity.Skill;
import com.myblog.infrastructure.persistence.mapper.file.FileRecordMapper;
import com.myblog.infrastructure.persistence.mapper.skills.SkillMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.bool;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.firstText;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.integer;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.iterable;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.key;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.mapOf;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.nullableUuid;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.text;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.uuid;

/**
 * skills 模块编解码器：skills 表与 JSON 快照之间的双向装配。
 * 读写走 MyBatis-Plus；级联软标记与物理清空保留原生 SQL（语义不由 @TableLogic 接管）。
 */
@Component
public class SkillsCodec implements ModuleCodec {

    private final JdbcTemplate jdbc;
    private final SkillMapper skillMapper;
    private final FileRecordMapper fileRecordMapper;

    public SkillsCodec(JdbcTemplate jdbc, SkillMapper skillMapper, FileRecordMapper fileRecordMapper) {
        this.jdbc = jdbc;
        this.skillMapper = skillMapper;
        this.fileRecordMapper = fileRecordMapper;
    }

    @Override
    public String moduleKey() {
        return "skills";
    }

    @Override
    public void write(UUID releaseId, JsonNode root) {
        writeSkills(releaseId, root.path("items"));
    }

    @Override
    public Object read(UUID releaseId) {
        return Map.of("items", readSkills(releaseId));
    }

    @Override
    public void softDeleteData(UUID releaseId, OffsetDateTime now) {
        CodecSql.softDelete(jdbc, "skills", "release_id = ?", now, releaseId);
    }

    @Override
    public void deleteData(UUID releaseId) {
        jdbc.update("DELETE FROM skills WHERE release_id = ?", releaseId);
    }

    /** skills 模块写入：JSON 快照逐行映射为实体插入，时间戳走数据库默认值 */
    private void writeSkills(UUID releaseId, JsonNode items) {
        int order = 0;
        for (JsonNode item : iterable(items)) {
            Skill entity = new Skill();
            entity.setId(uuid(item, "row_id"));
            entity.setReleaseId(releaseId);
            entity.setSkillKey(key(item, "skill_key", "id"));
            entity.setName(text(item, "name"));
            entity.setPercentage(integer(item, "percentage", 0));
            entity.setLevelCode(firstText(item, "level_code", "level"));
            entity.setLevelText(text(item, "level_text"));
            entity.setIconResourceId(nullableUuid(item, "icon_resource_id"));
            entity.setBarStyle(text(item, "bar_style"));
            entity.setIsNew(bool(item, "is_new", false));
            entity.setEnabled(bool(item, "enabled", true));
            entity.setSortOrder(integer(item, "sort_order", order++));
            skillMapper.insert(entity);
        }
    }

    /**
     * skills 模块读取：按 release 查实体列表（软删除由 @TableLogic 自动过滤），
     * 再批量取出图标资源对象键——以两次简单查询替代原来的 LEFT JOIN resources，行为等价。
     */
    private List<Map<String, Object>> readSkills(UUID releaseId) {
        List<Skill> rows = skillMapper.selectList(
                Wrappers.<Skill>lambdaQuery()
                        .eq(Skill::getReleaseId, releaseId)
                        .orderByAsc(Skill::getSortOrder)
                        .orderByAsc(Skill::getSkillKey));
        List<UUID> iconIds = rows.stream()
                .map(Skill::getIconResourceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, String> iconObjectKeys = new HashMap<>();
        if (!iconIds.isEmpty()) {
            for (FileRecord resource : fileRecordMapper.selectBatchIds(iconIds)) {
                iconObjectKeys.put(resource.getId(), resource.getObjectKey());
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Skill row : rows) {
            result.add(mapOf(
                    "row_id", row.getId(), "id", row.getSkillKey(), "skill_key", row.getSkillKey(),
                    "name", row.getName(), "percentage", row.getPercentage(),
                    "level", row.getLevelCode(), "level_code", row.getLevelCode(),
                    "level_text", row.getLevelText(), "icon_resource_id", row.getIconResourceId(),
                    "icon_object_key", row.getIconResourceId() == null ? null : iconObjectKeys.get(row.getIconResourceId()),
                    "bar_style", row.getBarStyle(), "is_new", row.getIsNew(),
                    "enabled", row.getEnabled(), "sort_order", row.getSortOrder()));
        }
        return result;
    }
}
