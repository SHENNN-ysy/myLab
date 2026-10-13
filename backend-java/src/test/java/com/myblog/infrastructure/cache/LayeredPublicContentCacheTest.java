package com.myblog.infrastructure.cache;

import com.myblog.application.port.PublicContentCache;
import com.myblog.common.properties.ContentCacheProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** LayeredPublicContentCache 的 L1/L2 读穿、回填、双写与双清测试。 */
class LayeredPublicContentCacheTest {

    private CaffeinePublicContentCache l1;
    private PublicContentCache l2;
    private LayeredPublicContentCache cache;

    @BeforeEach
    void setUp() {
        ContentCacheProperties properties = new ContentCacheProperties(true, Duration.ofHours(6),
                Duration.ofSeconds(5), Duration.ofSeconds(1), Duration.ofSeconds(60), 16);
        l1 = new CaffeinePublicContentCache(properties);
        l2 = mock(PublicContentCache.class);
        cache = new LayeredPublicContentCache(l1, l2);
    }

    /** L1 命中时直接返回，不访问 L2。 */
    @Test
    void l1HitSkipsL2() {
        Map<String, Object> expected = Map.of("home", Map.of());
        l1.putAll(expected);

        assertThat(cache.getAll()).containsSame(expected);
        verify(l2, never()).getAll();
    }

    /** L1 未命中而 L2 命中时回填 L1，后续读取不再访问 L2。 */
    @Test
    void l2HitBackfillsL1() {
        Map<String, Object> expected = Map.of("cards", java.util.List.of());
        when(l2.getMylabSummary()).thenReturn(Optional.of(expected));

        assertThat(cache.getMylabSummary()).containsSame(expected);
        assertThat(l1.getMylabSummary()).containsSame(expected);

        assertThat(cache.getMylabSummary()).containsSame(expected);
        verify(l2, org.mockito.Mockito.times(1)).getMylabSummary();
    }

    /** 详情按 postKey 经 L2 回填 L1，双未命中返回空。 */
    @Test
    void detailReadThroughAndDoubleMiss() {
        Map<String, Object> detail = Map.of("post_key", "post-a");
        when(l2.getMylabDetail("post-a")).thenReturn(Optional.of(detail));
        when(l2.getMylabDetail("missing")).thenReturn(Optional.empty());

        assertThat(cache.getMylabDetail("post-a")).containsSame(detail);
        assertThat(l1.getMylabDetail("post-a")).containsSame(detail);
        assertThat(cache.getMylabDetail("missing")).isEmpty();
    }

    /** 写入同时落到 L2 与 L1，L1 立即可读。 */
    @Test
    void putWritesBothLevels() {
        Map<String, Object> content = Map.of("home", Map.of());
        Map<String, Object> detail = Map.of("post_key", "post-a");

        cache.putAll(content);
        cache.putMylabSummary(Map.of("cards", java.util.List.of()));
        cache.putMylabDetail("post-a", detail);

        verify(l2).putAll(content);
        verify(l2).putMylabSummary(any());
        verify(l2).putMylabDetail("post-a", detail);
        assertThat(l1.getAll()).containsSame(content);
        assertThat(l1.getMylabDetail("post-a")).containsSame(detail);
    }

    /** 失效同时清除 L1 与 L2，含已缓存的负缓存空 Map。 */
    @Test
    void evictClearsBothLevelsIncludingNegativeEntry() {
        Map<String, Object> content = Map.of("home", Map.of());
        l1.putAll(content);
        l1.putMylabSummary(Map.of("cards", java.util.List.of()));
        l1.putMylabDetail("missing", Map.of());

        cache.evictAll();
        cache.evictMylabSummary();
        cache.evictMylabDetails();

        assertThat(l1.getAll()).isEmpty();
        assertThat(l1.getMylabSummary()).isEmpty();
        assertThat(l1.getMylabDetail("missing")).isEmpty();
        verify(l2).evictAll();
        verify(l2).evictMylabSummary();
        verify(l2).evictMylabDetails();
    }

    /** L2 读异常原样上抛，交由协调服务按既有降级语义处理。 */
    @Test
    void l2ReadFailurePropagates() {
        when(l2.getAll()).thenThrow(new IllegalStateException("redis unavailable"));

        assertThatThrownBy(cache::getAll).isInstanceOf(IllegalStateException.class);
    }

    /** L1 读异常降级为未命中，继续查 L2 并正常返回。 */
    @Test
    void l1ReadFailureDegradesToL2() {
        PublicContentCache brokenL1 = mock(PublicContentCache.class);
        when(brokenL1.getAll()).thenThrow(new IllegalStateException("l1 broken"));
        LayeredPublicContentCache resilient = new LayeredPublicContentCache(brokenL1, l2);
        Map<String, Object> expected = Map.of("home", Map.of());
        when(l2.getAll()).thenReturn(Optional.of(expected));

        assertThat(resilient.getAll()).containsSame(expected);
    }

    /** L1 回填失败不影响 L2 命中结果返回。 */
    @Test
    void l1BackfillFailureStillReturnsL2Value() {
        PublicContentCache brokenL1 = mock(PublicContentCache.class);
        when(brokenL1.getMylabSummary()).thenReturn(Optional.empty());
        doThrow(new IllegalStateException("l1 broken")).when(brokenL1).putMylabSummary(any());
        LayeredPublicContentCache resilient = new LayeredPublicContentCache(brokenL1, l2);
        Map<String, Object> expected = Map.of("cards", java.util.List.of());
        when(l2.getMylabSummary()).thenReturn(Optional.of(expected));

        assertThat(resilient.getMylabSummary()).containsSame(expected);
    }

    /** L2 写失败不抛出，L1 仍然写入，保障本地读路径可用。 */
    @Test
    void l2WriteFailureStillWritesL1WithoutThrowing() {
        doThrow(new IllegalStateException("redis unavailable")).when(l2).putAll(any());
        Map<String, Object> content = Map.of("home", Map.of());

        assertThatCode(() -> cache.putAll(content)).doesNotThrowAnyException();
        assertThat(l1.getAll()).containsSame(content);
    }

    /** 单层删除失败不抛出，另一层删除仍然执行。 */
    @Test
    void evictionFailureOnOneLevelDoesNotBlockTheOther() {
        doThrow(new IllegalStateException("redis unavailable")).when(l2).evictMylabDetails();
        l1.putMylabDetail("post-a", Map.of("post_key", "post-a"));

        assertThatCode(cache::evictMylabDetails).doesNotThrowAnyException();
        assertThat(l1.getMylabDetail("post-a")).isEmpty();
    }
}
