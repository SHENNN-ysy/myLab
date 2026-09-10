package com.myblog.controller.advice;

import com.myblog.common.enumeration.ErrorCode;
import com.myblog.common.exception.NotFoundException;
import com.myblog.common.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;

import static org.assertj.core.api.Assertions.assertThat;

/** 全局异常处理器：业务异常与参数缺失异常到统一错误响应的映射测试。 */
class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /** 业务异常按错误码返回 HTTP 状态与文案，error 字段携带安全的细节信息。 */
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

    /** 缺少必填请求参数时映射为稳定的参数校验错误码（10012），并在 error 中指明参数名。 */
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
