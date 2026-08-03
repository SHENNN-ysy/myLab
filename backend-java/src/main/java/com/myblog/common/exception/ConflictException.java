package com.myblog.common.exception;

import org.springframework.http.HttpStatus;

/** 10006 - resource conflict (duplicate). */
public class ConflictException extends BaseException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, 10006, message);
    }
}
