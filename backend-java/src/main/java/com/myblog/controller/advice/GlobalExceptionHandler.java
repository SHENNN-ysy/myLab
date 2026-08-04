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

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<Result<Void>> base(BaseException exception) {
        return response(exception.getErrorCode(), exception.getDetail());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> methodValidation(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));
        return response(ErrorCode.VALIDATION_FAILED, detail);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> constraintValidation(ConstraintViolationException exception) {
        String detail = exception.getConstraintViolations().stream()
                .map(error -> error.getPropertyPath() + ": " + error.getMessage())
                .distinct()
                .collect(Collectors.joining("; "));
        return response(ErrorCode.VALIDATION_FAILED, detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> malformedBody() {
        return response(ErrorCode.MALFORMED_REQUEST, "请检查 JSON 语法和字段类型");
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    public ResponseEntity<Result<Void>> missingParameter(Exception exception) {
        String name = exception instanceof MissingServletRequestParameterException parameter
                ? parameter.getParameterName()
                : ((MissingServletRequestPartException) exception).getRequestPartName();
        return response(ErrorCode.MISSING_REQUEST_PARAMETER, "缺少参数：" + name);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> typeMismatch(MethodArgumentTypeMismatchException exception) {
        return response(ErrorCode.PARAMETER_TYPE_MISMATCH, "参数类型错误：" + exception.getName());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> methodNotAllowed(HttpRequestMethodNotSupportedException exception) {
        return response(ErrorCode.METHOD_NOT_ALLOWED, "不支持方法：" + exception.getMethod());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Result<Void>> mediaTypeNotSupported(HttpMediaTypeNotSupportedException exception) {
        return response(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "媒体类型：" + exception.getContentType());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> uploadTooLarge() {
        return response(ErrorCode.FILE_TOO_LARGE, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> routeNotFound(NoResourceFoundException exception) {
        return response(ErrorCode.RESOURCE_NOT_FOUND, exception.getResourcePath());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> duplicate() {
        return response(ErrorCode.RESOURCE_CONFLICT, null);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Result<Void>> database(DataAccessException exception) {
        log.error("database error, request_id={}", RequestContext.get(), exception);
        return response(ErrorCode.DATABASE_ERROR, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> other(Exception exception) {
        log.error("unhandled error, request_id={}", RequestContext.get(), exception);
        return response(ErrorCode.INTERNAL_ERROR, null);
    }

    private ResponseEntity<Result<Void>> response(ErrorCode errorCode, String detail) {
        return ResponseEntity.status(errorCode.status()).body(Result.fail(errorCode, detail));
    }
}
