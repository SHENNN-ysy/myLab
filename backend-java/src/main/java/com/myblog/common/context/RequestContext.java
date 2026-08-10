package com.myblog.common.context;

/**
 * 基于 ThreadLocal 的请求 id 持有者。由请求过滤器写入，供 {@code Result} 等下游消费。
 */
public final class RequestContext {

    private static final ThreadLocal<String> ID = new ThreadLocal<>();

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

    /** 清除当前请求 id；请求结束后必须调用，避免线程池复用导致 id 串号。 */
    public static void clear() {
        ID.remove();
    }
}
