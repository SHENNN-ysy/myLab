package com.myblog.application.model.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 存储文件的出参视图，供后台文件管理接口返回。
 * url 仅对可公开访问的图片直接给出访问地址，其余类型为 null，需调 presign 接口换取限时签名地址。
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
        // 仅公开图片直接附带访问地址，其余类型为 null（需走 presign 接口）
        String url) {
}
