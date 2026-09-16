# Redis Stream 互动统计落库改造开发说明

## 1. 改造目标与范围

### 1.1 改造概述

MyLab 的浏览、点赞和站点访问统计以 Redis 实时绝对值为准，PostgreSQL 保存可恢复、可查询的聚合快照。改造前，落库任务每分钟扫描三组 `dirty → processing → ack` Set。该方案可以重试失败批次，但缺少标准的消息积压、Pending、消费者接管和消费延迟语义，也不利于扩展监控与独立消费者。

本次改造把落库触发链路切换为 Redis Stream Consumer Group：互动请求仍同步更新 Redis 绝对计数，但会在同一段 Lua 中追加“聚合维度已变脏”的通知；消费者批量合并通知，从 Redis 读取最新绝对值，并在一个 PostgreSQL 事务中覆盖三类统计快照，事务成功后才执行 `XACK`。

同一批代码还调整了匿名访客统计口径。访客凭证改为 24 小时滑动有效，一份有效凭证只累计一次访问；首页、MyLab 列表和每篇 MyLab 详情分别只累计一次浏览。旧访问接口与详情浏览接口由统一页面浏览接口替代。

### 1.2 改造范围

- 后端应用层：新增 Stream 与消费租约端口、落库服务、页面浏览 DTO 和统一页面浏览业务入口。
- 后端 Redis 适配层：新增 Stream、短租约和 Visitor v2 Hash，实现计数、去重、脏标记与 `XADD` 的 Lua 原子写入。
- 后端调度层：新增 Stream 消费、启动恢复、安全裁剪和 Micrometer 指标，并保留旧快照任务作为开关回滚通道。
- 后端 HTTP 层：新增统一页面浏览接口，删除旧访问和详情浏览写接口，统一刷新匿名访客 Cookie。
- 博客前台：按路由上报页面类型，并串行化所有会签发或刷新访客 Cookie 的写请求。
- Redis 命名空间：统一使用 `mylab:` 顶层，并将公开内容缓存归入 `mylab:blog:content:*`。
- 配置与部署：新增 Stream 批量、接管、保留、租约、调度和访客 TTL 配置。
- 文档与测试：同步 API、错误码、Key 命名、部署说明、单元测试和 Testcontainers 集成测试。

### 1.3 不受影响的范围

- 不修改 PostgreSQL 表结构，不新增 Flyway 迁移。
- 不把 visitor token、visitor hash、IP、User-Agent、Cookie 或点赞关系写入 PostgreSQL。
- 不改变 `mylab_engagement_stats`、`site_traffic_stats`、`site_daily_stats` 的表结构和绝对值 Upsert 语义。
- 管理后台认证仍使用 Redis 会话令牌；仅 Key 顶层前缀由 `auth:*` 变为 `mylab:auth:*`。
- 限流算法和阈值不变；仅 Key 顶层前缀由 `rate:*` 变为 `mylab:rate:*`。
- 公开内容读取与管理端趋势接口的业务契约不因 Stream 消费失败而改变；Redis 实时统计读取失败时仍可回退 PostgreSQL 快照。

### 1.4 关键否定声明

本次改造没有迁移、清零或校正历史累计统计，新口径只影响上线后的访客互动；没有把 Stream 作为计数真值，也没有尝试让 PostgreSQL 与 Redis 组成分布式 ACID 事务，而是使用 Redis 绝对值覆盖、至少一次消费和 Pending 重试实现最终一致。

本次改造也没有自动迁移或删除旧 Redis Key，没有自动清理死信 Stream，并且没有删除 dirty/processing 兼容链路。生产 Key 迁移必须在停机窗口显式执行；死信由运维确认原因后处理；兼容链路保留到观察期验收通过后再单独删除。

## 2. 核心数据结构与关键设计

### 2.1 Redis Key 总览

| Key | 类型 | 关键字段或成员 | TTL / 保留策略 | 作用 |
| --- | --- | --- | --- | --- |
| `mylab:blog:visitor:v2:{visitorHash}` | Hash | `created_at`、`last_seen_at`、`visit`、`view:*`、`like:*` | 默认 24 小时滑动过期 | 保存单个匿名凭证的访问、浏览去重和点赞状态 |
| `mylab:blog:engagement:{postKey}` | Hash | `view_count`、`like_count` | 无固定 TTL | 单篇 MyLab 内容的实时绝对计数 |
| `mylab:blog:site:metrics` | Hash | `visit_count`、`total_view_count`、`total_like_count` | 无固定 TTL | 全站实时绝对计数 |
| `mylab:blog:daily:{yyyy-MM-dd}` | Hash | `visit_count`、`view_count`、`like_count` | 120 天 | 按 Asia/Shanghai 日期保存实时聚合 |
| `mylab:blog:engagement:index:published-posts` | Set | 已发布且启用的 `post_key` | 24 小时，可重建 | 互动写入前快速校验内容可见性，未命中回源 PostgreSQL |
| `mylab:blog:stream:engagement:v1` | Stream | 聚合脏通知 | 默认保留 72 小时，按安全水位裁剪 | 触发异步快照落库 |
| `mylab:blog:stream:engagement:dlq:v1` | Stream | 原消息、原因、失败时间 | 不自动过期 | 隔离无法解析的内部消息 |
| `mylab:blog:lock:engagement-persistence` | String | 随机 UUID 租约令牌 | 默认 30 秒 | 保证同一时刻只有一个快照批次执行 |
| `mylab:blog:dirty:stats` / `processing:stats` | Set | `post_key` | 无固定 TTL | Stream 观察期的文章维度回滚链路 |
| `mylab:blog:dirty:site` / `processing:site` | Set | 固定成员 `site` | 无固定 TTL | Stream 观察期的站点维度回滚链路 |
| `mylab:blog:dirty:daily` / `processing:daily` | Set | `yyyy-MM-dd` | 无固定 TTL | Stream 观察期的按日维度回滚链路 |

`RedisKeyPrefix` 统一定义以下前缀，业务代码不得再创建并列顶层：

~~~text
mylab:
├── auth:
├── blog:
│   ├── content:
│   ├── engagement:
│   ├── visitor:
│   ├── site:
│   ├── daily:
│   ├── stream:
│   ├── dirty: / processing:
│   └── lock:
└── rate:
~~~

### 2.2 Visitor v2 Hash

浏览器保存 32 字节随机数经 Base64 URL 编码后的 43 字符凭证，Cookie 名为 `myblog_visitor`。Redis Key 不保存凭证原文，只使用 `ENGAGEMENT_HASH_SECRET` 计算的 HMAC-SHA256 十六进制摘要。

| Hash 字段 | 含义 | 写入方式 |
| --- | --- | --- |
| `created_at` | v2 凭证建立时的毫秒时间戳 | 创建身份时写入一次 |
| `last_seen_at` | 最近一次有效页面浏览、点赞或取消点赞时间 | 每次有效互动更新 |
| `visit` | 该凭证已经累计过一次访问量 | `HSETNX` |
| `view:home` | 该凭证已经浏览首页 | `HSETNX` |
| `view:mylab` | 该凭证已经浏览 MyLab 列表 | `HSETNX` |
| `view:post:{postKey}` | 该凭证已经浏览指定详情 | `HSETNX` |
| `like:{postKey}` | 该凭证当前已点赞指定内容 | 点赞 `HSETNX`，取消点赞 `HDEL` |

整个 Hash 使用 `VISITOR_IDENTITY_TTL`，默认 24 小时滑动过期。公开内容 GET 和统计摘要 GET 不刷新凭证；页面浏览、点赞和取消点赞成功后刷新 Redis TTL，并重新发送具有相同 Max-Age 的 Cookie。

升级前，访客身份是 72 小时固定 TTL 的 `blog:visitor:{visitorHash}` String，访问、浏览去重和点赞分别保存在 `blog:site:session:*`、`blog:view:dedupe:*`、`blog:visitor:likes:*`。新代码不再读写这些结构；即使运维曾把它们迁到 `mylab:blog:*` 下，也不会再被新版本使用。

旧 Visitor、Session、View 和 Likes Key 不自动迁移或删除，停止写入后按原 TTL 自然过期。旧 Cookie 对应的 v2 Hash 不存在时会签发新凭证。

### 2.3 Stream 消息协议

主 Stream 的 Consumer Group 固定为 `engagement-persistence-v1`，消费者名为 `<HOSTNAME>:<随机 8 位>`。消息只表达哪些聚合维度需要刷新：

| 字段 | 允许值 | 说明 |
| --- | --- | --- |
| `event_type` | `VIEW`、`LIKE`、`UNLIKE`、`VISIT` | 用于协议校验和排障，不直接作为数据库增量 |
| `post_key` | 合法 `post_key` 或空字符串 | `post_dirty=1` 时必填 |
| `stat_date` | `yyyy-MM-dd` 或空字符串 | `daily_dirty=1` 时必填，日期按 Asia/Shanghai |
| `post_dirty` | `0` / `1` | 是否读取并覆盖文章聚合 |
| `site_dirty` | `0` / `1` | 是否读取并覆盖站点聚合 |
| `daily_dirty` | `0` / `1` | 是否读取并覆盖当日聚合 |

事件与维度组合必须严格匹配：

| `event_type` | `post_dirty` | `site_dirty` | `daily_dirty` | 说明 |
| --- | ---: | ---: | ---: | --- |
| `VIEW` | 1 | 1 | 1 | 详情页首次浏览 |
| `LIKE` | 1 | 1 | 1 | 首次点赞；如需补记浏览，同一绝对值快照一并覆盖 |
| `UNLIKE` | 1 | 1 | 0 | 取消点赞；按日点赞统计不回减 |
| `VISIT` | 0 | 1 | 1 | 首页/MyLab 浏览，或只需刷新站点和当日维度 |

示例：

~~~text
XADD mylab:blog:stream:engagement:v1 * \
  event_type VIEW \
  post_key first-post \
  stat_date 2026-09-15 \
  post_dirty 1 \
  site_dirty 1 \
  daily_dirty 1
~~~

消息中不允许出现 visitor token、visitor hash、IP、User-Agent 或 Cookie。死信消息仅保存 `original_id`、`reason`、`failed_at` 和原字段映射文本。

### 2.4 PostgreSQL 快照

| 表 | 唯一维度 | 覆盖字段 |
| --- | --- | --- |
| `mylab_engagement_stats` | `post_key` | `view_count`、`like_count` |
| `site_traffic_stats` | 固定 `id=1` | `visit_count`、`total_view_count`、`total_like_count` |
| `site_daily_stats` | `stat_date` | `visit_count`、`view_count`、`like_count` |

`JdbcEngagementStatsRepository.saveSnapshot(...)` 使用 PostgreSQL 事务包住三类 Upsert。所有 `ON CONFLICT DO UPDATE` 都用 Redis 当前绝对值覆盖旧值，不做增量累加，因此相同 Stream 消息重复处理不会把计数重复增加。

### 2.5 为什么使用“脏通知 + 绝对值”

Redis Stream 提供至少一次处理语义。若消息直接携带增量，数据库提交成功但 `XACK` 响应丢失时，重放会造成重复累加。当前设计让消息只指出维度，消费者每次读取 Redis 最新值并整体覆盖 PostgreSQL：

- 同一消息重放是幂等的。
- 同一文章短时间产生多条消息时可在批次内合并为一次数据库写入。
- PostgreSQL 可以落后于 Redis，但不会因重放产生高于 Redis 的重复计数。
- Stream 丢失仍可能造成快照未及时刷新，因此 Redis AOF、备份和 Pending 监控仍是生产必需项。

## 3. 代码改动

以下清单只覆盖 Redis Stream、Visitor v2、互动 API、Redis 命名空间及其发布配套。提交中其他独立的公开内容投影调整不属于本文主题。

### 3.1 后端应用层

#### `backend-java/src/main/java/com/myblog/application/model/dto/EngagementDtos.java`

- 新增 `PageType`、`PageViewRequest`、`PageViewResult`，固化统一页面浏览请求与响应契约。
- 修改互动 DTO 注释，明确站点统计、业务时区和访客身份边界。

#### `backend-java/src/main/java/com/myblog/application/port/EngagementEventStream.java`

- 新增 Stream 端口，定义消费组初始化、新消息读取、Pending 接管、确认、死信、裁剪和状态读取能力，使应用服务不依赖 Spring Data Redis。

#### `backend-java/src/main/java/com/myblog/application/port/EngagementPersistenceLease.java`

- 新增落库租约端口，隔离分布式锁的获取与所有者安全释放。

#### `backend-java/src/main/java/com/myblog/application/port/EngagementStore.java`

- 替换独立 `registerView/registerVisit` 为 `registerPageView`，由统一页面类型驱动访问和浏览计数。
- 修改 `unlike` 接收业务日期，以便取消点赞时补记首次访问和浏览。

#### `backend-java/src/main/java/com/myblog/application/service/engagement/EngagementPersistenceService.java`

- 新增 Stream 消费核心服务，负责租约、Pending 优先、新消息读取、协议校验、维度合并、绝对值快照、事务成功后确认和 DLQ 隔离。

单批消费流程：

1. 使用 `SET NX PX` 获取单活租约；未获得租约时结束本轮。
2. 在租约内幂等确认 Consumer Group 存在。
3. 优先执行 `XPENDING + XCLAIM` 接管超过 `claim-idle` 的 Pending。
4. 没有可接管消息时执行 `XREADGROUP`，单批最多读取配置数量。
5. 校验事件类型、dirty 标记、`post_key`、日期和维度组合；非法消息先写 DLQ，再确认原消息。
6. 合并文章、站点和日期维度，从 Redis 读取最新绝对值。
7. 在一个 PostgreSQL 事务中 Upsert 文章、站点和按日快照。
8. 在事务成功返回后执行 `XACK`。
9. 在 `finally` 中按所有者令牌释放租约；释放失败只告警，等待 TTL 自动释放。

跨系统故障语义：

1. Redis Lua 失败时，计数与 `XADD` 同时不生效，公开写接口返回 503。
2. Redis Lua 成功但 HTTP 回包丢失时，计数和消息已经存在；同一 Cookie 重试由 `HSETNX` 保持幂等。
3. PostgreSQL 事务失败时不执行 `XACK`，消息留在 Pending。
4. PostgreSQL 提交成功但 `XACK` 失败时消息可能重放，绝对值覆盖不会重复累加。
5. DLQ 写入失败时不确认非法消息，后续轮次继续重试。
6. Redis 数据永久丢失时只能从 PostgreSQL 恢复最近快照，未落库变化和访客级状态无法恢复，因此生产必须启用 AOF、持久卷与备份。

#### `backend-java/src/main/java/com/myblog/application/service/engagement/EngagementService.java`

- 新增统一页面类型和 `post_key` 组合校验。
- 修改详情浏览、点赞和取消点赞在写 Redis 前复用已发布内容索引校验。
- 删除独立访问登记业务入口，避免访问量和页面浏览量分两次请求产生竞态。

统一页面浏览校验流程：

1. 校验请求体存在，否则返回参数校验错误。
2. 解析 `page_type`，只接受 `home`、`mylab`、`mylab_detail`。
3. 校验详情必须携带格式合法的 `post_key`，非详情不得携带该字段，空字符串也视为非法携带。
4. 查询发布索引；索引未命中时回源 PostgreSQL，并在确认已发布后补写索引。
5. 调用 `EngagementStore.registerPageView(...)`，把访问、浏览和 Stream 通知的原子性委托给 Redis 适配器。

#### `backend-java/src/main/java/com/myblog/application/service/engagement/VisitorIdentityService.java`

- 修改访客配置来源为 `VisitorProperties`。
- 修改身份复用语义为“Redis 中存在 v2 Hash 才复用”，旧 Cookie 自动轮换。
- 新增 `identityTtl()`，供控制器统一设置 Cookie Max-Age。

### 3.2 后端公共配置与装配

#### `backend-java/src/main/java/com/myblog/common/constant/RedisKeyPrefix.java`

- 新增统一 Redis 根前缀及 `AUTH/BLOG/CONTENT/RATE` 子前缀，消除多个业务顶层并存。

#### `backend-java/src/main/java/com/myblog/common/properties/EngagementStreamProperties.java`

- 新增 Stream 开关、批量、阻塞、接管、保留、租约、轮询和裁剪配置绑定。

#### `backend-java/src/main/java/com/myblog/common/properties/VisitorProperties.java`

- 新增访客 TTL 与 Cookie Secure 配置绑定，并拒绝零值或负值 TTL。

#### `backend-java/src/main/java/com/myblog/ApplicationLoader.java`

- 修改配置属性注册，启用 `EngagementStreamProperties` 和 `VisitorProperties`。

#### `backend-java/src/main/java/com/myblog/starter/config/SecurityConfig.java`

- 替换公开写接口白名单，开放新的页面浏览接口并移除两个旧接口。

### 3.3 后端控制器与基础设施

#### `backend-java/src/main/java/com/myblog/controller/PublicEngagementController.java`

- 新增统一页面浏览接口。
- 删除旧访问和详情浏览端点。
- 修改页面浏览、点赞、取消点赞成功响应统一刷新 24 小时 HttpOnly、SameSite=Lax Cookie，并保持 `Cache-Control: no-store`。

变更后的请求：

~~~json
{ "page_type": "home" }
~~~

~~~json
{ "page_type": "mylab" }
~~~

~~~json
{ "page_type": "mylab_detail", "post_key": "first-post" }
~~~

详情响应：

~~~json
{
  "code": 0,
  "message": "成功",
  "data": {
    "page_type": "mylab_detail",
    "post_key": "first-post",
    "view_count": 12,
    "like_count": 3,
    "liked": false,
    "site_statistics": {
      "visit_count": 100,
      "total_view_count": 360,
      "total_like_count": 25,
      "snapshot_at": "2026-09-16T10:30:00+08:00"
    }
  },
  "error": null
}
~~~

首页和 MyLab 列表响应保留 `page_type` 与 `site_statistics`；文章字段为 `null`，并由全局 non-null JSON 配置省略。点赞和取消点赞接口路径保持不变。

#### `backend-java/src/main/java/com/myblog/infrastructure/engagement/RedisEngagementStore.java`

- 替换旧 Visitor String、会话 Key、浏览去重 Key 和点赞 Set 为单个 Visitor v2 Hash。
- 替换页面浏览、点赞、取消点赞 Lua，使用 `HSETNX/HDEL` 原子实现访问、目标浏览和点赞幂等。
- 新增有效计数变化时的原子 `XADD`，并继续写 dirty Set 供回滚。
- 修改全部互动 Key 使用 `mylab:blog:*` 命名空间。
- 修改访客 TTL 为配置化的 24 小时滑动窗口。

页面浏览 Lua 原子步骤：

1. 校验 Visitor v2 Hash 存在。
2. 在同一 Lua 中更新 `last_seen_at` 并刷新整个 Hash TTL。
3. 使用 `HSETNX visit` 判定首次访问。
4. 使用目标 `view:*` 字段判定首次浏览。
5. 首次访问时增加站点和当日访问量。
6. 首次浏览时增加站点和当日浏览量；详情额外增加文章浏览量。
7. 写入文章、站点和日期 dirty 标记。
8. 在 Stream 开启且计数发生变化时执行 `XADD`。
9. 返回文章实时计数、当前点赞状态和站点实时计数。

点赞与取消点赞语义：

- 修改点赞行为，尚未浏览详情时在一个 Lua 中同时补记访问、详情浏览和点赞，保证新口径下浏览量不小于访问量。
- 修改取消点赞行为，尚未浏览详情时先补记访问与浏览，并按实际变化分别产生 `VIEW` 与 `UNLIKE` 通知。
- 修改按日点赞口径，将 `like_count` 定义为当天新增点赞次数；取消点赞不回减，所以 `UNLIKE` 使用 `daily_dirty=0`。
- 修改重复操作行为，重复浏览、重复点赞以及未点赞时取消点赞在没有其他首次变化时只续期，不增加计数也不产生通知。

同一访客并发竞态：

1. 浏览先执行、点赞后执行：浏览写入访问和详情标记，点赞只新增点赞，最终各计一次。
2. 点赞先执行、浏览后执行：点赞同时写入访问、详情和点赞标记，后续浏览只续期，最终仍各计一次。
3. 点赞先执行、取消点赞后执行：最终点赞字段被删除，当前点赞数回到原值。
4. 取消点赞先执行、点赞后执行：取消点赞先补记访问和浏览但无点赞可删，后续点赞使最终状态为已点赞。
5. 两个相同请求并发：Redis 串行执行 Lua，只有第一个 `HSETNX` 返回 1，第二个只读取当前值并续期。

#### `backend-java/src/main/java/com/myblog/infrastructure/engagement/RedisEngagementEventStream.java`

- 新增 Redis Stream 适配器，实现消费组、读取、Pending 接管、确认、DLQ 和安全裁剪。

Pending 接管与裁剪：

1. 原消费者读取消息后，消息进入 Pending。
2. 原消费者在 `XACK` 前退出时，消息保持未确认。
3. 新消费者等待消息空闲时间达到 `ENGAGEMENT_STREAM_CLAIM_IDLE` 后执行 `XCLAIM`。
4. 新消费者重新读取 Redis 最新绝对值并覆盖 PostgreSQL，事务成功后确认消息。
5. 定时裁剪使用 `XTRIM MINID`，水位取保留期截止 ID、消费组最后投递 ID和最老 Pending ID 三者最小值，因此不会删除未投递或未确认消息。

#### `backend-java/src/main/java/com/myblog/infrastructure/engagement/RedisEngagementPersistenceLease.java`

- 新增 `SET NX PX` 短租约和比较令牌释放 Lua，避免误删其他实例持有的租约。

#### `backend-java/src/main/java/com/myblog/infrastructure/engagement/EngagementStreamJob.java`

- 新增启动恢复、定时消费、安全裁剪和 Micrometer 指标。
- 新增启动失败不阻断、后续轮询重试机制。

#### `backend-java/src/main/java/com/myblog/infrastructure/engagement/EngagementSnapshotJob.java`

- 修改启用条件为 `ENGAGEMENT_STREAM_ENABLED=false`，保留旧 dirty 快照链路作为回滚实现。

#### `backend-java/src/main/java/com/myblog/infrastructure/cache/RedisPublishedPostCache.java`

- 修改已发布文章索引为 `mylab:blog:engagement:index:published-posts`，加入 `index` 层级，避免合法 `post_key=published-posts` 与文章计数 Hash 类型冲突。

#### `backend-java/src/main/java/com/myblog/infrastructure/cache/RedisDistributedLock.java`

- 修改公开内容缓存锁前缀为 `mylab:blog:content:*`。

#### `backend-java/src/main/java/com/myblog/infrastructure/cache/RedisPublicContentCache.java`

- 修改公开内容缓存 Key 统一归入 `mylab:blog:content:*`。

#### `backend-java/src/main/java/com/myblog/infrastructure/security/RedisSessionService.java`

- 修改管理会话 Key 为 `mylab:auth:*`，会话结构和认证行为不变。

#### `backend-java/src/main/java/com/myblog/starter/config/RateLimitFilter.java`

- 修改登录和全局限流 Key 为 `mylab:rate:*`，限流算法不变。

### 3.4 博客前台

#### `myblog/src/api/public.ts`

- 新增进程内访客写请求串行队列，避免首次页面浏览与点赞并发签发不同 Cookie。
- 新增 `postPageView(...)`，统一上报三类页面浏览。
- 删除 `postSiteVisit(...)` 和 `postContentView(...)` 对旧接口的调用。
- 修改点赞和取消点赞进入同一访客写队列。

访客写请求串行顺序：

1. 把页面浏览、点赞或取消点赞追加到同一个 Promise 队列。
2. 等待上一项无论成功或失败都完成后，再发送下一项。
3. 使用 `credentials: include` 接收并携带 HttpOnly Cookie。
4. 把队列尾转换为已完成的 `Promise<void>`，确保单次失败不会阻塞后续写入。
5. 不随组件卸载取消已经发出的统计请求，只阻止旧响应回写已卸载页面。

#### `myblog/src/types/engagement.ts`

- 新增 `PageType` 和 `PageViewResult` 类型，与后端 snake_case 响应保持一致。

#### `myblog/src/components/SiteShell.tsx`

- 修改路由统计：首页上报 `home`，MyLab 列表上报 `mylab`，其他路由不在壳组件重复上报。

#### `myblog/src/pages/MyLabPostView.tsx`

- 修改详情页按当前 `post_key` 上报 `mylab_detail`。
- 删除统计请求的卸载取消，只阻止旧请求回写已卸载页面状态。

#### `myblog/src/stores/engagementStore.ts`

- 替换详情浏览 API，并校验详情响应必须含文章计数和点赞状态。
- 删除“先初始化访问再互动”的两步时序，改由单次 Lua 自动补记。

#### `myblog/src/stores/siteStatisticsStore.ts`

- 替换独立访问登记为首页/MyLab 页面浏览登记。
- 新增按页面类型合并进行中请求，并保留写失败后读取统计快照的降级。

### 3.5 配置与文档

#### `backend-java/src/main/resources/application.yml`

- 新增八项 Stream 配置和 `VISITOR_IDENTITY_TTL`，默认启用 Stream、访客 TTL 为 24 小时。

变更后的配置示例：

~~~yaml
app:
  engagement-stream:
    enabled: true
    batch-size: 500
    block-timeout: 2s
    claim-idle: 30s
    retention: 72h
    lock-ttl: 30s
    poll-delay: 1s
    trim-interval: 1h
  visitor-identity-ttl: 24h
~~~

~~~env
ENGAGEMENT_STREAM_ENABLED=true
ENGAGEMENT_STREAM_BATCH_SIZE=500
ENGAGEMENT_STREAM_BLOCK_TIMEOUT=2s
ENGAGEMENT_STREAM_CLAIM_IDLE=30s
ENGAGEMENT_STREAM_RETENTION=72h
ENGAGEMENT_STREAM_LOCK_TTL=30s
ENGAGEMENT_STREAM_POLL_DELAY=1s
ENGAGEMENT_STREAM_TRIM_INTERVAL=1h
VISITOR_IDENTITY_TTL=24h
ENGAGEMENT_HASH_SECRET=change-this-engagement-hash-secret
~~~

`ENGAGEMENT_HASH_SECRET` 没有安全默认值，生产必须替换为独立高熵随机值，不得复用数据库密码、管理员密码或 OSS 密钥。所有 backend 实例必须使用相同的 Stream 开关；锁 TTL 必须大于阻塞读取与数据库批次 P99，claim idle 应大于正常批次耗时，retention 应覆盖最长故障排查窗口。

#### `.env.example`

- 新增本地 Stream 和 Visitor TTL 环境变量示例，不包含真实密钥。

#### `deploy/.env.example`

- 新增生产 Stream 和 Visitor TTL 环境变量示例。

#### `docker-compose.yml`

- 新增本地 backend 容器的 Stream 和 Visitor TTL 环境变量透传。

#### `deploy/docker-compose.yml`

- 新增生产 backend 容器的 Stream 和 Visitor TTL 环境变量透传。

#### `README.md`

- 修改互动统计、访客口径和 Redis Stream 能力说明。

#### `AGENTS.md`

- 修改项目架构约定，记录 Stream 落库、Visitor v2 和统一 Redis 命名空间。

#### `docs/API接口文档.md`

- 替换旧访问/浏览接口为统一页面浏览接口，并补充请求、响应和错误语义。

#### `docs/错误码文档.md`

- 修改 `14001` 的触发范围和客户端处理说明。

#### `article/Redis Key命名空间说明.md`

- 新增 Redis 统一树、旧新前缀映射、Visitor v2 字段和迁移注意事项。

#### `article/Redis Stream互动统计落库改造说明.md`

- 替换原简版说明为当前开发说明，补齐边界、协议、流程、文件级改动、测试证据、发布和回滚检查。

## 4. 异常处理与故障语义

### 4.1 HTTP 错误码

| code | HTTP | 枚举 | 含义 |
| ---: | ---: | --- | --- |
| `10005` | 404 | `RESOURCE_NOT_FOUND` | 详情或点赞目标不存在、未发布或已停用 |
| `10007` | 422 | `VALIDATION_FAILED` | 页面类型与 `post_key` 组合不合法 |
| `10009` | 400 | `MALFORMED_REQUEST` | 请求体缺失或 JSON 格式错误 |
| `14001` | 503 | `ENGAGEMENT_UNAVAILABLE` | Redis 无法创建/校验访客、执行互动 Lua 或读取实时统计 |
| `20002` | 500 | `DATABASE_ERROR` | Redis 降级读取与 PostgreSQL 同时失败，或数据库操作异常 |

Stream 消费异常不会直接返回给产生互动的访客。公开请求只要求 Redis 原子写成功；PostgreSQL 暂时不可用时，实时计数仍可成功返回。

### 4.2 故障矩阵

| 故障点 | 对公开请求的影响 | 消费与恢复行为 |
| --- | --- | --- |
| Redis 不可用 | 互动写返回 `14001`；统计查询回退 PG 快照 | 生产和消费暂停，Redis 恢复后调度重试 |
| PostgreSQL 不可用 | Redis 实时互动继续成功 | 批次失败且不 `XACK`，消息保留 Pending |
| 消费实例退出 | 无同步影响 | 超过 claim idle 后由其他消费者接管 |
| 租约被占用 | 无同步影响 | 本轮返回 0，下一轮继续竞争 |
| 租约释放失败 | 无同步影响 | 记录 WARN，等待 TTL 自动释放 |
| Consumer Group 初始化失败 | 应用仍可提供非消费功能 | 启动记录 WARN，后续消费轮次重试 |
| 消息格式非法 | 无同步影响 | 写 DLQ、确认原消息并继续处理合法消息 |
| Stream 裁剪失败 | 无同步影响 | 记录 WARN，下个裁剪周期重试 |

详情浏览、点赞和取消点赞在已发布索引未命中时仍需回源 PostgreSQL。因此“PostgreSQL 不可用时 Redis 实时互动继续成功”只适用于首页、MyLab 列表以及已命中发布索引的详情互动，不能理解为全部公开写路径完全不依赖数据库。

### 4.3 客户端区分原则

- 422/400 是调用契约错误，不应自动重试。
- 404 表示内容已不可互动，应停止该目标后续写入。
- `503 + 14001` 是临时依赖故障，可以提示稍后重试；不能误判为内容不存在。
- 页面浏览上报失败不阻断路由，站点数字尝试读取快照；点赞失败保留当前 UI 状态并提示失败。
- 所有互动响应使用 `no-store`，避免缓存旧计数或跨访客复用 `liked` 状态。

本次没有遗留或废弃错误码。Stream 消费异常发生在异步调度线程，不会把已经成功写入 Redis 的公开请求改写为失败响应。

## 5. 测试代码位置

### 5.1 后端单元测试

#### `backend-java/src/test/java/com/myblog/application/service/engagement/EngagementPersistenceServiceTest.java`

- 验证新消息按文章、站点和日期合并，读取 Redis 最新绝对值，并在数据库成功后确认。
- 验证超时 Pending 优先于新消息处理。
- 验证非法消息写入 DLQ 并确认，合法消息不被阻塞。
- 验证数据库失败时不确认消息，消息保留等待重试。
- 验证 Stream 关闭、租约竞争、空批次、租约释放失败和安全裁剪边界。

#### `backend-java/src/test/java/com/myblog/application/service/engagement/EngagementServiceTest.java`

- 验证详情浏览通过统一 `registerPageView(...)` 委托存储，并校验内容发布状态。
- 验证首页浏览不查询文章发布状态。
- 验证非法页面类型、详情缺少 `post_key`、非详情携带 `post_key` 被拒绝。
- 验证发布索引命中时跳过数据库，未命中且数据库确认发布后补写索引。
- 删除原独立访问委托测试，因为 `registerVisit(...)` 已被统一页面浏览入口删除。
- 替换原 `registerView*` 测试为统一页面浏览行为测试，继续保护发布状态校验。

#### `backend-java/src/test/java/com/myblog/application/service/engagement/VisitorIdentityServiceTest.java`

- 验证缺少、格式错误、未知或过期 Cookie 时签发新身份。
- 验证 Visitor v2 Hash 存在时复用原 Cookie。
- 验证相同密钥与凭证产生稳定摘要，不同密钥产生不同摘要。
- 验证 Cookie Secure 和 24 小时 TTL 来自 `VisitorProperties`。

#### `backend-java/src/test/java/com/myblog/common/constant/RedisKeyPrefixTest.java`

- 验证 Redis 只有 `mylab:` 一个业务根前缀。
- 验证 `auth`、`blog`、`content`、`rate` 子前缀值稳定。

### 5.2 后端集成测试

#### `backend-java/src/test/java/com/myblog/PublicEngagementApiIT.java`

- 验证统一页面浏览、点赞、重复点赞和取消点赞完整流程。
- 验证 24 小时滑动凭证内一份凭证只累计一次访问，各页面目标分别去重浏览。
- 验证首页、MyLab 和多个详情的唯一目标之和构成总浏览量。
- 验证 TTL、`last_seen_at`、Cookie Max-Age 刷新和旧 Cookie 轮换。
- 验证页面浏览、点赞、取消点赞并发时不重复累计访问和详情浏览。
- 验证直接点赞自动补记访问与详情浏览。
- 验证 Stream 负载不包含 visitor 字段。
- 验证 Consumer Group 写入 PostgreSQL 绝对快照、清空 Pending 并接管超时消息。
- 验证新流量不产生旧 Session/View/Likes Key，且全部业务 Key 以 `mylab:` 开头。
- 验证页面类型与 `post_key` 组合错误返回 HTTP 422 和稳定业务码。

#### `backend-java/src/test/java/com/myblog/AbstractApiIntegrationTest.java`

- 验证集成环境使用 10ms 阻塞读取和接管阈值，并关闭后台高频轮询，避免测试与手动消费竞争。

#### `backend-java/src/test/java/com/myblog/AuthApiIT.java`

- 验证会话 Key 迁入 `mylab:auth:*` 后，登录、续期、退出和批量吊销行为不变。

### 5.3 前端验证范围

前台未新增独立测试文件，改动通过 TypeScript 构建和 ESLint 验证以下静态契约：

- 验证 `PageType` 只能为三种合法值。
- 验证统一接口响应字段与 Store 消费字段一致。
- 验证已删除的 `postSiteVisit/postContentView` 不再有引用。
- 验证详情浏览不再接收 `AbortSignal`，避免组件卸载取消已经计数的写请求。

## 6. 测试结果

### 6.1 后端完整质量门

执行命令：

~~~bash
cd backend-java
mvn verify
~~~

实际结果：

- Maven `BUILD SUCCESS`。
- 单元测试 289 个，失败 0、错误 0、跳过 0。
- Testcontainers 集成测试 43 个，失败 0、错误 0、跳过 0。
- Checkstyle 违规 0。
- SpotBugs 问题 0。
- ArchUnit 分层规则通过。
- JaCoCo 报告生成完成，应用服务覆盖率门禁通过。

已知但不阻断构建的工具告警：Maven Checkstyle 插件提示未知 `encoding` 参数；Mockito 提示未来 JDK 将限制动态挂载 Byte Buddy Agent；Spring 测试启动提示 Commons Logging 发现冲突；JaCoCo 分析第三方 JSqlParser 大方法时记录 `MethodTooLargeException`，未影响项目代码覆盖率门禁和最终构建结果。

### 6.2 互动接口定向验证

执行命令：

~~~bash
cd backend-java
mvn -Dit.test=PublicEngagementApiIT verify
~~~

实际结果：

- 单元测试 289 个全部通过。
- `PublicEngagementApiIT` 13 个集成测试全部通过。
- Stream 生产、隐私字段、PG 快照、Pending 接管、Visitor v2 去重和旧 Key 停写均通过。

### 6.3 博客前台 ESLint

执行：

~~~bash
cd myblog
npm run lint
~~~

实际结果：

- ESLint：通过，错误 0。

### 6.4 博客前台生产构建

执行：

~~~bash
cd myblog
npm run build
~~~

实际结果：

- TypeScript 类型检查：通过。
- Vite：8.2.2。
- 转换模块：340 个。
- 生产构建：成功。

## 7. 上线与回滚

### 7.1 配置项

| 环境变量 | 默认值 | 作用 | 生产约束 |
| --- | --- | --- | --- |
| `ENGAGEMENT_STREAM_ENABLED` | `true` | 开启 Stream 生产和消费；`false` 使用旧快照任务 | 所有 backend 实例必须一致 |
| `ENGAGEMENT_STREAM_BATCH_SIZE` | `500` | 单批最大消息数 | 必须为正数；过大将增加 Redis/PG 批次延迟 |
| `ENGAGEMENT_STREAM_BLOCK_TIMEOUT` | `2s` | 新消息阻塞读取时长 | 应小于租约 TTL |
| `ENGAGEMENT_STREAM_CLAIM_IDLE` | `30s` | Pending 可被接管的最小空闲时间 | 应大于正常批次耗时 |
| `ENGAGEMENT_STREAM_RETENTION` | `72h` | 已确认 Stream 历史保留窗口 | 应覆盖预期最长故障排查窗口 |
| `ENGAGEMENT_STREAM_LOCK_TTL` | `30s` | 单活消费租约 | 应大于阻塞读取与 PG 批次 P99；当前不会自动续租 |
| `ENGAGEMENT_STREAM_POLL_DELAY` | `1s` | 消费调度间隔 | 多实例时不宜设为 0 |
| `ENGAGEMENT_STREAM_TRIM_INTERVAL` | `1h` | 安全裁剪周期 | 只影响空间回收 |
| `VISITOR_IDENTITY_TTL` | `24h` | Visitor Hash 和 Cookie 滑动窗口 | 必须大于 0 |
| `ENGAGEMENT_HASH_SECRET` | 无默认值 | HMAC 匿名访客凭证 | 必填；使用高熵独立密钥，不得提交仓库 |

配置已同步到应用配置、本地 Compose、生产 Compose 和两份环境变量模板。模板中的 `change-this-*` 仅是占位符，不能用于生产。

### 7.2 版本绑定与行为影响

- backend 与 `myblog` 前台必须同版本发布，因为两个旧上报接口已经删除。
- 上线后旧 Visitor Cookie 因找不到对应 v2 Hash 会自动轮换；首个有效请求会按新口径计入访问和页面浏览。
- 历史 PostgreSQL 统计不清零，所以旧数据中“访问量大于浏览量”的关系可能暂时保留；新流量遵循新口径。
- Redis Key 前缀发生不兼容变化。新旧 backend 禁止并行，否则会向两套命名空间写入。
- 如果不迁移 Redis 聚合，启动时只能从 PostgreSQL 最近快照恢复，最后一次快照之后的实时增量可能丢失。

### 7.3 上线步骤

1. 备份 PostgreSQL 和 Redis，确认 Redis AOF/持久卷工作正常。
2. 在旧版本仍运行时确认旧快照任务无持续错误；如旧环境已有 Stream，确认 Pending 为 0。
3. 停止全部 backend，确保迁移期间没有新写入。
4. 按 `article/Redis Key命名空间说明.md` 对确需保留的 Key 做显式旧新前缀迁移；不要让应用启动脚本隐式扫描删除生产数据。
5. 更新 backend 与博客前台为同一版本，保持所有实例 `ENGAGEMENT_STREAM_ENABLED=true`。
6. 启动 Redis、PostgreSQL 和 backend，确认消费组自动创建且启动恢复完成。
7. 执行一次首页、MyLab、详情浏览和点赞验收，核对 Cookie、Visitor Hash、聚合 Hash、Stream、Pending 和 PostgreSQL。
8. 观察至少一个完整业务高峰，重点关注 Pending、消费失败、批次耗时、DLQ 和 Redis 内存。
9. 观察期内保留 dirty/processing Set，不提前清理回滚数据。

### 7.4 回滚步骤

优先使用功能开关回滚落库消费者：

1. 停止全部 backend，禁止部分实例继续生产或消费 Stream。
2. 设置 `ENGAGEMENT_STREAM_ENABLED=false`。
3. 重新启动全部 backend，确认 `EngagementSnapshotJob` 启动并消费 dirty/processing Set。
4. 保留主 Stream 和 DLQ 供排障，不执行删除。
5. 核对 PostgreSQL 三类绝对快照继续更新。

如果需要回滚整个应用镜像，还必须：

1. 同时回滚博客前台，否则前后端接口版本不匹配。
2. 评估旧镜像读取的是旧 Redis 前缀还是 `mylab:` 前缀；必要时在停机状态反向迁移 Key。
3. 接受 Visitor 身份再次轮换以及回滚窗口内可能发生的重复访客统计。

回滚不需要数据库迁移，因为本次没有结构变更。

### 7.5 回滚观察期

观察期内必须保留以下对象，避免发现问题后失去回滚入口：

- `mylab:blog:dirty:*` 与 `mylab:blog:processing:*`。
- `ENGAGEMENT_STREAM_ENABLED` 与旧 `EngagementSnapshotJob`。
- 主 Stream、Consumer Group 和 DLQ。
- Redis 前缀迁移清单、备份和回滚前镜像 tag。

连续一个完整业务高峰满足 Pending 不持续积压、消费失败不持续增长、DLQ 无未知消息、数据库快照与 Redis 抽样一致，并在预发布环境完成开关回滚演练后，才能另行提交删除 dirty/processing 兼容链路。观察期结束前不得提前清理这些对象。

### 7.6 监控指标

| Micrometer 名称 | 含义 | 关注点 |
| --- | --- | --- |
| `engagement.stream.messages.processed` | 成功持久化并确认的有效消息累计数 | 有流量时应持续增长 |
| `engagement.stream.consume.failures` | 消费批次失败累计数 | 持续增长需立即排查 Redis/PG |
| `engagement.stream.batch.duration` | 批次耗时 | P99 应明显低于租约 TTL |
| `engagement.stream.last.success.epoch.millis` | 最近一次非空批次成功时间 | 有流量但长期不更新表示消费停滞 |
| `engagement.stream.length` | 主 Stream 长度 | 结合保留期观察是否异常增长 |
| `engagement.stream.pending` | 消费组 Pending 数 | 短暂非零正常，持续增长异常 |

## 8. 上线检查（验收清单）

### 8.1 发布前检查

1. backend 与博客前台镜像来自同一提交。
1. 所有 backend 实例的 `ENGAGEMENT_STREAM_ENABLED` 一致。
1. `ENGAGEMENT_HASH_SECRET` 已配置为独立高熵值，未使用模板占位符。
1. Redis AOF、持久卷和备份已确认。
1. Redis 旧新前缀迁移策略已确定，迁移窗口禁止新旧 backend 并行。
1. `ENGAGEMENT_STREAM_LOCK_TTL` 大于阻塞读取和数据库批次 P99。
1. 已记录回滚前镜像 tag 和 Redis Key 前缀状态。

### 8.2 正向功能验收

以下命令以 Bash 为例，`first-post` 必须替换为实际已发布且启用的 `post_key`：

~~~bash
API_BASE=http://localhost/api/v1
POST_KEY=first-post
COOKIE_FILE=/tmp/mylab-visitor.cookie

curl -i -c "$COOKIE_FILE" -b "$COOKIE_FILE" \
  -H 'Content-Type: application/json' \
  -d '{"page_type":"home"}' \
  "$API_BASE/public/analytics/page-views"

curl -i -c "$COOKIE_FILE" -b "$COOKIE_FILE" \
  -H 'Content-Type: application/json' \
  -d '{"page_type":"mylab"}' \
  "$API_BASE/public/analytics/page-views"

curl -i -c "$COOKIE_FILE" -b "$COOKIE_FILE" \
  -H 'Content-Type: application/json' \
  -d "{\"page_type\":\"mylab_detail\",\"post_key\":\"$POST_KEY\"}" \
  "$API_BASE/public/analytics/page-views"

curl -i -c "$COOKIE_FILE" -b "$COOKIE_FILE" \
  -X PUT "$API_BASE/public/mylab/$POST_KEY/likes"
~~~

1. `Set-Cookie` 包含 `myblog_visitor`、`HttpOnly`、`SameSite=Lax` 和 `Max-Age=86400`；HTTPS 环境还必须包含 `Secure`。
1. 相同 Cookie 重复请求同一页面时，访问量和该目标浏览量不增加。
1. 同一 Cookie 首次访问首页、MyLab 和不同详情时，总浏览量逐目标增加，访问量仍只增加一次。
1. 直接点赞尚未浏览的详情时，同时增加一次访问、一次详情浏览和一次点赞。
1. 取消点赞删除 Visitor Hash 中对应 `like:{postKey}`，文章和站点当前点赞数不为负。

### 8.3 Redis 与 Stream 验收

~~~bash
docker compose exec -T redis redis-cli --scan --pattern 'mylab:*'
docker compose exec -T redis redis-cli XLEN mylab:blog:stream:engagement:v1
docker compose exec -T redis redis-cli XINFO GROUPS mylab:blog:stream:engagement:v1
docker compose exec -T redis redis-cli XPENDING mylab:blog:stream:engagement:v1 engagement-persistence-v1
docker compose exec -T redis redis-cli XRANGE mylab:blog:stream:engagement:v1 - + COUNT 10
docker compose exec -T redis redis-cli XRANGE mylab:blog:stream:engagement:dlq:v1 - + COUNT 20
~~~

1. 新产生业务 Key 全部位于 `mylab:` 下。
1. Consumer Group `engagement-persistence-v1` 存在。
1. 正常消费后 Pending 回到 0 或短暂低位。
1. 主 Stream 消息不包含 visitor、Cookie、IP 或 User-Agent 字段。
1. 正常流量不会产生旧 Session/View/Likes Key。
1. DLQ 为空；若非空，已逐条记录原因并停止自动清理。

### 8.4 PostgreSQL 验收

~~~sql
SELECT post_key, view_count, like_count, updated_at
FROM mylab_engagement_stats
WHERE post_key = 'first-post';

SELECT visit_count, total_view_count, total_like_count, updated_at
FROM site_traffic_stats
WHERE id = 1;

SELECT stat_date, visit_count, view_count, like_count, updated_at
FROM site_daily_stats
ORDER BY stat_date DESC
LIMIT 3;
~~~

1. Redis 产生有效通知后，三类 PostgreSQL 快照在消费周期内更新。
1. 重复消费或 Pending 接管不会重复增加数据库计数。
1. 文章、站点、当日三个维度在一次快照事务中同时成功或同时回滚。

### 8.5 反向与故障语义验收

~~~bash
curl -i -H 'Content-Type: application/json' \
  -d '{"page_type":"unknown"}' \
  http://localhost/api/v1/public/analytics/page-views

curl -i -H 'Content-Type: application/json' \
  -d '{"page_type":"home","post_key":"first-post"}' \
  http://localhost/api/v1/public/analytics/page-views

curl -i -H 'Content-Type: application/json' \
  -H 'Cookie: myblog_visitor=invalid-cookie' \
  -d '{"page_type":"home"}' \
  http://localhost/api/v1/public/analytics/page-views
~~~

1. 两个非法请求均返回 HTTP 422、业务码 `10007`，且不创建有效浏览计数。
1. 使用无效访客 Cookie 请求首页，确认服务端签发新 Cookie 并正常计数，不把匿名凭证错误转换为管理端 401。
1. 停止 PostgreSQL 后，首页浏览仍能写入 Redis，Stream 消息进入 Pending 且不被确认；恢复 PostgreSQL 后 Pending 可自动清空。
1. 停止 Redis 后，互动写返回 HTTP 503、业务码 `14001`；公开内容仍可从 PostgreSQL 加载，统计摘要可回退最近快照。
1. 人工构造的非法 Stream 消息进入 DLQ，原消息被确认，后续合法消息继续处理。该项只允许在测试环境执行。
1. 消费实例在读取后退出时，等待超过 `ENGAGEMENT_STREAM_CLAIM_IDLE`，新实例能接管并完成落库。
1. Stream 历史裁剪后，未投递和 Pending 消息仍存在。

### 8.6 安全验收

~~~bash
VISITOR_KEY=$(docker compose exec -T redis redis-cli --raw --scan --pattern 'mylab:blog:visitor:v2:*' | head -n 1)
docker compose exec -T redis redis-cli TYPE "$VISITOR_KEY"
docker compose exec -T redis redis-cli HKEYS "$VISITOR_KEY"
docker compose exec -T redis redis-cli XRANGE mylab:blog:stream:engagement:v1 - + COUNT 10
docker compose exec -T redis redis-cli --scan --pattern 'blog:*'
~~~

1. 检查 Cookie，确认 `myblog_visitor` 包含 HttpOnly，HTTPS 环境包含 Secure，前端 JavaScript 无法读取原文。
1. 检查 `VISITOR_KEY`，确认动态段是 64 位 HMAC-SHA256 十六进制摘要，不是客户端 43 字符随机凭证。
1. 检查 Visitor Hash 字段，确认不包含 IP、User-Agent、用户名、角色或管理会话 Token。
1. 检查主 Stream，确认不包含 visitor token、visitor hash、Cookie、IP 或 User-Agent。
1. 检查 PostgreSQL 三张聚合表，确认不存在访客身份或点赞关系明细。
1. 扫描旧 `blog:*` 前缀，确认新流量不会重新创建旧 Visitor、Session、View 或 Likes Key。
1. 检查环境变量，确认 `ENGAGEMENT_HASH_SECRET` 已替换占位符，且不与数据库、管理员或 OSS 凭据复用。

### 8.7 观察期退出条件

1. 连续一个完整业务高峰无消费失败持续增长。
1. Pending 未持续积压，批次 P99 低于租约 TTL。
1. DLQ 无未知格式消息。
1. PostgreSQL 快照与 Redis 绝对值抽样一致。
1. 回滚开关在预发布环境演练成功。
1. 达成以上条件后，再单独提交删除旧 dirty/processing 兼容链路的改造，禁止在本次发布中提前删除。
