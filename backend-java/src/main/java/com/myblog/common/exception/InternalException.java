package com.myblog.common.exception;

import org.springframework.http.HttpStatus;

/** 20001 - internal server error. */
public class InternalException extends BaseException {
    public InternalException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, 20001, message);
    }
}
