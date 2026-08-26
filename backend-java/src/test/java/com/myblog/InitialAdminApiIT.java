package com.myblog;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 验证首次启动管理员完全由运行环境配置创建。 */
class InitialAdminApiIT extends AbstractApiIntegrationTest {

    @Test
    void createsInitialAdminFromConfigurationInsteadOfBaselineData() {
        Integer configuredAdminCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = 'api-it-initial-admin'",
                Integer.class);
        Integer seededAdminCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = 'admin'",
                Integer.class);

        assertThat(configuredAdminCount).isEqualTo(1);
        assertThat(seededAdminCount).isZero();
        assertThat(loginRaw("api-it-initial-admin", "api-it-initial-admin-password").getStatusCode().is2xxSuccessful())
                .isTrue();
    }
}
