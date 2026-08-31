package com.myblog.application.model.entity;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 内容发布版本实体：内容模块的版本记录，是版本化内容系统（草稿/发布/回滚）的核心数据。
 * 同一模块同一时刻至多一条 DRAFT 和一条 PUBLISHED：发布会把旧线上版本归档（ARCHIVED）、
 * 草稿转为新线上版本；下线把 PUBLISHED 置为 OFFLINE；再编辑生成更高版本号的新草稿，
 * 恢复历史版本时直接把目标版本切换为当前草稿，原草稿转为归档版本。
 */
@Data
public class ContentRelease {
    private UUID id;
    // 内容模块标识（如首页、关于页等）
    private String moduleKey;
    // 同一模块内递增的版本号
    private Integer versionNo;
    // 便于管理员识别版本的名称
    private String versionName;
    // 记录本次内容变更目的和范围的描述
    private String versionDescription;
    // 版本状态：DRAFT（草稿）、PUBLISHED（线上）、ARCHIVED（历史归档）、OFFLINE（已下线）
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
