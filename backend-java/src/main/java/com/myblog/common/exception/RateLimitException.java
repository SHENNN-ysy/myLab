package com.myblog.common.exception;

import com.myblog.common.enumeration.ErrorCode;

/** 10008 - rate limit exceeded. */
public class RateLimitException extends BaseException {
    public RateLimitException() {
        super(ErrorCode.RATE_LIMIT_EXCEEDED);
    }
}
