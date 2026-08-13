package com.myblog.infrastructure.persistence.codec;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.myblog.application.model.entity.VibeTool;
import com.myblog.infrastructure.persistence.mapper.vibe.VibeToolMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.bool;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.integer;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.iterable;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.key;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.mapOf;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.text;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.uuid;

/**
 * vibe 模块编解码器：vibe_tools 表与 JSON 快照之间的双向装配。
 * 读写走 MyBatis-Plus；级联软标记与物理清空保留原生 SQL（语义不由 @TableLogic 接管）。
 */
@Component
public class VibeCodec implements ModuleCodec {

    private final JdbcTemplate jdbc;
    private final VibeToolMapper vibeToolMapper;

    public VibeCodec(JdbcTemplate jdbc, VibeToolMapper vibeToolMapper) {
        this.jdbc = jdbc;
        this.vibeToolMapper = vibeToolMapper;
    }

    @Override
    public String moduleKey() {
        return "vibe";
    }

    @Override
    public void write(UUID releaseId, JsonNode root) {
        writeVibeTools(releaseId, root.path("tools"));
    }

    @Override
    public Object read(UUID releaseId) {
        return Map.of("tools", readVibeTools(releaseId));
    }

    @Override
    public void softDeleteData(UUID releaseId, OffsetDateTime now) {
        CodecSql.softDelete(jdbc, "vibe_tools", "release_id = ?", now, releaseId);
    }

    @Override
    public void deleteData(UUID releaseId) {
        jdbc.update("DELETE FROM vibe_tools WHERE release_id = ?", releaseId);
    }

    /** vibe 模块写入：JSON 快照逐行映射为实体插入，时间戳走数据库默认值 */
    private void writeVibeTools(UUID releaseId, JsonNode items) {
        int order = 0;
        for (JsonNode item : iterable(items)) {
            VibeTool entity = new VibeTool();
            entity.setId(uuid(item, "row_id"));
            entity.setReleaseId(releaseId);
            entity.setToolKey(key(item, "tool_key", "id"));
            entity.setName(text(item, "name"));
            entity.setPercentage(integer(item, "percentage", 0));
            entity.setDescription(text(item, "description"));
            entity.setEnabled(bool(item, "enabled", true));
            entity.setSortOrder(integer(item, "sort_order", order++));
            vibeToolMapper.insert(entity);
        }
    }

    /** vibe 模块读取：按 release 查实体列表（软删除由 @TableLogic 自动过滤） */
    private List<Map<String, Object>> readVibeTools(UUID releaseId) {
        List<VibeTool> rows = vibeToolMapper.selectList(
                Wrappers.<VibeTool>lambdaQuery()
                        .eq(VibeTool::getReleaseId, releaseId)
                        .orderByAsc(VibeTool::getSortOrder)
                        .orderByAsc(VibeTool::getToolKey));
        List<Map<String, Object>> result = new ArrayList<>();
        for (VibeTool row : rows) {
            result.add(mapOf(
                    "row_id", row.getId(), "id", row.getToolKey(), "tool_key", row.getToolKey(),
                    "name", row.getName(), "percentage", row.getPercentage(),
                    "description", row.getDescription(), "enabled", row.getEnabled(),
                    "sort_order", row.getSortOrder()));
        }
        return result;
    }
}
