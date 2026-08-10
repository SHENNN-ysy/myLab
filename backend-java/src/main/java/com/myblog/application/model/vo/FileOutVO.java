package com.myblog.application.model.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 存储文件的出参视图，url 在对象存储场景下为预签名访问地址。
 */
public record FileOutVO(
        UUID id,
        // 对象在存储桶中的完整键（含目录前缀）
        @com.fasterxml.jackson.annotation.JsonProperty("object_key") String objectKey,
        String directory,
        String bucket,
        @com.fasterxml.jackson.annotation.JsonProperty("original_name") String originalName,
        @com.fasterxml.jackson.annotation.JsonProperty("mime_type") String mimeType,
        Long size,
        @com.fasterxml.jackson.annotation.JsonProperty("created_at") OffsetDateTime createdAt,
        // 可公开访问的地址，通常为预签名 URL
        String url) {
}
