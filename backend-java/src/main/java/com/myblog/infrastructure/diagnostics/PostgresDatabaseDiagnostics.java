package com.myblog.infrastructure.diagnostics;

import com.myblog.application.port.DatabaseDiagnostics;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * PostgreSQL 数据库健康诊断：提供可用性探测及表数量、库大小、连接数等运行指标，供健康检查接口使用。
 */
@Repository
public class PostgresDatabaseDiagnostics implements DatabaseDiagnostics {

    private final JdbcTemplate jdbc; // 执行诊断 SQL 的入口

    public PostgresDatabaseDiagnostics(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 探测数据库是否可用（执行 {@code select 1}）。
     *
     * @return 查询成功返回 true，任何异常均视为不可用
     */
    @Override
    public boolean available() {
        try {
            jdbc.queryForObject("select 1", Integer.class);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /** @return public schema 下的表数量 */
    @Override
    public long tableCount() {
        return value("select count(*) from information_schema.tables where table_schema='public'");
    }

    /** @return 当前数据库占用的磁盘字节数 */
    @Override
    public long databaseSize() {
        return value("select pg_database_size(current_database())");
    }

    /** @return 当前数据库的活动连接数 */
    @Override
    public long connectionCount() {
        return value("select count(*) from pg_stat_activity where datname=current_database()");
    }

    /** 执行返回单个数值的诊断 SQL，结果为 null 时按 0 处理 */
    private long value(String sql) {
        Long result = jdbc.queryForObject(sql, Long.class);
        return result == null ? 0L : result;
    }
}
