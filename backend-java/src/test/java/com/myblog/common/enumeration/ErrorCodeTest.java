package com.myblog.common.enumeration;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

    @Test
    void codesAreUniqueAndMessagesArePresent() {
        ErrorCode[] values = ErrorCode.values();
        assertThat(Arrays.stream(values).map(ErrorCode::code))
                .doesNotHaveDuplicates();
        assertThat(Arrays.stream(values).map(ErrorCode::message))
                .allMatch(message -> message != null && !message.isBlank());
    }

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
