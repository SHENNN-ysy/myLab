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

全量摘要不保存 MyLab Markdown 正文；详情 Hash 保存 Markdown 正文，且只缓存卡片本体，不冗余保存全局标签字典（标签展开所需的 tags 字典由全量摘要提供）。缓存保存数据库原始公开数据和 OSS object key，不保存临时签名 URL。每次响应前仍执行停用项过滤、标签展开和当前有效 URL 生成，避免缓存中的签名 URL 过期。

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

### 6.1 Application Port 与事件模型

#### `backend-java/src/main/java/com/myblog/application/port/PublicContentCache.java`

- 新增公开内容缓存端口，使 Application 层不依赖 `StringRedisTemplate`。
- `getAll()` / `putAll()` 对应全量摘要 String Key。
- `getMylabDetail(postKey)` / `putMylabDetail(postKey, detail)` 对应 MyLab 详情 Hash field。
- `evictAll()` / `evictMylabDetails()` 为发布、下线后的失效操作。

#### `backend-java/src/main/java/com/myblog/application/port/DistributedLock.java`

- 新增分布式锁端口，参数由逻辑锁名称、UUID token 和租约时间组成。
- `tryAcquire()` 只表达“尝试获得锁”，Redis 命令细节留在 Infrastructure 层。
- `release()` 要求实现方校验 token，避免旧请求释放其他请求的新锁。

#### `backend-java/src/main/java/com/myblog/application/model/event/PublishedContentChangedEvent.java`

- 新增 `moduleKey` 事件，表示某模块的公开版本已因发布或下线发生变化。
- 事件本身不操作 Redis，用于隔离内容事务与缓存基础设施。

### 6.2 Application Service

#### `backend-java/src/main/java/com/myblog/application/service/content/PublicContentCacheService.java`

- `readAll(loader)`：把全量摘要的缓存读写函数和数据库加载函数交给通用 `readThrough()`。
- `readMylabDetail(postKey, loader)`：把指定 `postKey` 的 Hash field 读写交给同一套 Cache-Aside 流程。
- `readThrough(...)`：依次执行开关判断、首次缓存读取、UUID 锁竞争、锁内双重检查、数据库加载、缓存写回和 token 安全解锁。
- `awaitCache(...)`：未获锁请求按 50～100 毫秒随机间隔轮询；达到 `waitTimeout` 后回源数据库但不写缓存。
- `readCache(...)`：用 `CacheRead.available` 区分 Redis 故障和正常缓存未命中；Redis 故障直接回源，不继续竞争锁。
- `invalidate(moduleKey)`：所有模块失效 `all`；`mylab` 额外失效详情 Hash。
- `invalidateUnderLock(...)`：缓存失效与重建共用相同逻辑锁，先等待正在执行的重建结束，再删除其结果。
- `writeCache()`、`evict()`、`release()`：分别封装 Redis 写入、删除和解锁异常，保证缓存故障不影响主业务结果。

#### `backend-java/src/main/java/com/myblog/application/service/content/ContentModuleServiceImpl.java`

- `publicContent()`：改为调用 `publicCache.readAll(this::loadPublicContent)`；缓存只返回原始摘要，随后仍逐模块执行 `publicData()`。
- `loadPublicContent()`：作为冷缓存数据库加载器，读取七个模块的当前发布版本；MyLab 只调用 `readSummary()`，不把 Markdown 正文放入全量缓存。
- `publicModule(moduleKey)`：保持原有 PostgreSQL 直读，不调用 `PublicContentCacheService`。
- `publicMylabDetail(postKey)`：改为调用 `readMylabDetail()`；命中后仍调用 `publicData()` 展开标签、过滤停用项并生成当前有效图片 URL。
- `loadPublicMylabDetail(postKey)`：作为详情冷缓存加载器，仅从当前已发布 MyLab 版本读取指定文章。
- `publish()` / `offline()`：数据库写操作完成后发布 `PublishedContentChangedEvent`，不在事务方法中直接访问 Redis。

### 6.3 Redis Adapter 与事务监听

#### `backend-java/src/main/java/com/myblog/infrastructure/cache/RedisPublicContentCache.java`

- 定义 `ALL_KEY` 和 `MYLAB_DETAILS_KEY` 两个固定业务 Key。
- `getAll()` / `putAll()` 使用 Redis String 保存全量 JSON，并在写入时设置 `CONTENT_CACHE_TTL`。
- `getMylabDetail()` / `putMylabDetail()` 使用 Redis Hash 保存文章详情；写入 field 后刷新整个 Hash 的 TTL。
- `encode()` / `decode()` 复用项目 Jackson 配置，保持 Map 序列化行为一致。
- `decode()` 捕获损坏 JSON 后只删除对应 String Key 或 Hash field，并将本次读取视为未命中。

#### `backend-java/src/main/java/com/myblog/infrastructure/cache/RedisDistributedLock.java`

- `tryAcquire()` 使用 `StringRedisTemplate.opsForValue().setIfAbsent(key, token, lease)`，对应 Redis `SET NX PX` 语义。
- `release()` 执行 Lua 脚本，在 Redis 服务端原子比较 token 并删除锁。
- `LOCK_PREFIX` 将逻辑锁名称统一映射到 `myblog:content:v1:lock:*`。

#### `backend-java/src/main/java/com/myblog/infrastructure/cache/PublicContentCacheInvalidationListener.java`

- `onPublishedContentChanged()` 使用 `@TransactionalEventListener(AFTER_COMMIT)`。
- 只有数据库事务提交成功才调用 `PublicContentCacheService.invalidate()`；事务回滚时监听器不执行。

### 6.4 配置注册与部署传递

#### `backend-java/src/main/java/com/myblog/common/properties/ContentCacheProperties.java`

- 新增 `app.content-cache` 配置记录，集中绑定 `enabled`、`ttl`、`lockTtl` 和 `waitTimeout`。
- 使用 Spring Boot `Duration` 转换，支持 `6h`、`5s`、`1s` 等配置格式。

#### `backend-java/src/main/java/com/myblog/ApplicationLoader.java`

- 在 `@EnableConfigurationProperties` 中注册 `ContentCacheProperties`，使缓存服务可通过构造器注入配置。

#### `backend-java/src/main/resources/application.yml`

- 增加 `app.content-cache` 配置映射及默认值。

#### `.env.example` 与 `deploy/.env.example`

- 增加本地、生产环境的缓存开关、数据 TTL、锁 TTL 和等待超时示例。

#### `docker-compose.yml` 与 `deploy/docker-compose.yml`

- 将四个 `CONTENT_CACHE_*` 环境变量传入 backend 容器；本地源码构建和生产镜像部署保持一致。

配置项如下：

```env
CONTENT_CACHE_ENABLED=true
CONTENT_CACHE_TTL=6h
CONTENT_CACHE_LOCK_TTL=5s
CONTENT_CACHE_WAIT_TIMEOUT=1s
```

### 6.5 测试代码位置

#### `backend-java/src/test/java/com/myblog/application/service/content/PublicContentCacheServiceTest.java`

- 覆盖缓存命中、双重检查、Redis 异常降级、锁等待超时、并发冷加载和并发失效。
- 使用线程池和内存锁验证两个冷请求只加载一次数据库，以及失效操作最终删除旧加载结果。

#### `backend-java/src/test/java/com/myblog/PublicContentApiIT.java`

- 使用真实 Redis Testcontainer 验证 String、Hash、TTL、损坏 JSON 恢复、锁 token 所有权和自动过期。
- 验证全量缓存不含 Markdown、详情缓存包含 Markdown，并确认临时签名 URL 不进入缓存。

#### `backend-java/src/test/java/com/myblog/AdminContentApiIT.java`

- 验证发布事务提交后删除公开缓存。
- 验证发布校验失败导致事务未提交时，原缓存仍然保留。

#### `ContentModuleServiceImplTest`、`ContentModuleServiceImplCoverageTest`、`ContentModuleServiceImplValidationTest`

- 调整构造器依赖并验证全量、详情缓存接入，公开单模块绕过缓存，以及发布、下线事件发送。

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
