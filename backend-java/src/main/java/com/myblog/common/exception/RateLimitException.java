package com.myblog.common.exception;

import org.springframework.http.HttpStatus;

/** 10008 - rate limit exceeded. */
public class RateLimitException extends BaseException {
    public RateLimitException() {
        super(HttpStatus.TOO_MANY_REQUESTS, 10008, "Too many requests");
    }
}
