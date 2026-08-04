package com.myblog.common.exception;

import com.myblog.common.enumeration.ErrorCode;

/** 10005 - resource not found. */
public class NotFoundException extends BaseException {
    public NotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }

    public NotFoundException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
