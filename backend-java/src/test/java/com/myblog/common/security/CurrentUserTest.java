package com.myblog.common.security;

import com.myblog.common.security.CurrentUser;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentUserTest {

    @Test
    void roleHierarchyMatchesPython() {
        assertThat(new CurrentUser(UUID.randomUUID(), "root", "superadmin").atLeast("superadmin")).isTrue();
        assertThat(new CurrentUser(UUID.randomUUID(), "admin", "admin").atLeast("admin")).isTrue();
        assertThat(new CurrentUser(UUID.randomUUID(), "editor", "editor").atLeast("admin")).isFalse();
        assertThat(new CurrentUser(UUID.randomUUID(), "viewer", "viewer").atLeast("admin")).isFalse();
    }
}
