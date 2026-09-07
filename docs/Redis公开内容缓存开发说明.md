# Redis 公开内容缓存开发说明

## 1. 改造目标与范围

本次改造仅优化博客前台高频读取接口，降低 PostgreSQL 的重复查询压力，同时保持公开 API 响应格式不变。

缓存接口：

- `GET /api/v1/public/content`：博客前台全部模块公开摘要。
- `GET /api/v1/public/mylab/{postKey}`：MyLab 单篇 Markdown 详情。

不使用缓存的接口：

- `GET /api/v1/public/content/{moduleKey}`：公开单模块接口仍直接查询 PostgreSQL。
- `/api/v1/admin/content/**`：博客后台内容管理接口仍直接查询 PostgreSQL。

本次改造不修改数据库结构，不新增 Domain 层，不改变前端调用方式。

## 2. 缓存结构

| Redis Key | 类型 | 内容 | TTL |
|---|---|---|---|
| `myblog:content:v1:all` | String | 全部模块的原始公开摘要 JSON | 6 小时 |
| `myblog:content:v1:mylab:details` | Hash | field 为 `postKey`，value 为原始 MyLab 详情 JSON | 6 小时 |
| `myblog:content:v1:lock:all` | String | 全量摘要重建锁的 UUID token | 5 秒 |
| `myblog:content:v1:lock:mylab:details` | String | MyLab 详情 Hash 重建锁的 UUID token | 5 秒 |

全量摘要不保存 MyLab Markdown 正文；详情 Hash 保存 Markdown 正文。缓存保存数据库原始公开数据和 OSS object key，不保存临时签名 URL。每次响应前仍执行停用项过滤、标签展开和当前有效 URL 生成，避免缓存中的签名 URL 过期。

## 3. 读取流程

读取采用 Cache-Aside、Redis 分布式互斥锁和双重检查：

1. 读取 Redis，命中时直接返回原始公开数据。
2. 未命中时使用 `SET key token NX PX` 尝试获取重建锁。
3. 获取锁后再次读取缓存，防止等待锁期间其他请求已完成重建。
4. 双重检查仍未命中时查询 PostgreSQL，并将结果写入 Redis。
5. 释放锁时执行 Lua 脚本，仅当 Redis 中 token 与当前请求 UUID 一致时删除锁。
6. 未获得锁的请求以 50～100 毫秒随机间隔轮询，最多等待 1 秒。
7. 等待超时后直接查询 PostgreSQL，但不写缓存，避免并发覆盖锁持有者的结果。

两个不同 MyLab 文章的冷请求共享详情 Hash 锁，确保 Hash 初始化和 TTL 设置过程互斥。

## 4. 异常降级

Redis 读取、反序列化、加锁或写入异常均不影响公开接口可用性：

- Redis 不可用或锁操作异常时直接查询 PostgreSQL。
- 缓存 JSON 损坏时删除损坏条目并回源数据库重建。
- 数据库查询成功但缓存写入失败时仍返回数据库结果。
- `CONTENT_CACHE_ENABLED=false` 时完全绕过公开内容缓存。

Redis 故障不会阻断博客访问，但在故障期间数据库读取压力会恢复到未缓存状态。

## 5. 发布与下线失效

内容发布或下线成功后，由应用服务发布 `PublishedContentChangedEvent`。监听器使用 `AFTER_COMMIT` 事务阶段执行缓存失效：

- 任意模块发布或下线：删除 `myblog:content:v1:all`。
- MyLab 发布或下线：额外删除 `myblog:content:v1:mylab:details`。

缓存失效与缓存重建使用相同的分布式锁，避免旧事务读取的数据在失效完成后重新写回。事务回滚不会触发失效。Redis 清理失败不回滚已提交的内容，陈旧缓存最迟由 6 小时 TTL 淘汰。

## 6. 代码改动

### Application Port

- `PublicContentCache`：定义全量摘要和 MyLab 详情的读、写、删除能力。
- `DistributedLock`：定义带 token 和租约时间的加锁、解锁能力。

### Application Service

- `PublicContentCacheService`：统一实现缓存命中、冷缓存重建、双重检查、等待超时、异常降级和缓存失效。
- `ContentModuleServiceImpl`：仅在全量公开内容和 MyLab 详情入口调用缓存服务；公开单模块和后台接口保持数据库直读。
- `PublishedContentChangedEvent`：承载事务提交后的模块失效通知。

### Redis Adapter

- `RedisPublicContentCache`：负责 JSON 序列化、String/Hash 读写、TTL 和损坏缓存清理。
- `RedisDistributedLock`：使用 Redis `SET NX` 获取锁，并使用 Lua 校验所有权后释放。
- `PublicContentCacheInvalidationListener`：事务提交后调用缓存失效逻辑。

### 配置与部署

`application.yml`、本地及生产 Compose、`.env.example` 和 `deploy/.env.example` 新增以下配置：

```env
CONTENT_CACHE_ENABLED=true
CONTENT_CACHE_TTL=6h
CONTENT_CACHE_LOCK_TTL=5s
CONTENT_CACHE_WAIT_TIMEOUT=1s
```

## 7. 测试结果

### 单元测试

覆盖以下场景：

- 缓存命中、未命中、双重检查和数据库写回。
- Redis 读取异常、损坏缓存、写入异常时回源数据库。
- 未获锁轮询、等待超时后回源且不写缓存。
- UUID 锁所有权、错误 token 不解锁、锁租约自动过期。
- 两个并发冷缓存请求只执行一次数据库加载。
- 全量摘要不包含 Markdown，MyLab 详情包含 Markdown。
- 原始 object key 被缓存，临时签名 URL 每次响应重新生成。
- 公开单模块接口不访问缓存。
- 发布和下线产生事务事件并按模块清理缓存。

### 集成测试

使用现有 PostgreSQL 16 和 Redis 7 Testcontainers 验证真实 Redis String、Hash、TTL、Lua 解锁、损坏数据恢复、发布后失效和事务失败不失效。

### 完整质量门

执行：

```bash
cd backend-java
mvn verify
```

实际结果：

- 单元测试：271 个，全部通过。
- Testcontainers 集成测试：33 个，全部通过。
- Checkstyle：通过。
- SpotBugs：0 个问题。
- JaCoCo 报告：生成成功。
- Maven：`BUILD SUCCESS`。

## 8. 上线检查

部署后可在 Redis 容器中检查缓存：

```bash
docker compose exec redis redis-cli GET myblog:content:v1:all
docker compose exec redis redis-cli TTL myblog:content:v1:all
docker compose exec redis redis-cli HKEYS myblog:content:v1:mylab:details
docker compose exec redis redis-cli HGET myblog:content:v1:mylab:details <postKey>
docker compose exec redis redis-cli TTL myblog:content:v1:mylab:details
docker compose exec redis redis-cli SCAN 0 MATCH 'myblog:content:v1:*' COUNT 100
```

建议验收步骤：首次访问全量公开接口后确认 `all` 存在；首次访问 MyLab 详情后确认对应 Hash field 存在；发布或下线内容后确认相关缓存被删除；再次访问公开接口后确认缓存重新生成。
