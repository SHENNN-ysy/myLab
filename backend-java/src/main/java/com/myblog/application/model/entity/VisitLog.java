package com.myblog.application.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@TableName("visit_logs")
public class VisitLog {

    @TableId(type = IdType.ASSIGN_UUID)
    private UUID id;

    private String ip;
    private String userAgent;
    private String path;
    private String referer;
    private UUID userId;
    private OffsetDateTime visitedAt;
}
