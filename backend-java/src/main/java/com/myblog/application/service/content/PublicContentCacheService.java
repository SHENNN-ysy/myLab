package com.myblog.application.service.content;

import com.myblog.application.port.DistributedLock;
import com.myblog.application.port.PublicContentCache;
import com.myblog.common.properties.ContentCacheProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 公开内容 Cache-Aside 协调服务：处理双重检查、锁等待、数据库回源与发布后失效。
 */
@Slf4j
@Service
public class PublicContentCacheService {
    static final String ALL_LOCK = "all";
    static final String MYLAB_DETAILS_LOCK = "mylab:details";
    private static final long MIN_POLL_MILLIS = 50;
    private static final long MAX_POLL_MILLIS = 100;

    private final PublicContentCache cache;
    private final DistributedLock lock;
    private final ContentCacheProperties properties;

    public PublicContentCacheService(PublicContentCache cache, DistributedLock lock,
                                     ContentCacheProperties properties) {
        this.cache = cache;
        this.lock = lock;
        this.properties = properties;
    }

    /** 优先读取全部公开摘要缓存，未命中时由单个锁持有者回源并写入。 */
    public Map<String, Object> readAll(Supplier<Map<String, Object>> loader) {
        return readThrough(cache::getAll, cache::putAll, loader, ALL_LOCK);
    }

    /** 优先读取指定 MyLab 文章详情，所有详情冷加载共享同一把 Hash 锁。 */
    public Map<String, Object> readMylabDetail(String postKey, Supplier<Map<String, Object>> loader) {
        return readThrough(() -> cache.getMylabDetail(postKey),
                detail -> cache.putMylabDetail(postKey, detail), loader, MYLAB_DETAILS_LOCK);
    }

    /** 发布或下线后清理受影响的公开缓存。 */
    public void invalidate(String moduleKey) {
        if (!properties.enabled()) return;
        invalidateUnderLock(ALL_LOCK, cache::evictAll);
        if ("mylab".equals(moduleKey)) {
            invalidateUnderLock(MYLAB_DETAILS_LOCK, cache::evictMylabDetails);
        }
    }

    private Map<String, Object> readThrough(Supplier<Optional<Map<String, Object>>> reader,
                                            Consumer<Map<String, Object>> writer,
                                            Supplier<Map<String, Object>> loader,
                                            String lockName) {
        // 开关关闭时完全绕过 Redis，便于故障排查和紧急降级。
        if (!properties.enabled()) return loader.get();
        // 第一次检查覆盖正常缓存命中；Redis 异常时立即回源，不再尝试加锁。
        CacheRead cached = readCache(reader, lockName);
        if (!cached.available()) return loader.get();
        if (cached.value().isPresent()) return cached.value().get();

        // 每次竞争生成独立 token，解锁时据此校验锁所有权。
        String token = UUID.randomUUID().toString();
        final boolean acquired;
        try {
            acquired = lock.tryAcquire(lockName, token, properties.lockTtl());
        } catch (RuntimeException exception) {
            log.debug("公开内容缓存锁不可用，直接回源数据库：lock={}, error={}",
                    lockName, exception.toString());
            return loader.get();
        }
        // 未获得锁的请求等待锁持有者写缓存，避免并发冷缓存同时访问数据库。
        if (!acquired) return awaitCache(reader, loader, lockName);

        try {
            // 获取锁后双重检查，处理加锁前其他请求已完成重建的情况。
            CacheRead checked = readCache(reader, lockName);
            if (checked.value().isPresent()) return checked.value().get();
            // 只有锁持有者负责数据库加载和缓存写回。
            Map<String, Object> loaded = loader.get();
            writeCache(writer, loaded, lockName);
            return loaded;
        } finally {
            release(lockName, token);
        }
    }

    private Map<String, Object> awaitCache(Supplier<Optional<Map<String, Object>>> reader,
                                           Supplier<Map<String, Object>> loader,
                                           String lockName) {
        long deadline = System.nanoTime() + properties.waitTimeout().toNanos();
        while (System.nanoTime() < deadline) {
            // 随机短暂休眠，减少等待请求同时轮询造成的 Redis 瞬时压力。
            if (!pause(deadline)) break;
            CacheRead cached = readCache(reader, lockName);
            if (!cached.available()) return loader.get();
            if (cached.value().isPresent()) return cached.value().get();
        }
        // 超时请求只回源、不写缓存，避免覆盖仍在执行的锁持有者结果。
        return loader.get();
    }

    private void invalidateUnderLock(String lockName, Runnable eviction) {
        // 失效与重建共用逻辑锁，确保已提交的新版本不会被并发旧数据重新写回。
        String token = UUID.randomUUID().toString();
        long deadline = System.nanoTime()
                + properties.lockTtl().plus(properties.waitTimeout()).toNanos();
        try {
            while (!lock.tryAcquire(lockName, token, properties.lockTtl())) {
                if (!pause(deadline)) {
                    log.warn("等待公开内容缓存锁超时，执行无锁失效：lock={}", lockName);
                    // 锁长期未释放时优先保证内容最终可见，退化为直接删除缓存。
                    evict(eviction, lockName);
                    return;
                }
            }
            try {
                evict(eviction, lockName);
            } finally {
                release(lockName, token);
            }
        } catch (RuntimeException exception) {
            log.warn("公开内容缓存失效失败：lock={}", lockName, exception);
        }
    }

    private CacheRead readCache(Supplier<Optional<Map<String, Object>>> reader, String lockName) {
        try {
            return new CacheRead(true, reader.get());
        } catch (RuntimeException exception) {
            log.debug("公开内容缓存读取失败，直接回源数据库：lock={}, error={}",
                    lockName, exception.toString());
            return new CacheRead(false, Optional.empty());
        }
    }

    private void writeCache(Consumer<Map<String, Object>> writer, Map<String, Object> value,
                            String lockName) {
        try {
            writer.accept(value);
        } catch (RuntimeException exception) {
            log.debug("公开内容缓存写入失败：lock={}, error={}", lockName, exception.toString());
        }
    }

    private void evict(Runnable eviction, String lockName) {
        try {
            eviction.run();
        } catch (RuntimeException exception) {
            log.warn("公开内容缓存删除失败：lock={}", lockName, exception);
        }
    }

    private void release(String lockName, String token) {
        try {
            lock.release(lockName, token);
        } catch (RuntimeException exception) {
            log.debug("公开内容缓存锁释放失败：lock={}, error={}", lockName, exception.toString());
        }
    }

    private boolean pause(long deadline) {
        long remainingNanos = deadline - System.nanoTime();
        if (remainingNanos <= 0) return false;
        long randomMillis = ThreadLocalRandom.current().nextLong(MIN_POLL_MILLIS, MAX_POLL_MILLIS + 1);
        long sleepMillis = Math.min(randomMillis, Duration.ofNanos(remainingNanos).toMillis());
        if (sleepMillis <= 0) return false;
        try {
            Thread.sleep(sleepMillis);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** 区分 Redis 故障与正常未命中，避免故障场景继续参与锁竞争。 */
    private record CacheRead(boolean available, Optional<Map<String, Object>> value) {
    }
}
