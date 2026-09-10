package com.myblog.common.security;

import com.myblog.common.security.CurrentUser;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 当前用户角色层级判定（atLeast）测试。 */
class CurrentUserTest {

    /** 角色层级判定与旧 Python 实现保持一致：同级及以上为真，低级别为假。 */
    @Test
    void roleHierarchyMatchesPython() {
        assertThat(new CurrentUser(UUID.randomUUID(), "root", "superadmin").atLeast("superadmin")).isTrue();
        assertThat(new CurrentUser(UUID.randomUUID(), "admin", "admin").atLeast("admin")).isTrue();
        assertThat(new CurrentUser(UUID.randomUUID(), "editor", "editor").atLeast("admin")).isFalse();
        assertThat(new CurrentUser(UUID.randomUUID(), "viewer", "viewer").atLeast("admin")).isFalse();
    }
}
