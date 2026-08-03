package com.myblog.common.result;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Unified API response envelope. Compatible with the Python backend contract.
 */
public record Result<T>(int code, String message, T data, String error,
                        @JsonProperty("request_id") String requestId,
                        long timestamp) {

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "success", data, null,
                com.myblog.common.context.RequestContext.get(),
                Instant.now().getEpochSecond());
    }

    public static <T> Result<T> ok(T data, String message) {
        return new Result<>(0, message, data, null,
                com.myblog.common.context.RequestContext.get(),
                Instant.now().getEpochSecond());
    }

    public static Result<Void> fail(int code, String message, String error) {
        return new Result<>(code, message, null, error,
                com.myblog.common.context.RequestContext.get(),
                Instant.now().getEpochSecond());
    }
}
