package com.myblog.common.exception;

import com.myblog.common.enumeration.ErrorCode;

/** 登录凭证失效异常（10003）：Token 已被主动吊销（如退出登录、修改密码后）。 */
public class TokenRevokedException extends BaseException {
    public TokenRevokedException() {
        super(ErrorCode.TOKEN_REVOKED);
    }
}
