package com.myblog.application.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

/**
 * 足迹-资源关联实体，对应 footprint_resources 表。
 * 记录某条足迹引用的图片/文件资源，按 sort_order 排序。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("footprint_resources")
public class FootprintResource extends BaseEntity {

    // 所属足迹（footprints 表外键）
    private UUID footprintId;

    // 资源引用（resources 表外键）
    private UUID resourceId;

    private Integer sortOrder;
}
