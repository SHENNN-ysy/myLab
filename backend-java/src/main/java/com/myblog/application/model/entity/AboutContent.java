package com.myblog.application.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

/**
 * 关于页内容实体，对应 about_contents 表。
 * 每个 content_releases 版本至多一条（部分唯一索引兜底）；
 * 头像内容本体在对象存储中，avatarResourceId 引用 resources 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("about_contents")
public class AboutContent extends BaseEntity {

    // 所属发布版本
    private UUID releaseId;

    // 个人资料区标题
    private String profileTitle;

    // 头像资源引用（resources 表外键）
    private UUID avatarResourceId;

    // 头像替代文本
    private String avatarAlt;

    // 开场白与结束语
    private String intro;
    private String outro;

    // "配料"区标题与描述
    private String ingredientsTitle;
    private String ingredientsDescription;
}
