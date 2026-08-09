package com.myblog.application.service.user;

import com.myblog.application.model.entity.User;
import com.myblog.application.model.vo.UserOutVO;
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
    public PageResult<UserOutVO> page(CurrentUser actor, long page, long size) {
        Authorization.requireAdmin(actor);
        PageResult<User> result = users.findPage(page, size);
        return PageResult.of(result.records().stream().map(this::toOut).toList(),
                result.page(), result.pageSize(), result.total());
    }

    @Override
    @Transactional
    public UserOutVO create(CurrentUser actor, UserCommands.Create command) {
        Authorization.requireSuperadmin(actor);
        if (command.username() == null || command.password() == null) {
            throw new ValidationException("username 和 password 为必填字段");
        }
        String username = command.username();
        String password = command.password();
        if (username.length() < 3 || password.length() < 8) {
            throw new ValidationException("用户名至少 3 位且密码至少 8 位");
        }
        if (users.usernameExists(username)) {
            throw new ConflictException(ErrorCode.USER_ALREADY_EXISTS, null);
        }
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setRole(Objects.requireNonNullElse(command.role(), "viewer"));
        user.setPasswordHash(auth.hash(password));
        user.setIsActive(true);
        OffsetDateTime now = OffsetDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        users.add(user);
        return toOut(user);
    }

    @Override
    @Transactional
    public UserOutVO update(CurrentUser actor, UUID id, UserCommands.Update command) {
        Authorization.requireAdmin(actor);
        User user = users.findById(id);
        if (user == null) {
            throw new NotFoundException(ErrorCode.USER_NOT_FOUND, null);
        }
        if (command.role() != null) {
            user.setRole(command.role());
        }
        if (command.isActive() != null) {
            user.setIsActive(command.isActive());
        }
        if (command.password() != null) {
            user.setPasswordHash(auth.hash(command.password()));
        }
        user.setUpdatedAt(OffsetDateTime.now());
        users.save(user);
        return toOut(user);
    }

    @Override
    @Transactional
    public void delete(CurrentUser actor, UUID id) {
        Authorization.requireSuperadmin(actor);
        if (!users.remove(id)) {
            throw new NotFoundException(ErrorCode.USER_NOT_FOUND, null);
        }
    }

    private UserOutVO toOut(User user) {
        return new UserOutVO(user.getId(), user.getUsername(), user.getRole(), user.getIsActive(),
                user.getLastLoginAt(), user.getCreatedAt(), user.getUpdatedAt());
    }
}
