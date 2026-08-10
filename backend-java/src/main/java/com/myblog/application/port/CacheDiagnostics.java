package com.myblog.application.port;

/**
 * 缓存诊断端口：向应用层暴露缓存中间件（如 Redis）的可用性，用于健康检查。
 */
public interface CacheDiagnostics {

    /**
     * 缓存当前是否可用。
     */
    boolean available();
}
