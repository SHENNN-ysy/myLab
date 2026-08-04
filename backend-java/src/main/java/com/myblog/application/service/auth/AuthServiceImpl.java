package com.myblog.application.service.auth;

import com.myblog.application.model.entity.User;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository users;
    private final TokenService tokens;
    private final AppProperties props;
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(12);

    public AuthServiceImpl(UserRepository users, TokenService tokens, AppProperties props) {
        this.users = users;
        this.tokens = tokens;
        this.props = props;
    }

    @Override
    @Transactional
    public AuthResultVO login(String username, String password) {
        User user = users.findByUsername(username);
        if (user == null
                || !Boolean.TRUE.equals(user.getIsActive())
                || !bcrypt.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS, null);
        }
        user.setLastLoginAt(OffsetDateTime.now());
        users.save(user);
        return new AuthResultVO(tokens.pair(user), publicUser(user));
    }

    @Override
    public TokenPairVO refresh(String token) {
        TokenClaims claims = tokens.parse(token, "refresh");
        User user = users.findById(claims.userId());
        if (user == null || !Boolean.TRUE.equals(user.getIsActive())) {
            throw new UnauthorizedException(ErrorCode.ACCOUNT_DISABLED, null);
        }
        return tokens.pair(user);
    }

    @Override
    public User current(UUID id) {
        User user = users.findById(id);
        if (user == null) {
            throw new UnauthorizedException(ErrorCode.AUTHENTICATION_FAILED, null);
        }
        return user;
    }

    @Override
    public UserPublicVO publicUser(User user) {
        return new UserPublicVO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getAvatarUrl());
    }

    @Override
    @Transactional
    public void change(UUID id, String oldPassword, String newPassword) {
        User user = current(id);
        if (!bcrypt.matches(oldPassword, user.getPasswordHash())) {
            throw new ValidationException(ErrorCode.OLD_PASSWORD_INCORRECT, null);
        }
        user.setPasswordHash(bcrypt.encode(newPassword));
        users.save(user);
    }

    @Override
    @Transactional
    public void ensureAdmin() {
        if (users.countAll() > 0) {
            return;
        }
        User admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setUsername(props.initialAdminUsername());
        admin.setEmail(props.initialAdminEmail());
        admin.setNickname("Administrator");
        admin.setPasswordHash(bcrypt.encode(props.initialAdminPassword()));
        admin.setRole("superadmin");
        admin.setIsActive(true);
        OffsetDateTime now = OffsetDateTime.now();
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        users.add(admin);
    }

    @Override
    public String hash(String password) {
        return bcrypt.encode(password);
    }
}
