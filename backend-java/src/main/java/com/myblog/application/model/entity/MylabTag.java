package com.myblog.application.model.entity;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 标签实体，供内容模块（如实验室）打标与前台展示使用。
 */
@Data
public class MylabTag {
    private UUID id;
    // 标签的唯一标识键，供程序引用
    private String tagKey;
    private String name;
    // 是否启用，停用的标签不在前台展示
    private Boolean enabled;
    // 展示排序权重，越小越靠前
    private Integer sortOrder;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
