package com.myblog.common.exception;

import com.myblog.common.enumeration.ErrorCode;

/** 10001 - invalid credentials / token. */
public class UnauthorizedException extends BaseException {
    public UnauthorizedException(String message) {
        super(ErrorCode.AUTHENTICATION_FAILED, message);
    }

    public UnauthorizedException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
