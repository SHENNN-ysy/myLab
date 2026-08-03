package com.myblog.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myblog.application.model.entity.User;
import com.myblog.application.repository.UserRepository;
import com.myblog.common.result.PageResult;

import java.util.UUID;

public interface UserMapper extends BaseMapper<User>, UserRepository {
    @Override default User findByUsername(String username) {
        return selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }
    @Override default User findById(UUID id) { return selectById(id); }
    @Override default PageResult<User> findPage(long page, long size) {
        Page<User> result = selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<User>().orderByDesc(User::getCreatedAt));
        return PageResult.of(result.getRecords(), page, size, result.getTotal());
    }
    @Override default long countAll() { return selectCount(null); }
    @Override default boolean usernameOrEmailExists(String username, String email) {
        return selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username).or().eq(User::getEmail, email)) > 0;
    }
    @Override default void add(User user) { insert(user); }
    @Override default void save(User user) { updateById(user); }
    @Override default boolean remove(UUID id) { return deleteById(id) > 0; }
}
