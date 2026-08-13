package com.myblog.infrastructure.persistence.codec;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.myblog.application.model.entity.FileRecord;
import com.myblog.application.model.entity.MylabCard;
import com.myblog.application.model.entity.MylabCardTag;
import com.myblog.application.model.entity.MylabResource;
import com.myblog.infrastructure.persistence.mapper.file.FileRecordMapper;
import com.myblog.infrastructure.persistence.mapper.mylab.MylabCardMapper;
import com.myblog.infrastructure.persistence.mapper.mylab.MylabCardTagMapper;
import com.myblog.infrastructure.persistence.mapper.mylab.MylabResourceMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.array;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.bool;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.firstText;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.integer;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.iterable;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.key;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.localDate;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.mapOf;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.nullableUuid;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.uuid;

/**
 * mylab 模块编解码器：mylab_cards/mylab_card_tags/mylab_resources 三表
 * 与 JSON 快照之间的双向装配。
 * 读写走 MyBatis-Plus；级联软标记与物理清空保留原生 SQL（语义不由 @TableLogic 接管）。
 * 注意：mylab 的全局标签（mylab_tags）不属于版本快照，由主仓储的 readActiveTags 装配，不在本类。
 */
@Component
public class MylabCodec implements ModuleCodec {

    private final JdbcTemplate jdbc;
    private final MylabCardMapper mylabCardMapper;
    private final MylabCardTagMapper mylabCardTagMapper;
    private final MylabResourceMapper mylabResourceMapper;
    private final FileRecordMapper fileRecordMapper;

    public MylabCodec(JdbcTemplate jdbc, MylabCardMapper mylabCardMapper,
                      MylabCardTagMapper mylabCardTagMapper, MylabResourceMapper mylabResourceMapper,
                      FileRecordMapper fileRecordMapper) {
        this.jdbc = jdbc;
        this.mylabCardMapper = mylabCardMapper;
        this.mylabCardTagMapper = mylabCardTagMapper;
        this.mylabResourceMapper = mylabResourceMapper;
        this.fileRecordMapper = fileRecordMapper;
    }

    @Override
    public String moduleKey() {
        return "mylab";
    }

    @Override
    public void write(UUID releaseId, JsonNode root) {
        writeMylabCards(releaseId, array(root, "cards", "posts"));
    }

    @Override
    public Object read(UUID releaseId) {
        // 仅版本化数据（cards）；全局 tags 由主仓储装配在前面
        return Map.of("cards", readMylabCards(releaseId));
    }

    @Override
    public void softDeleteData(UUID releaseId, OffsetDateTime now) {
        String parent = "card_id IN (SELECT id FROM mylab_cards WHERE release_id = ?)";
        CodecSql.softDelete(jdbc, "mylab_card_tags", parent, now, releaseId);
        CodecSql.softDelete(jdbc, "mylab_resources", parent, now, releaseId);
        CodecSql.softDelete(jdbc, "mylab_cards", "release_id = ?", now, releaseId);
    }

    @Override
    public void deleteData(UUID releaseId) {
        jdbc.update("DELETE FROM mylab_cards WHERE release_id = ?", releaseId);
    }

    /**
     * mylab 模块写入：JSON 快照拆为卡片主记录 + 标签关联/资源子表实体插入，
     * 时间戳走数据库默认值。
     */
    private void writeMylabCards(UUID releaseId, JsonNode items) {
        int order = 0;
        for (JsonNode item : iterable(items)) {
            UUID id = uuid(item, "row_id");
            // 未显式给出 card_type 时按 post_key 前缀推断：project- 开头视为项目卡片
            String type = Objects.requireNonNullElse(firstText(item, "card_type"),
                    key(item, "post_key", "id").startsWith("project-") ? "PROJECT" : "ARTICLE").toUpperCase();
            Integer projectOrder = "PROJECT".equals(type)
                    ? integer(item, "project_show_order", integer(item, "sort_order", order)) : null;
            MylabCard entity = new MylabCard();
            entity.setId(id);
            entity.setReleaseId(releaseId);
            entity.setPostKey(key(item, "post_key", "id"));
            entity.setCardTitle(firstText(item, "card_title", "title"));
            entity.setCardSummary(firstText(item, "card_summary", "summary"));
            entity.setPostDate(localDate(item, "post_date", "date"));
            entity.setEnabled(bool(item, "enabled", true));
            entity.setSortOrder(integer(item, "sort_order", order++));
            entity.setCardType(type);
            entity.setProjectShowOrder(projectOrder);
            entity.setProjectContents(firstText(item, "project_contents", "project_content"));
            mylabCardMapper.insert(entity);

            int tagOrder = 0;
            for (JsonNode tagId : iterable(item.path("tag_ids"))) {
                MylabCardTag link = new MylabCardTag();
                link.setId(UUID.randomUUID());
                link.setCardId(id);
                link.setTagId(UUID.fromString(tagId.asText()));
                link.setSortOrder(tagOrder++);
                mylabCardTagMapper.insert(link);
            }
            UUID imageId = nullableUuid(item, "image_resource_id");
            UUID contentId = nullableUuid(item, "content_resource_id");
            if (imageId != null || contentId != null) {
                MylabResource resource = new MylabResource();
                resource.setId(UUID.randomUUID());
                resource.setCardId(id);
                resource.setImageResourceId(imageId);
                resource.setContentResourceId(contentId);
                mylabResourceMapper.insert(resource);
            }
        }
    }

    /**
     * mylab 模块读取：先查卡片（软删除由 @TableLogic 自动过滤），再批量取出标签关联、
     * 资源关联与资源对象键——以多次简单查询替代原来的 ARRAY 子查询与两层 LEFT JOIN，行为等价。
     */
    private List<Map<String, Object>> readMylabCards(UUID releaseId) {
        List<MylabCard> rows = mylabCardMapper.selectList(
                Wrappers.<MylabCard>lambdaQuery()
                        .eq(MylabCard::getReleaseId, releaseId)
                        .orderByAsc(MylabCard::getSortOrder)
                        .orderByAsc(MylabCard::getPostKey));
        List<UUID> cardIds = rows.stream().map(MylabCard::getId).toList();

        // 标签关联：按 card_id、sort_order 全局排序后分组，保持每张卡片内的标签顺序
        Map<UUID, List<UUID>> tagIdsByCard = new HashMap<>();
        Map<UUID, MylabResource> resourceByCard = new HashMap<>();
        if (!cardIds.isEmpty()) {
            for (MylabCardTag link : mylabCardTagMapper.selectList(
                    Wrappers.<MylabCardTag>lambdaQuery()
                            .in(MylabCardTag::getCardId, cardIds)
                            .orderByAsc(MylabCardTag::getCardId)
                            .orderByAsc(MylabCardTag::getSortOrder))) {
                tagIdsByCard.computeIfAbsent(link.getCardId(), key -> new ArrayList<>()).add(link.getTagId());
            }
            for (MylabResource resource : mylabResourceMapper.selectList(
                    Wrappers.<MylabResource>lambdaQuery().in(MylabResource::getCardId, cardIds))) {
                resourceByCard.put(resource.getCardId(), resource);
            }
        }
        List<UUID> resourceIds = resourceByCard.values().stream()
                .flatMap(resource -> java.util.stream.Stream.of(resource.getImageResourceId(), resource.getContentResourceId()))
                .filter(Objects::nonNull).distinct().toList();
        Map<UUID, String> objectKeys = new HashMap<>();
        if (!resourceIds.isEmpty()) {
            for (FileRecord resource : fileRecordMapper.selectBatchIds(resourceIds)) {
                objectKeys.put(resource.getId(), resource.getObjectKey());
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (MylabCard row : rows) {
            MylabResource resource = resourceByCard.get(row.getId());
            UUID imageId = resource == null ? null : resource.getImageResourceId();
            UUID contentId = resource == null ? null : resource.getContentResourceId();
            result.add(mapOf(
                    "row_id", row.getId(), "id", row.getPostKey(), "post_key", row.getPostKey(),
                    "title", row.getCardTitle(), "card_title", row.getCardTitle(),
                    "summary", row.getCardSummary(), "card_summary", row.getCardSummary(),
                    "date", row.getPostDate(), "post_date", row.getPostDate(),
                    "tag_ids", tagIdsByCard.getOrDefault(row.getId(), List.of()),
                    "enabled", row.getEnabled(),
                    "sort_order", row.getSortOrder(), "card_type", row.getCardType(),
                    "project_show_order", row.getProjectShowOrder(),
                    "project_contents", row.getProjectContents(),
                    "image_resource_id", imageId,
                    "image_object_key", imageId == null ? null : objectKeys.get(imageId),
                    "content_resource_id", contentId,
                    "content_object_key", contentId == null ? null : objectKeys.get(contentId)));
        }
        return result;
    }
}
