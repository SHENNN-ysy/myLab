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

/** 访客身份服务：Cookie 令牌的签发、复用与 HMAC 哈希行为测试。 */
@ExtendWith(MockitoExtension.class)
class VisitorIdentityServiceTest {
    private static final String VALID_TOKEN = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQ"; // 43 位合法令牌

    @Mock EngagementStore store;

    private VisitorIdentityService service;

    @BeforeEach
    void setUp() {
        // 测试密钥 + 非 HTTPS 环境（cookieSecure=false）
        service = new VisitorIdentityService(store, "test-secret", false);
    }

    /** Cookie 缺失时签发新令牌与访客哈希，并在存储中登记新访客。 */
    @Test
    void issuesNewIdentityWhenCookieIsMissing() {
        EngagementDtos.VisitorIdentity identity = service.resolve(null);

        assertThat(identity.issued()).isTrue();
        assertThat(identity.token()).matches("^[A-Za-z0-9_-]{43}$");
        assertThat(identity.visitorHash()).matches("^[0-9a-f]{64}$");
        verify(store).createVisitor(identity.visitorHash());
        verify(store, never()).visitorExists(anyString());
    }

    /** Cookie 格式非法时按新访客处理：重新签发并登记，不查询既有访客。 */
    @Test
    void issuesNewIdentityWhenCookieIsMalformed() {
        EngagementDtos.VisitorIdentity identity = service.resolve("not-a-valid-token");

        assertThat(identity.issued()).isTrue();
        assertThat(identity.token()).matches("^[A-Za-z0-9_-]{43}$");
        verify(store).createVisitor(identity.visitorHash());
        verify(store, never()).visitorExists(anyString());
    }

    /** 存储已认识该令牌时复用原 Cookie 身份，不签发新令牌也不重复登记。 */
    @Test
    void reusesCookieWhenIdentityIsStillKnownToStore() {
        when(store.visitorExists(anyString())).thenReturn(true);

        EngagementDtos.VisitorIdentity identity = service.resolve(VALID_TOKEN);

        assertThat(identity.issued()).isFalse();
        assertThat(identity.token()).isEqualTo(VALID_TOKEN);
        assertThat(identity.visitorHash()).matches("^[0-9a-f]{64}$");
        verify(store, never()).createVisitor(anyString());
    }

    /** 令牌格式合法但存储不认识时，视为已失效的旧访客，重新签发新身份。 */
    @Test
    void issuesNewIdentityWhenCookieIsUnknownToStore() {
        when(store.visitorExists(anyString())).thenReturn(false);

        EngagementDtos.VisitorIdentity identity = service.resolve(VALID_TOKEN);

        assertThat(identity.issued()).isTrue();
        assertThat(identity.token()).isNotEqualTo(VALID_TOKEN);
        verify(store).createVisitor(identity.visitorHash());
    }

    /** 相同密钥与令牌产生相同哈希，保证同一访客的互动统计可关联。 */
    @Test
    void hashIsDeterministicForSameSecretAndToken() {
        when(store.visitorExists(anyString())).thenReturn(true);
        VisitorIdentityService sameSecret = new VisitorIdentityService(store, "test-secret", false);

        EngagementDtos.VisitorIdentity first = service.resolve(VALID_TOKEN);
        EngagementDtos.VisitorIdentity second = sameSecret.resolve(VALID_TOKEN);

        assertThat(first.visitorHash()).isEqualTo(second.visitorHash());
    }

    /** 不同密钥对同一令牌产生不同哈希，防止跨环境哈希碰撞泄露访客关联。 */
    @Test
    void differentSecretsProduceDifferentHashes() {
        when(store.visitorExists(anyString())).thenReturn(true);
        VisitorIdentityService otherSecret = new VisitorIdentityService(store, "other-secret", false);

        assertThat(service.resolve(VALID_TOKEN).visitorHash())
                .isNotEqualTo(otherSecret.resolve(VALID_TOKEN).visitorHash());
    }

    /** Cookie 的 Secure 属性跟随构造时的配置项，而不是固定值。 */
    @Test
    void cookieSecureReflectsConfiguration() {
        assertThat(service.cookieSecure()).isFalse();
        assertThat(new VisitorIdentityService(store, "test-secret", true).cookieSecure()).isTrue();
    }
}
