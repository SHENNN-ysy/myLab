package com.myblog.common.result;

import com.myblog.common.json.JacksonObjectMapper;
import com.myblog.common.exception.ForbiddenException;
import com.myblog.common.exception.NotFoundException;
import com.myblog.common.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 统一响应体 Result：成功信封契约与业务异常错误码稳定性测试。 */
class ResultTest {

    /** 成功响应 code 为 0、文案为“成功”，序列化不含 request_id/timestamp 等多余字段。 */
    @Test
    void successEnvelopeMatchesContract() throws Exception {
        Result<Map<String, String>> r = Result.ok(Map.of("status", "healthy"));
        assertThat(r.code()).isZero();
        assertThat(r.message()).isEqualTo("成功");
        assertThat(r.data()).containsEntry("status", "healthy");
        assertThat(JacksonObjectMapper.get().writeValueAsString(r))
                .doesNotContain("request_id", "timestamp");
    }

    /** 常见业务异常的错误码保持稳定：401→10001、403→10004、404→10005。 */
    @Test
    void businessErrorCodesRemainStable() {
        assertThat(new UnauthorizedException("x").getCode()).isEqualTo(10001);
        assertThat(new ForbiddenException().getCode()).isEqualTo(10004);
        assertThat(new NotFoundException("x").getCode()).isEqualTo(10005);
    }
}
