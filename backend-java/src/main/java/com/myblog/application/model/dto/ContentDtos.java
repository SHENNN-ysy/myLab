package com.myblog.application.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class ContentDtos {
    private ContentDtos() { }

    public record SaveDraft(
            @JsonProperty("expected_updated_at") OffsetDateTime expectedUpdatedAt,
            Object data) { }

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

    public record VersionView(
            UUID id,
            @JsonProperty("module_key") String moduleKey,
            @JsonProperty("version_no") Integer versionNo,
            String state,
            Object data,
            @JsonProperty("source_release_id") UUID sourceReleaseId,
            @JsonProperty("published_at") OffsetDateTime publishedAt) { }

    public record TagWrite(
            @JsonProperty("tag_key") String tagKey,
            String name,
            Boolean enabled,
            @JsonProperty("sort_order") Integer sortOrder) { }

    public record TagList(List<com.myblog.application.model.entity.MylabTag> tags) { }
}
