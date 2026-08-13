package com.myblog.application.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 实验室卡片实体，对应 mylab_cards 表。
 * 每条记录属于某个 content_releases 版本，按 sort_order、post_key 排序；
 * card_type 为 PROJECT 时携带 project_show_order/project_contents（表约束兜底），
 * 标签与资源分别通过 mylab_card_tags、mylab_resources 关联表挂载。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mylab_cards")
public class MylabCard extends BaseEntity {

    // 所属发布版本
    private UUID releaseId;

    // 文章/项目唯一标识（对外同时暴露为 id 与 post_key）
    private String postKey;

    // 卡片标题（对外同时暴露为 title 与 card_title）
    private String cardTitle;

    // 卡片摘要（对外同时暴露为 summary 与 card_summary）
    private String cardSummary;

    // 发布日期（对外同时暴露为 date 与 post_date）
    private LocalDate postDate;

    private Boolean enabled;

    private Integer sortOrder;

    // 卡片类型（PROJECT/ARTICLE）
    private String cardType;

    // 项目卡片在首页的展示顺序（仅 PROJECT 类型非空）
    private Integer projectShowOrder;

    // 项目正文（仅 PROJECT 类型非空）
    private String projectContents;
}
