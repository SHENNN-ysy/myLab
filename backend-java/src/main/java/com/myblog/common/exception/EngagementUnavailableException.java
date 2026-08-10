package com.myblog.common.exception;

import com.myblog.common.enumeration.ErrorCode;

/** Redis 无法安全完成互动写入时返回的 503 业务异常。 */
public class EngagementUnavailableException extends BaseException {
    public EngagementUnavailableException() {
        super(ErrorCode.ENGAGEMENT_UNAVAILABLE);
    }
}
