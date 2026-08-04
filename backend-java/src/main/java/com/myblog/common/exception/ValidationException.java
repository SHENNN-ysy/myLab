package com.myblog.common.exception;

import com.myblog.common.enumeration.ErrorCode;

/** 10007 - validation failure. */
public class ValidationException extends BaseException {
    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_FAILED, message);
    }

    public ValidationException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
