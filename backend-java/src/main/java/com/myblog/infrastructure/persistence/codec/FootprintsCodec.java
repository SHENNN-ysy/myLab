package com.myblog.infrastructure.persistence.codec;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.myblog.application.model.entity.FileRecord;
import com.myblog.application.model.entity.Footprint;
import com.myblog.application.model.entity.FootprintResource;
import com.myblog.infrastructure.persistence.mapper.file.FileRecordMapper;
import com.myblog.infrastructure.persistence.mapper.footprints.FootprintMapper;
import com.myblog.infrastructure.persistence.mapper.footprints.FootprintResourceMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.array;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.bool;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.integer;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.iterable;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.key;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.mapOf;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.splitParagraphs;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.text;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.uuid;

/**
 * footprints 模块编解码器：footprints/footprint_resources 两表与 JSON 快照之间的双向装配。
 * 读写走 MyBatis-Plus；级联软标记与物理清空保留原生 SQL（语义不由 @TableLogic 接管）。
 */
@Component
public class FootprintsCodec implements ModuleCodec {

    private final JdbcTemplate jdbc;
    private final FootprintMapper footprintMapper;
    private final FootprintResourceMapper footprintResourceMapper;
    private final FileRecordMapper fileRecordMapper;

    public FootprintsCodec(JdbcTemplate jdbc, FootprintMapper footprintMapper,
                           FootprintResourceMapper footprintResourceMapper, FileRecordMapper fileRecordMapper) {
        this.jdbc = jdbc;
        this.footprintMapper = footprintMapper;
        this.footprintResourceMapper = footprintResourceMapper;
        this.fileRecordMapper = fileRecordMapper;
    }

    @Override
    public String moduleKey() {
        return "footprints";
    }

    @Override
    public void write(UUID releaseId, JsonNode root) {
        writeFootprints(releaseId, array(root, "details", "items"));
    }

    @Override
    public Object read(UUID releaseId) {
        return Map.of("details", readFootprints(releaseId));
    }

    @Override
    public void softDeleteData(UUID releaseId, OffsetDateTime now) {
        CodecSql.softDelete(jdbc, "footprint_resources",
                "footprint_id IN (SELECT id FROM footprints WHERE release_id = ?)", now, releaseId);
        CodecSql.softDelete(jdbc, "footprints", "release_id = ?", now, releaseId);
    }

    @Override
    public void deleteData(UUID releaseId) {
        jdbc.update("DELETE FROM footprints WHERE release_id = ?", releaseId);
    }

    /**
     * footprints 模块写入：JSON 快照逐行映射为实体插入，时间戳走数据库默认值；
     * 每条足迹的 resource_ids 同步写入 footprint_resources 关联表。
     */
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
            Footprint entity = new Footprint();
            entity.setId(id);
            entity.setReleaseId(releaseId);
            entity.setCityKey(key(item, "city_key", "id"));
            entity.setTitle(text(item, "title"));
            entity.setSummary(text(item, "summary"));
            entity.setContents(contents);
            entity.setEnabled(bool(item, "enabled", true));
            entity.setSortOrder(integer(item, "sort_order", order++));
            footprintMapper.insert(entity);

            int resourceOrder = 0;
            for (JsonNode node : iterable(item.path("resource_ids"))) {
                FootprintResource link = new FootprintResource();
                link.setId(UUID.randomUUID());
                link.setFootprintId(id);
                link.setResourceId(UUID.fromString(node.asText()));
                link.setSortOrder(resourceOrder++);
                footprintResourceMapper.insert(link);
            }
        }
    }

    /** footprints 模块读取：按 release 查实体列表（软删除由 @TableLogic 自动过滤），每条足迹再装配其资源列表 */
    private List<Map<String, Object>> readFootprints(UUID releaseId) {
        List<Footprint> rows = footprintMapper.selectList(
                Wrappers.<Footprint>lambdaQuery()
                        .eq(Footprint::getReleaseId, releaseId)
                        .orderByAsc(Footprint::getSortOrder)
                        .orderByAsc(Footprint::getCityKey));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Footprint row : rows) {
            String contents = row.getContents();
            Map<String, Object> item = mapOf(
                    "row_id", row.getId(), "id", row.getCityKey(), "city_key", row.getCityKey(),
                    "title", row.getTitle(), "summary", row.getSummary(), "contents", contents,
                    "paragraphs", splitParagraphs(contents), "enabled", row.getEnabled(),
                    "sort_order", row.getSortOrder());
            item.put("resources", readFootprintResources(row.getId()));
            result.add(item);
        }
        return result;
    }

    /**
     * 读取某条足迹的资源列表：先查关联表（软删除由 @TableLogic 自动过滤），
     * 再批量取出资源记录——以两次简单查询替代原来的 JOIN resources，行为等价（INNER JOIN 语义：
     * 资源行缺失的关联记录跳过）。
     */
    private List<Map<String, Object>> readFootprintResources(UUID footprintId) {
        List<FootprintResource> links = footprintResourceMapper.selectList(
                Wrappers.<FootprintResource>lambdaQuery()
                        .eq(FootprintResource::getFootprintId, footprintId)
                        .orderByAsc(FootprintResource::getSortOrder));
        List<UUID> resourceIds = links.stream()
                .map(FootprintResource::getResourceId)
                .distinct()
                .toList();
        Map<UUID, FileRecord> resources = new HashMap<>();
        if (!resourceIds.isEmpty()) {
            for (FileRecord resource : fileRecordMapper.selectBatchIds(resourceIds)) {
                resources.put(resource.getId(), resource);
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (FootprintResource link : links) {
            FileRecord resource = resources.get(link.getResourceId());
            if (resource == null) continue; // INNER JOIN 语义：资源行不存在则整条关联记录不出现
            result.add(mapOf("id", resource.getId(), "object_key", resource.getObjectKey(),
                    "mime_type", resource.getMimeType(), "sort_order", link.getSortOrder()));
        }
        return result;
    }
}
