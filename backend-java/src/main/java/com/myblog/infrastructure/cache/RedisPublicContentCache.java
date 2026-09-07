package com.myblog.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myblog.application.port.PublicContentCache;
import com.myblog.common.json.JacksonObjectMapper;
import com.myblog.common.properties.ContentCacheProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Redis 公开内容缓存适配器，值统一使用 JSON 保存。 */
@Slf4j
@Repository
public class RedisPublicContentCache implements PublicContentCache {
    public static final String ALL_KEY = "myblog:content:v1:all";
    public static final String MYLAB_DETAILS_KEY = "myblog:content:v1:mylab:details";
    private static final ObjectMapper OBJECT_MAPPER = JacksonObjectMapper.get();
    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() { };

    private final StringRedisTemplate redis;
    private final ContentCacheProperties properties;

    public RedisPublicContentCache(StringRedisTemplate redis, ContentCacheProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    @Override
    public Optional<Map<String, Object>> getAll() {
        String value = redis.opsForValue().get(ALL_KEY);
        return decode(value, () -> redis.delete(ALL_KEY), ALL_KEY);
    }

    @Override
    public void putAll(Map<String, Object> content) {
        redis.opsForValue().set(ALL_KEY, encode(content), properties.ttl());
    }

    @Override
    public Optional<Map<String, Object>> getMylabDetail(String postKey) {
        Object value = redis.opsForHash().get(MYLAB_DETAILS_KEY, postKey);
        return decode(value instanceof String text ? text : null,
                () -> redis.opsForHash().delete(MYLAB_DETAILS_KEY, postKey),
                MYLAB_DETAILS_KEY + ":" + postKey);
    }

    @Override
    public void putMylabDetail(String postKey, Map<String, Object> detail) {
        redis.opsForHash().put(MYLAB_DETAILS_KEY, postKey, encode(detail));
        redis.expire(MYLAB_DETAILS_KEY, properties.ttl());
    }

    @Override
    public void evictAll() {
        redis.delete(ALL_KEY);
    }

    @Override
    public void evictMylabDetails() {
        redis.delete(MYLAB_DETAILS_KEY);
    }

    private Optional<Map<String, Object>> decode(String value, Runnable delete, String key) {
        if (value == null) return Optional.empty();
        try {
            return Optional.of(OBJECT_MAPPER.readValue(value, MAP_TYPE));
        } catch (JsonProcessingException exception) {
            log.warn("公开内容缓存 JSON 损坏，已删除：key={}", key, exception);
            delete.run();
            return Optional.empty();
        }
    }

    private String encode(Map<String, Object> value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("公开内容缓存序列化失败", exception);
        }
    }
}
