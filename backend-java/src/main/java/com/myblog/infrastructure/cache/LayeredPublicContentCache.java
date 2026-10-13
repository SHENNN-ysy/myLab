package com.myblog.infrastructure.cache;

import com.myblog.application.port.PublicContentCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 两级公开内容缓存组合适配器：L1 Caffeine（JVM 本地）在前，L2 Redis（共享权威层）在后。
 * 读：先 L1，未命中查 L2 并回填 L1；写：先 L2 后 L1；删：先 L1 后 L2，缩短本机脏读窗口。
 * L1 故障一律降级为未命中；L2 读异常原样上抛，由协调服务按既有 fail-open 语义处理。
 */
@Slf4j
@Primary
@Component
public class LayeredPublicContentCache implements PublicContentCache {

    private final PublicContentCache l1;
    private final PublicContentCache l2;

    public LayeredPublicContentCache(@Qualifier("caffeinePublicContentCache") PublicContentCache l1,
                                     @Qualifier("redisPublicContentCache") PublicContentCache l2) {
        this.l1 = l1;
        this.l2 = l2;
    }

    @Override
    public Optional<Map<String, Object>> getAll() {
        return readThrough("all", l1::getAll, l2::getAll, l1::putAll);
    }

    @Override
    public void putAll(Map<String, Object> content) {
        writeThrough("all", () -> l2.putAll(content), () -> l1.putAll(content));
    }

    @Override
    public Optional<Map<String, Object>> getMylabSummary() {
        return readThrough("mylab:summary", l1::getMylabSummary, l2::getMylabSummary, l1::putMylabSummary);
    }

    @Override
    public void putMylabSummary(Map<String, Object> content) {
        writeThrough("mylab:summary", () -> l2.putMylabSummary(content), () -> l1.putMylabSummary(content));
    }

    @Override
    public Optional<Map<String, Object>> getMylabDetail(String postKey) {
        return readThrough("mylab:details:" + postKey,
                () -> l1.getMylabDetail(postKey),
                () -> l2.getMylabDetail(postKey),
                detail -> l1.putMylabDetail(postKey, detail));
    }

    @Override
    public void putMylabDetail(String postKey, Map<String, Object> detail) {
        writeThrough("mylab:details:" + postKey,
                () -> l2.putMylabDetail(postKey, detail),
                () -> l1.putMylabDetail(postKey, detail));
    }

    @Override
    public void evictAll() {
        evictThrough("all", l1::evictAll, l2::evictAll);
    }

    @Override
    public void evictMylabSummary() {
        evictThrough("mylab:summary", l1::evictMylabSummary, l2::evictMylabSummary);
    }

    @Override
    public void evictMylabDetails() {
        evictThrough("mylab:details", l1::evictMylabDetails, l2::evictMylabDetails);
    }

    /** 先读 L1，未命中再读 L2 并把命中结果回填 L1；L1 读写失败仅降级不阻断。 */
    private Optional<Map<String, Object>> readThrough(String name,
                                                      Supplier<Optional<Map<String, Object>>> l1Reader,
                                                      Supplier<Optional<Map<String, Object>>> l2Reader,
                                                      Consumer<Map<String, Object>> l1Writer) {
        Optional<Map<String, Object>> l1Value = readL1(name, l1Reader);
        if (l1Value.isPresent()) return l1Value;
        Optional<Map<String, Object>> l2Value = l2Reader.get();
        l2Value.ifPresent(value -> writeL1(name, () -> l1Writer.accept(value)));
        return l2Value;
    }

    /** 先写权威共享层 L2，再写本地 L1；单层失败只记日志，不影响另一层。 */
    private void writeThrough(String name, Runnable l2Writer, Runnable l1Writer) {
        try {
            l2Writer.run();
        } catch (RuntimeException exception) {
            log.debug("公开内容 L2 缓存写入失败：cache={}, error={}", name, exception.toString());
        }
        writeL1(name, l1Writer);
    }

    /** 先清本机 L1 缩短脏读窗口，再清 L2；单层失败只记日志。 */
    private void evictThrough(String name, Runnable l1Eviction, Runnable l2Eviction) {
        try {
            l1Eviction.run();
        } catch (RuntimeException exception) {
            log.debug("公开内容 L1 缓存删除失败：cache={}, error={}", name, exception.toString());
        }
        try {
            l2Eviction.run();
        } catch (RuntimeException exception) {
            log.warn("公开内容 L2 缓存删除失败：cache={}", name, exception);
        }
    }

    private Optional<Map<String, Object>> readL1(String name,
                                                 Supplier<Optional<Map<String, Object>>> reader) {
        try {
            return reader.get();
        } catch (RuntimeException exception) {
            log.debug("公开内容 L1 缓存读取失败，按未命中处理：cache={}, error={}",
                    name, exception.toString());
            return Optional.empty();
        }
    }

    private void writeL1(String name, Runnable writer) {
        try {
            writer.run();
        } catch (RuntimeException exception) {
            log.debug("公开内容 L1 缓存写入失败：cache={}, error={}", name, exception.toString());
        }
    }
}
