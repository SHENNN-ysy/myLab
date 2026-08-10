/**
 * 统一业务异常体系：以 {@link com.myblog.common.exception.BaseException} 为基类，
 * 按错误场景派生认证、权限、资源、校验、限流等具体异常，均携带规范错误码
 * （{@link com.myblog.common.enumeration.ErrorCode}），由全局异常处理器
 * 统一转换为标准错误响应。
 */
package com.myblog.common.exception;
