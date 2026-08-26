package com.myblog.application.service.auth;

import com.myblog.application.model.entity.User;
import com.myblog.application.model.vo.AuthResultVO;
import com.myblog.application.model.vo.TokenPairVO;
import com.myblog.application.model.vo.UserPublicVO;
import com.myblog.application.port.TokenClaims;
import com.myblog.application.port.TokenService;
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

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock UserRepository users;
    @Mock TokenService tokens;
    @Mock AppProperties props;

    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(users, tokens, props);
    }

    @Test
    void loginRejectsUnknownUsername() {
        when(users.findByUsername("ghost")).thenReturn(null);

        assertThatThrownBy(() -> service.login("ghost", "whatever"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void loginRejectsDisabledAccount() {
        User user = activeUser("correct-password");
        user.setIsActive(false);
        when(users.findByUsername("admin")).thenReturn(user);

        assertThatThrownBy(() -> service.login("admin", "correct-password"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void loginRejectsWrongPassword() {
        when(users.findByUsername("admin")).thenReturn(activeUser("correct-password"));

        assertThatThrownBy(() -> service.login("admin", "wrong-password"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void loginUpdatesLastLoginAndIssuesTokenPair() {
        User user = activeUser("correct-password");
        TokenPairVO pair = new TokenPairVO("access", "refresh", "Bearer", 3600);
        when(users.findByUsername("admin")).thenReturn(user);
        when(tokens.pair(user)).thenReturn(pair);

        AuthResultVO result = service.login("admin", "correct-password");

        assertThat(result.tokens()).isEqualTo(pair);
        assertThat(result.user()).isEqualTo(new UserPublicVO(user.getId(), "admin", "admin"));
        assertThat(user.getLastLoginAt()).isNotNull();
        verify(users).save(user);
    }

    @Test
    void refreshRejectsUnknownUser() {
        UUID userId = UUID.randomUUID();
        when(tokens.parse("refresh-token", "refresh")).thenReturn(new TokenClaims(userId, "admin"));
        when(users.findById(userId)).thenReturn(null);

        assertThatThrownBy(() -> service.refresh("refresh-token"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refreshRejectsDisabledUser() {
        User user = activeUser("correct-password");
        user.setIsActive(false);
        when(tokens.parse("refresh-token", "refresh")).thenReturn(new TokenClaims(user.getId(), "admin"));
        when(users.findById(user.getId())).thenReturn(user);

        assertThatThrownBy(() -> service.refresh("refresh-token"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refreshIssuesNewPairForActiveUser() {
        User user = activeUser("correct-password");
        TokenPairVO pair = new TokenPairVO("new-access", "new-refresh", "Bearer", 3600);
        when(tokens.parse("refresh-token", "refresh")).thenReturn(new TokenClaims(user.getId(), "admin"));
        when(users.findById(user.getId())).thenReturn(user);
        when(tokens.pair(user)).thenReturn(pair);

        assertThat(service.refresh("refresh-token")).isEqualTo(pair);
    }

    @Test
    void currentRejectsUnknownUser() {
        UUID id = UUID.randomUUID();
        when(users.findById(id)).thenReturn(null);

        assertThatThrownBy(() -> service.current(id))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void currentReturnsExistingUser() {
        User user = activeUser("correct-password");
        when(users.findById(user.getId())).thenReturn(user);

        assertThat(service.current(user.getId())).isSameAs(user);
    }

    @Test
    void publicUserExposesOnlyPublicFields() {
        User user = activeUser("correct-password");

        UserPublicVO view = service.publicUser(user);

        assertThat(view.id()).isEqualTo(user.getId());
        assertThat(view.username()).isEqualTo("admin");
        assertThat(view.role()).isEqualTo("admin");
    }

    @Test
    void changeRejectsWrongOldPassword() {
        User user = activeUser("old-password");
        when(users.findById(user.getId())).thenReturn(user);

        assertThatThrownBy(() -> service.change(user.getId(), "not-old-password", "new-password-1"))
                .isInstanceOf(ValidationException.class);
        verify(users, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void changeStoresHashOfNewPassword() {
        User user = activeUser("old-password");
        String previousHash = user.getPasswordHash();
        when(users.findById(user.getId())).thenReturn(user);

        service.change(user.getId(), "old-password", "new-password-1");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo(previousHash);
        assertThat(new BCryptPasswordEncoder().matches("new-password-1", saved.getValue().getPasswordHash())).isTrue();
    }

    @Test
    void updateAccountRejectsWrongCurrentPassword() {
        User user = activeUser("old-password");
        when(users.findById(user.getId())).thenReturn(user);

        assertThatThrownBy(() -> service.updateAccount(
                user.getId(), "new-admin", "wrong-password", null))
                .isInstanceOf(ValidationException.class);
        verify(users, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateAccountRejectsInvalidUsernameAndPassword() {
        User user = activeUser("old-password");
        when(users.findById(user.getId())).thenReturn(user);

        assertThatThrownBy(() -> service.updateAccount(user.getId(), "ab", "old-password", null))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.updateAccount(user.getId(), "new-admin", "old-password", "short"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void updateAccountRejectsDuplicateUsername() {
        User user = activeUser("old-password");
        when(users.findById(user.getId())).thenReturn(user);
        when(users.usernameExists("taken-name")).thenReturn(true);

        assertThatThrownBy(() -> service.updateAccount(
                user.getId(), "taken-name", "old-password", null))
                .isInstanceOf(ConflictException.class);
        verify(users, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateAccountChangesUsernameAndPassword() {
        User user = activeUser("old-password");
        when(users.findById(user.getId())).thenReturn(user);

        UserPublicVO updated = service.updateAccount(
                user.getId(), " new-admin ", "old-password", "new-password-1");

        assertThat(updated.username()).isEqualTo("new-admin");
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(new BCryptPasswordEncoder().matches("new-password-1", user.getPasswordHash())).isTrue();
        verify(users).save(user);
    }

    @Test
    void updateAccountKeepsPasswordWhenNewPasswordIsOmitted() {
        User user = activeUser("old-password");
        String previousHash = user.getPasswordHash();
        when(users.findById(user.getId())).thenReturn(user);

        service.updateAccount(user.getId(), "admin", "old-password", null);

        assertThat(user.getPasswordHash()).isEqualTo(previousHash);
        verify(users).save(user);
    }

    @Test
    void ensureAdminDoesNothingWhenUsersExist() {
        when(users.countAll()).thenReturn(1L);

        service.ensureAdmin();

        verify(users, never()).add(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void ensureAdminMigratesLegacySeedFromConfiguration() {
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

    @Test
    void ensureAdminRejectsLegacyMigrationToDuplicateUsername() {
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
