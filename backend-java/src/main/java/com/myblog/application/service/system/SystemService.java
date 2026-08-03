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

@Service
public class SystemService {

    private final DatabaseDiagnostics database;
    private final CacheDiagnostics cache;
    private final ObjectStorage storage;
    private final long startedAt = System.currentTimeMillis();

    public SystemService(DatabaseDiagnostics database, CacheDiagnostics cache, ObjectStorage storage) {
        this.database = database;
        this.cache = cache;
        this.storage = storage;
    }

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
