package com.myblog.common.security;

import com.myblog.common.enumeration.Role;
import com.myblog.common.exception.ForbiddenException;

/** 共享内核中的角色策略，不包含任何领域业务逻辑。 */
public final class Authorization {

    private Authorization() {
    }

    /**
     * 要求当前用户至少为管理员。
     *
     * @throws ForbiddenException 用户未登录或角色不足时抛出
     */
    public static void requireAdmin(CurrentUser user) {
        if (user == null || !user.atLeast(Role.ADMIN)) {
            throw new ForbiddenException();
        }
    }

    /**
     * 要求当前用户为超级管理员。
     *
     * @throws ForbiddenException 用户未登录或角色不足时抛出
     */
    public static void requireSuperadmin(CurrentUser user) {
        if (user == null || !user.atLeast(Role.SUPERADMIN)) {
            throw new ForbiddenException();
        }
    }
}
