package com.myblog.application.service.system;

import com.myblog.application.port.ObjectStorage;
import com.myblog.application.port.CacheDiagnostics;
import com.myblog.application.port.DatabaseDiagnostics;
import com.myblog.common.security.Authorization;
import com.myblog.common.security.CurrentUser;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统监控服务：汇总数据库、缓存、对象存储的健康状态，以及服务器的静态/动态运行指标。
 */
@Service
public class SystemService {

    private final DatabaseDiagnostics database;
    private final CacheDiagnostics cache;
    private final ObjectStorage storage;
    private final long startedAt = System.currentTimeMillis(); // 应用启动时间，用于估算运行时长

    public SystemService(DatabaseDiagnostics database, CacheDiagnostics cache, ObjectStorage storage) {
        this.database = database;
        this.cache = cache;
        this.storage = storage;
    }

    /**
     * 健康检查：数据库不可用时整体状态降级为 degraded。
     */
    public Map<String, Object> health() {
        Map<String, String> components = new LinkedHashMap<>();
        components.put("database", database.available() ? "up" : "down");
        components.put("redis", cache.available() ? "up" : "down");
        components.put("oss", storage.configured() ? "configured" : "not_configured");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", database.available() ? "healthy" : "degraded");
        result.put("components", components);
        return result;
    }

    /**
     * 服务器静态信息：主机、操作系统、CPU、内存、磁盘、数据库类型与表数量等（仅管理员）。
     */
    public Map<String, Object> staticInfo(CurrentUser actor) throws Exception {
        Authorization.requireAdmin(actor);
        File root = new File("/");
        var os = ManagementFactory.getOperatingSystemMXBean();
        InetAddress host = InetAddress.getLocalHost();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hostname", host.getHostName());
        result.put("os", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        result.put("serverIp", host.getHostAddress());
        result.put("timezone", ZoneId.systemDefault().toString());
        result.put("cpuCore", os.getAvailableProcessors());
        result.put("cpuModel", System.getProperty("os.arch"));
        result.put("cpuArch", System.getProperty("os.arch"));
        result.put("memoryTotal", Runtime.getRuntime().maxMemory());
        result.put("swapTotal", 0);
        result.put("diskTotal", root.getTotalSpace());
        result.put("dbType", "PostgreSQL");
        result.put("dbTables", database.tableCount());
        result.put("appVersion", "1.0.0");
        result.put("storageStatus", storage.configured() ? "OSS已配置" : "OSS未配置");
        result.put("emailStatus", "未配置");
        return result;
    }

    /**
     * 服务器动态指标：负载、内存、磁盘、运行时长与数据库连接等实时数据（仅管理员）。
     * 注意：hostUptime 实际是本应用进程的运行秒数，并非宿主机开机时长。
     */
    public Map<String, Object> dynamicInfo(CurrentUser actor) {
        Authorization.requireAdmin(actor);
        File root = new File("/");
        Runtime runtime = Runtime.getRuntime();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cpuUsage", 0);
        result.put("load1", ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());
        result.put("load5", 0);
        result.put("load15", 0);
        result.put("memoryUsed", runtime.totalMemory() - runtime.freeMemory());
        result.put("memoryAvailable", runtime.freeMemory());
        result.put("swapUsed", 0);
        result.put("hostUptime", (System.currentTimeMillis() - startedAt) / 1000);
        result.put("diskUsed", root.getTotalSpace() - root.getFreeSpace());
        result.put("diskFree", root.getFreeSpace());
        result.put("dbStatus", database.available() ? "正常" : "异常");
        result.put("dbSize", database.databaseSize());
        result.put("dbConnCount", database.connectionCount());
        return result;
    }
}
