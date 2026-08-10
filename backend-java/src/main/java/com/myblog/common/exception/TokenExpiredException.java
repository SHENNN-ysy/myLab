package com.myblog.common.exception;

import com.myblog.common.enumeration.ErrorCode;

/** 登录凭证过期异常（10002）：Token 已过有效期，需重新登录或刷新凭证。 */
public class TokenExpiredException extends BaseException {
    public TokenExpiredException() {
        super(ErrorCode.TOKEN_EXPIRED);
    }
}
