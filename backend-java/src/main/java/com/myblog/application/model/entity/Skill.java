package com.myblog.application.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("skills")
public class Skill extends BaseEntity {

    private String name;
    private String category;
    private Integer percentage;
    private String level;
    private String icon;
    private Integer orderNum;
    private String barStyle;
}
