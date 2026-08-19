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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 用户管理服务实现：创建/删除限定超级管理员，密码统一经 AuthService 哈希后落库。
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    // 与 users 表 users_role_check 约束保持一致，服务层先拦截非法角色，
    // 避免约束冲突冒泡成 500。
    private static final Set<String> ALLOWED_ROLES = Set.of("viewer", "editor", "admin", "superadmin");

    private final UserRepository users;
    private final AuthService auth;

    public UserServiceImpl(UserRepository users, AuthService auth) {
        this.users = users;
        this.auth = auth;
    }

    @Override
    /**
     * 分页查询用户列表（仅管理员）。
     */
    public PageResult<UserOutVO> page(CurrentUser actor, long page, long size) {
        Authorization.requireAdmin(actor);
        PageResult<User> result = users.findPage(page, size);
        return PageResult.of(result.records().stream().map(this::toOut).toList(),
                result.page(), result.pageSize(), result.total());
    }

    @Override
    @Transactional
    /**
     * 创建用户：校验用户名/密码长度与用户名唯一性，角色缺省为 viewer（仅超级管理员）。
     */
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
        String role = Objects.requireNonNullElse(command.role(), "viewer");
        requireValidRole(role);
        user.setRole(role);
        user.setPasswordHash(auth.hash(password));
        user.setIsActive(true);
        OffsetDateTime now = OffsetDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        users.add(user);
        log.info("用户已创建：operator={}, username={}, role={}", actor.username(), username, role);
        return toOut(user);
    }

    @Override
    @Transactional
    /**
     * 更新用户：仅更新请求中显式给出的字段（角色、启用状态、密码）。
     */
    public UserOutVO update(CurrentUser actor, UUID id, UserCommands.Update command) {
        Authorization.requireAdmin(actor);
        User user = users.findById(id);
        if (user == null) {
            throw new NotFoundException(ErrorCode.USER_NOT_FOUND, null);
        }
        List<String> changed = new ArrayList<>();
        if (command.role() != null) {
            requireValidRole(command.role());
            user.setRole(command.role());
            changed.add("role");
        }
        if (command.isActive() != null) {
            user.setIsActive(command.isActive());
            changed.add("is_active");
        }
        if (command.password() != null) {
            user.setPasswordHash(auth.hash(command.password()));
            changed.add("password");
        }
        user.setUpdatedAt(OffsetDateTime.now());
        users.save(user);
        log.info("用户已更新：operator={}, target={}, fields={}", actor.username(), user.getUsername(), changed);
        return toOut(user);
    }

    @Override
    @Transactional
    /**
     * 删除用户，不存在时抛 NotFoundException（仅超级管理员）。
     */
    public void delete(CurrentUser actor, UUID id) {
        Authorization.requireSuperadmin(actor);
        if (!users.remove(id)) {
            throw new NotFoundException(ErrorCode.USER_NOT_FOUND, null);
        }
        log.info("用户已删除：operator={}, targetId={}", actor.username(), id);
    }

    /** 实体转输出视图，剥离密码哈希等敏感字段。 */
    private UserOutVO toOut(User user) {
        return new UserOutVO(user.getId(), user.getUsername(), user.getRole(), user.getIsActive(),
                user.getLastLoginAt(), user.getCreatedAt(), user.getUpdatedAt());
    }

    private static void requireValidRole(String role) {
        if (!ALLOWED_ROLES.contains(role)) {
            throw new ValidationException("role 只允许 viewer、editor、admin 或 superadmin");
        }
    }
}
