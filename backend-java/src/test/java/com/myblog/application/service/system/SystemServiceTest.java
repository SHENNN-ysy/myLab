package com.myblog.application.service.system;

import com.myblog.application.port.CacheDiagnostics;
import com.myblog.application.port.DatabaseDiagnostics;
import com.myblog.application.port.ObjectStorage;
import com.myblog.common.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SystemServiceTest {
    @Mock DatabaseDiagnostics database;
    @Mock CacheDiagnostics cache;
    @Mock ObjectStorage storage;

    private SystemService service;
    private CurrentUser admin;

    @BeforeEach
    void setUp() {
        service = new SystemService(database, cache, storage);
        admin = new CurrentUser(UUID.randomUUID(), "admin", "admin");
    }

    @Test
    void cpuUsageIsRealMetricWithinPercentRange() {
        Map<String, Object> info = service.dynamicInfo(admin);

        assertThat(info.get("cpuUsage")).isInstanceOf(Number.class);
        double cpuUsage = ((Number) info.get("cpuUsage")).doubleValue();
        assertThat(cpuUsage).isBetween(0.0, 100.0);
    }
}
