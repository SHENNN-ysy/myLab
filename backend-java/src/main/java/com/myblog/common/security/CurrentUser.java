package com.myblog.common.security;

import com.myblog.common.enumeration.Role;

import java.util.UUID;

/**
 * 当前登录用户的认证主体。角色等级的比较逻辑复刻自旧版实现中的内联逻辑。
 */
public record CurrentUser(UUID id, String username, String role) {

    /** 判断当前用户角色是否达到指定名称角色的最低等级。 */
    public boolean atLeast(String needed) {
        return atLeast(Role.parse(needed));
    }

    /** 判断当前用户角色是否达到指定角色的最低等级。 */
    public boolean atLeast(Role needed) {
        return Role.parse(this.role).atLeast(needed);
    }
}
