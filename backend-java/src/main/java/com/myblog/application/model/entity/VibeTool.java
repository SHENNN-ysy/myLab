package com.myblog.application.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

/**
 * Vibe 工具条目实体，对应 vibe_tools 表。
 * 每条记录属于某个 content_releases 版本，按 sort_order、tool_key 排序。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vibe_tools")
public class VibeTool extends BaseEntity {

    // 所属发布版本
    private UUID releaseId;

    // 工具唯一标识（对外同时暴露为 id 与 tool_key）
    private String toolKey;

    private String name;

    // 使用频率百分比（0-100）
    private Integer percentage;

    private String description;

    private Boolean enabled;

    private Integer sortOrder;
}
