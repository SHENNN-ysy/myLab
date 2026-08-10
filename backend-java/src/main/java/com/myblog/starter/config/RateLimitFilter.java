package com.myblog.starter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myblog.common.enumeration.ErrorCode;
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

/**
 * 基于 Redis 的 IP 限流过滤器：全局请求与登录接口按分钟窗口分别计数，超限返回 429。
 * Redis 不可用时放行（best-effort），执行优先级仅次于 {@link WebFilters}。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redis; // 限流计数存储
    private final AppProperties props;       // 限流阈值等应用配置
    private final ObjectMapper om = JacksonObjectMapper.get();

    public RateLimitFilter(StringRedisTemplate redis, AppProperties props) {
        this.redis = redis;
        this.props = props;
    }

    /**
     * 判断当前请求是否跳过限流：Swagger 文档与健康检查接口不参与限流。
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui/")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator/health");
    }

    /**
     * 对请求来源 IP 按分钟窗口计数：登录接口使用独立（通常更严格的）阈值，超限直接返回 429。
     */
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
                        Result.fail(ErrorCode.RATE_LIMIT_EXCEEDED, null));
                return;
            }
        } catch (Exception ignored) {
            // best-effort: allow request through if Redis is down
        }
        chain.doFilter(req, res);
    }
}
