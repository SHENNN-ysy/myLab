package com.myblog.controller.advice;

import com.myblog.common.enumeration.ErrorCode;
import com.myblog.common.exception.NotFoundException;
import com.myblog.common.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void usesDomainCodeAndSafeDetailForBusinessException() {
        ResponseEntity<Result<Void>> response = handler.base(
                new NotFoundException(ErrorCode.CONTENT_VERSION_NOT_FOUND, "projects v99"));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(12003);
        assertThat(response.getBody().message()).isEqualTo("内容版本不存在");
        assertThat(response.getBody().error()).isEqualTo("projects v99");
    }

    @Test
    void mapsMissingParameterToStableCode() {
        ResponseEntity<Result<Void>> response = handler.missingParameter(
                new MissingServletRequestParameterException("cutoff", "OffsetDateTime"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(10012);
        assertThat(response.getBody().error()).contains("cutoff");
    }
}
