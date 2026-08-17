package com.myblog.application.service.system;

import com.myblog.application.port.CacheDiagnostics;
import com.myblog.application.port.DatabaseDiagnostics;
import com.myblog.application.port.ObjectStorage;
import com.myblog.common.exception.ForbiddenException;
import com.myblog.common.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

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

    @Test
    void cpuUsageIsRealMetricWithinPercentRange() {
        Map<String, Object> info = service.dynamicInfo(admin);

        assertThat(info.get("cpuUsage")).isInstanceOf(Number.class);
        double cpuUsage = ((Number) info.get("cpuUsage")).doubleValue();
        assertThat(cpuUsage).isBetween(0.0, 100.0);
    }

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

    @Test
    void staticInfoRequiresAdmin() {
        CurrentUser viewer = new CurrentUser(UUID.randomUUID(), "guest", "viewer");

        assertThatThrownBy(() -> service.staticInfo(viewer))
                .isInstanceOf(ForbiddenException.class);
    }

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

    @Test
    void staticInfoFallsBackToDefaultProfiles() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[0]);
        when(environment.getDefaultProfiles()).thenReturn(new String[] {"default"});

        Map<String, Object> info = service.staticInfo(admin);

        assertThat(info.get("runMode")).isEqualTo("default");
    }

    @Test
    void dynamicInfoRequiresAdmin() {
        CurrentUser viewer = new CurrentUser(UUID.randomUUID(), "guest", "viewer");

        assertThatThrownBy(() -> service.dynamicInfo(viewer))
                .isInstanceOf(ForbiddenException.class);
    }

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
}
