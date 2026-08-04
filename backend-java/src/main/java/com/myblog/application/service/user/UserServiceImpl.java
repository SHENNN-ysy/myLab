package com.myblog.application.service.user;

import com.myblog.application.model.entity.User;
import com.myblog.common.exception.ConflictException;
import com.myblog.common.exception.NotFoundException;
import com.myblog.common.exception.ValidationException;
import com.myblog.common.enumeration.ErrorCode;
import com.myblog.application.model.command.user.UserCommands;
import com.myblog.application.repository.UserRepository;
import com.myblog.common.result.PageResult;
import com.myblog.common.security.CurrentUser;
import com.myblog.common.security.Authorization;
import com.myblog.application.service.auth.AuthService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository users;
    private final AuthService auth;

    public UserServiceImpl(UserRepository users, AuthService auth) {
        this.users = users;
        this.auth = auth;
    }

    @Override
    public PageResult<User> page(CurrentUser actor, long page, long size) {
        Authorization.requireAdmin(actor);
        return users.findPage(page, size);
    }

    @Override
    @Transactional
    public User create(CurrentUser actor, UserCommands.Create command) {
        Authorization.requireSuperadmin(actor);
        if (command.username() == null || command.email() == null || command.password() == null) {
            throw new ValidationException("username、email 和 password 为必填字段");
        }
        String username = command.username();
        String email = command.email();
        String password = command.password();
        if (username.length() < 3 || password.length() < 8 || !email.contains("@")) {
            throw new ValidationException("用户名至少 3 位、密码至少 8 位且邮箱格式必须正确");
        }
        if (users.usernameOrEmailExists(username, email)) {
            throw new ConflictException(ErrorCode.USER_ALREADY_EXISTS, null);
        }
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setEmail(email);
        user.setNickname(command.nickname());
        user.setRole(Objects.requireNonNullElse(command.role(), "viewer"));
        user.setPasswordHash(auth.hash(password));
        user.setIsActive(true);
        OffsetDateTime now = OffsetDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        users.add(user);
        return user;
    }

    @Override
    @Transactional
    public User update(CurrentUser actor, UUID id, UserCommands.Update command) {
        Authorization.requireAdmin(actor);
        User user = users.findById(id);
        if (user == null) {
            throw new NotFoundException(ErrorCode.USER_NOT_FOUND, null);
        }
        if (command.email() != null) {
            user.setEmail(command.email());
        }
        if (command.nickname() != null) {
            user.setNickname(command.nickname());
        }
        if (command.role() != null) {
            user.setRole(command.role());
        }
        if (command.isActive() != null) {
            user.setIsActive(command.isActive());
        }
        if (command.avatarUrl() != null) {
            user.setAvatarUrl(command.avatarUrl());
        }
        if (command.website() != null) {
            user.setWebsite(command.website());
        }
        if (command.bio() != null) {
            user.setBio(command.bio());
        }
        if (command.password() != null) {
            user.setPasswordHash(auth.hash(command.password()));
        }
        user.setUpdatedAt(OffsetDateTime.now());
        users.save(user);
        return user;
    }

    @Override
    @Transactional
    public void delete(CurrentUser actor, UUID id) {
        Authorization.requireSuperadmin(actor);
        if (!users.remove(id)) {
            throw new NotFoundException(ErrorCode.USER_NOT_FOUND, null);
        }
    }
}
