package com.myblog.common.exception;

import com.myblog.common.enumeration.ErrorCode;

/** 10002 - token expired. */
public class TokenExpiredException extends BaseException {
    public TokenExpiredException() {
        super(ErrorCode.TOKEN_EXPIRED);
    }
}
