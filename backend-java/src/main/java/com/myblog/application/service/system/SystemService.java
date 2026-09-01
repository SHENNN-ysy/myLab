package com.myblog.application.service.system;

import com.myblog.application.port.ObjectStorage;
import com.myblog.application.port.CacheDiagnostics;
import com.myblog.application.port.DatabaseDiagnostics;
import com.myblog.common.security.Authorization;
import com.myblog.common.security.CurrentUser;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统监控服务：汇总数据库、缓存、对象存储的健康状态，以及服务器操作系统的静态/动态运行指标。
 */
@Service
public class SystemService {

    /** Linux 宿主机内存信息文件：容器内该文件不做 cgroup 虚拟化，读到的始终为宿主机口径 */
    private static final Path MEMINFO_PATH = Path.of("/proc/meminfo");
    private static final long KB_TO_BYTES = 1024L;

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
        result.put("memoryTotal", hostMemory(MEMINFO_PATH).totalBytes());
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
     * 内存取宿主机口径（/proc/meminfo），Swap、负载为操作系统口径，appUptime 是本应用进程的运行秒数。
     */
    public Map<String, Object> dynamicInfo(CurrentUser actor) {
        Authorization.requireAdmin(actor);
        File root = new File("/");
        var os = ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class);

        HostMemory memory = hostMemory(MEMINFO_PATH);
        long swapTotal = os.getTotalSwapSpaceSize();
        long swapFree = os.getFreeSwapSpaceSize();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cpuUsage", cpuUsagePercent(os));
        // Windows 等平台返回 -1 表示不可用，由前端兜底显示
        result.put("load1", os.getSystemLoadAverage());
        result.put("memoryUsed", memory.totalBytes() - memory.availableBytes());
        result.put("memoryAvailable", memory.availableBytes());
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

    /**
     * 宿主机内存视图（字节）。容器内 JDK 的 OperatingSystemMXBean 返回 cgroup 限额（本项目
     * 为 compose 的 mem_limit），而 /proc/meminfo 不做 cgroup 虚拟化、始终为宿主机口径；
     * 仅在 /proc/meminfo 不可用（非 Linux 环境或读取失败）时回退 MXBean。
     */
    HostMemory hostMemory(Path meminfoPath) {
        HostMemory meminfo = readMeminfo(meminfoPath);
        if (meminfo != null) {
            return meminfo;
        }
        var os = ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class);
        return new HostMemory(os.getTotalMemorySize(), os.getFreeMemorySize());
    }

    /**
     * 解析 /proc/meminfo 的 MemTotal 与 MemAvailable；文件缺失、字段不全或格式非法返回 null。
     * 可用量用 MemAvailable 而非 MemFree（后者不含可回收缓存，会严重低估可用内存）。
     */
    private static HostMemory readMeminfo(Path path) {
        try {
            if (!Files.isRegularFile(path)) {
                return null;
            }
            Long totalKb = null;
            Long availableKb = null;
            for (String line : Files.readAllLines(path)) {
                if (line.startsWith("MemTotal:")) {
                    totalKb = parseKb(line);
                } else if (line.startsWith("MemAvailable:")) {
                    availableKb = parseKb(line);
                }
                if (totalKb != null && availableKb != null) {
                    break;
                }
            }
            return (totalKb != null && availableKb != null)
                    ? new HostMemory(totalKb * KB_TO_BYTES, availableKb * KB_TO_BYTES)
                    : null;
        } catch (IOException e) {
            return null; // 读取失败时回退 MXBean 口径
        }
    }

    /** 解析 "MemTotal:    16384000 kB" 形式的行，返回 kB 数值；格式非法返回 null */
    private static Long parseKb(String line) {
        String[] tokens = line.split("\\s+");
        if (tokens.length <= 1) {
            return null;
        }
        try {
            return Long.parseLong(tokens[1]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 宿主机内存视图：总量与可用量（字节） */
    record HostMemory(long totalBytes, long availableBytes) { }
}
