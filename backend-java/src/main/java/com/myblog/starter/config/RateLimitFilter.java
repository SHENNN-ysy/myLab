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
import lombok.extern.slf4j.Slf4j;
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
 *
 * <p>算法为固定窗口：以 {@code rate:{login|global}:{ip}} 为计数键，窗口内首个请求顺带设置
 * 1 分钟过期，键过期即进入下一窗口、计数自然清零；只需 INCR/EXPIRE 两条命令，实现最简单。
 * 登录接口单独计数是为防密码爆破，阈值通常低于全局阈值。
 * Redis 故障时选择放行而非拒绝：可用性优先，认证与参数校验仍是安全兜底。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@Slf4j
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
        // 经反向代理部署时真实客户端 IP 在 X-Forwarded-For 首个地址，直连时退化为 remoteAddr
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
        } catch (Exception e) {
            // 尽力而为：Redis 故障时放行，避免存储抖动拖垮全站可用性
            log.warn("rate limit check failed, fail open, key={}, err={}", key, e.toString());
        }
        chain.doFilter(req, res);
    }
}
