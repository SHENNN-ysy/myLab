package com.myblog.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 链路追踪过滤器：为每个请求生成（或透传）traceId 并放入 MDC，
 * 使该请求处理过程中的所有日志都带上同一 traceId；
 * 同时通过响应头 X-Trace-Id 回传给调用方，并记录请求耗时。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_KEY = "traceId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final int TRACE_ID_MAX_LENGTH = 64;
    private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        MDC.put(TRACE_ID_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long cost = System.currentTimeMillis() - start;
            log.info("{} {} {} {}ms", request.getMethod(), request.getRequestURI(),
                    response.getStatus(), cost);
            MDC.remove(TRACE_ID_KEY);
        }
    }

    /**
     * 优先透传调用方带来的 traceId（长度与字符集受限，防止日志注入），否则生成新的。
     */
    private String resolveTraceId(HttpServletRequest request) {
        String incoming = request.getHeader(TRACE_ID_HEADER);
        if (incoming != null && !incoming.isBlank()
                && incoming.length() <= TRACE_ID_MAX_LENGTH
                && incoming.chars().allMatch(c -> c == '-' || Character.isLetterOrDigit(c))) {
            return incoming;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
