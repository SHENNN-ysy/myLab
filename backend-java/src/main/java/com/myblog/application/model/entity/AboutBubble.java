package com.myblog.application.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

/**
 * 关于页气泡实体，对应 about_bubbles 表。
 * 每个气泡属于某个 about_contents 记录，按 sort_order 排序。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("about_bubbles")
public class AboutBubble extends BaseEntity {

    // 所属关于页内容（about_contents 表外键）
    private UUID aboutContentId;

    // 气泡文本（对外字段名为 text）
    private String bubbleText;

    // 气泡尺寸（big/mid，对外字段名为 size）
    private String bubbleSize;

    private String backgroundColor;
    private String textColor;
    private String glowColor;

    private Integer sortOrder;
}
