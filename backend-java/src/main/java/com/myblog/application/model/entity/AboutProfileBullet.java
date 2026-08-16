package com.myblog.application.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

/**
 * 关于页个人资料要点实体，对应 about_profile_bullets 表。
 * 每条要点属于某个 about_contents 记录，按 sort_order 排序。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("about_profile_bullets")
public class AboutProfileBullet extends BaseEntity {

    // 所属关于页内容（about_contents 表外键）
    private UUID aboutContentId;

    // 要点文本
    private String contents;

    private Integer sortOrder;
}
