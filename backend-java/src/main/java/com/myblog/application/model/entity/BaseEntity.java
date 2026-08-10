package com.myblog.application.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 所有支持逻辑删除的实体共享的基础字段（主键与审计时间）。
 */
@Data
public abstract class BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private UUID id;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    // 逻辑删除标记：未删除为 NULL，删除时写入当前时间
    @TableLogic(value = "NULL", delval = "now()")
    private OffsetDateTime deletedAt;
}
