package com.myblog.common.result;

import com.myblog.common.enumeration.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 统一 API 响应包络，与原 Python 后端的响应契约保持一致。
 */
@Schema(name = "ApiResponse", description = "统一 API 响应包络")
public record Result<T>(
        @Schema(description = "业务状态码，0 表示成功", example = "0") int code,
        @Schema(description = "稳定的中文结果说明", example = "成功") String message,
        @Schema(description = "业务数据；失败时为 null") T data,
        @Schema(description = "可安全展示的错误细节；成功时为 null", example = "username: 长度必须在 3 到 64 之间") String error) {

    /**
     * 构造成功响应，使用标准成功文案。
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(ErrorCode.SUCCESS.code(), ErrorCode.SUCCESS.message(), data, null);
    }

    /**
     * 构造成功响应，自定义提示文案。
     */
    public static <T> Result<T> ok(T data, String message) {
        return new Result<>(0, message, data, null);
    }

    /**
     * 构造失败响应。
     *
     * @param error 可安全展示给前端的错误细节
     */
    public static Result<Void> fail(int code, String message, String error) {
        return new Result<>(code, message, null, error);
    }

    /**
     * 按全局错误码构造失败响应。
     *
     * @param detail 可安全展示给前端的错误细节
     */
    public static Result<Void> fail(ErrorCode errorCode, String detail) {
        return new Result<>(errorCode.code(), errorCode.message(), null, detail);
    }
}
