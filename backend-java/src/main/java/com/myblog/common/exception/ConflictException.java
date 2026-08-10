package com.myblog.common.exception;

import com.myblog.common.enumeration.ErrorCode;

/** 资源冲突异常（10006）：目标资源已存在或状态冲突，如用户名、邮箱重复注册。 */
public class ConflictException extends BaseException {
    /** 以冲突详情（如重复的用户名）作为错误信息。 */
    public ConflictException(String message) {
        super(ErrorCode.RESOURCE_CONFLICT, message);
    }

    /** 使用更细分的错误码（如特定业务的冲突码）及补充细节构造。 */
    public ConflictException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
