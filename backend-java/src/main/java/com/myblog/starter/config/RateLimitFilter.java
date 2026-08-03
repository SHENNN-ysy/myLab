package com.myblog.starter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myblog.common.constant.MessageConstant;
import com.myblog.common.json.JacksonObjectMapper;
import com.myblog.common.properties.AppProperties;
import com.myblog.common.result.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redis;
    private final AppProperties props;
    private final ObjectMapper om = JacksonObjectMapper.get();

    public RateLimitFilter(StringRedisTemplate redis, AppProperties props) {
        this.redis = redis;
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null) {
            ip = req.getRemoteAddr();
        }
        boolean login = req.getRequestURI().endsWith("/auth/login");
        int limit = login ? props.loginRateLimitPerMinute() : props.rateLimitPerMinute();
        String key = "rate:" + (login ? "login" : "global") + ":" + ip.split(",")[0].trim();
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redis.expire(key, Duration.ofMinutes(1));
            }
            if (count != null && count > limit) {
                res.setStatus(429);
                res.setContentType("application/json");
                om.writeValue(res.getOutputStream(),
                        Result.fail(10008, MessageConstant.TOO_MANY_REQUESTS, null));
                return;
            }
        } catch (Exception ignored) {
            // best-effort: allow request through if Redis is down
        }
        chain.doFilter(req, res);
    }
}
