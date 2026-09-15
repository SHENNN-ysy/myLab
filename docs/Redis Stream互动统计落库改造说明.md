# Redis Stream 互动统计落库改造说明

## 1. 改造背景

MyBlog 的浏览、点赞和站点访问统计以 Redis 为实时数据源，原链路通过三组 `dirty → processing → ack` Set 每分钟把绝对值快照写入 PostgreSQL。该方案可以恢复失败批次，但没有标准消息积压、消费者 Pending、消费延迟等可观测状态，也不便于后续扩展独立消费者。

本次 Stream 改造不修改数据库结构，也不持久化访客明细；落库触发链路改为 Redis Stream Consumer Group。Redis Hash 仍保存实时绝对计数，Stream 只负责通知“哪些聚合维度需要刷新”，不会作为计数真值来源。后续访客统计口径升级统一了公开页面上报接口，但没有改变这里的 Stream 消息结构和消费协议。

## 2. 改造目标与边界

- 浏览、点赞、取消点赞和访问发生有效计数变化时，在同一个 Lua 原子操作中更新 Hash 并追加 Stream 消息。
- 消费者按批合并文章、站点、日期维度，再读取 Redis 最新绝对值写入 PostgreSQL，降低重复 SQL。
- PostgreSQL 写入成功后才执行 `XACK`；异常消息进入死信 Stream 后单独确认。
- 消费者异常退出后，其他实例可接管超过空闲阈值的 Pending 消息。
- 多实例通过短租约保证同一时刻只有一个落库批次，避免绝对值快照乱序覆盖。
- 保留旧 dirty Set 写入与旧快照任务，支持配置开关快速回滚。
- 访客状态使用 `mylab:blog:visitor:v2:{visitorHash}` Hash：凭证默认 24 小时滑动有效，一份凭证只计一次访问，首页、MyLab 列表和每篇 MyLab 详情分别只计一次浏览。

没有新增 Flyway 迁移或修改 PostgreSQL 表。页面浏览统一通过 `POST /api/v1/public/analytics/page-views` 上报；Stream 事件类型与字段保持兼容。

## 3. 新链路

```text
公开互动请求
  → 校验已发布文章/访客身份
  → Redis Lua：去重 + 更新实时 Hash + XADD 聚合脏通知
  → 立即返回 Redis 实时计数

EngagementStreamJob
  → 获取单活租约
  → 优先 XPENDING + XCLAIM 接管超时消息
  → 无超时消息时 XREADGROUP 读取新消息
  → 合并 post_key / site / stat_date
  → 从 Redis 读取最新绝对值
  → PostgreSQL 事务 Upsert
  → XACK
```

这里采用“脏通知 + 最新绝对值”，而不是把 `+1/-1` 增量直接落库。相同消息重复消费、同一文章短时间出现多条消息时，最终都会用 Redis 当前值覆盖 PostgreSQL 快照，因此至少一次投递不会造成重复累加。

## 4. Redis 数据结构

### 4.1 主 Stream

- Key：`mylab:blog:stream:engagement:v1`
- Consumer Group：`engagement-persistence-v1`
- 消费者名：`<hostname>:<随机 8 位>`
- 默认保留期：72 小时

消息字段：

| 字段 | 说明 |
| --- | --- |
| `event_type` | `VIEW`、`LIKE`、`UNLIKE`、`VISIT` |
| `post_key` | 文章维度；访问事件为空 |
| `stat_date` | 业务日期；取消点赞为空，因为当前按日表不回减点赞 |
| `post_dirty` | 是否刷新文章聚合，值为 `0/1` |
| `site_dirty` | 是否刷新站点聚合，值为 `0/1` |
| `daily_dirty` | 是否刷新按日聚合，值为 `0/1` |

消息不写入 visitor token、visitor hash、IP、User-Agent 或 Cookie。

### 4.2 死信 Stream

- Key：`mylab:blog:stream:engagement:dlq:v1`
- 字段：原消息 ID、失败原因、失败时间、原始字段文本

只有内部格式非法、无法安全解析的消息会进入死信。PostgreSQL 或 Redis 暂时故障不会进入死信，也不会确认，仍保留在 Pending 中等待重试。

### 4.3 消费租约

- Key：`mylab:blog:lock:engagement-persistence`
- 获取：`SET key <uuid> NX PX <ttl>`
- 释放：Lua 比较 UUID 后删除，避免误删已被其他实例重新获得的锁
- 默认 TTL：30 秒

兼容链路使用 `mylab:blog:dirty:*` 与 `mylab:blog:processing:*`。Stream 模式下旧快照任务不启动；关闭 Stream 后旧任务恢复消费这些集合。

## 5. 消费与一致性

### 5.1 批量合并

每批默认最多读取 500 条消息。消费者先校验消息字段，再把重复的 `post_key` 和 `stat_date` 去重；站点维度只读取一次。即使 500 条消息都来自同一篇文章，数据库通常只执行该文章、站点和相关日期的绝对值 Upsert。

### 5.2 确认顺序

1. 读取或接管消息。
2. 非法消息写死信并确认。
3. 从 Redis 读取有效消息涉及的最新绝对值。
4. 调用 `EngagementStatsRepository.saveSnapshot` 完成 PostgreSQL 事务。
5. 数据库提交成功后确认有效消息。

如果第 3～4 步失败，不执行 `XACK`；如果数据库成功而确认响应失败，消息会重放，幂等绝对值 Upsert 会再次得到相同或更新的结果。

### 5.3 Pending 接管

每轮先通过 `XPENDING` 查找空闲时间超过 `ENGAGEMENT_STREAM_CLAIM_IDLE` 的消息，再使用 `XCLAIM` 转移给当前消费者。Spring Data Redis 当前适配层采用 `XPENDING + XCLAIM`，效果等价于本场景所需的 `XAUTOCLAIM` 故障接管语义。

### 5.4 安全清理

定时清理使用 `XTRIM MINID`，清理水位取以下三者的最小值：

- 当前时间减去 Stream 保留期；
- Consumer Group 最后投递消息 ID；
- 最老 Pending 消息 ID。

因此未投递消息和 Pending 消息不会因时间清理被删除。死信 Stream 不自动删除，需先排查原因再由运维人工处理。

## 6. 故障行为

| 故障点 | 对公开请求的影响 | 落库行为 |
| --- | --- | --- |
| Redis 不可用 | 写互动返回 `14001`；查询回退 PG 最近快照 | 消费暂停，Redis 恢复后继续 |
| PostgreSQL 不可用 | 实时互动仍由 Redis 正常响应 | 消息留在 Pending，恢复后接管重试 |
| 消费实例退出 | 无影响 | Pending 超过 claim idle 后由其他实例接管 |
| 消息格式非法 | 无影响 | 写入 DLQ 后确认，避免阻塞队列 |
| 租约被其他实例持有 | 无影响 | 本轮跳过，下一轮重试 |

Redis 仍是尚未落库数据的唯一实时来源，生产环境应开启 AOF 持久化并妥善备份 Redis 数据。PG 快照用于查询降级和 Redis 重启后的聚合值兜底恢复，不保存访客级去重状态。

## 7. 配置项

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `ENGAGEMENT_STREAM_ENABLED` | `true` | 启用 Stream 消费；`false` 回退旧快照任务 |
| `ENGAGEMENT_STREAM_BATCH_SIZE` | `500` | 单批最大消息数 |
| `ENGAGEMENT_STREAM_BLOCK_TIMEOUT` | `2s` | 读取新消息的阻塞时间 |
| `ENGAGEMENT_STREAM_CLAIM_IDLE` | `30s` | Pending 可被接管的最小空闲时间 |
| `ENGAGEMENT_STREAM_RETENTION` | `72h` | 已确认历史的最短保留时间 |
| `ENGAGEMENT_STREAM_LOCK_TTL` | `30s` | 单活消费租约时间 |
| `ENGAGEMENT_STREAM_POLL_DELAY` | `1s` | 消费轮询间隔 |
| `ENGAGEMENT_STREAM_TRIM_INTERVAL` | `1h` | 安全清理周期 |
| `VISITOR_IDENTITY_TTL` | `24h` | 匿名访客 Hash 与 Cookie 的滑动有效期 |

配置已同步到 `application.yml`、根目录与生产 Compose、`.env.example` 和 `deploy/.env.example`。

## 8. 监控与排障

Actuator/Micrometer 新增：

| 指标 | 含义 |
| --- | --- |
| `engagement.stream.messages.processed` | 已成功持久化并确认的有效消息累计数 |
| `engagement.stream.consume.failures` | 消费批次失败累计数 |
| `engagement.stream.batch.duration` | 消费批次耗时 |
| `engagement.stream.last.success.epoch.millis` | 最近一次成功处理非空批次的时间戳 |
| `engagement.stream.length` | 主 Stream 当前长度 |
| `engagement.stream.pending` | Consumer Group 当前 Pending 数量 |

常用检查命令：

```bash
redis-cli XLEN mylab:blog:stream:engagement:v1
redis-cli XINFO GROUPS mylab:blog:stream:engagement:v1
redis-cli XPENDING mylab:blog:stream:engagement:v1 engagement-persistence-v1
redis-cli XRANGE mylab:blog:stream:engagement:dlq:v1 - + COUNT 20
```

`pending` 短暂出现是正常的；持续增长、失败计数持续增加或最近成功时间长期不更新，通常表示 PostgreSQL、Redis 网络或消息格式异常。排障期间不要直接 `DEL` 主 Stream，不要在 Stream 和旧快照两种 backend 版本并行运行。

## 9. 上线与回滚

上线步骤：

1. 保持默认 `ENGAGEMENT_STREAM_ENABLED=true` 发布 backend。
2. 验证 Consumer Group 自动创建，执行一次浏览/点赞后观察 Stream、PG 快照和 Pending。
3. 观察消费失败、Pending、批次耗时和日志。
4. 稳定运行一个观察周期后，再决定是否在后续版本删除兼容 dirty Set 链路。

回滚步骤：

1. 停止或重建所有新版本 backend，避免两种落库任务并行。
2. 设置 `ENGAGEMENT_STREAM_ENABLED=false`。
3. 重建 backend，确认 `EngagementSnapshotJob` 恢复运行。
4. 保留主 Stream 与死信 Stream 用于排障，不需要清空。

## 10. 代码改动清单

- 新增应用端口 `EngagementEventStream`、`EngagementPersistenceLease`。
- 新增 `EngagementPersistenceService`，负责消息校验、维度合并、绝对值落库和确认。
- 新增 Redis Stream、短租约适配器及 `EngagementStreamJob` 调度器。
- 扩展 `RedisEngagementStore` 的四段 Lua，在有效计数变化时原子 `XADD`。
- 旧 `EngagementSnapshotJob` 改为仅在 Stream 开关关闭时启用。
- 新增配置绑定、Compose 与环境变量模板。
- 新增单元测试以及基于 Testcontainers 的写入、幂等、隐私、落库确认和 Pending 接管集成测试。

## 11. 验证结果

- Stream 消息仅在有效计数变化时产生；同一凭证重复浏览同一目标、重复点赞或重复访问不会产生重复通知。
- 消息中不包含 visitor 字段。
- 同批消息会按聚合维度去重并保存 Redis 最新绝对值。
- PG 成功后 Pending 归零；模拟消费者退出后可由新消费者接管。
- 数据库失败时不确认消息；非法消息进入 DLQ。
- `mvn verify` 通过：289 个单元测试和 43 个 Testcontainers 集成测试全部成功，Checkstyle 0 违规、SpotBugs 0 问题，并完成 JaCoCo 与 ArchUnit 检查。
