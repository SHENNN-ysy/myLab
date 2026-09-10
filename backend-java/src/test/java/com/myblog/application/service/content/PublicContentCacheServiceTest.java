package com.myblog.application.service.content;

import com.myblog.application.port.DistributedLock;
import com.myblog.application.port.PublicContentCache;
import com.myblog.common.properties.ContentCacheProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** PublicContentCacheService 的 Cache-Aside、锁等待、降级与失效测试。 */
@SuppressWarnings("unchecked")
class PublicContentCacheServiceTest {

    /** 缓存命中时直接返回缓存值，不触发回源加载也不加锁。 */
    @Test
    void cacheHitSkipsLoaderAndLock() {
        PublicContentCache cache = mock(PublicContentCache.class);
        DistributedLock lock = mock(DistributedLock.class);
        Map<String, Object> expected = Map.of("home", Map.of());
        when(cache.getAll()).thenReturn(Optional.of(expected));
        PublicContentCacheService service = service(cache, lock, Duration.ofSeconds(1));
        AtomicInteger loads = new AtomicInteger();

        Map<String, Object> result = service.readAll(() -> {
            loads.incrementAndGet();
            return Map.of();
        });

        assertThat(result).isSameAs(expected);
        assertThat(loads).hasValue(0);
        verify(lock, never()).tryAcquire(any(), any(), any());
    }

    /** 缓存未命中时走双检查：加锁后再次读缓存，加载结果写回缓存并释放锁。 */
    @Test
    void cacheMissUsesDoubleCheckAndWritesLoadedValue() {
        PublicContentCache cache = mock(PublicContentCache.class);
        DistributedLock lock = mock(DistributedLock.class);
        Map<String, Object> loaded = Map.of("about", Map.of("profile", Map.of()));
        when(cache.getAll()).thenReturn(Optional.empty());
        when(lock.tryAcquire(any(), any(), any())).thenReturn(true);
        PublicContentCacheService service = service(cache, lock, Duration.ofSeconds(1));

        assertThat(service.readAll(() -> loaded)).isSameAs(loaded);

        verify(cache, org.mockito.Mockito.times(2)).getAll();
        verify(cache).putAll(loaded);
        verify(lock).release(org.mockito.ArgumentMatchers.eq(PublicContentCacheService.ALL_LOCK), any());
    }

    /** Redis 读缓存异常时立即降级为回源加载，且不再尝试加锁。 */
    @Test
    void redisReadFailureImmediatelyFallsBackToLoader() {
        PublicContentCache cache = mock(PublicContentCache.class);
        DistributedLock lock = mock(DistributedLock.class);
        when(cache.getAll()).thenThrow(new IllegalStateException("redis unavailable"));
        PublicContentCacheService service = service(cache, lock, Duration.ofSeconds(1));
        Map<String, Object> loaded = Map.of("home", Map.of());

        assertThat(service.readAll(() -> loaded)).isSameAs(loaded);
        verify(lock, never()).tryAcquire(any(), any(), any());
    }

    /** 回源成功但写缓存失败时仍返回数据库结果，并正常释放锁。 */
    @Test
    void cacheWriteFailureStillReturnsDatabaseValue() {
        PublicContentCache cache = mock(PublicContentCache.class);
        DistributedLock lock = mock(DistributedLock.class);
        when(cache.getAll()).thenReturn(Optional.empty());
        when(lock.tryAcquire(any(), any(), any())).thenReturn(true);
        doThrow(new IllegalStateException("redis unavailable")).when(cache).putAll(any());
        PublicContentCacheService service = service(cache, lock, Duration.ofSeconds(1));
        Map<String, Object> loaded = Map.of("home", Map.of());

        assertThat(service.readAll(() -> loaded)).isSameAs(loaded);
        verify(lock).release(org.mockito.ArgumentMatchers.eq(PublicContentCacheService.ALL_LOCK), any());
    }

    /** 抢锁失败时轮询等待持锁者写入缓存，拿到其写入的值而不再回源。 */
    @Test
    void busyLockWaitsForValueWrittenByOwner() {
        PublicContentCache cache = mock(PublicContentCache.class);
        DistributedLock lock = mock(DistributedLock.class);
        Map<String, Object> expected = Map.of("home", Map.of());
        // 第一次读缓存未命中、第二次读到持锁者写入的值
        when(cache.getAll()).thenReturn(Optional.empty(), Optional.of(expected));
        when(lock.tryAcquire(any(), any(), any())).thenReturn(false);
        PublicContentCacheService service = service(cache, lock, Duration.ofSeconds(1));
        AtomicInteger loads = new AtomicInteger();

        Map<String, Object> result = service.readAll(() -> {
            loads.incrementAndGet();
            return Map.of();
        });

        assertThat(result).isSameAs(expected);
        assertThat(loads).hasValue(0);
        verify(cache, never()).putAll(any());
    }

    /** 等锁超时后自行回源数据库，但不写缓存以避免与持锁者竞争。 */
    @Test
    void busyLockTimeoutLoadsDatabaseWithoutWritingCache() {
        PublicContentCache cache = mock(PublicContentCache.class);
        DistributedLock lock = mock(DistributedLock.class);
        when(cache.getAll()).thenReturn(Optional.empty());
        when(lock.tryAcquire(any(), any(), any())).thenReturn(false);
        // 等待窗口压到 1ms 强制走超时分支
        PublicContentCacheService service = service(cache, lock, Duration.ofMillis(1));
        Map<String, Object> loaded = Map.of("home", Map.of());

        assertThat(service.readAll(() -> loaded)).isSameAs(loaded);
        verify(cache, never()).putAll(any());
    }

    /** MyLab 单篇详情走独立的 Hash 结构缓存与专用锁，写回后释放 MYLAB_DETAILS_LOCK。 */
    @Test
    void mylabDetailUsesHashReaderAndWriter() {
        PublicContentCache cache = mock(PublicContentCache.class);
        DistributedLock lock = mock(DistributedLock.class);
        when(cache.getMylabDetail("post-a")).thenReturn(Optional.empty());
        when(lock.tryAcquire(any(), any(), any())).thenReturn(true);
        PublicContentCacheService service = service(cache, lock, Duration.ofSeconds(1));
        Map<String, Object> loaded = Map.of("cards", java.util.List.of(
                Map.of("post_key", "post-a", "markdown_content", "# 正文")));

        assertThat(service.readMylabDetail("post-a", () -> loaded)).isSameAs(loaded);
        verify(cache).putMylabDetail("post-a", loaded);
        verify(lock).release(org.mockito.ArgumentMatchers.eq(PublicContentCacheService.MYLAB_DETAILS_LOCK), any());
    }

    /** 缓存关闭时始终回源加载，失效操作也不触碰缓存。 */
    @Test
    void disabledCacheAlwaysUsesLoader() {
        PublicContentCache cache = mock(PublicContentCache.class);
        DistributedLock lock = mock(DistributedLock.class);
        ContentCacheProperties properties = new ContentCacheProperties(false, Duration.ofHours(6),
                Duration.ofSeconds(5), Duration.ofSeconds(1));
        PublicContentCacheService service = new PublicContentCacheService(cache, lock, properties);
        Map<String, Object> loaded = Map.of("home", Map.of());

        assertThat(service.readAll(() -> loaded)).isSameAs(loaded);
        service.invalidate("mylab");

        verify(cache, never()).getAll();
        verify(cache, never()).evictAll();
    }

    /** 失效操作在锁内同时清除全量缓存与 MyLab 详情缓存，两把锁各释放一次。 */
    @Test
    void invalidationDeletesAllAndMylabDetailsUnderLocks() {
        PublicContentCache cache = mock(PublicContentCache.class);
        DistributedLock lock = mock(DistributedLock.class);
        when(lock.tryAcquire(any(), any(), any())).thenReturn(true);
        PublicContentCacheService service = service(cache, lock, Duration.ofSeconds(1));

        service.invalidate("mylab");

        verify(cache).evictAll();
        verify(cache).evictMylabDetails();
        verify(lock, org.mockito.Mockito.times(2)).release(any(), any());
    }

    /** 并发冷读只回源一次：起跑门闩保证两线程同时进入，先到者持锁加载，后者等锁后命中缓存。 */
    @Test
    void concurrentColdReadsLoadDatabaseOnlyOnce() throws Exception {
        InMemoryCache cache = new InMemoryCache();
        InMemoryLock lock = new InMemoryLock();
        PublicContentCacheService service = service(cache, lock, Duration.ofSeconds(2));
        AtomicInteger loads = new AtomicInteger();
        // 起跑门闩：两个读线程都阻塞在此，countDown 后同时发起冷读
        CountDownLatch start = new CountDownLatch(1);
        Map<String, Object> expected = Map.of("home", Map.of("images", java.util.List.of()));

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Map<String, Object>> first = executor.submit(() -> loadAfter(start, service, loads, expected));
            Future<Map<String, Object>> second = executor.submit(() -> loadAfter(start, service, loads, expected));
            start.countDown();

            assertThat(first.get()).isEqualTo(expected);
            assertThat(second.get()).isEqualTo(expected);
        }
        assertThat(loads).hasValue(1);
    }

    /** 冷加载进行中发起失效：失效等锁直到加载完成，并清除其刚写入的旧值。 */
    @Test
    void concurrentInvalidationWaitsForColdLoadAndRemovesItsResult() throws Exception {
        InMemoryCache cache = new InMemoryCache();
        InMemoryLock lock = new InMemoryLock();
        PublicContentCacheService service = service(cache, lock, Duration.ofSeconds(2));
        // loading：加载已开始；allowLoad：放行加载返回，两者夹出“失效发生在加载中途”的时序
        CountDownLatch loading = new CountDownLatch(1);
        CountDownLatch allowLoad = new CountDownLatch(1);
        Map<String, Object> stale = Map.of("home", Map.of("title", "old"));

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Map<String, Object>> read = executor.submit(() -> service.readAll(() -> {
                loading.countDown();
                await(allowLoad);
                return stale;
            }));
            loading.await();
            Future<?> invalidation = executor.submit(() -> service.invalidate("home"));
            allowLoad.countDown();

            assertThat(read.get()).isEqualTo(stale);
            invalidation.get();
        }
        assertThat(cache.getAll()).isEmpty();
    }

    private Map<String, Object> loadAfter(CountDownLatch start, PublicContentCacheService service,
                                          AtomicInteger loads, Map<String, Object> expected) throws Exception {
        start.await();
        return service.readAll(() -> {
            loads.incrementAndGet();
            try {
                // 模拟 150ms 回源耗时，让并发线程有足够窗口竞争同一把锁
                Thread.sleep(150);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            return expected;
        });
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private PublicContentCacheService service(PublicContentCache cache, DistributedLock lock,
                                              Duration waitTimeout) {
        ContentCacheProperties properties = new ContentCacheProperties(true, Duration.ofHours(6),
                Duration.ofSeconds(5), waitTimeout);
        return new PublicContentCacheService(cache, lock, properties);
    }

    /** 内存版缓存实现：用 AtomicReference/ConcurrentHashMap 模拟 Redis 行为，供并发用例使用。 */
    private static final class InMemoryCache implements PublicContentCache {
        private final AtomicReference<Map<String, Object>> all = new AtomicReference<>();
        private final Map<String, Map<String, Object>> details = new ConcurrentHashMap<>();

        @Override
        public Optional<Map<String, Object>> getAll() {
            return Optional.ofNullable(all.get());
        }

        @Override
        public void putAll(Map<String, Object> content) {
            all.set(content);
        }

        @Override
        public Optional<Map<String, Object>> getMylabDetail(String postKey) {
            return Optional.ofNullable(details.get(postKey));
        }

        @Override
        public void putMylabDetail(String postKey, Map<String, Object> detail) {
            details.put(postKey, detail);
        }

        @Override
        public void evictAll() {
            all.set(null);
        }

        @Override
        public void evictMylabDetails() {
            details.clear();
        }
    }

    /** 内存版分布式锁：putIfAbsent 模拟互斥获取，release 仅持锁者可释放。 */
    private static final class InMemoryLock implements DistributedLock {
        private final Map<String, String> owners = new ConcurrentHashMap<>();

        @Override
        public boolean tryAcquire(String name, String token, Duration lease) {
            return owners.putIfAbsent(name, token) == null;
        }

        @Override
        public void release(String name, String token) {
            owners.remove(name, token);
        }
    }
}
