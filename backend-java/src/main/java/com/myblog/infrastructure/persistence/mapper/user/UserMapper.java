package com.myblog.infrastructure.persistence.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myblog.application.model.entity.User;
import com.myblog.application.repository.UserRepository;
import com.myblog.common.result.PageResult;

import java.util.UUID;

/**
 * 用户表 Mapper：既是 MyBatis-Plus 的 {@link BaseMapper}，又通过 default 方法
 * 把 BaseMapper 的能力适配成应用层的 {@link UserRepository} 端口，
 * 使仓储接口无需单独的实现类。
 */
public interface UserMapper extends BaseMapper<User>, UserRepository {
    /** 按用户名精确查询用户，用于登录与唯一性校验 */
    @Override default User findByUsername(String username) {
        return selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }
    @Override default User findById(UUID id) { return selectById(id); }
    /** 按创建时间倒序分页查询用户 */
    @Override default PageResult<User> findPage(long page, long size) {
        Page<User> result = selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<User>().orderByDesc(User::getCreatedAt));
        return PageResult.of(result.getRecords(), page, size, result.getTotal());
    }
    @Override default long countAll() { return selectCount(null); }
    /** @return 用户名是否已存在（注册唯一性校验用） */
    @Override default boolean usernameExists(String username) {
        return selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)) > 0;
    }
    /** 新增用户 */
    @Override default void add(User user) { insert(user); }
    @Override default void save(User user) { updateById(user); }
    @Override default boolean remove(UUID id) { return deleteById(id) > 0; }
}
