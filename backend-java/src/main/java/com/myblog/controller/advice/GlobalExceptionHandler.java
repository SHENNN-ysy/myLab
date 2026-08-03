package com.myblog.controller.advice;

import com.myblog.common.constant.MessageConstant;
import com.myblog.common.exception.BaseException;
import com.myblog.common.result.Result;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<Result<Void>> base(BaseException e) {
        return ResponseEntity.status(e.getStatus()).body(Result.fail(e.getCode(), e.getMessage(), null));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<Result<Void>> validation(Exception e) {
        return ResponseEntity.unprocessableEntity()
                .body(Result.fail(10007, MessageConstant.VALIDATION_FAILED, e.getMessage()));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> duplicate(Exception e) {
        return ResponseEntity.status(409)
                .body(Result.fail(10006, MessageConstant.RESOURCE_CONFLICT, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> other(Exception e) {
        return ResponseEntity.status(500)
                .body(Result.fail(20001, MessageConstant.INTERNAL_ERROR, e.getMessage()));
    }
}
