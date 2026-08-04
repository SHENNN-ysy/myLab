package com.myblog.common.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.myblog.common.enumeration.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Unified API response envelope. Compatible with the Python backend contract.
 */
@Schema(name = "ApiResponse", description = "统一 API 响应包络")
public record Result<T>(
        @Schema(description = "业务状态码，0 表示成功", example = "0") int code,
        @Schema(description = "稳定的中文结果说明", example = "成功") String message,
        @Schema(description = "业务数据；失败时为 null") T data,
        @Schema(description = "可安全展示的错误细节；成功时为 null", example = "username: 长度必须在 3 到 64 之间") String error,
        @JsonProperty("request_id") @Schema(description = "请求追踪 ID", example = "01J5MYBLOG7P8ABCDEF12345678") String requestId,
        @Schema(description = "响应生成时间，Unix 秒", example = "1785832213") long timestamp) {

    public static <T> Result<T> ok(T data) {
        return new Result<>(ErrorCode.SUCCESS.code(), ErrorCode.SUCCESS.message(), data, null,
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

    public static Result<Void> fail(ErrorCode errorCode, String detail) {
        return new Result<>(errorCode.code(), errorCode.message(), null, detail,
                com.myblog.common.context.RequestContext.get(),
                Instant.now().getEpochSecond());
    }
}
