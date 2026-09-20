package com.myblog.infrastructure.cache;

import com.myblog.application.model.event.PublishedContentChangedEvent;
import com.myblog.application.service.content.PublicContentCacheService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 在发布或下线事务提交后清理前台公开内容缓存。 */
@Component
public class PublicContentCacheInvalidationListener {
    private final PublicContentCacheService cache;

    public PublicContentCacheInvalidationListener(PublicContentCacheService cache) {
        this.cache = cache;
    }

    /** 仅在发布或下线事务成功提交后执行，回滚事务不会污染缓存。 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPublishedContentChanged(PublishedContentChangedEvent event) {
        // 提交后失效失败（如 Redis 不可用）仅被框架记日志，不回滚已落库的发布结果；残留旧缓存靠 TTL 兜底
        cache.invalidate(event.moduleKey());
    }
}
