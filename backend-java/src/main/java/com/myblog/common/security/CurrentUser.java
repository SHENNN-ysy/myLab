package com.myblog.common.security;

import com.myblog.common.enumeration.Role;

import java.util.UUID;

/**
 * Authentication principal representing the current user. Mirrors the role ranking logic
 * that previously lived inline in the legacy implementation.
 */
public record CurrentUser(UUID id, String username, String role) {

    public boolean atLeast(String needed) {
        return atLeast(Role.parse(needed));
    }

    public boolean atLeast(Role needed) {
        return Role.parse(this.role).atLeast(needed);
    }
}
