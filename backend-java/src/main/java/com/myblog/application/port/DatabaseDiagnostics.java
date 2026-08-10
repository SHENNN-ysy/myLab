package com.myblog.application.port;

/**
 * 数据库诊断端口：向应用层暴露数据库健康状态与规模指标，用于监控与健康检查。
 */
public interface DatabaseDiagnostics {

    /**
     * 数据库连接当前是否可用。
     */
    boolean available();

    /**
     * 数据库中的表数量。
     */
    long tableCount();

    /**
     * 数据库占用空间（字节）。
     */
    long databaseSize();

    /**
     * 当前数据库连接数。
     */
    long connectionCount();
}
