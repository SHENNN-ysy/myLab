package com.myblog.common.enumeration;

import java.util.Locale;

/**
 * 用户角色层级。rank 越大权限越高，高等级角色隐含低等级角色的全部权限。
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

    /** 判断当前角色是否达到 {@code needed} 要求的最低等级。 */
    public boolean atLeast(Role needed) {
        return this.rank >= needed.rank;
    }

    /** 大小写不敏感地解析角色名；无法识别时回退为 VIEWER。 */
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
