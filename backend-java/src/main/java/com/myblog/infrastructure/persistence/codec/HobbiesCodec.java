package com.myblog.infrastructure.persistence.codec;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.myblog.application.model.entity.FileRecord;
import com.myblog.application.model.entity.Hobby;
import com.myblog.application.model.entity.HobbyResource;
import com.myblog.application.model.entity.HobbyTimePoint;
import com.myblog.application.model.entity.HobbyTimeTag;
import com.myblog.infrastructure.persistence.mapper.file.FileRecordMapper;
import com.myblog.infrastructure.persistence.mapper.hobbies.HobbyMapper;
import com.myblog.infrastructure.persistence.mapper.hobbies.HobbyResourceMapper;
import com.myblog.infrastructure.persistence.mapper.hobbies.HobbyTimePointMapper;
import com.myblog.infrastructure.persistence.mapper.hobbies.HobbyTimeTagMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.bool;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.decimal;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.integer;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.iterable;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.key;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.mapOf;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.nullableUuid;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.text;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.uuid;

/**
 * hobbies 模块编解码器：hobbies/hobby_resources/hobby_time_tags/hobby_time_points 四表
 * 与 JSON 快照之间的双向装配。
 * 读写走 MyBatis-Plus；级联软标记与物理清空保留原生 SQL（语义不由 @TableLogic 接管）。
 */
@Component
public class HobbiesCodec implements ModuleCodec {

    private final JdbcTemplate jdbc;
    private final HobbyMapper hobbyMapper;
    private final HobbyResourceMapper hobbyResourceMapper;
    private final HobbyTimeTagMapper hobbyTimeTagMapper;
    private final HobbyTimePointMapper hobbyTimePointMapper;
    private final FileRecordMapper fileRecordMapper;

    public HobbiesCodec(JdbcTemplate jdbc, HobbyMapper hobbyMapper, HobbyResourceMapper hobbyResourceMapper,
                        HobbyTimeTagMapper hobbyTimeTagMapper, HobbyTimePointMapper hobbyTimePointMapper,
                        FileRecordMapper fileRecordMapper) {
        this.jdbc = jdbc;
        this.hobbyMapper = hobbyMapper;
        this.hobbyResourceMapper = hobbyResourceMapper;
        this.hobbyTimeTagMapper = hobbyTimeTagMapper;
        this.hobbyTimePointMapper = hobbyTimePointMapper;
        this.fileRecordMapper = fileRecordMapper;
    }

    @Override
    public String moduleKey() {
        return "hobbies";
    }

    @Override
    public void write(UUID releaseId, JsonNode root) {
        writeHobbies(releaseId, root);
    }

    @Override
    public Object read(UUID releaseId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cards", readHobbies(releaseId));
        result.put("time_tags", readHobbyTimeTags(releaseId));
        result.put("time_points", readHobbyTimePoints(releaseId));
        return result;
    }

    @Override
    public void softDeleteData(UUID releaseId, OffsetDateTime now) {
        CodecSql.softDelete(jdbc, "hobby_resources",
                "hobby_id IN (SELECT id FROM hobbies WHERE release_id = ?)", now, releaseId);
        CodecSql.softDelete(jdbc, "hobbies", "release_id = ?", now, releaseId);
        CodecSql.softDelete(jdbc, "hobby_time_tags", "release_id = ?", now, releaseId);
        CodecSql.softDelete(jdbc, "hobby_time_points", "release_id = ?", now, releaseId);
    }

    @Override
    public void deleteData(UUID releaseId) {
        jdbc.update("DELETE FROM hobby_time_points WHERE release_id = ?", releaseId);
        jdbc.update("DELETE FROM hobby_time_tags WHERE release_id = ?", releaseId);
        jdbc.update("DELETE FROM hobbies WHERE release_id = ?", releaseId);
    }

    /**
     * hobbies 模块写入：JSON 快照拆为爱好卡片（含资源关联）、时间标签、时间数据点
     * 三组实体插入，时间戳走数据库默认值。
     */
    private void writeHobbies(UUID releaseId, JsonNode root) {
        int order = 0;
        for (JsonNode item : iterable(root.path("cards"))) {
            UUID id = uuid(item, "row_id");
            Hobby entity = new Hobby();
            entity.setId(id);
            entity.setReleaseId(releaseId);
            entity.setHobbyKey(key(item, "hobby_key", "id"));
            entity.setTitle(text(item, "title"));
            entity.setDescription(text(item, "description"));
            entity.setEnabled(bool(item, "enabled", true));
            entity.setSortOrder(integer(item, "sort_order", order++));
            hobbyMapper.insert(entity);

            UUID resourceId = nullableUuid(item, "resource_id", "image_resource_id");
            if (resourceId != null) {
                HobbyResource link = new HobbyResource();
                link.setId(UUID.randomUUID());
                link.setHobbyId(id);
                link.setResourceId(resourceId);
                hobbyResourceMapper.insert(link);
            }
        }
        order = 0;
        for (JsonNode tag : iterable(root.path("time_tags"))) {
            HobbyTimeTag entity = new HobbyTimeTag();
            entity.setId(uuid(tag, "row_id"));
            entity.setReleaseId(releaseId);
            entity.setDataKey(text(tag, "data_key"));
            entity.setName(text(tag, "name"));
            entity.setColor(text(tag, "color"));
            entity.setLabelX(integer(tag, "label_x", 0));
            entity.setLabelY(integer(tag, "label_y", 0));
            entity.setLabelScale(BigDecimal.valueOf(decimal(tag, "label_scale", 1.0)));
            entity.setEnabled(bool(tag, "enabled", true));
            entity.setSortOrder(order++);
            hobbyTimeTagMapper.insert(entity);
        }
        for (JsonNode point : iterable(root.path("time_points"))) {
            JsonNode values = point.path("values");
            HobbyTimePoint entity = new HobbyTimePoint();
            entity.setId(uuid(point, "row_id"));
            entity.setReleaseId(releaseId);
            entity.setAge(integer(point, "age", -2));
            entity.setHobby1(BigDecimal.valueOf(decimal(values, "爱好1", 0)));
            entity.setHobby2(BigDecimal.valueOf(decimal(values, "爱好2", 0)));
            entity.setHobby3(BigDecimal.valueOf(decimal(values, "爱好3", 0)));
            entity.setHobby4(BigDecimal.valueOf(decimal(values, "爱好4", 0)));
            entity.setHobby5(BigDecimal.valueOf(decimal(values, "爱好5", 0)));
            hobbyTimePointMapper.insert(entity);
        }
    }

    /**
     * hobbies 模块读取：先查爱好卡片（软删除由 @TableLogic 自动过滤），再批量取出资源关联与
     * 资源对象键——以三次简单查询替代原来的两层 LEFT JOIN，行为等价。
     */
    private List<Map<String, Object>> readHobbies(UUID releaseId) {
        List<Hobby> rows = hobbyMapper.selectList(
                Wrappers.<Hobby>lambdaQuery()
                        .eq(Hobby::getReleaseId, releaseId)
                        .orderByAsc(Hobby::getSortOrder)
                        .orderByAsc(Hobby::getHobbyKey));
        List<UUID> hobbyIds = rows.stream().map(Hobby::getId).toList();
        Map<UUID, UUID> resourceIdByHobby = new HashMap<>();
        if (!hobbyIds.isEmpty()) {
            for (HobbyResource link : hobbyResourceMapper.selectList(
                    Wrappers.<HobbyResource>lambdaQuery().in(HobbyResource::getHobbyId, hobbyIds))) {
                resourceIdByHobby.put(link.getHobbyId(), link.getResourceId());
            }
        }
        List<UUID> resourceIds = resourceIdByHobby.values().stream().distinct().toList();
        Map<UUID, String> objectKeys = new HashMap<>();
        if (!resourceIds.isEmpty()) {
            for (FileRecord resource : fileRecordMapper.selectBatchIds(resourceIds)) {
                objectKeys.put(resource.getId(), resource.getObjectKey());
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Hobby row : rows) {
            UUID resourceId = resourceIdByHobby.get(row.getId());
            // 原 SQL 取的是 LEFT JOIN resources 的 r.id：资源行缺失时 resource_id 也为 null（FK RESTRICT 下实际不会发生）
            UUID joinedResourceId = resourceId != null && objectKeys.containsKey(resourceId) ? resourceId : null;
            result.add(mapOf(
                    "row_id", row.getId(), "id", row.getHobbyKey(), "hobby_key", row.getHobbyKey(),
                    "title", row.getTitle(), "description", row.getDescription(),
                    "enabled", row.getEnabled(), "sort_order", row.getSortOrder(),
                    "image_resource_id", joinedResourceId,
                    "image_object_key", joinedResourceId == null ? null : objectKeys.get(joinedResourceId)));
        }
        return result;
    }

    private List<Map<String, Object>> readHobbyTimeTags(UUID releaseId) {
        List<HobbyTimeTag> rows = hobbyTimeTagMapper.selectList(
                Wrappers.<HobbyTimeTag>lambdaQuery()
                        .eq(HobbyTimeTag::getReleaseId, releaseId)
                        .orderByAsc(HobbyTimeTag::getSortOrder)
                        .orderByAsc(HobbyTimeTag::getDataKey));
        List<Map<String, Object>> result = new ArrayList<>();
        for (HobbyTimeTag row : rows) {
            result.add(mapOf(
                    "row_id", row.getId(), "data_key", row.getDataKey(), "name", row.getName(),
                    "color", row.getColor(), "label_x", row.getLabelX(), "label_y", row.getLabelY(),
                    "label_scale", row.getLabelScale(), "enabled", row.getEnabled(),
                    "sort_order", row.getSortOrder()));
        }
        return result;
    }

    private List<Map<String, Object>> readHobbyTimePoints(UUID releaseId) {
        List<HobbyTimePoint> rows = hobbyTimePointMapper.selectList(
                Wrappers.<HobbyTimePoint>lambdaQuery()
                        .eq(HobbyTimePoint::getReleaseId, releaseId)
                        .orderByAsc(HobbyTimePoint::getAge));
        List<Map<String, Object>> result = new ArrayList<>();
        for (HobbyTimePoint row : rows) {
            // 数据库列名为英文 hobby1~5（V6 迁移），对外 JSON 键仍为中文「爱好1~爱好5」
            Map<String, Object> values = mapOf(
                    "爱好1", row.getHobby1(), "爱好2", row.getHobby2(),
                    "爱好3", row.getHobby3(), "爱好4", row.getHobby4(),
                    "爱好5", row.getHobby5());
            result.add(mapOf("row_id", row.getId(), "age", row.getAge(), "values", values));
        }
        return result;
    }
}
