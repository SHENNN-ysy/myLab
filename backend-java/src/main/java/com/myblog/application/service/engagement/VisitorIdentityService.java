package com.myblog.application.service.engagement;

import com.myblog.application.model.dto.EngagementDtos;
import com.myblog.application.port.EngagementStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * 匿名访客身份服务：为未登录访客签发和解析身份，供浏览/点赞按访客去重。
 * 设计上只把 Cookie 令牌的 HMAC-SHA256 哈希写入服务端存储，原始令牌不落库，
 * 服务端无法反推访客标识，满足互动统计的隐私边界。
 */
@Service
public class VisitorIdentityService {
    public static final String COOKIE_NAME = "myblog_visitor";
    private static final Pattern TOKEN_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{43}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EngagementStore store;
    private final byte[] secret;
    private final boolean cookieSecure;

    public VisitorIdentityService(EngagementStore store,
                                  @Value("${app.engagement-hash-secret}") String hashSecret,
                                  @Value("${app.visitor-cookie-secure:false}") boolean cookieSecure) {
        this.store = store;
        this.secret = hashSecret.getBytes(StandardCharsets.UTF_8);
        this.cookieSecure = cookieSecure;
    }

    /**
     * 解析或签发访客身份：Cookie 令牌格式合法且对应身份仍在有效期内（Redis 未过期）则直接复用；
     * 否则生成新的随机令牌并登记其哈希，issued=true 提示控制器回写新 Cookie。
     * 复用时不刷新有效期，访客身份到期后自然换新。
     */
    public EngagementDtos.VisitorIdentity resolve(String cookieToken) {
        if (cookieToken != null && TOKEN_PATTERN.matcher(cookieToken).matches()) {
            String visitorHash = hash(cookieToken);
            if (store.visitorExists(visitorHash)) {
                return new EngagementDtos.VisitorIdentity(cookieToken, visitorHash, false);
            }
        }

        byte[] random = new byte[32];
        RANDOM.nextBytes(random);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        String visitorHash = hash(token);
        store.createVisitor(visitorHash);
        return new EngagementDtos.VisitorIdentity(token, visitorHash, true);
    }

    public boolean cookieSecure() {
        return cookieSecure;
    }

    /** 令牌转访客哈希：带服务端密钥的 HMAC-SHA256，数据库泄漏时无法由哈希反推或伪造令牌。 */
    private String hash(String token) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(token.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", exception);
        }
    }
}
