package com.myblog.application.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@TableName("files")
public class FileRecord {

    @TableId(type = IdType.ASSIGN_UUID)
    private UUID id;

    private String objectKey;
    private String bucket;
    private String originalName;
    private String mimeType;
    private Long size;
    private UUID uploadedBy;
    private Boolean isDeleted;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
