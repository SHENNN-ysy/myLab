package com.myblog.application.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@TableName("users")
public class User {

    @TableId(type = IdType.ASSIGN_UUID)
    private UUID id;

    private String username;
    private String passwordHash;
    private String role;
    private Boolean isActive;
    private OffsetDateTime lastLoginAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @TableLogic(value = "NULL", delval = "now()")
    private OffsetDateTime deletedAt;
}
