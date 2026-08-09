package com.myblog.common.result;

import com.myblog.common.json.JacksonObjectMapper;
import com.myblog.common.exception.ForbiddenException;
import com.myblog.common.exception.NotFoundException;
import com.myblog.common.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ResultTest {

    @Test
    void successEnvelopeMatchesContract() throws Exception {
        Result<Map<String, String>> r = Result.ok(Map.of("status", "healthy"));
        assertThat(r.code()).isZero();
        assertThat(r.message()).isEqualTo("成功");
        assertThat(r.data()).containsEntry("status", "healthy");
        assertThat(JacksonObjectMapper.get().writeValueAsString(r))
                .doesNotContain("request_id", "timestamp");
    }

    @Test
    void businessErrorCodesRemainStable() {
        assertThat(new UnauthorizedException("x").getCode()).isEqualTo(10001);
        assertThat(new ForbiddenException().getCode()).isEqualTo(10004);
        assertThat(new NotFoundException("x").getCode()).isEqualTo(10005);
    }
}
