package com.myblog.starter.config;

import com.myblog.common.context.RequestContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Web 层全局过滤器（最高优先级执行）：为每个请求生成或透传 X-Request-ID 并写入 {@link RequestContext}，
 * 便于全链路日志追踪；同时统一设置响应字符集与安全响应头。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebFilters implements Filter {

    /**
     * 处理请求：提取或生成请求 ID，设置响应头后放行，结束时清理请求上下文。
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String id = req.getHeader("X-Request-ID");
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString().replace("-", "");
        }
        RequestContext.set(id);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.setHeader("X-Request-ID", id);
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader("X-Frame-Options", "DENY");
        res.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        try {
            chain.doFilter(request, response);
        } finally {
            // 必须清理 ThreadLocal，避免线程池复用时请求上下文串号
            RequestContext.clear();
        }
    }
}
