package com.myblog.application.service.auth;

import com.myblog.application.model.entity.User;
import com.myblog.application.model.vo.AuthResultVO;
import com.myblog.application.model.vo.TokenPairVO;
import com.myblog.application.model.vo.UserPublicVO;

import java.util.UUID;

/**
 * 认证用例接口：登录、令牌刷新、当前用户查询与密码管理。
 */
public interface AuthService {

    /**
     * 校验用户名密码并签发访问令牌与刷新令牌。
     */
    AuthResultVO login(String username, String password);

    /**
     * 用刷新令牌换发一对新的访问/刷新令牌。
     */
    TokenPairVO refresh(String token);

    /**
     * 按用户 ID 取当前登录用户，不存在时抛未认证异常。
     */
    User current(UUID id);

    /**
     * 将用户实体转换为对外公开视图（不含敏感字段）。
     */
    UserPublicVO publicUser(User user);

    /**
     * 校验旧密码后修改当前用户密码。
     */
    void change(UUID id, String oldPassword, String newPassword);

    /**
     * 校验当前密码后修改当前用户的账号名称，并按需修改密码。
     */
    UserPublicVO updateAccount(UUID id, String username, String oldPassword, String newPassword);

    /**
     * 确保系统中至少存在一个管理员：仅在无任何用户时按配置创建初始超级管理员。
     */
    void ensureAdmin();

    /**
     * 对明文密码做哈希，供创建/重置密码时使用。
     */
    String hash(String password);
}
