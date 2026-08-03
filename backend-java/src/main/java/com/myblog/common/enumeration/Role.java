package com.myblog.common.enumeration;

import java.util.Locale;

/**
 * User role hierarchy. Higher rank implies all lower privileges.
 */
public enum Role {

    VIEWER(1),
    EDITOR(2),
    ADMIN(3),
    SUPERADMIN(4);

    private final int rank;

    Role(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }

    public boolean atLeast(Role needed) {
        return this.rank >= needed.rank;
    }

    /** Parse a role name case-insensitively. Falls back to VIEWER for unknown values. */
    public static Role parse(String name) {
        if (name == null) {
            return VIEWER;
        }
        try {
            return Role.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return VIEWER;
        }
    }
}
