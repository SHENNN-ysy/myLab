package com.myblog.starter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myblog.common.constant.SecurityConstant;
import com.myblog.common.enumeration.ErrorCode;
import com.myblog.common.properties.AppProperties;
import com.myblog.common.result.Result;
import com.myblog.infrastructure.security.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 配置：无状态 JWT 认证、公开接口白名单、统一 401/403 JSON 响应及 CORS。
 *
 * <p>设计要点：JwtFilter 只负责校验令牌并在 SecurityContext 中建立身份，
 * 授权规则全部集中在本类一处（白名单放行 + 其余请求需认证），便于审计与调整；
 * 认证失败（401）与越权（403）同样返回统一 {@link Result} 包络，与业务错误响应结构保持一致。</p>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * 构建安全过滤链：关闭 CSRF、启用无状态会话，放行健康检查/登录/刷新令牌/Swagger 及公开内容 GET 接口，
     * 其余请求需认证；认证失败与越权分别返回统一结构的 401/403 JSON 响应。
     *
     * @return 安全过滤链
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtFilter jwtFilter,
                                                  ObjectMapper objectMapper) throws Exception {
        return http
                .csrf(csrf -> csrf.disable()) // 无状态 JWT 认证、不使用 Cookie 会话，无需 CSRF 防护
                .cors(cors -> {})
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SecurityConstant.HEALTH_PREFIX + "/**",
                                SecurityConstant.HEALTH_API,
                                SecurityConstant.AUTH_LOGIN,
                                SecurityConstant.AUTH_REFRESH,
                                SecurityConstant.OPENAPI_JSON,
                                SecurityConstant.OPENAPI_YAML,
                                SecurityConstant.SWAGGER_UI,
                                SecurityConstant.SWAGGER_UI_RESOURCES)
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, SecurityConstant.PUBLIC_GET_PREFIXES.stream()
                                .map(p -> p + "/**").toArray(String[]::new))
                        .permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/public/analytics/visits",
                                "/api/v1/public/mylab/*/views")
                        .permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/public/mylab/*/likes")
                        .permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/public/mylab/*/likes")
                        .permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint((req, res, x) -> {
                            res.setStatus(401);
                            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(res.getOutputStream(),
                                    Result.fail(ErrorCode.AUTHENTICATION_FAILED, null));
                        })
                        .accessDeniedHandler((req, res, x) -> {
                            res.setStatus(403);
                            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(res.getOutputStream(),
                                    Result.fail(ErrorCode.FORBIDDEN, null));
                        }))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * 从 app.cors-origins 读取允许的跨域来源并注册全局 CORS 规则；允许携带凭证，
     * 供管理端与前台跨域调用使用。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(AppProperties props) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(Arrays.stream(props.corsOrigins().split(","))
                .map(String::trim).toList());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        cors.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
