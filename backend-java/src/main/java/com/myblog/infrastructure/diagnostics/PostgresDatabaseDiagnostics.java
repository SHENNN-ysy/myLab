package com.myblog.infrastructure.diagnostics;

import com.myblog.application.port.DatabaseDiagnostics;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresDatabaseDiagnostics implements DatabaseDiagnostics {

    private final JdbcTemplate jdbc;

    public PostgresDatabaseDiagnostics(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean available() {
        try {
            jdbc.queryForObject("select 1", Integer.class);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public long tableCount() {
        return value("select count(*) from information_schema.tables where table_schema='public'");
    }

    @Override
    public long databaseSize() {
        return value("select pg_database_size(current_database())");
    }

    @Override
    public long connectionCount() {
        return value("select count(*) from pg_stat_activity where datname=current_database()");
    }

    private long value(String sql) {
        Long result = jdbc.queryForObject(sql, Long.class);
        return result == null ? 0L : result;
    }
}
