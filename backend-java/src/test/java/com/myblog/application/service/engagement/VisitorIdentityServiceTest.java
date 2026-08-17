package com.myblog.application.service.engagement;

import com.myblog.application.model.dto.EngagementDtos;
import com.myblog.application.port.EngagementStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitorIdentityServiceTest {
    private static final String VALID_TOKEN = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQ"; // 43 位合法令牌

    @Mock EngagementStore store;

    private VisitorIdentityService service;

    @BeforeEach
    void setUp() {
        service = new VisitorIdentityService(store, "test-secret", false);
    }

    @Test
    void issuesNewIdentityWhenCookieIsMissing() {
        EngagementDtos.VisitorIdentity identity = service.resolve(null);

        assertThat(identity.issued()).isTrue();
        assertThat(identity.token()).matches("^[A-Za-z0-9_-]{43}$");
        assertThat(identity.visitorHash()).matches("^[0-9a-f]{64}$");
        verify(store).createVisitor(identity.visitorHash());
        verify(store, never()).visitorExists(anyString());
    }

    @Test
    void issuesNewIdentityWhenCookieIsMalformed() {
        EngagementDtos.VisitorIdentity identity = service.resolve("not-a-valid-token");

        assertThat(identity.issued()).isTrue();
        assertThat(identity.token()).matches("^[A-Za-z0-9_-]{43}$");
        verify(store).createVisitor(identity.visitorHash());
        verify(store, never()).visitorExists(anyString());
    }

    @Test
    void reusesCookieWhenIdentityIsStillKnownToStore() {
        when(store.visitorExists(anyString())).thenReturn(true);

        EngagementDtos.VisitorIdentity identity = service.resolve(VALID_TOKEN);

        assertThat(identity.issued()).isFalse();
        assertThat(identity.token()).isEqualTo(VALID_TOKEN);
        assertThat(identity.visitorHash()).matches("^[0-9a-f]{64}$");
        verify(store, never()).createVisitor(anyString());
    }

    @Test
    void issuesNewIdentityWhenCookieIsUnknownToStore() {
        when(store.visitorExists(anyString())).thenReturn(false);

        EngagementDtos.VisitorIdentity identity = service.resolve(VALID_TOKEN);

        assertThat(identity.issued()).isTrue();
        assertThat(identity.token()).isNotEqualTo(VALID_TOKEN);
        verify(store).createVisitor(identity.visitorHash());
    }

    @Test
    void hashIsDeterministicForSameSecretAndToken() {
        when(store.visitorExists(anyString())).thenReturn(true);
        VisitorIdentityService sameSecret = new VisitorIdentityService(store, "test-secret", false);

        EngagementDtos.VisitorIdentity first = service.resolve(VALID_TOKEN);
        EngagementDtos.VisitorIdentity second = sameSecret.resolve(VALID_TOKEN);

        assertThat(first.visitorHash()).isEqualTo(second.visitorHash());
    }

    @Test
    void differentSecretsProduceDifferentHashes() {
        when(store.visitorExists(anyString())).thenReturn(true);
        VisitorIdentityService otherSecret = new VisitorIdentityService(store, "other-secret", false);

        assertThat(service.resolve(VALID_TOKEN).visitorHash())
                .isNotEqualTo(otherSecret.resolve(VALID_TOKEN).visitorHash());
    }

    @Test
    void cookieSecureReflectsConfiguration() {
        assertThat(service.cookieSecure()).isFalse();
        assertThat(new VisitorIdentityService(store, "test-secret", true).cookieSecure()).isTrue();
    }
}
