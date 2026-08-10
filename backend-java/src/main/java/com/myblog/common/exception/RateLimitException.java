package com.myblog.common.exception;

import com.myblog.common.enumeration.ErrorCode;

/** 限流异常（10008）：请求频率超过限制，客户端应稍后重试。 */
public class RateLimitException extends BaseException {
    public RateLimitException() {
        super(ErrorCode.RATE_LIMIT_EXCEEDED);
    }
}
