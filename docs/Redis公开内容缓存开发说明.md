# Redis 公开内容缓存开发说明

## 1. 总体结构

前台三个公开内容缓存（`/public/content` 聚合、`/public/content/mylab` 摘要、MyLab 单篇详情）采用三级缓存：

```
L1  Caffeine（JVM 本地缓存，纳秒~微秒级，无网络开销）
      ↓ 未命中
L2  Redis（分布式缓存，毫秒级，所有实例共享）
      ↓ 未命中
L3  PostgreSQL（真实数据源）
```

- 端口：`application/port/PublicContentCache`（9 个方法，载荷为不可变 `Map<String, Object>`）。
- 组合适配器：`infrastructure/cache/LayeredPublicContentCache`（`@Primary`）对端口透明地串联两级：
  - L1 = `CaffeinePublicContentCache`（本地，短 TTL）
  - L2 = `RedisPublicContentCache`（共享权威层，JSON 存 StringRedisTemplate）
- 协调服务：`application/service/content/PublicContentCacheService`，负责双重检查、分布式锁、锁等待、回源与失效，代码不感知层级数。

## 2. 读路径：双重检测机制

"先查询，后加锁"——加锁前先查一次缓存，拿到锁之后再查一次缓存。两级结构下，两次检查都由组合适配器完成 L1→L2 查找：

```
读请求
  ├─ 第一次检查：L1 命中？→ 返回（无网络开销）
  │              L1 未命中 → 查 L2，命中则回填 L1 后返回
  ├─ 双未命中 → tryAcquire 分布式锁（SET NX PX + token）
  │    ├─ 未抢到锁 → 轮询等待持锁者写缓存（50~100ms 随机间隔，waitTimeout 截止）
  │    │            超时只回源、不写缓存，避免与持锁者竞争
  │    └─ 抢到锁 → 第二次检查（L1→L2，处理加锁窗口内已被重建的情况）
  │               仍空 → 回源 PG → 写回（先 L2 后 L1）→ 释放锁（Lua 校验 token）
  └─ 任一级读异常：L1 故障按未命中降级；L2 故障直接回源，不再参与锁竞争（fail-open）
```

详情缓存按 `post_key` 分 entry；不存在/未发布的 key 缓存空 `Map` 做负缓存防穿透，同样进入 L1。

## 3. 写回与失效

- **写回**：仅锁持有者执行，先写 L2（权威共享层）再写 L1；单层失败只记日志不影响另一层。
- **失效**：发布/下线事务提交后（`@TransactionalEventListener(AFTER_COMMIT)` 监听 `PublishedContentChangedEvent`）调用 `invalidate(moduleKey)`，在同一逻辑锁内执行，组合适配器先清 L1（缩短本机脏读窗口）再清 L2：
  - 任意模块发布 → 清 `all`
  - `mylab` / `about` → 额外清 `mylab:summary`（摘要内嵌 about 头像）
  - `mylab` → 额外清整个详情 Hash 与 L1 详情缓存（负缓存一并清除）
- **兜底**：失效失败不回滚发布；L2 靠 6h TTL、L1 靠 60s TTL 自愈。

## 4. 缓存 Key 与版本号

| 缓存 | Key | 结构 |
|------|-----|------|
| 聚合 | `mylab:blog:content:v3:all` | String JSON |
| MyLab 摘要 | `mylab:blog:content:v2:mylab:summary` | String JSON |
| MyLab 详情 | `mylab:blog:content:v1:mylab:details` | Hash，field=postKey |
| 逻辑锁 | `mylab:blog:content:v1:lock:{all\|mylab:summary\|mylab:details}` | String，SET NX PX |

载荷结构变化时必须升级对应 key 的版本段（如 `v3`→`v4`），否则旧结构缓存会存活至 TTL（最长 6 小时）。L1 在 JVM 内，随重启失效、TTL 短，不参与版本号管理。

## 5. 配置项

| 环境变量 | 默认 | 说明 |
|----------|------|------|
| `CONTENT_CACHE_ENABLED` | `true` | 总开关，关闭后完全绕过缓存层 |
| `CONTENT_CACHE_TTL` | `6h` | L2 Redis 缓存 TTL |
| `CONTENT_CACHE_LOCK_TTL` | `5s` | 分布式锁租约 |
| `CONTENT_CACHE_WAIT_TIMEOUT` | `1s` | 等锁轮询超时 |
| `CONTENT_CACHE_L1_TTL` | `60s` | L1 Caffeine 过期时间（远短于 L2，兜底自愈） |
| `CONTENT_CACHE_L1_MAX_SIZE` | `512` | L1 详情缓存最大条目数 |

## 6. 一致性边界

- 发布提交后到失效执行前的毫秒级窗口内，L1/L2 可能返回旧值，与单级 Redis 缓存同量级，可接受。
- 当前 backend 为单实例部署，L1 失效只需本 JVM 内清除。**扩副本前必须改为跨实例广播失效**（如 Redis Pub/Sub），否则其他实例的 L1 最长脏读 60s。
- 缓存载荷为原始数据库摘要（存 `object_key` 而非签名 URL），公开 URL 在每次响应时动态生成，因此 L1 直接共享同一 `Map` 引用，无 URL 过期问题；调用方均先复制再修改，不会原地改缓存对象。

## 7. 故障降级

| 场景 | 行为 |
|------|------|
| L1 读/写异常 | 按未命中/跳过处理，仅 DEBUG 日志 |
| L2 读异常 | 直接回源 PG，不加锁不写缓存 |
| L2 写异常 | 回源结果正常返回，仅记日志 |
| 锁不可用 | 直接回源，不等待 |
| 失效时锁超时 | 退化为无锁删除并 WARN，保证内容最终可见 |
