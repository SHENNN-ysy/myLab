package com.myblog.common.exception;

import org.springframework.http.HttpStatus;

/** 10005 - resource not found. */
public class NotFoundException extends BaseException {
    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, 10005, message);
    }
}
