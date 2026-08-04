package com.myblog.application.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.myblog.infrastructure.persistence.handler.JsonbTypeHandler;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@TableName(value = "content_publications", autoResultMap = true)
@Schema(name = "ContentPublication", description = "不可变的内容发布历史快照")
public class ContentPublication {
    @TableId(type = IdType.ASSIGN_UUID)
    private UUID id;
    @Schema(description = "模块标识", example = "projects")
    private String moduleKey;
    @Schema(description = "发布版本号", example = "2")
    private Integer version;
    @TableField(typeHandler = JsonbTypeHandler.class)
    @Schema(description = "发布时的完整内容 JSON")
    private Object data;
    private UUID publishedBy;
    private OffsetDateTime publishedAt;
}
