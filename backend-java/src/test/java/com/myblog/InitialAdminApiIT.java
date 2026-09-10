package com.myblog;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 验证首次启动管理员完全由运行环境配置创建。 */
class InitialAdminApiIT extends AbstractApiIntegrationTest {

    /** 初始管理员由 INIT_ADMIN_* 配置创建（替代基线种子数据），且可正常登录。 */
    @Test
    void createsInitialAdminFromConfigurationInsteadOfBaselineData() {
        // api-it-initial-admin 来自测试环境 INIT_ADMIN_USERNAME；admin 为基线种子用户名
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
