package com.myblog.common.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * 共享 ObjectMapper：蛇形命名 + 忽略 null 字段，序列化约定与原 Python 后端契约保持一致。
 */
public final class JacksonObjectMapper {

    private static final ObjectMapper INSTANCE = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);

    private JacksonObjectMapper() {
    }

    /** 获取全局共享的 ObjectMapper 实例（线程安全）。 */
    public static ObjectMapper get() {
        return INSTANCE;
    }
}
