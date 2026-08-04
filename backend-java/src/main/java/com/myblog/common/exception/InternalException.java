package com.myblog.common.exception;

import com.myblog.common.enumeration.ErrorCode;

/** 20001 - internal server error. */
public class InternalException extends BaseException {
    public InternalException(String message) {
        super(ErrorCode.INTERNAL_ERROR, message);
    }

    public InternalException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
