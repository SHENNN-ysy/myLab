package com.myblog.starter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myblog.common.constant.SecurityConstant;
import com.myblog.common.enumeration.ErrorCode;
import com.myblog.common.json.JacksonObjectMapper;
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

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtFilter jwtFilter,
                                                  ObjectMapper objectMapper) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
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
                        .requestMatchers(SecurityConstant.VISIT_TRACK).permitAll()
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
