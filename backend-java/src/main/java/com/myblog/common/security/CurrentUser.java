package com.myblog.common.security;

import com.myblog.common.enumeration.Role;

import java.util.UUID;

/**
 * 当前登录用户的认证主体，由认证过滤器解析令牌后写入 SecurityContext。
 * 角色等级比较委托给 {@link Role} 的 rank 层级。
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
