package com.myblog.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.myblog.application.port.PublicContentCache;
import com.myblog.common.properties.ContentCacheProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * L1 本地公开内容缓存适配器：Caffeine 实现，与 L2 Redis 共享相同的端口契约。
 * 短 TTL 兜底自愈，主动失效由 {@link LayeredPublicContentCache} 双清保证。
 */
@Component
public class CaffeinePublicContentCache implements PublicContentCache {
    // 全量摘要与 MyLab 摘要均为单值缓存，使用固定 key 存取。
    private static final String SINGLE_KEY = "value";

    private final Cache<String, Map<String, Object>> allCache;
    private final Cache<String, Map<String, Object>> mylabSummaryCache;
    private final Cache<String, Map<String, Object>> mylabDetailsCache;

    public CaffeinePublicContentCache(ContentCacheProperties properties) {
        this(properties, Ticker.systemTicker());
    }

    /** 测试专用构造：注入可控 Ticker 以验证 TTL 过期行为。 */
    CaffeinePublicContentCache(ContentCacheProperties properties, Ticker ticker) {
        this.allCache = singleValueCache(properties, ticker);
        this.mylabSummaryCache = singleValueCache(properties, ticker);
        this.mylabDetailsCache = Caffeine.newBuilder()
                .expireAfterWrite(properties.l1Ttl())
                .maximumSize(properties.l1MaxSize())
                .ticker(ticker)
                .build();
    }

    private static Cache<String, Map<String, Object>> singleValueCache(ContentCacheProperties properties,
                                                                       Ticker ticker) {
        return Caffeine.newBuilder()
                .expireAfterWrite(properties.l1Ttl())
                .maximumSize(1)
                .ticker(ticker)
                .build();
    }

    @Override
    public Optional<Map<String, Object>> getAll() {
        return Optional.ofNullable(allCache.getIfPresent(SINGLE_KEY));
    }

    @Override
    public void putAll(Map<String, Object> content) {
        allCache.put(SINGLE_KEY, content);
    }

    @Override
    public Optional<Map<String, Object>> getMylabSummary() {
        return Optional.ofNullable(mylabSummaryCache.getIfPresent(SINGLE_KEY));
    }

    @Override
    public void putMylabSummary(Map<String, Object> content) {
        mylabSummaryCache.put(SINGLE_KEY, content);
    }

    @Override
    public Optional<Map<String, Object>> getMylabDetail(String postKey) {
        return Optional.ofNullable(mylabDetailsCache.getIfPresent(postKey));
    }

    @Override
    public void putMylabDetail(String postKey, Map<String, Object> detail) {
        mylabDetailsCache.put(postKey, detail);
    }

    @Override
    public void evictAll() {
        allCache.invalidateAll();
    }

    @Override
    public void evictMylabSummary() {
        mylabSummaryCache.invalidateAll();
    }

    @Override
    public void evictMylabDetails() {
        mylabDetailsCache.invalidateAll();
    }
}
