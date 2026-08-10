package com.myblog.application.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 管理员用户实体，对应 users 表，承载后台账号体系与登录状态。
 */
@Data
@TableName("users")
public class User {

    @TableId(type = IdType.ASSIGN_UUID)
    private UUID id;

    private String username;
    // 密码哈希，任何出参都不得携带该字段
    private String passwordHash;
    private String role;
    private Boolean isActive;
    private OffsetDateTime lastLoginAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // 逻辑删除标记：未删除为 NULL，删除时写入当前时间
    @TableLogic(value = "NULL", delval = "now()")
    private OffsetDateTime deletedAt;
}
