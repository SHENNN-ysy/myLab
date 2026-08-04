package com.myblog.common.exception;

import com.myblog.common.enumeration.ErrorCode;

/** 10006 - resource conflict (duplicate). */
public class ConflictException extends BaseException {
    public ConflictException(String message) {
        super(ErrorCode.RESOURCE_CONFLICT, message);
    }

    public ConflictException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
