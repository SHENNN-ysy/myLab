package com.myblog.infrastructure.cache;

import com.myblog.application.model.event.PublishedContentChangedEvent;
import com.myblog.application.service.engagement.EngagementService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** MyLab 发布或下线事务提交后，重建互动写接口使用的已发布文章索引。 */
@Component
public class PublishedPostIndexRefreshListener {
    private final EngagementService engagement;

    public PublishedPostIndexRefreshListener(EngagementService engagement) {
        this.engagement = engagement;
    }

    /** 仅在事务提交后重建：已发布集合以数据库为准，回滚不产生索引变更。 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPublishedContentChanged(PublishedContentChangedEvent event) {
        // 只有 mylab 模块的发布/下线会影响互动索引，其他模块直接忽略
        if (!"mylab".equals(event.moduleKey())) return;
        // rebuild 在 Redis 不可用时静默降级：缺失索引由互动写路径回源数据库补写，无需补偿重试
        engagement.refreshPublishedPostIndex();
    }
}
