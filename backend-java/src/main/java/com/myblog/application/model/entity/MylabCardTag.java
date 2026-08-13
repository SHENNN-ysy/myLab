package com.myblog.application.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

/**
 * 实验室卡片-标签关联实体，对应 mylab_card_tags 表。
 * 记录某张卡片引用的标签，按 sort_order 排序。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mylab_card_tags")
public class MylabCardTag extends BaseEntity {

    // 所属卡片（mylab_cards 表外键）
    private UUID cardId;

    // 标签引用（mylab_tags 表外键）
    private UUID tagId;

    private Integer sortOrder;
}
