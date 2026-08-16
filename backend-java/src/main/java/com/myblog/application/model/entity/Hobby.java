package com.myblog.application.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

/**
 * 爱好卡片实体，对应 hobbies 表。
 * 每条记录属于某个 content_releases 版本，按 sort_order、hobby_key 排序；
 * 配图资源通过 hobby_resources 关联表引用 resources 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hobbies")
public class Hobby extends BaseEntity {

    // 所属发布版本
    private UUID releaseId;

    // 爱好唯一标识（对外同时暴露为 id 与 hobby_key）
    private String hobbyKey;

    private String title;

    private String description;

    private Boolean enabled;

    private Integer sortOrder;
}
