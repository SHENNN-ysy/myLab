package com.myblog.application.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 内容模块（版本化内容系统）相关的 DTO 集合，
 * 覆盖草稿保存、模块视图、历史版本与标签读写等场景。
 */
public final class ContentDtos {
    private ContentDtos() { }

    /** 保存草稿请求，data 为模块对应的草稿内容 JSON。 */
    public record SaveDraft(
            // 调用方已知的最近更新时间，用于乐观并发控制，不匹配则拒绝保存
            @JsonProperty("expected_updated_at") OffsetDateTime expectedUpdatedAt,
            Object data) { }

    /** 单个内容模块的聚合视图，同时携带草稿与已发布两份数据及版本号。 */
    public record ModuleView(
            @JsonProperty("module_key") String moduleKey,
            @JsonProperty("draft_release_id") UUID draftReleaseId,
            @JsonProperty("published_release_id") UUID publishedReleaseId,
            @JsonProperty("draft_data") Object draftData,
            @JsonProperty("published_data") Object publishedData,
            @JsonProperty("draft_version") Integer draftVersion,
            @JsonProperty("published_version") Integer publishedVersion,
            String status,
            @JsonProperty("updated_at") OffsetDateTime updatedAt,
            @JsonProperty("published_at") OffsetDateTime publishedAt) { }

    /** 内容历史版本视图，用于版本列表与回滚。 */
    public record VersionView(
            UUID id,
            @JsonProperty("module_key") String moduleKey,
            @JsonProperty("version_no") Integer versionNo,
            String state,
            Object data,
            // 该版本基于哪个历史版本创建（回滚场景），无来源时为 null
            @JsonProperty("source_release_id") UUID sourceReleaseId,
            @JsonProperty("published_at") OffsetDateTime publishedAt) { }

    /** 标签的写操作入参（新增或更新）。 */
    public record TagWrite(
            @JsonProperty("tag_key") String tagKey,
            String name,
            Boolean enabled,
            @JsonProperty("sort_order") Integer sortOrder) { }

    /** 标签列表出参。 */
    public record TagList(List<com.myblog.application.model.entity.MylabTag> tags) { }
}
