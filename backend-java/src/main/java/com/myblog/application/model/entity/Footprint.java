package com.myblog.application.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.myblog.infrastructure.persistence.handler.JsonbTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "footprints", autoResultMap = true)
public class Footprint extends BaseEntity {

    private String name;
    private String slug;
    private String tag;
    private Double positionX;
    private Double positionY;
    private Boolean isSelf;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private Object tipData;

    private Integer orderNum;
}
