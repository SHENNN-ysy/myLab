package com.myblog.application.port;

import java.time.Duration;

/**
 * 分布式互斥锁端口：隔离应用层与具体 Redis 命令。
 */
public interface DistributedLock {

    /** 尝试在租期内取得指定逻辑锁。 */
    boolean tryAcquire(String name, String token, Duration lease);

    /** 仅由持有相同 token 的调用方释放锁。 */
    void release(String name, String token);
}
