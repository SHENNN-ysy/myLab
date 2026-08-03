package com.myblog.common.exception;

import org.springframework.http.HttpStatus;

/** 10002 - token expired. */
public class TokenExpiredException extends BaseException {
    public TokenExpiredException() {
        super(HttpStatus.UNAUTHORIZED, 10002, "Token expired");
    }
}
