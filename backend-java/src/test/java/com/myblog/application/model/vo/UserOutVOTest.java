package com.myblog.application.model.vo;

import com.myblog.common.json.JacksonObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 用户出参 VO 的序列化安全测试：对外 JSON 绝不泄露密码哈希。 */
class UserOutVOTest {

    /** 序列化结果包含公开字段，但不得出现 password/password_hash 字段。 */
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
