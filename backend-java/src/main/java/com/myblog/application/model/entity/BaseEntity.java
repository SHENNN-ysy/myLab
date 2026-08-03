package com.myblog.application.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Base fields shared by every soft-deletable entity.
 */
@Data
public abstract class BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private UUID id;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    @TableLogic(value = "NULL", delval = "now()")
    private OffsetDateTime deletedAt;
}
