package com.myblog.common.exception;

import org.springframework.http.HttpStatus;

/** 10003 - token revoked. */
public class TokenRevokedException extends BaseException {
    public TokenRevokedException() {
        super(HttpStatus.UNAUTHORIZED, 10003, "Token has been revoked");
    }
}
