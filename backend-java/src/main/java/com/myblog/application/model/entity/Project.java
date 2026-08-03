package com.myblog.application.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.myblog.infrastructure.persistence.handler.JsonbTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "projects", autoResultMap = true)
public class Project extends BaseEntity {

    private String title;
    private String slug;
    private String description;
    private String content;
    private String tag;
    private Integer year;
    private String imageUrl;
    private String projectUrl;
    private String repoUrl;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private Object tech;

    private Integer orderNum;
}
