package com.myblog.application.model.vo;

import com.myblog.common.json.JacksonObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserOutVOTest {

    @Test
    void serializationNeverContainsPasswordHash() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();
        UserOutVO user = new UserOutVO(UUID.randomUUID(), "admin", "superadmin", true,
                now, now, now);

        String json = JacksonObjectMapper.get().writeValueAsString(user);

        assertThat(json)
                .contains("\"username\":\"admin\"")
                .contains("\"is_active\":true")
                .doesNotContain("password", "password_hash");
    }
}
