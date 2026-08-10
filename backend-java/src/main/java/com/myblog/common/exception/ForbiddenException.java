package com.myblog.common.exception;

import com.myblog.common.enumeration.ErrorCode;

/** 权限不足异常（10004）：用户已认证，但无权执行该操作。 */
public class ForbiddenException extends BaseException {
    public ForbiddenException() {
        super(ErrorCode.FORBIDDEN);
    }
}
