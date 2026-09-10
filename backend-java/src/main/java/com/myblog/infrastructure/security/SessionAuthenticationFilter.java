package com.myblog.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myblog.application.model.entity.User;
import com.myblog.application.port.SessionIdentity;
import com.myblog.application.port.SessionService;
import com.myblog.common.constant.SecurityConstant;
import com.myblog.common.enumeration.ErrorCode;
import com.myblog.common.exception.AuthenticationUnavailableException;
import com.myblog.common.exception.UnauthorizedException;
import com.myblog.common.result.Result;
import com.myblog.common.security.CurrentUser;
import com.myblog.infrastructure.persistence.mapper.user.UserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Redis Bearer 会话认证过滤器；公开接口完全跳过 Redis 会话访问。 */
@Slf4j
@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final SessionService sessions;
    private final UserMapper users;
    private final ObjectMapper objectMapper;

    public SessionAuthenticationFilter(SessionService sessions, UserMapper users, ObjectMapper objectMapper) {
        this.sessions = sessions;
        this.users = users;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals(SecurityConstant.AUTH_LOGIN)
                || path.equals(SecurityConstant.HEALTH_API)
                || path.startsWith(SecurityConstant.HEALTH_PREFIX)
                || path.startsWith("/v3/api-docs")
                || path.equals(SecurityConstant.SWAGGER_UI)
                || path.startsWith("/swagger-ui/")
                || path.equals("/api/v1/public")
                || path.startsWith("/api/v1/public/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = bearerToken(request);
        if (token == null) {
            chain.doFilter(request, response);
            return;
        }
        try {
            SessionIdentity identity = sessions.authenticate(token);
            User user = users.selectById(identity.userId());
            if (user == null || !Boolean.TRUE.equals(user.getIsActive())) {
                sessions.revokeAll(identity.userId());
                throw new UnauthorizedException(ErrorCode.AUTHENTICATION_FAILED, "账号不存在或已停用");
            }
            CurrentUser principal = new CurrentUser(user.getId(), user.getUsername(), user.getRole());
            var authentication = new UsernamePasswordAuthenticationToken(
                    principal, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase())));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        } catch (UnauthorizedException exception) {
            log.debug("管理会话被拒绝：{}", exception.getDetail());
            chain.doFilter(request, response);
        } catch (AuthenticationUnavailableException exception) {
            writeError(response, ErrorCode.AUTHENTICATION_UNAVAILABLE);
        } catch (DataAccessException exception) {
            log.error("认证过滤器读取用户失败", exception);
            writeError(response, ErrorCode.DATABASE_ERROR);
        }
    }

    /** 只接受标准 Bearer 请求头，格式不正确时按未认证请求处理。 */
    private String bearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    /** Filter 位于 MVC 之前，需在此直接输出统一 Result 错误响应。 */
    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.status().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Result.fail(errorCode, null));
    }
}
