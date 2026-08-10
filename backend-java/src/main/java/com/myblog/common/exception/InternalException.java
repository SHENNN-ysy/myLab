package com.myblog.common.exception;

import com.myblog.common.enumeration.ErrorCode;

/** 服务器内部错误异常（20001）：未预期的服务端故障。 */
public class InternalException extends BaseException {
    /** 以内部错误描述作为错误信息。 */
    public InternalException(String message) {
        super(ErrorCode.INTERNAL_ERROR, message);
    }

    /** 使用更细分的错误码及补充细节构造。 */
    public InternalException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
