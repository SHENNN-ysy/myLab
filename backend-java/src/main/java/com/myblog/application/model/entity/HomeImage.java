package com.myblog.application.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

/**
 * 首页轮播图片实体，对应 home_images 表。
 * 每条记录属于某个 content_releases 版本，按 sort_order 排序；
 * 图片内容本体在对象存储中，imageResourceId 引用 resources 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("home_images")
public class HomeImage extends BaseEntity {

    // 所属发布版本
    private UUID releaseId;

    // 图片资源引用（resources 表外键）
    private UUID imageResourceId;

    // 图片替代文本（对外字段名为 alt）
    private String altText;

    // CSS object-position，控制裁剪时的取景位置
    private String objectPosition;

    private Integer sortOrder;
}
