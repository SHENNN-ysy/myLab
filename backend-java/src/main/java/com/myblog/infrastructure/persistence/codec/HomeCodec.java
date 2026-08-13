package com.myblog.infrastructure.persistence.codec;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.myblog.application.model.entity.FileRecord;
import com.myblog.application.model.entity.HomeImage;
import com.myblog.infrastructure.persistence.mapper.file.FileRecordMapper;
import com.myblog.infrastructure.persistence.mapper.home.HomeImageMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.iterable;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.mapOf;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.nullableUuid;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.text;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.uuid;

/**
 * home 模块编解码器：home_images 表与 JSON 快照之间的双向装配。
 * 读写走 MyBatis-Plus；级联软标记与物理清空保留原生 SQL（语义不由 @TableLogic 接管）。
 */
@Component
public class HomeCodec implements ModuleCodec {

    private final JdbcTemplate jdbc;
    private final HomeImageMapper homeImageMapper;
    private final FileRecordMapper fileRecordMapper;

    public HomeCodec(JdbcTemplate jdbc, HomeImageMapper homeImageMapper, FileRecordMapper fileRecordMapper) {
        this.jdbc = jdbc;
        this.homeImageMapper = homeImageMapper;
        this.fileRecordMapper = fileRecordMapper;
    }

    @Override
    public String moduleKey() {
        return "home";
    }

    @Override
    public void write(UUID releaseId, JsonNode root) {
        writeHomeImages(releaseId, root.path("images"));
    }

    @Override
    public Object read(UUID releaseId) {
        return Map.of("images", readHomeImages(releaseId));
    }

    @Override
    public void softDeleteData(UUID releaseId, OffsetDateTime now) {
        CodecSql.softDelete(jdbc, "home_images", "release_id = ?", now, releaseId);
    }

    @Override
    public void deleteData(UUID releaseId) {
        jdbc.update("DELETE FROM home_images WHERE release_id = ?", releaseId);
    }

    /** home 模块写入：JSON 快照逐行映射为实体插入，时间戳走数据库默认值 */
    private void writeHomeImages(UUID releaseId, JsonNode images) {
        int order = 0;
        for (JsonNode image : iterable(images)) {
            HomeImage entity = new HomeImage();
            entity.setId(uuid(image, "row_id"));
            entity.setReleaseId(releaseId);
            entity.setImageResourceId(nullableUuid(image, "image_resource_id"));
            entity.setAltText(text(image, "alt"));
            entity.setObjectPosition(Objects.requireNonNullElse(text(image, "object_position"), "50% 50%"));
            entity.setSortOrder(order++);
            homeImageMapper.insert(entity);
        }
    }

    /**
     * home 模块读取：按 release 查实体列表（软删除由 @TableLogic 自动过滤），
     * 再批量取出资源对象键——以两次简单查询替代原来的 LEFT JOIN resources，行为等价。
     */
    private List<Map<String, Object>> readHomeImages(UUID releaseId) {
        List<HomeImage> rows = homeImageMapper.selectList(
                Wrappers.<HomeImage>lambdaQuery()
                        .eq(HomeImage::getReleaseId, releaseId)
                        .orderByAsc(HomeImage::getSortOrder));
        List<UUID> resourceIds = rows.stream()
                .map(HomeImage::getImageResourceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, String> objectKeys = new HashMap<>();
        if (!resourceIds.isEmpty()) {
            for (FileRecord resource : fileRecordMapper.selectBatchIds(resourceIds)) {
                objectKeys.put(resource.getId(), resource.getObjectKey());
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (HomeImage row : rows) {
            result.add(mapOf(
                    "row_id", row.getId(), "image_resource_id", row.getImageResourceId(),
                    "image_object_key", row.getImageResourceId() == null ? null : objectKeys.get(row.getImageResourceId()),
                    "alt", row.getAltText(), "object_position", row.getObjectPosition(),
                    "sort_order", row.getSortOrder()));
        }
        return result;
    }
}
