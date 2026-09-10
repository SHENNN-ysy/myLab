package com.myblog.common.exception;

import com.myblog.common.enumeration.ErrorCode;

/** Redis 无法完成会话认证或撤销时返回的 503 业务异常。 */
public class AuthenticationUnavailableException extends BaseException {
    public AuthenticationUnavailableException() {
        super(ErrorCode.AUTHENTICATION_UNAVAILABLE);
    }
}
