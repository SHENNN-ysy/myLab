package com.myblog.common.enumeration;

import org.springframework.http.HttpStatus;

import java.util.Arrays;

/**
 * 全局稳定错误码。
 *
 * <p>编码约定：10xxx 为通用请求与安全错误，11xxx 为账号错误，
 * 12xxx 为内容管理错误，13xxx 为文件与存储错误，20xxx 为服务端错误。</p>
 */
public enum ErrorCode {
    SUCCESS(0, HttpStatus.OK, "成功"),

    AUTHENTICATION_FAILED(10001, HttpStatus.UNAUTHORIZED, "身份认证失败"),
    TOKEN_EXPIRED(10002, HttpStatus.UNAUTHORIZED, "登录凭证已过期"),
    TOKEN_REVOKED(10003, HttpStatus.UNAUTHORIZED, "登录凭证已失效"),
    FORBIDDEN(10004, HttpStatus.FORBIDDEN, "无权执行该操作"),
    RESOURCE_NOT_FOUND(10005, HttpStatus.NOT_FOUND, "请求的资源不存在"),
    RESOURCE_CONFLICT(10006, HttpStatus.CONFLICT, "资源状态冲突"),
    VALIDATION_FAILED(10007, HttpStatus.UNPROCESSABLE_ENTITY, "请求参数校验失败"),
    RATE_LIMIT_EXCEEDED(10008, HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁"),
    MALFORMED_REQUEST(10009, HttpStatus.BAD_REQUEST, "请求体格式错误"),
    METHOD_NOT_ALLOWED(10010, HttpStatus.METHOD_NOT_ALLOWED, "请求方法不支持"),
    UNSUPPORTED_MEDIA_TYPE(10011, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "请求媒体类型不支持"),
    MISSING_REQUEST_PARAMETER(10012, HttpStatus.BAD_REQUEST, "缺少必填请求参数"),
    PARAMETER_TYPE_MISMATCH(10013, HttpStatus.BAD_REQUEST, "请求参数类型错误"),

    INVALID_CREDENTIALS(11001, HttpStatus.UNAUTHORIZED, "用户名或密码错误"),
    ACCOUNT_DISABLED(11002, HttpStatus.UNAUTHORIZED, "账号已被停用"),
    OLD_PASSWORD_INCORRECT(11003, HttpStatus.UNPROCESSABLE_ENTITY, "原密码错误"),
    USER_NOT_FOUND(11004, HttpStatus.NOT_FOUND, "用户不存在"),
    USER_ALREADY_EXISTS(11005, HttpStatus.CONFLICT, "用户名或邮箱已存在"),

    CONTENT_MODULE_NOT_FOUND(12001, HttpStatus.NOT_FOUND, "内容模块不存在"),
    CONTENT_MODULE_OFFLINE(12002, HttpStatus.NOT_FOUND, "内容模块已下线"),
    CONTENT_VERSION_NOT_FOUND(12003, HttpStatus.NOT_FOUND, "内容版本不存在"),
    CONTENT_VALIDATION_FAILED(12004, HttpStatus.UNPROCESSABLE_ENTITY, "内容数据校验失败"),
    CONTENT_DEPENDENCY_CONFLICT(12005, HttpStatus.CONFLICT, "内容关联关系冲突"),

    FILE_EMPTY(13001, HttpStatus.UNPROCESSABLE_ENTITY, "上传文件为空"),
    FILE_TYPE_UNSUPPORTED(13002, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "文件类型不支持"),
    FILE_TOO_LARGE(13003, HttpStatus.PAYLOAD_TOO_LARGE, "上传文件超过大小限制"),
    FILE_NOT_FOUND(13004, HttpStatus.NOT_FOUND, "文件不存在"),
    STORAGE_UNAVAILABLE(13005, HttpStatus.SERVICE_UNAVAILABLE, "文件存储服务不可用"),

    ENGAGEMENT_UNAVAILABLE(14001, HttpStatus.SERVICE_UNAVAILABLE, "互动统计服务暂不可用"),

    INTERNAL_ERROR(20001, HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误"),
    DATABASE_ERROR(20002, HttpStatus.INTERNAL_SERVER_ERROR, "数据库操作失败");

    private final int code;
    private final HttpStatus status;
    private final String message;

    ErrorCode(int code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }

    /**
     * 按数字错误码反查枚举。
     *
     * @throws IllegalArgumentException 错误码不存在时抛出
     */
    public static ErrorCode fromCode(int code) {
        return Arrays.stream(values())
                .filter(item -> item.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown error code: " + code));
    }
}
