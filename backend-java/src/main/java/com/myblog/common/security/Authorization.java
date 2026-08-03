package com.myblog.common.security;

import com.myblog.common.enumeration.Role;
import com.myblog.common.exception.ForbiddenException;

/** 共享内核中的角色策略，不包含任何领域业务逻辑。 */
public final class Authorization {

    private Authorization() {
    }

    public static void requireAdmin(CurrentUser user) {
        if (user == null || !user.atLeast(Role.ADMIN)) {
            throw new ForbiddenException();
        }
    }

    public static void requireSuperadmin(CurrentUser user) {
        if (user == null || !user.atLeast(Role.SUPERADMIN)) {
            throw new ForbiddenException();
        }
    }
}
