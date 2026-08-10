package com.myblog.common.exception;

import com.myblog.common.enumeration.ErrorCode;

/** 资源不存在异常（10005）：请求的资源不存在或已被删除。 */
public class NotFoundException extends BaseException {
    /** 以缺失资源的描述（如"文章不存在"）作为错误信息。 */
    public NotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }

    /** 使用更细分的错误码及补充细节构造。 */
    public NotFoundException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
