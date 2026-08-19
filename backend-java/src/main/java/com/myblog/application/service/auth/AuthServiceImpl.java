package com.myblog.application.service.auth;

import com.myblog.application.model.entity.User;
import com.myblog.common.context.RequestContext;
import com.myblog.common.exception.UnauthorizedException;
import com.myblog.common.exception.ValidationException;
import com.myblog.common.enumeration.ErrorCode;
import com.myblog.application.port.TokenClaims;
import com.myblog.application.port.TokenService;
import com.myblog.application.repository.UserRepository;
import com.myblog.common.properties.AppProperties;
import com.myblog.application.model.vo.AuthResultVO;
import com.myblog.application.model.vo.TokenPairVO;
import com.myblog.application.model.vo.UserPublicVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 认证服务实现：基于 BCrypt 校验密码，依赖 TokenService 签发令牌。
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository users;
    private final TokenService tokens;
    private final AppProperties props;
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(12); // 强度 12 的 BCrypt 密码编码器

    public AuthServiceImpl(UserRepository users, TokenService tokens, AppProperties props) {
        this.users = users;
        this.tokens = tokens;
        this.props = props;
    }

    @Override
    @Transactional
    /**
     * 登录：用户不存在、被停用或密码不匹配统一按凭证无效处理；成功后更新最后登录时间并签发令牌对。
     */
    public AuthResultVO login(String username, String password) {
        User user = users.findByUsername(username);
        if (user == null
                || !Boolean.TRUE.equals(user.getIsActive())
                || !bcrypt.matches(password, user.getPasswordHash())) {
            log.warn("登录失败：username={}, ip={}", username, RequestContext.getIp());
            throw new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS, null);
        }
        user.setLastLoginAt(OffsetDateTime.now());
        users.save(user);
        log.info("登录成功：username={}, ip={}", username, RequestContext.getIp());
        return new AuthResultVO(tokens.pair(user), publicUser(user));
    }

    @Override
    /**
     * 刷新令牌：解析 refresh 令牌并确认账号仍启用，然后换发新令牌对。
     */
    public TokenPairVO refresh(String token) {
        TokenClaims claims = tokens.parse(token, "refresh");
        User user = users.findById(claims.userId());
        if (user == null || !Boolean.TRUE.equals(user.getIsActive())) {
            throw new UnauthorizedException(ErrorCode.ACCOUNT_DISABLED, null);
        }
        return tokens.pair(user);
    }

    @Override
    /**
     * 按 ID 取当前用户，不存在时视为认证失败。
     */
    public User current(UUID id) {
        User user = users.findById(id);
        if (user == null) {
            throw new UnauthorizedException(ErrorCode.AUTHENTICATION_FAILED, null);
        }
        return user;
    }

    @Override
    /**
     * 转换为对外公开的用户视图（仅 id、用户名、角色）。
     */
    public UserPublicVO publicUser(User user) {
        return new UserPublicVO(
                user.getId(),
                user.getUsername(),
                user.getRole());
    }

    @Override
    @Transactional
    /**
     * 修改密码：先校验旧密码，再写入新密码哈希。
     */
    public void change(UUID id, String oldPassword, String newPassword) {
        User user = current(id);
        if (!bcrypt.matches(oldPassword, user.getPasswordHash())) {
            throw new ValidationException(ErrorCode.OLD_PASSWORD_INCORRECT, null);
        }
        user.setPasswordHash(bcrypt.encode(newPassword));
        users.save(user);
        log.info("密码已修改：username={}, ip={}", user.getUsername(), RequestContext.getIp());
    }

    @Override
    @Transactional
    /**
     * 初始化兜底：仅当系统没有任何用户时，按配置创建初始超级管理员。
     */
    public void ensureAdmin() {
        if (users.countAll() > 0) {
            return;
        }
        User admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setUsername(props.initialAdminUsername());
        admin.setPasswordHash(bcrypt.encode(props.initialAdminPassword()));
        admin.setRole("superadmin");
        admin.setIsActive(true);
        OffsetDateTime now = OffsetDateTime.now();
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        users.add(admin);
    }

    @Override
    /**
     * 计算明文密码的 BCrypt 哈希。
     */
    public String hash(String password) {
        return bcrypt.encode(password);
    }
}
