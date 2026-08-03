package com.myblog.common.context;

/**
 * Thread-local request id holder. Bound by a request filter and consumed by {@code Result}.
 */
public final class RequestContext {

    private static final ThreadLocal<String> ID = new ThreadLocal<>();

    private RequestContext() {
    }

    public static void set(String id) {
        ID.set(id);
    }

    public static String get() {
        return ID.get();
    }

    public static void clear() {
        ID.remove();
    }
}
