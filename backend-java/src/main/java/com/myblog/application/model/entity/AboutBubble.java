package com.myblog.application.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("about_bubbles")
public class AboutBubble extends BaseEntity {

    private String label;
    private String bgColor;
    private String glowColor;
    private String textColor;
    private Double positionX;
    private Double positionY;
    private Double radius;
    private String tier;
    private Integer orderNum;
    private Boolean enabled;
    private String remark;
}
