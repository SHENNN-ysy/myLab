package com.myblog.common.exception;

import lombok.Getter;
import com.myblog.common.enumeration.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Base business exception carrying an HTTP status and a stable business code.
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

    public BaseException(HttpStatus status, int code, String message) {
        super(message);
        this.status = status;
        this.code = code;
        this.errorCode = ErrorCode.fromCode(code);
        this.detail = message;
    }

    public BaseException(ErrorCode errorCode) {
        this(errorCode, null);
    }

    public BaseException(ErrorCode errorCode, String detail) {
        super(errorCode.message());
        this.status = errorCode.status();
        this.code = errorCode.code();
        this.errorCode = errorCode;
        this.detail = detail;
    }
}
