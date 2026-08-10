package com.myblog.application.model.entity;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 内容发布版本实体：内容模块每次保存草稿或发布都会生成一条版本记录，
 * 是版本化内容系统（草稿/发布/回滚）的核心数据。
 */
@Data
public class ContentRelease {
    private UUID id;
    // 内容模块标识（如首页、关于页等）
    private String moduleKey;
    // 同一模块内递增的版本号
    private Integer versionNo;
    // 版本状态，如草稿（draft）或已发布（published）
    private String state;
    // 发布操作人的用户 ID
    private UUID publishedBy;
    // 该版本基于哪个历史版本创建（回滚场景），无来源时为 null
    private UUID sourceReleaseId;
    private OffsetDateTime publishedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
