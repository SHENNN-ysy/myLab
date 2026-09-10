package com.myblog.application.service.system;

import com.myblog.application.port.CacheDiagnostics;
import com.myblog.application.port.DatabaseDiagnostics;
import com.myblog.application.port.ObjectStorage;
import com.myblog.common.exception.ForbiddenException;
import com.myblog.common.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * SystemService 单元测试：健康状态聚合、系统静态/动态信息的管理员权限校验与指标范围，
 * 以及宿主机内存（/proc/meminfo）解析的各类降级分支。
 */
@ExtendWith(MockitoExtension.class)
class SystemServiceTest {
    @Mock DatabaseDiagnostics database;
    @Mock CacheDiagnostics cache;
    @Mock ObjectStorage storage;
    @Mock Environment environment;

    private SystemService service;
    private CurrentUser admin;

    @BeforeEach
    void setUp() {
        service = new SystemService(database, cache, storage, environment);
        admin = new CurrentUser(UUID.randomUUID(), "admin", "admin");
    }

    /** CPU 使用率取自真实系统指标，取值必须在 0-100 百分比范围内。 */
    @Test
    void cpuUsageIsRealMetricWithinPercentRange() {
        Map<String, Object> info = service.dynamicInfo(admin);

        assertThat(info.get("cpuUsage")).isInstanceOf(Number.class);
        double cpuUsage = ((Number) info.get("cpuUsage")).doubleValue();
        assertThat(cpuUsage).isBetween(0.0, 100.0);
    }

    /** 数据库、Redis、OSS 全部正常时整体状态为 healthy，各组件分别上报 up/configured。 */
    @Test
    void healthReportsHealthyWhenDatabaseIsUp() {
        when(database.available()).thenReturn(true);
        when(cache.available()).thenReturn(true);
        when(storage.configured()).thenReturn(true);

        Map<String, Object> result = service.health();

        assertThat(result.get("status")).isEqualTo("healthy");
        @SuppressWarnings("unchecked")
        Map<String, String> components = (Map<String, String>) result.get("components");
        assertThat(components).containsEntry("database", "up")
                .containsEntry("redis", "up")
                .containsEntry("oss", "configured");
    }

    /** 依赖全部不可用时整体状态降级为 degraded，各组件如实上报 down/not_configured。 */
    @Test
    void healthDegradesWhenDatabaseIsDown() {
        when(database.available()).thenReturn(false);
        when(cache.available()).thenReturn(false);
        when(storage.configured()).thenReturn(false);

        Map<String, Object> result = service.health();

        assertThat(result.get("status")).isEqualTo("degraded");
        @SuppressWarnings("unchecked")
        Map<String, String> components = (Map<String, String>) result.get("components");
        assertThat(components).containsEntry("database", "down")
                .containsEntry("redis", "down")
                .containsEntry("oss", "not_configured");
    }

    /** 非管理员查询系统静态信息被拒绝（ForbiddenException）。 */
    @Test
    void staticInfoRequiresAdmin() {
        CurrentUser viewer = new CurrentUser(UUID.randomUUID(), "guest", "viewer");

        assertThatThrownBy(() -> service.staticInfo(viewer))
                .isInstanceOf(ForbiddenException.class);
    }

    /** 静态信息包含主机名、OS、CPU、内存等字段；runMode 取激活 profile，未打包时版本号固定为 dev。 */
    @Test
    void staticInfoReportsHostOsAndActiveProfiles() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[] {"prod"});

        Map<String, Object> info = service.staticInfo(admin);

        assertThat(info.get("hostname")).isInstanceOf(String.class);
        assertThat(info.get("os")).isInstanceOf(String.class);
        assertThat(info.get("serverIp")).isInstanceOf(String.class);
        assertThat(info.get("timezone")).isInstanceOf(String.class);
        assertThat(info.get("cpuCore")).isInstanceOf(Integer.class);
        assertThat(info.get("cpuArch")).isInstanceOf(String.class);
        assertThat(info.get("memoryTotal")).isInstanceOf(Long.class);
        assertThat(info.get("swapTotal")).isInstanceOf(Long.class);
        assertThat(info.get("diskTotal")).isInstanceOf(Long.class);
        assertThat(info.get("appVersion")).isEqualTo("dev");
        assertThat(info.get("runMode")).isEqualTo("prod");
    }

    /** 无激活 profile 时 runMode 回退到默认 profile。 */
    @Test
    void staticInfoFallsBackToDefaultProfiles() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[0]);
        when(environment.getDefaultProfiles()).thenReturn(new String[] {"default"});

        Map<String, Object> info = service.staticInfo(admin);

        assertThat(info.get("runMode")).isEqualTo("default");
    }

    /** 非管理员查询系统动态信息被拒绝（ForbiddenException）。 */
    @Test
    void dynamicInfoRequiresAdmin() {
        CurrentUser viewer = new CurrentUser(UUID.randomUUID(), "guest", "viewer");

        assertThatThrownBy(() -> service.dynamicInfo(viewer))
                .isInstanceOf(ForbiddenException.class);
    }

    /** 动态信息包含负载、内存、swap、磁盘与运行时长，各数值非负。 */
    @Test
    void dynamicInfoReportsMemorySwapDiskAndUptime() {
        Map<String, Object> info = service.dynamicInfo(admin);

        assertThat(info.get("load1")).isInstanceOf(Number.class);
        assertThat(info.get("memoryUsed")).isInstanceOf(Long.class);
        assertThat(info.get("memoryAvailable")).isInstanceOf(Long.class);
        assertThat(info.get("swapUsed")).isInstanceOf(Long.class);
        assertThat(info.get("diskUsed")).isInstanceOf(Long.class);
        assertThat(info.get("diskFree")).isInstanceOf(Long.class);
        assertThat((Long) info.get("appUptime")).isGreaterThanOrEqualTo(0L);
        long memoryUsed = (Long) info.get("memoryUsed");
        long memoryAvailable = (Long) info.get("memoryAvailable");
        assertThat(memoryUsed).isGreaterThanOrEqualTo(0L);
        assertThat(memoryAvailable).isGreaterThanOrEqualTo(0L);
    }

    /** 按宿主机口径解析 meminfo：MemTotal/MemAvailable 的 kB 值换算为字节数返回。 */
    @Test
    void hostMemoryParsesMeminfoWithHostCaliber(@TempDir Path dir) throws IOException {
        Path meminfo = dir.resolve("meminfo");
        Files.write(meminfo, List.of(
                "MemTotal:       16384000 kB",
                "MemFree:         2048000 kB",
                "MemAvailable:   12288000 kB"));

        SystemService.HostMemory memory = service.hostMemory(meminfo);

        assertThat(memory.totalBytes()).isEqualTo(16384000L * 1024);
        assertThat(memory.availableBytes()).isEqualTo(12288000L * 1024);
    }

    /** meminfo 文件不存在时回退到 JVM MXBean 口径读取内存。 */
    @Test
    void hostMemoryFallsBackToMxBeanWhenMeminfoMissing(@TempDir Path dir) {
        SystemService.HostMemory memory = service.hostMemory(dir.resolve("nonexistent"));

        assertThat(memory.totalBytes()).isGreaterThan(0L);
        assertThat(memory.availableBytes()).isGreaterThanOrEqualTo(0L);
    }

    /** meminfo 缺少 MemAvailable 行时同样回退到 MXBean 口径。 */
    @Test
    void hostMemoryFallsBackWhenMemAvailableAbsent(@TempDir Path dir) throws IOException {
        Path meminfo = dir.resolve("meminfo");
        Files.write(meminfo, List.of("MemTotal:       16384000 kB"));

        SystemService.HostMemory memory = service.hostMemory(meminfo);

        assertThat(memory.totalBytes()).isGreaterThan(0L);
    }

    /** meminfo 数值非法（非数字）时回退到 MXBean 口径，不抛异常。 */
    @Test
    void hostMemoryFallsBackWhenMeminfoValueMalformed(@TempDir Path dir) throws IOException {
        Path meminfo = dir.resolve("meminfo");
        Files.write(meminfo, List.of(
                "MemTotal:       not-a-number kB",
                "MemAvailable:   12288000 kB"));

        SystemService.HostMemory memory = service.hostMemory(meminfo);

        assertThat(memory.totalBytes()).isGreaterThan(0L);
    }

    /** meminfo 行缺少数值（仅有键名）时回退到 MXBean 口径。 */
    @Test
    void hostMemoryFallsBackWhenMeminfoLineHasNoValue(@TempDir Path dir) throws IOException {
        Path meminfo = dir.resolve("meminfo");
        Files.write(meminfo, List.of(
                "MemTotal:",
                "MemAvailable:   12288000 kB"));

        SystemService.HostMemory memory = service.hostMemory(meminfo);

        assertThat(memory.totalBytes()).isGreaterThan(0L);
    }
}
