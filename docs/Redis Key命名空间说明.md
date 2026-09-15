# Redis Key 命名空间说明

## 1. 统一结构

项目业务 Key 统一使用 `mylab:` 作为唯一顶层前缀：

```text
mylab
├── auth       # 管理后台会话与用户会话反向索引
├── blog
│   ├── content    # 公开内容缓存与缓存重建锁
│   ├── engagement # 文章浏览/点赞聚合与已发布文章索引
│   ├── visitor    # 访客访问、页面浏览与点赞状态
│   ├── site       # 站点聚合
│   ├── daily      # 按日统计
│   ├── stream     # 互动落库 Stream 与死信
│   ├── dirty / processing
│   └── lock
└── rate       # 登录和全局 IP 限流
```

## 2. 旧新映射

| 旧前缀 | 新前缀 | 行为变化 |
| --- | --- | --- |
| `auth:*` | `mylab:auth:*` | 仅增加统一顶层，数据结构与 TTL 不变 |
| `rate:*` | `mylab:rate:*` | 仅增加统一顶层，限流算法不变 |
| `blog:*` | `mylab:blog:*` | 互动、访客、统计、Stream 和锁统一归入 blog |
| `myblog:content:*` | `mylab:blog:content:*` | 原 myblog 前缀并入 blog，content 成为 blog 子项 |

前缀由 `RedisKeyPrefix` 统一定义，业务代码不得自行创建新的并列顶层前缀。

## 3. Visitor v2 Hash

每份匿名访客凭证对应一个 `mylab:blog:visitor:v2:{visitorHash}` Hash。浏览器持有随机凭证原文，Redis Key 只使用经 `ENGAGEMENT_HASH_SECRET` HMAC 后的摘要。Hash 字段如下：

| 字段 | 含义 |
| --- | --- |
| `created_at` | 凭证首次在 v2 结构中建立的毫秒时间戳 |
| `last_seen_at` | 最近一次有效页面浏览、点赞或取消点赞的毫秒时间戳 |
| `visit` | 已为该凭证累计过一次访问量 |
| `view:home` | 已浏览首页 |
| `view:mylab` | 已浏览 MyLab 列表 |
| `view:post:{postKey}` | 已浏览指定 MyLab 详情 |
| `like:{postKey}` | 当前凭证已点赞指定 MyLab 内容 |

整个 Hash 默认采用 24 小时滑动 TTL，由 `VISITOR_IDENTITY_TTL` 配置。有效页面浏览、点赞和取消点赞会更新 `last_seen_at` 并整体续期；公开内容查询和统计摘要查询不会续期。字段通过 Lua `HSETNX` 原子判定首次访问与首次浏览，避免同一凭证并发请求造成重复计数。

新代码不再读写以下旧结构：

- `mylab:blog:site:session:*`
- `mylab:blog:view:dedupe:*`
- `mylab:blog:visitor:likes:*`

旧 Visitor、Session、View 和 Likes Key 不自动迁移或删除，停止写入后按原 TTL 自然过期。旧 Cookie 对应的 v2 Hash 不存在时会轮换新凭证。

## 4. 发布注意事项

代码不会在应用启动时自动删除或重命名旧 Key，避免未确认范围的生产 Redis 数据被隐式修改。因此首次发布该版本前需要选择以下方式之一：

- 本地或测试环境不需要保留数据时，停止 backend 后清理 Redis，再启动新版本。
- 生产环境需要保留数据时，先确认旧 `blog:stream:engagement:v1` 的 Pending 为 0，停止全部 backend，再使用 Redis `RENAME` 将四组旧前缀逐项迁移到新前缀，最后启动新版本。

迁移期间禁止新旧 backend 并行运行，否则会同时向两套命名空间写入。若不迁移旧数据，公开内容缓存和已发布文章索引会自动重建，限流窗口会自然重置，管理员需要重新登录；互动聚合只能从 PostgreSQL 最近快照恢复，尚未落库的实时变化会丢失。
