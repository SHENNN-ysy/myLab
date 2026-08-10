package com.myblog.common.exception;

import com.myblog.common.enumeration.ErrorCode;

/** 参数校验失败异常（10007）：请求参数未通过业务校验。 */
public class ValidationException extends BaseException {
    /** 以校验失败的描述（如具体字段错误）作为错误信息。 */
    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_FAILED, message);
    }

    /** 使用更细分的错误码及补充细节构造。 */
    public ValidationException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
