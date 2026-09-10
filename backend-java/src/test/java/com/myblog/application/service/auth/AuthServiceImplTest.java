package com.myblog.application.service.auth;

import com.myblog.application.model.entity.User;
import com.myblog.application.model.vo.AccessTokenVO;
import com.myblog.application.model.vo.AuthResultVO;
import com.myblog.application.model.vo.UserPublicVO;
import com.myblog.application.port.SessionService;
import com.myblog.application.repository.UserRepository;
import com.myblog.common.exception.ConflictException;
import com.myblog.common.exception.UnauthorizedException;
import com.myblog.common.exception.ValidationException;
import com.myblog.common.properties.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** AuthServiceImpl 单元测试：登录、当前用户、密码与账号修改、初始管理员创建与种子接管，仓储与会话全部 mock。 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock UserRepository users;
    @Mock SessionService sessions;
    @Mock AppProperties props;

    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(users, sessions, props);
    }

    /** 用户名不存在时登录按未认证拒绝。 */
    @Test
    void loginRejectsUnknownUsername() {
        when(users.findByUsernameForUpdate("ghost")).thenReturn(null);

        assertThatThrownBy(() -> service.login("ghost", "whatever"))
                .isInstanceOf(UnauthorizedException.class);
    }

    /** 已停用账号即使密码正确也按未认证拒绝。 */
    @Test
    void loginRejectsDisabledAccount() {
        User user = activeUser("correct-password");
        user.setIsActive(false);
        when(users.findByUsernameForUpdate("admin")).thenReturn(user);

        assertThatThrownBy(() -> service.login("admin", "correct-password"))
                .isInstanceOf(UnauthorizedException.class);
    }

    /** 密码错误时登录按未认证拒绝。 */
    @Test
    void loginRejectsWrongPassword() {
        when(users.findByUsernameForUpdate("admin")).thenReturn(activeUser("correct-password"));

        assertThatThrownBy(() -> service.login("admin", "wrong-password"))
                .isInstanceOf(UnauthorizedException.class);
    }

    /** 登录成功签发令牌、回写最后登录时间并保存用户。 */
    @Test
    void loginUpdatesLastLoginAndIssuesSession() {
        User user = activeUser("correct-password");
        AccessTokenVO token = new AccessTokenVO("access", "bearer", 28800); // 28800 秒即 8 小时会话时长
        when(users.findByUsernameForUpdate("admin")).thenReturn(user);
        when(sessions.issue(user)).thenReturn(token);

        AuthResultVO result = service.login("admin", "correct-password");

        assertThat(result.tokens()).isEqualTo(token);
        assertThat(result.user()).isEqualTo(new UserPublicVO(user.getId(), "admin", "admin"));
        assertThat(user.getLastLoginAt()).isNotNull();
        verify(users).save(user);
    }

    /** 会话对应的用户不存在时按未认证处理。 */
    @Test
    void currentRejectsUnknownUser() {
        UUID id = UUID.randomUUID();
        when(users.findById(id)).thenReturn(null);

        assertThatThrownBy(() -> service.current(id))
                .isInstanceOf(UnauthorizedException.class);
    }

    /** 按 ID 返回已存在的用户实体。 */
    @Test
    void currentReturnsExistingUser() {
        User user = activeUser("correct-password");
        when(users.findById(user.getId())).thenReturn(user);

        assertThat(service.current(user.getId())).isSameAs(user);
    }

    /** 公开视图仅暴露 ID、用户名与角色。 */
    @Test
    void publicUserExposesOnlyPublicFields() {
        User user = activeUser("correct-password");

        UserPublicVO view = service.publicUser(user);

        assertThat(view.id()).isEqualTo(user.getId());
        assertThat(view.username()).isEqualTo("admin");
        assertThat(view.role()).isEqualTo("admin");
    }

    /** 旧密码错误时拒绝修改，不落库也不吊销会话。 */
    @Test
    void changeRejectsWrongOldPassword() {
        User user = activeUser("old-password");
        when(users.findByIdForUpdate(user.getId())).thenReturn(user);

        assertThatThrownBy(() -> service.change(user.getId(), "not-old-password", "new-password-1"))
                .isInstanceOf(ValidationException.class);
        verify(users, never()).save(org.mockito.ArgumentMatchers.any());
        verify(sessions, never()).revokeAll(user.getId());
    }

    /** 修改密码保存新哈希并吊销该用户全部会话。 */
    @Test
    void changeStoresHashOfNewPassword() {
        User user = activeUser("old-password");
        String previousHash = user.getPasswordHash();
        when(users.findByIdForUpdate(user.getId())).thenReturn(user);

        service.change(user.getId(), "old-password", "new-password-1");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        verify(sessions).revokeAll(user.getId());
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo(previousHash);
        assertThat(new BCryptPasswordEncoder().matches("new-password-1", saved.getValue().getPasswordHash())).isTrue();
    }

    /** 当前密码校验失败时拒绝账号修改。 */
    @Test
    void updateAccountRejectsWrongCurrentPassword() {
        User user = activeUser("old-password");
        when(users.findByIdForUpdate(user.getId())).thenReturn(user);

        assertThatThrownBy(() -> service.updateAccount(
                user.getId(), "new-admin", "wrong-password", null))
                .isInstanceOf(ValidationException.class);
        verify(users, never()).save(org.mockito.ArgumentMatchers.any());
    }

    /** 新用户名过短或新密码不合规时拒绝修改。 */
    @Test
    void updateAccountRejectsInvalidUsernameAndPassword() {
        User user = activeUser("old-password");
        when(users.findByIdForUpdate(user.getId())).thenReturn(user);

        assertThatThrownBy(() -> service.updateAccount(user.getId(), "ab", "old-password", null))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.updateAccount(user.getId(), "new-admin", "old-password", "short"))
                .isInstanceOf(ValidationException.class);
    }

    /** 新用户名已被占用时抛冲突异常且不落库。 */
    @Test
    void updateAccountRejectsDuplicateUsername() {
        User user = activeUser("old-password");
        when(users.findByIdForUpdate(user.getId())).thenReturn(user);
        when(users.usernameExists("taken-name")).thenReturn(true);

        assertThatThrownBy(() -> service.updateAccount(
                user.getId(), "taken-name", "old-password", null))
                .isInstanceOf(ConflictException.class);
        verify(users, never()).save(org.mockito.ArgumentMatchers.any());
    }

    /** 同时修改用户名与密码：用户名去空格保存、密码重新哈希并吊销会话。 */
    @Test
    void updateAccountChangesUsernameAndPassword() {
        User user = activeUser("old-password");
        when(users.findByIdForUpdate(user.getId())).thenReturn(user);

        UserPublicVO updated = service.updateAccount(
                user.getId(), " new-admin ", "old-password", "new-password-1");

        assertThat(updated.username()).isEqualTo("new-admin");
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(new BCryptPasswordEncoder().matches("new-password-1", user.getPasswordHash())).isTrue();
        verify(users).save(user);
        verify(sessions).revokeAll(user.getId());
    }

    /** 不传新密码时保留原密码哈希，仅更新账号并吊销会话。 */
    @Test
    void updateAccountKeepsPasswordWhenNewPasswordIsOmitted() {
        User user = activeUser("old-password");
        String previousHash = user.getPasswordHash();
        when(users.findByIdForUpdate(user.getId())).thenReturn(user);

        service.updateAccount(user.getId(), "admin", "old-password", null);

        assertThat(user.getPasswordHash()).isEqualTo(previousHash);
        verify(users).save(user);
        verify(sessions).revokeAll(user.getId());
    }

    /** 已有用户时不创建初始管理员。 */
    @Test
    void ensureAdminDoesNothingWhenUsersExist() {
        when(users.countAll()).thenReturn(1L);

        service.ensureAdmin();

        verify(users, never()).add(org.mockito.ArgumentMatchers.any());
    }

    /** 基线种子管理员未被改动时按 INIT_ADMIN 配置接管用户名与密码。 */
    @Test
    void ensureAdminMigratesLegacySeedFromConfiguration() {
        // V1 基线种子管理员的固定 ID，与基线用户名、密码哈希完全一致才会被配置接管
        UUID seedId = UUID.fromString("b7b1a013-fc83-579a-a1e3-bb1cc0483bac");
        User seedAdmin = new User();
        seedAdmin.setId(seedId);
        seedAdmin.setUsername("admin");
        seedAdmin.setPasswordHash("$2a$12$6feNM80PGgXs0en.BWDbzeUZzp71yNmPNGakhiHmuzf5TKUxdPOPG");
        when(users.countAll()).thenReturn(1L);
        when(users.findById(seedId)).thenReturn(seedAdmin);
        when(props.initialAdminUsername()).thenReturn("configured-admin");
        when(props.initialAdminPassword()).thenReturn("configured-password");

        service.ensureAdmin();

        assertThat(seedAdmin.getUsername()).isEqualTo("configured-admin");
        assertThat(new BCryptPasswordEncoder().matches(
                "configured-password", seedAdmin.getPasswordHash())).isTrue();
        assertThat(seedAdmin.getUpdatedAt()).isNotNull();
        verify(users).save(seedAdmin);
    }

    /** 种子接管目标用户名已被占用时抛冲突异常。 */
    @Test
    void ensureAdminRejectsLegacyMigrationToDuplicateUsername() {
        // V1 基线种子管理员的固定 ID，与基线用户名、密码哈希完全一致才会被配置接管
        UUID seedId = UUID.fromString("b7b1a013-fc83-579a-a1e3-bb1cc0483bac");
        User seedAdmin = new User();
        seedAdmin.setId(seedId);
        seedAdmin.setUsername("admin");
        seedAdmin.setPasswordHash("$2a$12$6feNM80PGgXs0en.BWDbzeUZzp71yNmPNGakhiHmuzf5TKUxdPOPG");
        when(users.countAll()).thenReturn(2L);
        when(users.findById(seedId)).thenReturn(seedAdmin);
        when(props.initialAdminUsername()).thenReturn("existing-admin");
        when(users.usernameExists("existing-admin")).thenReturn(true);

        assertThatThrownBy(service::ensureAdmin).isInstanceOf(ConflictException.class);
        verify(users, never()).save(org.mockito.ArgumentMatchers.any());
    }

    /** 无任何用户时按配置创建初始 superadmin，密码以 BCrypt 哈希保存。 */
    @Test
    void ensureAdminCreatesInitialSuperadminFromConfiguration() {
        when(users.countAll()).thenReturn(0L);
        when(props.initialAdminUsername()).thenReturn("root");
        when(props.initialAdminPassword()).thenReturn("initial-password");

        service.ensureAdmin();

        ArgumentCaptor<User> added = ArgumentCaptor.forClass(User.class);
        verify(users).add(added.capture());
        User admin = added.getValue();
        assertThat(admin.getId()).isNotNull();
        assertThat(admin.getUsername()).isEqualTo("root");
        assertThat(admin.getRole()).isEqualTo("superadmin");
        assertThat(admin.getIsActive()).isTrue();
        assertThat(admin.getCreatedAt()).isNotNull();
        assertThat(admin.getUpdatedAt()).isNotNull();
        assertThat(new BCryptPasswordEncoder().matches("initial-password", admin.getPasswordHash())).isTrue();
    }

    /** hash 产出 $2 前缀且可被 BCrypt 校验的哈希。 */
    @Test
    void hashProducesVerifiableBcryptHash() {
        String hash = service.hash("plain-password");

        assertThat(hash).startsWith("$2");
        assertThat(new BCryptPasswordEncoder().matches("plain-password", hash)).isTrue();
    }

    private User activeUser(String password) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("admin");
        user.setRole("admin");
        user.setIsActive(true);
        // 低强度仅为加速测试，matches 与强度无关
        user.setPasswordHash(new BCryptPasswordEncoder(4).encode(password));
        return user;
    }
}
