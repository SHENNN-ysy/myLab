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

/** 创建和解析固定三天的匿名身份，原始 Cookie 令牌不会写入服务端存储。 */
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
