package com.myblog.common.exception;

import lombok.Getter;
import com.myblog.common.enumeration.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * 业务异常基类，所有自定义业务异常的父类。
 * 携带返回给客户端的 HTTP 状态码与稳定的业务错误码，由全局异常处理器统一转换为标准错误响应。
 */
@Getter
public class BaseException extends RuntimeException {

    /** HTTP status returned to the client. */
    private final HttpStatus status;

    /** Stable business error code (e.g. 10001, 20001). */
    private final int code;

    /** 规范化错误码定义。 */
    private final ErrorCode errorCode;

    /** 可安全返回给调用方的错误细节。 */
    private final String detail;

    /**
     * 以 HTTP 状态码 + 业务错误码构造；errorCode 由 code 反查得到，
     * 未定义的 code 会抛出 IllegalArgumentException。
     */
    public BaseException(HttpStatus status, int code, String message) {
        super(message);
        this.status = status;
        this.code = code;
        this.errorCode = ErrorCode.fromCode(code);
        this.detail = message;
    }

    /**
     * 以规范错误码构造，错误信息取 ErrorCode 的默认文案，无附加细节。
     */
    public BaseException(ErrorCode errorCode) {
        this(errorCode, null);
    }

    /**
     * 以规范错误码构造，detail 为可安全返回给调用方的补充说明（不影响对外 message）。
     */
    public BaseException(ErrorCode errorCode, String detail) {
        super(errorCode.message());
        this.status = errorCode.status();
        this.code = errorCode.code();
        this.errorCode = errorCode;
        this.detail = detail;
    }
}
