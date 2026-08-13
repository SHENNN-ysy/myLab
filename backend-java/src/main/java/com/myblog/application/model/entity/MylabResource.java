package com.myblog.application.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

/**
 * 实验室卡片资源实体，对应 mylab_resources 表。
 * 每张卡片至多一条（部分唯一索引兜底），封面图与正文文件至少存在一个（表约束兜底）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mylab_resources")
public class MylabResource extends BaseEntity {

    // 所属卡片（mylab_cards 表外键）
    private UUID cardId;

    // 封面图资源引用（resources 表外键）
    private UUID imageResourceId;

    // 正文文件资源引用（resources 表外键）
    private UUID contentResourceId;
}
