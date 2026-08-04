package com.myblog.common.exception;

import com.myblog.common.enumeration.ErrorCode;

/** 10003 - token revoked. */
public class TokenRevokedException extends BaseException {
    public TokenRevokedException() {
        super(ErrorCode.TOKEN_REVOKED);
    }
}
