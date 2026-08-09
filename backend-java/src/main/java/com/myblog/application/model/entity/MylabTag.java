package com.myblog.application.model.entity;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class MylabTag {
    private UUID id;
    private String tagKey;
    private String name;
    private Boolean enabled;
    private Integer sortOrder;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
