package com.myblog.common.exception;

import lombok.Getter;
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

    public BaseException(HttpStatus status, int code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
