package com.myblog.application.model.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Outbound representation of a stored file (with optional presigned url). */
public record FileOutVO(
        UUID id,
        @com.fasterxml.jackson.annotation.JsonProperty("object_key") String objectKey,
        String bucket,
        @com.fasterxml.jackson.annotation.JsonProperty("original_name") String originalName,
        @com.fasterxml.jackson.annotation.JsonProperty("mime_type") String mimeType,
        Long size,
        @com.fasterxml.jackson.annotation.JsonProperty("created_at") OffsetDateTime createdAt,
        String url) {
}
