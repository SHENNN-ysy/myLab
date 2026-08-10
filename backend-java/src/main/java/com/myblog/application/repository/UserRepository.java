package com.myblog.application.repository;

import com.myblog.application.model.entity.User;
import com.myblog.common.result.PageResult;

import java.util.UUID;

/**
 * 用户仓储接口：用户实体的持久化抽象，由基础设施层实现。
 */
public interface UserRepository {
    /**
     * 按用户名查询用户，不存在时返回 null。
     */
    User findByUsername(String username);
    /**
     * 按 ID 查询用户，不存在时返回 null。
     */
    User findById(UUID id);
    /**
     * 分页查询用户列表。
     */
    PageResult<User> findPage(long page, long size);
    /**
     * 用户总数。
     */
    long countAll();
    /**
     * 用户名是否已被占用。
     */
    boolean usernameExists(String username);
    /**
     * 新增用户。
     */
    void add(User user);
    /**
     * 更新已有用户。
     */
    void save(User user);
    /**
     * 按 ID 删除用户。
     *
     * @return 删除成功返回 true，用户不存在返回 false
     */
    boolean remove(UUID id);
}
