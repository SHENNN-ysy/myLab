package com.myblog.application.model.entity;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class ContentRelease {
    private UUID id;
    private String moduleKey;
    private Integer versionNo;
    private String state;
    private UUID publishedBy;
    private UUID sourceReleaseId;
    private OffsetDateTime publishedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
