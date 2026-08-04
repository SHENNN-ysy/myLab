package com.myblog.common.exception;

import com.myblog.common.enumeration.ErrorCode;

/** 10004 - permission denied. */
public class ForbiddenException extends BaseException {
    public ForbiddenException() {
        super(ErrorCode.FORBIDDEN);
    }
}
