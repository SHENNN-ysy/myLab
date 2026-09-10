package com.myblog.application.service.user;

import com.myblog.application.model.command.user.UserCommands;
import com.myblog.application.model.entity.User;
import com.myblog.application.model.vo.UserOutVO;
import com.myblog.application.port.SessionService;
import com.myblog.application.repository.UserRepository;
import com.myblog.application.service.auth.AuthService;
import com.myblog.common.exception.ConflictException;
import com.myblog.common.exception.ForbiddenException;
import com.myblog.common.exception.NotFoundException;
import com.myblog.common.exception.ValidationException;
import com.myblog.common.result.PageResult;
import com.myblog.common.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** UserServiceImpl 单元测试：后台用户分页、创建、更新、删除及角色权限控制，仓储/认证/会话全部 mock。 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock UserRepository users;
    @Mock AuthService auth;
    @Mock SessionService sessions;

    private UserServiceImpl service;
    private CurrentUser admin;
    private CurrentUser superadmin;
    private CurrentUser viewer;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(users, auth, sessions);
        admin = new CurrentUser(UUID.randomUUID(), "admin", "admin");
        superadmin = new CurrentUser(UUID.randomUUID(), "root", "superadmin");
        viewer = new CurrentUser(UUID.randomUUID(), "guest", "viewer");
    }

    /** viewer 角色查询用户分页被拒绝。 */
    @Test
    void pageRequiresAdmin() {
        assertThatThrownBy(() -> service.page(viewer, 1, 20))
                .isInstanceOf(ForbiddenException.class);
    }

    /** 分页结果正确映射为输出视图并保留总数。 */
    @Test
    void pageMapsUsersToOutputView() {
        User user = persistedUser("editor", "editor");
        when(users.findPage(1, 20)).thenReturn(PageResult.of(List.of(user), 1, 20, 1));

        PageResult<UserOutVO> result = service.page(admin, 1, 20);

        assertThat(result.total()).isEqualTo(1);
        UserOutVO view = result.records().getFirst();
        assertThat(view.id()).isEqualTo(user.getId());
        assertThat(view.username()).isEqualTo("editor");
        assertThat(view.role()).isEqualTo("editor");
        assertThat(view.isActive()).isTrue();
    }

    /** 普通 admin 创建用户被拒绝，仅 superadmin 可创建。 */
    @Test
    void createRequiresSuperadmin() {
        assertThatThrownBy(() -> service.create(admin, new UserCommands.Create("newuser", null, "password-1")))
                .isInstanceOf(ForbiddenException.class);
    }

    /** 缺少用户名或密码时拒绝创建。 */
    @Test
    void createRejectsMissingUsernameOrPassword() {
        assertThatThrownBy(() -> service.create(superadmin, new UserCommands.Create(null, null, "password-1")))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.create(superadmin, new UserCommands.Create("newuser", null, null)))
                .isInstanceOf(ValidationException.class);
    }

    /** 用户名或密码长度不足时拒绝创建。 */
    @Test
    void createRejectsShortUsernameOrPassword() {
        assertThatThrownBy(() -> service.create(superadmin, new UserCommands.Create("ab", null, "password-1")))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.create(superadmin, new UserCommands.Create("newuser", null, "short")))
                .isInstanceOf(ValidationException.class);
    }

    /** 用户名已存在时抛冲突异常且不落库。 */
    @Test
    void createRejectsDuplicateUsername() {
        when(users.usernameExists("taken")).thenReturn(true);

        assertThatThrownBy(() -> service.create(superadmin, new UserCommands.Create("taken", null, "password-1")))
                .isInstanceOf(ConflictException.class);
        verify(users, never()).add(any());
    }

    /** 未指定角色时默认 viewer，密码经哈希后落库。 */
    @Test
    void createDefaultsRoleToViewerAndHashesPassword() {
        when(auth.hash("password-1")).thenReturn("hashed-password");

        UserOutVO result = service.create(superadmin, new UserCommands.Create("newuser", null, "password-1"));

        ArgumentCaptor<User> added = ArgumentCaptor.forClass(User.class);
        verify(users).add(added.capture());
        User user = added.getValue();
        assertThat(user.getId()).isNotNull();
        assertThat(user.getUsername()).isEqualTo("newuser");
        assertThat(user.getRole()).isEqualTo("viewer");
        assertThat(user.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(user.getIsActive()).isTrue();
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(result.username()).isEqualTo("newuser");
        assertThat(result.role()).isEqualTo("viewer");
    }

    /** 非法角色被拒绝且不落库。 */
    @Test
    void createRejectsInvalidRole() {
        // "root" 不在允许的角色集合内
        assertThatThrownBy(() -> service.create(superadmin, new UserCommands.Create("newuser", "root", "password-1")))
                .isInstanceOf(ValidationException.class);
        verify(users, never()).add(any());
    }

    /** 显式指定合法角色时按指定角色创建。 */
    @Test
    void createKeepsExplicitRole() {
        when(auth.hash("password-1")).thenReturn("hashed-password");

        UserOutVO result = service.create(superadmin, new UserCommands.Create("newuser", "editor", "password-1"));

        assertThat(result.role()).isEqualTo("editor");
    }

    /** viewer 角色更新用户被拒绝。 */
    @Test
    void updateRequiresAdmin() {
        assertThatThrownBy(() -> service.update(viewer, UUID.randomUUID(), new UserCommands.Update("editor", null, null)))
                .isInstanceOf(ForbiddenException.class);
    }

    /** 更新不存在的用户抛未找到异常。 */
    @Test
    void updateRejectsUnknownUser() {
        UUID id = UUID.randomUUID();
        when(users.findByIdForUpdate(id)).thenReturn(null);

        assertThatThrownBy(() -> service.update(admin, id, new UserCommands.Update("editor", null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    /** 更新为非法角色被拒绝且不落库。 */
    @Test
    void updateRejectsInvalidRole() {
        User user = persistedUser("editor", "editor");
        when(users.findByIdForUpdate(user.getId())).thenReturn(user);

        assertThatThrownBy(() -> service.update(admin, user.getId(), new UserCommands.Update("root", null, null)))
                .isInstanceOf(ValidationException.class);
        verify(users, never()).save(any());
    }

    /** superadmin 账号不允许被修改。 */
    @Test
    void updateRejectsSuperadminTarget() {
        User user = persistedUser("root", "superadmin");
        when(users.findByIdForUpdate(user.getId())).thenReturn(user);

        assertThatThrownBy(() -> service.update(
                superadmin, user.getId(), new UserCommands.Update(null, false, null)))
                .isInstanceOf(ForbiddenException.class);
        verify(users, never()).save(any());
    }

    /** 仅更新提供的字段：未给密码不重新哈希，更新后吊销会话。 */
    @Test
    void updateAppliesOnlyProvidedFields() {
        User user = persistedUser("editor", "editor");
        OffsetDateTime previousUpdate = user.getUpdatedAt();
        when(users.findByIdForUpdate(user.getId())).thenReturn(user);

        UserOutVO result = service.update(admin, user.getId(), new UserCommands.Update(null, false, null));

        assertThat(result.role()).isEqualTo("editor");
        assertThat(result.isActive()).isFalse();
        assertThat(result.updatedAt()).isAfterOrEqualTo(previousUpdate);
        verify(users).save(user);
        verify(sessions).revokeAll(user.getId());
        verify(auth, never()).hash(any());
    }

    /** 提供新密码时重新哈希保存，并吊销该用户全部会话。 */
    @Test
    void updateHashesProvidedPassword() {
        User user = persistedUser("editor", "editor");
        when(users.findByIdForUpdate(user.getId())).thenReturn(user);
        when(auth.hash("new-password-1")).thenReturn("new-hash");

        service.update(admin, user.getId(), new UserCommands.Update("admin", null, "new-password-1"));

        assertThat(user.getRole()).isEqualTo("admin");
        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(users).save(user);
        verify(sessions).revokeAll(user.getId());
    }

    /** 普通 admin 删除用户被拒绝，仅 superadmin 可删除。 */
    @Test
    void deleteRequiresSuperadmin() {
        assertThatThrownBy(() -> service.delete(admin, UUID.randomUUID()))
                .isInstanceOf(ForbiddenException.class);
    }

    /** 删除不存在的用户抛未找到异常。 */
    @Test
    void deleteRejectsUnknownUser() {
        UUID id = UUID.randomUUID();
        when(users.findByIdForUpdate(id)).thenReturn(null);

        assertThatThrownBy(() -> service.delete(superadmin, id))
                .isInstanceOf(NotFoundException.class);
    }

    /** superadmin 账号不允许被删除。 */
    @Test
    void deleteRejectsSuperadminTarget() {
        User user = persistedUser("root", "superadmin");
        when(users.findByIdForUpdate(user.getId())).thenReturn(user);

        assertThatThrownBy(() -> service.delete(superadmin, user.getId()))
                .isInstanceOf(ForbiddenException.class);
        verify(users, never()).remove(user.getId());
    }

    /** 行锁等待期间目标被并发删除时按未找到处理。 */
    @Test
    void deleteRejectsConcurrentRemoval() {
        User user = persistedUser("editor", "editor");
        when(users.findByIdForUpdate(user.getId())).thenReturn(user);
        // remove 返回 false 模拟行锁等待期间记录已被并发删除
        when(users.remove(user.getId())).thenReturn(false);

        assertThatThrownBy(() -> service.delete(superadmin, user.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    /** 删除成功后吊销该用户全部会话。 */
    @Test
    void deleteRemovesExistingUser() {
        User user = persistedUser("editor", "editor");
        when(users.findByIdForUpdate(user.getId())).thenReturn(user);
        when(users.remove(user.getId())).thenReturn(true);

        service.delete(superadmin, user.getId());

        verify(users).remove(user.getId());
        verify(sessions).revokeAll(user.getId());
    }

    private User persistedUser(String username, String role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setRole(role);
        user.setIsActive(true);
        user.setPasswordHash("stored-hash");
        OffsetDateTime now = OffsetDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return user;
    }
}
