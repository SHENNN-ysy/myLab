package com.myblog.application.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 爱好时间分布数据点实体，对应 hobby_time_points 表。
 * 每条记录属于某个 content_releases 版本，age 为年龄（-1 表示"现在"之前的占位），
 * hobby1~hobby5 为五个爱好的时间占比（合计恒为 10.0，由表约束兜底）；
 * 数据库列名为英文 hobby1~5（V6 迁移），对外 JSON 键仍为中文「爱好1~爱好5」。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hobby_time_points")
public class HobbyTimePoint extends BaseEntity {

    // 所属发布版本
    private UUID releaseId;

    // 年龄
    private Integer age;

    // 五个爱好的时间占比（0-10），对外 JSON 键为「爱好1~爱好5」
    private BigDecimal hobby1;
    private BigDecimal hobby2;
    private BigDecimal hobby3;
    private BigDecimal hobby4;
    private BigDecimal hobby5;
}
