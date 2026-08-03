package com.myblog.common.exception;

import org.springframework.http.HttpStatus;

/** 10004 - permission denied. */
public class ForbiddenException extends BaseException {
    public ForbiddenException() {
        super(HttpStatus.FORBIDDEN, 10004, "Permission denied");
    }
}
