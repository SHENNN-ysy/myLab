package com.myblog.application.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 文件资源记录实体，对应 resources 表，
 * 保存对象存储中已上传文件的元数据，文件内容本体在对象存储中。
 */
@Data
@TableName("resources")
public class FileRecord {

    @TableId(type = IdType.ASSIGN_UUID)
    private UUID id;

    // 对象在存储桶中的完整键（含目录前缀）
    private String objectKey;
    private String bucket;
    private String originalName;
    private String mimeType;
    private Long size;
    // 上传者的用户 ID
    private UUID uploadedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
