package com.myblog.common.context;

/**
 * 基于 ThreadLocal 的请求上下文持有者：保存请求 id 与客户端 IP。
 * 由请求过滤器写入，供 {@code Result}、业务审计日志等下游消费。
 */
public final class RequestContext {

    private static final ThreadLocal<String> ID = new ThreadLocal<>();
    private static final ThreadLocal<String> IP = new ThreadLocal<>();

    private RequestContext() {
    }

    /** 写入当前请求的 id（由请求过滤器在请求进入时调用）。 */
    public static void set(String id) {
        ID.set(id);
    }

    /** 读取当前请求的 id，未设置时返回 null。 */
    public static String get() {
        return ID.get();
    }

    /** 写入当前请求的客户端 IP（由请求过滤器在请求进入时调用）。 */
    public static void setIp(String ip) {
        IP.set(ip);
    }

    /** 读取当前请求的客户端 IP，未设置时（如非请求线程）返回 null。 */
    public static String getIp() {
        return IP.get();
    }

    /** 清除当前请求上下文；请求结束后必须调用，避免线程池复用导致上下文串号。 */
    public static void clear() {
        ID.remove();
        IP.remove();
    }
}
