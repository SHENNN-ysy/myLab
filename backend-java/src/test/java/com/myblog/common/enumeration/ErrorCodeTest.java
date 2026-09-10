package com.myblog.common.enumeration;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/** 错误码枚举：编码唯一性、文案完整性与既有公开编码稳定性测试。 */
class ErrorCodeTest {

    /** 所有错误码不得重复，且每个错误码都有非空文案。 */
    @Test
    void codesAreUniqueAndMessagesArePresent() {
        ErrorCode[] values = ErrorCode.values();
        assertThat(Arrays.stream(values).map(ErrorCode::code))
                .doesNotHaveDuplicates();
        assertThat(Arrays.stream(values).map(ErrorCode::message))
                .allMatch(message -> message != null && !message.isBlank());
    }

    /** 已对外公开的错误码数值保持不变，防止破坏前端与 API 契约。 */
    @Test
    void preservesExistingPublicCodes() {
        assertThat(ErrorCode.AUTHENTICATION_FAILED.code()).isEqualTo(10001);
        assertThat(ErrorCode.TOKEN_EXPIRED.code()).isEqualTo(10002);
        assertThat(ErrorCode.TOKEN_REVOKED.code()).isEqualTo(10003);
        assertThat(ErrorCode.FORBIDDEN.code()).isEqualTo(10004);
        assertThat(ErrorCode.RESOURCE_NOT_FOUND.code()).isEqualTo(10005);
        assertThat(ErrorCode.RESOURCE_CONFLICT.code()).isEqualTo(10006);
        assertThat(ErrorCode.VALIDATION_FAILED.code()).isEqualTo(10007);
        assertThat(ErrorCode.RATE_LIMIT_EXCEEDED.code()).isEqualTo(10008);
        assertThat(ErrorCode.INTERNAL_ERROR.code()).isEqualTo(20001);
    }
}
