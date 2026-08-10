package com.myblog.common.exception;

import com.myblog.common.enumeration.ErrorCode;

/** 认证失败异常（10001）：凭证无效，如用户名密码错误或 Token 非法。 */
public class UnauthorizedException extends BaseException {
    /** 以认证失败的描述作为错误信息。 */
    public UnauthorizedException(String message) {
        super(ErrorCode.AUTHENTICATION_FAILED, message);
    }

    /** 使用更细分的错误码及补充细节构造。 */
    public UnauthorizedException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
