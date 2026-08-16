package com.myblog.infrastructure.security;

import com.myblog.application.model.entity.User;
import com.myblog.application.port.TokenClaims;
import com.myblog.application.port.TokenService;
import com.myblog.infrastructure.persistence.mapper.user.UserMapper;
import com.myblog.common.security.CurrentUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器：解析请求头中的 Bearer 令牌，校验通过且用户处于启用状态时，
 * 把当前用户写入 Spring Security 上下文；令牌缺失或无效时按匿名请求放行，
 * 是否拦截由后续授权规则决定。
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final TokenService tokens; // 令牌解析与校验
    private final UserMapper users;    // 按令牌中的用户 id 加载用户

    public JwtFilter(TokenService tokens, UserMapper users) {
        this.tokens = tokens;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.toLowerCase().startsWith("bearer ")) {
            try {
                TokenClaims claims = tokens.parse(header.substring(7).trim(), "access");
                User user = users.selectById(claims.userId());
                if (user != null && Boolean.TRUE.equals(user.getIsActive())) {
                    CurrentUser principal = new CurrentUser(user.getId(), user.getUsername(), user.getRole());
                    var auth = new UsernamePasswordAuthenticationToken(
                            principal, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase())));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ignored) {
                // 令牌无效/过期/已吊销时静默放行，请求以匿名身份继续
            }
        }
        chain.doFilter(req, res);
    }
}
