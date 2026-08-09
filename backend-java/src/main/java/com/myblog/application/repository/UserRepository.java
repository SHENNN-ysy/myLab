package com.myblog.application.repository;

import com.myblog.application.model.entity.User;
import com.myblog.common.result.PageResult;

import java.util.UUID;

public interface UserRepository {
    User findByUsername(String username);
    User findById(UUID id);
    PageResult<User> findPage(long page, long size);
    long countAll();
    boolean usernameExists(String username);
    void add(User user);
    void save(User user);
    boolean remove(UUID id);
}
