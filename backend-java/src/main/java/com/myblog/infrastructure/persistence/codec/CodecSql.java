package com.myblog.infrastructure.persistence.codec;

import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * codec 包内共享的原生 SQL 小工具：模块数据表的软标记。
 * 级联软删除语义（按条件批量打 deleted_at、幂等）不是单表 @TableLogic 能表达的，保留手写 SQL。
 */
final class CodecSql {

    private CodecSql() { }

    /** 按条件为指定表的数据行打 deleted_at 标记（幂等） */
    static void softDelete(JdbcTemplate jdbc, String table, String condition, OffsetDateTime now, UUID releaseId) {
        jdbc.update("UPDATE " + table + " SET deleted_at = ? WHERE " + condition + " AND deleted_at IS NULL",
                now, releaseId);
    }
}
