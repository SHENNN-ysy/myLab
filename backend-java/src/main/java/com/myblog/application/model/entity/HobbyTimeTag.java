package com.myblog.application.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 爱好时间分布标签实体，对应 hobby_time_tags 表。
 * 每条记录属于某个 content_releases 版本，data_key 固定为「爱好1~爱好5」之一；
 * label_x/label_y/label_scale 描述雷达图上的标签摆放位置与缩放。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hobby_time_tags")
public class HobbyTimeTag extends BaseEntity {

    // 所属发布版本
    private UUID releaseId;

    // 数据键（爱好1~爱好5，与 JSON 契约同名）
    private String dataKey;

    private String name;

    // 标签颜色（#RRGGBB）
    private String color;

    private Integer labelX;
    private Integer labelY;

    // 标签缩放比例（0.5-3.0）
    private BigDecimal labelScale;

    private Boolean enabled;

    private Integer sortOrder;
}
