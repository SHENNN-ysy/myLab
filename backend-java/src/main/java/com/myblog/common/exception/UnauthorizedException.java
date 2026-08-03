package com.myblog.common.exception;

import org.springframework.http.HttpStatus;

/** 10001 - invalid credentials / token. */
public class UnauthorizedException extends BaseException {
    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, 10001, message);
    }
}
