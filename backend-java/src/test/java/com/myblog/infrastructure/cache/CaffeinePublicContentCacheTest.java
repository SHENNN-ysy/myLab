package com.myblog.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Ticker;
import com.myblog.common.properties.ContentCacheProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/** CaffeinePublicContentCache 的本地存取、TTL 过期与容量驱逐测试。 */
class CaffeinePublicContentCacheTest {

    /** 三类缓存写入后可读回，删除后返回空。 */
    @Test
    void putGetAndEvictRoundTrip() {
        CaffeinePublicContentCache cache = cache(new AtomicLong()::get, Duration.ofSeconds(60), 8);
        Map<String, Object> all = Map.of("home", Map.of());
        Map<String, Object> summary = Map.of("cards", java.util.List.of());
        Map<String, Object> detail = Map.of("post_key", "post-a");

        cache.putAll(all);
        cache.putMylabSummary(summary);
        cache.putMylabDetail("post-a", detail);

        assertThat(cache.getAll()).containsSame(all);
        assertThat(cache.getMylabSummary()).containsSame(summary);
        assertThat(cache.getMylabDetail("post-a")).containsSame(detail);

        cache.evictAll();
        cache.evictMylabSummary();
        cache.evictMylabDetails();

        assertThat(cache.getAll()).isEmpty();
        assertThat(cache.getMylabSummary()).isEmpty();
        assertThat(cache.getMylabDetail("post-a")).isEmpty();
    }

    /** 详情缓存按 postKey 隔离，删除全部详情不影响聚合与摘要缓存。 */
    @Test
    void detailsAreIsolatedPerPostKeyAndEvictionKeepsOtherCaches() {
        CaffeinePublicContentCache cache = cache(new AtomicLong()::get, Duration.ofSeconds(60), 8);
        Map<String, Object> detailA = Map.of("post_key", "post-a");
        Map<String, Object> detailB = Map.of("post_key", "post-b");
        cache.putMylabDetail("post-a", detailA);
        cache.putMylabDetail("post-b", detailB);
        cache.putAll(Map.of("home", Map.of()));

        assertThat(cache.getMylabDetail("post-a")).containsSame(detailA);
        assertThat(cache.getMylabDetail("post-b")).containsSame(detailB);

        cache.evictMylabDetails();

        assertThat(cache.getMylabDetail("post-a")).isEmpty();
        assertThat(cache.getAll()).isPresent();
    }

    /** 超过 l1Ttl 后条目自动过期，读取返回空。 */
    @Test
    void entriesExpireAfterL1Ttl() {
        AtomicLong nanos = new AtomicLong();
        CaffeinePublicContentCache cache = cache(nanos::get, Duration.ofSeconds(60), 8);
        cache.putAll(Map.of("home", Map.of()));
        cache.putMylabDetail("post-a", Map.of("post_key", "post-a"));

        assertThat(cache.getAll()).isPresent();

        nanos.addAndGet(Duration.ofSeconds(61).toNanos());

        assertThat(cache.getAll()).isEmpty();
        assertThat(cache.getMylabDetail("post-a")).isEmpty();
    }

    /** 详情数量超过 l1MaxSize 时触发容量驱逐，不抛异常。 */
    @Test
    void detailsEvictByMaximumSizeWithoutError() {
        CaffeinePublicContentCache cache = cache(new AtomicLong()::get, Duration.ofSeconds(60), 2);

        assertThatCode(() -> {
            for (int i = 0; i < 10; i++) {
                cache.putMylabDetail("post-" + i, Map.of("post_key", "post-" + i));
            }
        }).doesNotThrowAnyException();
    }

    /** 负缓存空 Map 也按普通条目缓存，避免穿透。 */
    @Test
    void emptyMapNegativeEntryIsCached() {
        CaffeinePublicContentCache cache = cache(new AtomicLong()::get, Duration.ofSeconds(60), 8);
        Map<String, Object> negative = Map.of();

        cache.putMylabDetail("missing", negative);
        Optional<Map<String, Object>> cached = cache.getMylabDetail("missing");

        assertThat(cached).isPresent();
        assertThat(cached.get()).isEmpty();
    }

    private CaffeinePublicContentCache cache(Ticker ticker, Duration l1Ttl, long l1MaxSize) {
        ContentCacheProperties properties = new ContentCacheProperties(true, Duration.ofHours(6),
                Duration.ofSeconds(5), Duration.ofSeconds(1), l1Ttl, l1MaxSize);
        return new CaffeinePublicContentCache(properties, ticker);
    }
}
