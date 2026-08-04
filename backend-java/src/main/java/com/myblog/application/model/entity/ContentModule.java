package com.myblog.application.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.myblog.infrastructure.persistence.handler.JsonbTypeHandler;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Data
@TableName(value = "content_modules", autoResultMap = true)
@Schema(name = "ContentModule", description = "内容模块草稿与线上快照")
public class ContentModule {

    @TableId(value = "module_key", type = IdType.INPUT)
    @Schema(description = "模块标识", example = "projects")
    private String moduleKey;

    @TableField(typeHandler = JsonbTypeHandler.class)
    @Schema(description = "当前草稿 JSON")
    private Object draftData;

    @TableField(typeHandler = JsonbTypeHandler.class)
    @Schema(description = "最近一次已发布 JSON")
    private Object publishedData;

    @Schema(description = "草稿版本号", example = "3")
    private Integer draftVersion;
    @Schema(description = "线上版本号", example = "2")
    private Integer publishedVersion;
    @Schema(description = "状态", allowableValues = {"draft", "published", "offline"}, example = "published")
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime publishedAt;
}
