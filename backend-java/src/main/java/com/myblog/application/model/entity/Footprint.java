package com.myblog.application.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

/**
 * 足迹条目实体，对应 footprints 表。
 * 每条记录属于某个 content_releases 版本，按 sort_order、city_key 排序；
 * 正文 contents 以空行分段，读取时切分为 paragraphs 数组对外输出。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("footprints")
public class Footprint extends BaseEntity {

    // 所属发布版本
    private UUID releaseId;

    // 城市唯一标识（对外同时暴露为 id 与 city_key）
    private String cityKey;

    private String title;

    private String summary;

    // 正文（段落之间以空行分隔）
    private String contents;

    private Boolean enabled;

    private Integer sortOrder;
}
