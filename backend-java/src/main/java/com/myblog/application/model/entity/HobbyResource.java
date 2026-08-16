package com.myblog.application.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

/**
 * 爱好-资源关联实体，对应 hobby_resources 表。
 * 每个爱好至多关联一条资源（部分唯一索引兜底）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hobby_resources")
public class HobbyResource extends BaseEntity {

    // 所属爱好（hobbies 表外键）
    private UUID hobbyId;

    // 资源引用（resources 表外键）
    private UUID resourceId;
}
