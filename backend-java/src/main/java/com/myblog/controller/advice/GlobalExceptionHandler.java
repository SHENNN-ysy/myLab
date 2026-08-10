package com.myblog.controller.advice;

import com.myblog.common.context.RequestContext;
import com.myblog.common.enumeration.ErrorCode;
import com.myblog.common.exception.BaseException;
import com.myblog.common.result.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器：将 controller 层抛出的各类异常统一转换为携带错误码的 {@link Result} 响应。
 * 业务异常按约定错误码返回；未知异常记录日志后返回通用内部错误，避免堆栈信息外泄。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常，按异常自身携带的错误码与明细响应。
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<Result<Void>> base(BaseException exception) {
        return response(exception.getErrorCode(), exception.getDetail());
    }

    /**
     * 处理 @RequestBody 参数校验失败，将全部字段错误去重后拼接为明细。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> methodValidation(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));
        return response(ErrorCode.VALIDATION_FAILED, detail);
    }

    /**
     * 处理方法级参数（如 @RequestParam、@PathVariable）约束校验失败。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> constraintValidation(ConstraintViolationException exception) {
        String detail = exception.getConstraintViolations().stream()
                .map(error -> error.getPropertyPath() + ": " + error.getMessage())
                .distinct()
                .collect(Collectors.joining("; "));
        return response(ErrorCode.VALIDATION_FAILED, detail);
    }

    /**
     * 处理请求体 JSON 解析失败（语法错误或字段类型不匹配）。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> malformedBody() {
        return response(ErrorCode.MALFORMED_REQUEST, "请检查 JSON 语法和字段类型");
    }

    /**
     * 处理缺少必需请求参数或请求部分（multipart part）的情况。
     */
    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    public ResponseEntity<Result<Void>> missingParameter(Exception exception) {
        // 两类异常的取值方式不同，分别提取缺失参数名
        String name = exception instanceof MissingServletRequestParameterException parameter
                ? parameter.getParameterName()
                : ((MissingServletRequestPartException) exception).getRequestPartName();
        return response(ErrorCode.MISSING_REQUEST_PARAMETER, "缺少参数：" + name);
    }

    /**
     * 处理路径/请求参数类型转换失败。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> typeMismatch(MethodArgumentTypeMismatchException exception) {
        return response(ErrorCode.PARAMETER_TYPE_MISMATCH, "参数类型错误：" + exception.getName());
    }

    /**
     * 处理请求使用了接口不支持的 HTTP 方法。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> methodNotAllowed(HttpRequestMethodNotSupportedException exception) {
        return response(ErrorCode.METHOD_NOT_ALLOWED, "不支持方法：" + exception.getMethod());
    }

    /**
     * 处理请求 Content-Type 不受支持的情况。
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Result<Void>> mediaTypeNotSupported(HttpMediaTypeNotSupportedException exception) {
        return response(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "媒体类型：" + exception.getContentType());
    }

    /**
     * 处理上传文件大小超限。
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> uploadTooLarge() {
        return response(ErrorCode.FILE_TOO_LARGE, null);
    }

    /**
     * 处理路由或静态资源未找到。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> routeNotFound(NoResourceFoundException exception) {
        return response(ErrorCode.RESOURCE_NOT_FOUND, exception.getResourcePath());
    }

    /**
     * 处理数据库唯一键冲突，返回资源冲突错误。
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> duplicate() {
        return response(ErrorCode.RESOURCE_CONFLICT, null);
    }

    /**
     * 处理其他数据访问异常，记录错误日志后返回通用数据库错误。
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Result<Void>> database(DataAccessException exception) {
        log.error("database error, request_id={}", RequestContext.get(), exception);
        return response(ErrorCode.DATABASE_ERROR, null);
    }

    /**
     * 兜底处理所有未单独处理的异常，记录错误日志后返回通用内部错误。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> other(Exception exception) {
        log.error("unhandled error, request_id={}", RequestContext.get(), exception);
        return response(ErrorCode.INTERNAL_ERROR, null);
    }

    /**
     * 统一构造错误响应：HTTP 状态码取自错误码，响应体为统一格式的失败结果。
     */
    private ResponseEntity<Result<Void>> response(ErrorCode errorCode, String detail) {
        return ResponseEntity.status(errorCode.status()).body(Result.fail(errorCode, detail));
    }
}
