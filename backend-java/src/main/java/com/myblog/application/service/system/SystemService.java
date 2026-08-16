package com.myblog.application.service.system;

import com.myblog.application.port.ObjectStorage;
import com.myblog.application.port.CacheDiagnostics;
import com.myblog.application.port.DatabaseDiagnostics;
import com.myblog.common.security.Authorization;
import com.myblog.common.security.CurrentUser;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统监控服务：汇总数据库、缓存、对象存储的健康状态，以及服务器操作系统的静态/动态运行指标。
 */
@Service
public class SystemService {

    private final DatabaseDiagnostics database;
    private final CacheDiagnostics cache;
    private final ObjectStorage storage;
    private final Environment environment;
    private final long startedAt = System.currentTimeMillis(); // 应用启动时间，用于计算应用运行时长

    public SystemService(DatabaseDiagnostics database, CacheDiagnostics cache, ObjectStorage storage,
                         Environment environment) {
        this.database = database;
        this.cache = cache;
        this.storage = storage;
        this.environment = environment;
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
     * 服务器静态信息：主机、操作系统、CPU、物理内存、Swap、磁盘等（仅管理员）。
     */
    public Map<String, Object> staticInfo(CurrentUser actor) throws Exception {
        Authorization.requireAdmin(actor);
        File root = new File("/");
        var os = ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class);
        InetAddress host = InetAddress.getLocalHost();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hostname", host.getHostName());
        result.put("os", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        result.put("serverIp", host.getHostAddress());
        result.put("timezone", ZoneId.systemDefault().toString());
        result.put("cpuCore", os.getAvailableProcessors());
        result.put("cpuArch", System.getProperty("os.arch"));
        result.put("memoryTotal", os.getTotalMemorySize());
        result.put("swapTotal", os.getTotalSwapSpaceSize());
        result.put("diskTotal", root.getTotalSpace());
        String version = getClass().getPackage().getImplementationVersion();
        result.put("appVersion", version != null ? version : "dev");
        result.put("runMode", String.join(",", environment.getActiveProfiles().length > 0
                ? environment.getActiveProfiles() : environment.getDefaultProfiles()));
        return result;
    }

    /**
     * 服务器动态指标：CPU、负载、物理内存、Swap、磁盘与应用运行时长等实时数据（仅管理员）。
     * 内存与 Swap 为操作系统口径（容器内为容器可见的宿主机口径）；appUptime 是本应用进程的运行秒数。
     */
    public Map<String, Object> dynamicInfo(CurrentUser actor) {
        Authorization.requireAdmin(actor);
        File root = new File("/");
        var os = ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class);

        long memoryTotal = os.getTotalMemorySize();
        long memoryFree = os.getFreeMemorySize();
        long swapTotal = os.getTotalSwapSpaceSize();
        long swapFree = os.getFreeSwapSpaceSize();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cpuUsage", cpuUsagePercent(os));
        // Windows 等平台返回 -1 表示不可用，由前端兜底显示
        result.put("load1", os.getSystemLoadAverage());
        result.put("memoryUsed", memoryTotal - memoryFree);
        result.put("memoryAvailable", memoryFree);
        result.put("swapUsed", swapTotal - swapFree);
        result.put("appUptime", (System.currentTimeMillis() - startedAt) / 1000);
        result.put("diskUsed", root.getTotalSpace() - root.getFreeSpace());
        result.put("diskFree", root.getFreeSpace());
        return result;
    }

    /**
     * 系统 CPU 使用率（0~100，保留一位小数）：优先取整机负载，不可用时退化为 JVM 进程负载，
     * 仍不可用返回 0。容器内读到的整机负载为宿主机口径。
     */
    private double cpuUsagePercent(com.sun.management.OperatingSystemMXBean osBean) {
        double load = osBean.getCpuLoad();
        if (load < 0) load = osBean.getProcessCpuLoad();
        if (load < 0) return 0;
        return Math.round(load * 1000) / 10.0;
    }
}
