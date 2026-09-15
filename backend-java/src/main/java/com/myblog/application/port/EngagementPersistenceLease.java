package com.myblog.application.port;

import java.time.Duration;

/** 互动落库单活租约，防止多实例并发覆盖 PostgreSQL 绝对值快照。 */
public interface EngagementPersistenceLease {

    /** 尝试获取租约，成功返回唯一 token，竞争失败返回 null。 */
    String tryAcquire(Duration lease);

    /** 仅释放当前 token 持有的租约。 */
    void release(String token);
}
