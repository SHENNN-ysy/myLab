package com.myblog.application.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

/**
 * 技能条目实体，对应 skills 表。
 * 每条记录属于某个 content_releases 版本，按 sort_order、skill_key 排序；
 * 图标内容本体在对象存储中，iconResourceId 引用 resources 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("skills")
public class Skill extends BaseEntity {

    // 所属发布版本
    private UUID releaseId;

    // 技能唯一标识（对外同时暴露为 id 与 skill_key）
    private String skillKey;

    private String name;

    // 熟练度百分比（0-100）
    private Integer percentage;

    // 等级编码（novice/competent/proficient，对外同时暴露为 level 与 level_code）
    private String levelCode;

    // 等级展示文案
    private String levelText;

    // 图标资源引用（resources 表外键）
    private UUID iconResourceId;

    // 进度条样式
    private String barStyle;

    // 是否为新增技能标记
    private Boolean isNew;

    private Boolean enabled;

    private Integer sortOrder;
}
