package com.myblog.common.exception;

import org.springframework.http.HttpStatus;

/** 10007 - validation failure. */
public class ValidationException extends BaseException {
    public ValidationException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, 10007, message);
    }
}
