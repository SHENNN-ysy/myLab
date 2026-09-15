# AGENTS.md

## 1. 项目概述

MyBlog 个人博客系统：面向访客的博客前台 + 面向管理员的后台管理系统，前后端分离，Docker 容器化部署，Jenkins CI/CD。

- **博客前台**：Vue 3 + TypeScript + Vite + GSAP + Tailwind CSS 4（localhost:5173）
- **管理后台**：Vue 3 + TypeScript + Vite + Ant Design Vue 4 + ECharts 5（localhost:5174，vite base 由 `ADMIN_ROUTE` 构建）
- **后端**：Spring Boot 3.5 + Java 21 + MyBatis-Plus，模块化单体 + 端口适配器（localhost:8000）

```
MyBlog/
├── myblog/                  # 博客前台（Vue SPA）
├── admin/                   # 管理后台前端（Vue SPA）
├── backend-java/            # Spring Boot 后端
│   └── src/main/java/com/myblog/
│       ├── controller/      # REST 接口 + 全局异常处理（只做协议转换）
│       ├── application/     # 业务层：service / model / port / repository 接口
│       ├── infrastructure/  # 适配层：Persistence、Redis 会话、OSS 实现
│       ├── starter/         # 装配层：Security、过滤器、Bean 配置、启动初始化
│       └── common/          # Result、ErrorCode、常量、属性、上下文
│   └── src/main/resources/
│       ├── application.yml        # 主配置（占位符走环境变量）
│       ├── application-dev.yml    # 本地开发配置（含敏感值，已 gitignore，需自建）
│       └── db/migration/          # Flyway 迁移脚本（V1__baseline.sql 起）
├── nginx/                   # 本地 HTTP 网关配置
├── deploy/                  # 生产 Compose、HTTPS Nginx、Registry、Jenkins
├── docs/                    # API 文档、数据库设计、测试工作流
├── scripts/                 # 运维脚本（Registry 镜像查询与清理）
├── docker-compose.yml       # 本地一键部署（从源码构建）
├── Jenkinsfile.ci           # CI：质量门 + master 镜像构建与推送
├── Jenkinsfile.cd           # CD：指定 Registry tag 拉取部署
└── Jenkinsfile.registry-cleanup # Registry 定时清理
```

详细文档：[README.md](README.md) | [后端架构](backend-java/ARCHITECTURE.md) | [API 接口文档](docs/API接口文档.md)

## 2. 快速命令

### 博客前台 / 管理后台（分别在 myblog/、admin/ 目录下，命令一致）
```bash
npm ci            # 安装依赖（CI 用，严格按 lock 文件）
npm run dev       # 开发服务器（5173 / 5174，/api 自动代理到 :8000）
npm run build     # vue-tsc 类型检查 + 构建（类型错误即失败）
npm run lint      # ESLint
```

### 后端（在 backend-java/ 目录下）
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev   # 本地启动（dev profile，需先建 application-dev.yml）
mvn test        # 单元测试（240+，纯单测，不依赖 DB/Redis）
mvn verify      # 完整质量门：单测 + Checkstyle + SpotBugs + JaCoCo + ArchUnit
```

### 本地依赖容器
```bash
docker compose up -d postgres redis   # PG 映射 127.0.0.1:15432，Redis 映射 127.0.0.1:6379
```

### 本地一键部署
```bash
docker compose up -d --build
docker compose logs -f backend    # 跟踪后端日志（另见宿主机 ./logs/myblog.log）
```

## 3. 后端架构

### 分层与依赖方向（ArchUnit 强制，见 LayeredArchitectureTest）
```
controller ─────> application <──────── infrastructure
    │                  │                       │
    └─────────────────> common <───────────────┘
starter ────────> application / common / infrastructure
```

硬性规则：
1. `controller` 只做 HTTP 协议转换，禁止业务逻辑，禁止直接访问 Mapper/JDBC/Redis/OSS，且只允许调用应用服务，禁止直接依赖 `application.port`/`application.repository`（ArchUnit 强制）
2. 应用服务只依赖 `repository`/`port` 接口，禁止依赖 `infrastructure` 实现
3. 应用服务禁止接收 `HttpServletRequest`、`MultipartFile` 等 Web 对象（文件上传走 `UploadFile` 命令）
4. `common` 不得反向依赖其他层；`infrastructure` 不得依赖 `controller`/`starter`
5. 写操作使用明确的命令对象（`model/command/`），不用通用反射 CRUD

### 业务模块（application/service/）
| 模块 | 职责 |
|------|------|
| `auth` | 登录、Redis 会话认证、退出、修改密码 |
| `user` | 后台用户管理（创建/删除限 superadmin） |
| `content` | 七个内容模块（home/about/skills/footprints/hobbies/vibe/mylab）的草稿、发布、下线、历史版本与恢复 |
| `file` | 文件元数据、OSS 上传、预签名 URL |
| `engagement` | 页面浏览/点赞/访问由 Redis Lua 原子计数并写 Stream 脏聚合通知；一份访客凭证只计一次访问，首页、MyLab 列表及每篇详情分别去重浏览；Consumer Group 批量读取最新绝对值落 PG，成功后 XACK，Redis 故障降级读 PG 快照；写接口防刷校验优先读 Redis 已发布索引（`PublishedPostCache`），未命中回源 PG 补写，MyLab 发布/下线提交后整体重建 |
| `system` | 健康状态与系统信息 |

### 版本化内容系统
- 每个内容模块同一时刻至多一个 DRAFT 草稿和一个 PUBLISHED 线上版本，发布即生成不可变历史版本
- 发布内容只读；内容必须先保存具名且带描述的草稿才能发布；线上版本须先下线才能删除
- 草稿保存用 `expected_updated_at` 乐观锁防并发覆盖；历史版本恢复时目标记录原地转为草稿、原草稿转为归档，不创建新版本
- 草稿可手动归档（`POST /admin/content/{moduleKey}/draft/archive`）：草稿原地转为归档版本，不产生新草稿，不影响线上内容
- 历史列表包含当前线上、当前草稿和其他未删除版本；仪表盘历史数量不得包含软删除版本
- MyLab Markdown 正文保存在 `mylab_cards.markdown_content` 并参与版本复制；公开列表不返回正文，单篇详情接口按 `post_key` 返回正文
- MyLab 全局标签不再支持人工排序和后台启停；`mylab_tags.sort_order` 仅为历史兼容保留且应用不读写，前台按当前公开卡片引用次数降序展示，后台标签管理按当前草稿卡片引用次数降序展示；标签新增、显式保存名称和删除使用独立接口，MyLab 草稿保存只提交卡片
- `mylab_cards.sort_order` 仅为历史兼容保留，程序不再读写；MyLab 管理视图与公开列表按 `post_date DESC`、`post_key ASC` 排序
- MyLab PROJECT 卡片的 `project_show_order`为 null 表示不在首页项目区展示（卡片仍在 MyLab 列出）；仅参与展示的卡片校验位次（0-5）唯一且发布时必填侧边栏正文
- 首页 `/public/content` 使用 `myproject` 返回展示项目摘要，并以 `mylab` 返回最新 5 张启用卡片摘要（文章与项目混合，按 `post_date DESC`、`post_key ASC`），两者均只带各卡片实际引用的标签名称，不返回全局标签字典；`/public/content/mylab` 通过独立 Redis Cache-Aside 返回全部公开 MyLab 卡片摘要与标签
- `mylab_resources` 只保存卡片封面图片引用；Markdown 文件可在后台本地读取到编辑区，但不上传 OSS
- 新上传 OSS 图片的 object key 固定为 `业务目录/UUID.扩展名`；不配置统一前缀和日期目录，历史 key 继续兼容读取
- 前台 `/public/content`、`/public/content/mylab` 与 MyLab 单篇详情使用相互独立的 Redis Cache-Aside；其他公开单模块和后台管理接口直查 PG；发布/下线提交后失效缓存；MyLab 单篇详情先校验 post_key 格式（`ContentConstant.POST_KEY_PATTERN`），不存在/未发布的 key 以空详情做负缓存防穿透，负缓存随发布失效事件一并清除；缓存载荷结构变化（如聚合接口新增字段）时必须升级 key 版本号（如 `v3`→`v4`），否则旧结构缓存会存活至 TTL（最长 6 小时）导致新字段不生效

### 认证与安全
- 管理后台使用随机 UUID Bearer Token；Redis 仅保存 SHA-256 摘要和会话 Hash，并以用户 ZSet 反向索引全部会话；空闲 8 小时滑动过期
- 登录与敏感账号写操作使用 PostgreSQL 用户行锁；修改用户名、密码、角色、启用状态或删除用户时批量吊销该用户全部会话
- Redis 会话服务不可用时登录和受保护接口返回 503，公开接口与访客 HMAC 不依赖管理会话
- 挂载进 SecurityFilterChain 的自定义 `@Component` Filter（如 SessionAuthenticationFilter）必须同时声明 `FilterRegistrationBean` 并 `setEnabled(false)` 禁用容器自动注册，否则每个请求会在容器链与安全链中各执行一次（OncePerRequestFilter 两次注册的去重键不同，去重失效）
- 密码 BCrypt（强度 12）；初始管理员仅在系统无用户时创建一次（`INIT_ADMIN_*`，未配置时本地兜底 admin/admin123，生产 compose 强制必填）
- 种子接管：`V1__baseline.sql` 内置固定 ID/用户名的种子管理员，启动时仅当该行用户名与密码哈希**均与基线完全一致**才按 `INIT_ADMIN_*` 接管；一旦某次启动（如 `.env` 缺失密码被置空）已覆写该行，之后补回配置不会再生效，需把该行重置回基线种子值后重启才能重新接管
- 限流走 Redis：登录接口独立（更严）阈值 + 全局限流；Redis 故障 fail-open
- 访客标识经 HMAC 哈希（`ENGAGEMENT_HASH_SECRET`），访问、页面浏览和点赞状态合并保存在 `mylab:blog:visitor:v2:{visitorHash}` Hash，不落库；凭证默认 24 小时滑动过期（`VISITOR_IDENTITY_TTL`），有效页面浏览、点赞或取消点赞都会刷新 Redis TTL 和 HttpOnly Cookie
- 一份有效访客凭证只累计一次访问；首页、MyLab 列表和每个已发布 MyLab 详情分别只累计一次浏览，详情首次互动会补记该详情浏览，保证新口径下总浏览量不小于访问量

### 数据库
- PostgreSQL 16，schema 由 Flyway 管理：全新库执行 `V1__baseline.sql` 初始化；存量库以 V1 为基线接管
- **后续结构变更一律新增 `V2__xxx.sql`，禁止改动已应用的迁移脚本**
- 连接池 HikariCP 用默认值（最大 10），PG 侧 `max_connections=30`

## 4. 前端架构

- Vue 3 Composition API + `<script setup>` + TypeScript；`@/` 映射 `src/`
- 前台（myblog/）：GSAP 动效 + Tailwind CSS 4，七个内容模块展示
- 后台（admin/）：Ant Design Vue 4 优先，避免自造组件；ECharts 仪表盘；vite base 由 `ADMIN_ROUTE` 注入
- `npm run build` 内含 vue-tsc 类型检查，即编译验证；提交前至少跑 `lint` + `build`

## 5. 部署架构

- 根目录 `docker-compose.yml`：本地一键部署，从源码构建，使用 `nginx/default.conf.template`，仅 HTTP，不运行 Jenkins 或 Registry。
- `deploy/docker-compose.yml`：生产 image-only 部署，使用 `deploy/nginx/default.conf.template`，提供 HTTPS 和 Jenkins 反向代理。

```
Internet → nginx 网关（80 仅 301，443 HTTPS，唯一对外入口）
             ├── /        → frontend-web   容器（前台静态站点，nginx 托管）
             ├── ${ADMIN_ROUTE}/ → frontend-admin 容器（后台静态站点，去前缀反代）
             ├── /api/    → backend        容器（Spring Boot，仅 127.0.0.1:8000）
                               ├── postgres（PG 16，127.0.0.1:15432）
                               └── redis（Redis 7，127.0.0.1:6379）
             └── ${JENKINS_ROUTE}/ → Jenkins（专用外部网络，不发布主机端口）
```

- web/admin 不暴露主机端口，仅内部网络可达；生产 SSL 证书挂载 `deploy/nginx/certs/`
- 本地配置使用根目录 `.env`；生产配置使用 `deploy/.env`，两者不得混用
- 管理后台路径由生产 `deploy/.env` 的 `ADMIN_ROUTE` 同时注入 CI 构建和 Nginx，修改后必须重新发布镜像
- 镜像名可用环境变量覆盖：`API_IMAGE` / `WEB_IMAGE` / `ADMIN_IMAGE` / `POSTGRES_IMAGE` / `REDIS_IMAGE`
- 应用镜像由 CI 推送到 `registry:2`，生产 Compose 不含 `build`；CD 只允许指定不可变 tag 拉取部署
- Registry 仅绑定 `127.0.0.1:5000`；每个仓库保留最近 5 个 tag，并额外保护当前成功部署 tag
- 注意：`depends_on` 仅在 `docker compose up` 时生效；服务器重启后 Docker 按 `restart: unless-stopped` 自行拉起容器，backend 可能先于 postgres 启动失败几轮后自愈，属预期行为

### CI/CD（Jenkins）
- **Jenkinsfile.ci**：生产 Multibranch Job 只索引 `master`；PR 合并后执行质量门并封装、推送三个已验证镜像
- **Jenkinsfile.cd**：必填 `IMAGE_TAG`，先检查 Registry 与三镜像，再拉取并通过 `deploy/docker-compose.yml` 执行 `up -d --no-build`
- **Jenkinsfile.registry-cleanup**：每天 03:30 通过 Registry V2 HTTP API 删除非保留 manifest，再执行 garbage collection
- Jenkins 容器使用 `deploy/jenkins/Dockerfile` 安装 Docker CLI、Buildx、Compose、`curl`、`jq` 与 `flock`
- `Jenkinsfile.ci` 顶层为 `agent none`，禁止在顶层 `environment` 调用 `sh`；`DOCKER_GID` 必须在已分配节点的阶段内校验并传给 Maven 容器
- Maven 依赖缓存挂载点必须是 `/home/ubuntu/.m2`：Jenkins `inside()` 强制容器以 `-u 1000:1000` 运行（maven 镜像内为 `ubuntu` 用户，home=`/home/ubuntu`），挂 `/root/.m2` 永远不会被读到，表现为每次 CI 全量下载依赖
- Jenkins 使用 `deploy/.env` 的 `JENKINS_ROUTE` 作为控制器 `--prefix`，并通过外部网络 `myblog-jenkins-proxy` 由生产 Nginx 提供 HTTPS 与 GitHub Webhook 入口
- 回滚：从 Registry 最近保留版本中选择 tag，重新触发 CD
- ARMS 应用监控（可选）：探针放 `deploy/AliyunJavaAgent/`（gitignore 不入库），compose 挂载到 backend `/opt/arms`，在 `.env` 配 `ARMS_AGENT_OPTS` JVM 参数（经 `JDK_JAVA_OPTIONS` 注入）即启用，留空不启用；详见 deploy/README.md 第 11 节

## 6. 关键约定

### 后端硬性规则
1. **统一响应**：`Result<T>`（`code`/`message`/`data`/`error`），`code=0` 成功；HTTP 状态码取自错误码（不是统一 200）
2. **异常处理**：业务异常抛 `BaseException` 子类（`ValidationException`/`NotFoundException`/`ConflictException`/`UnauthorizedException`），由 `GlobalExceptionHandler` 统一转换为错误码响应；错误码用 `ErrorCode` 枚举，不硬编码
3. **JSON 字段 snake_case**、`/api/v1` 路径契约保持不变
4. **日志规范**：
   - 每个请求由 `TraceIdFilter` 打一行访问日志（方法/URI/状态码/耗时），MDC 带 traceId 贯穿全链
   - 健康检查（`/api/v1/health`、`/actuator/health`）日志为 DEBUG，不入日志文件
   - 关键业务操作必须记审计日志（登录成功/失败含 IP、内容发布/下线、用户管理、文件上传/删除），用 `@Slf4j`
   - 敏感值（密码、令牌、密钥）禁止落日志；客户端 IP 从 `RequestContext.getIp()` 取
   - 日志文件：`logs/myblog.log`，按天滚动保留 30 天
5. **配置**：敏感配置一律走环境变量占位符（`${VAR:默认值}`），不提交真实值；`.env`、`application-dev.yml`、`nginx/certs/`、`deploy/nginx/certs/` 已 gitignore
6. **质量门**：改动后端后至少跑 `mvn verify`；`application.service` 行覆盖率 ≥ 99%，新增业务逻辑必须补单测
7. **注释要求**：生成的代码必须有注释，注释要简洁精准
8. **Redis key 命名**：统一使用 `mylab:` 顶层命名空间，下分 `auth:`、`blog:`、`rate:`；公开内容缓存在 `mylab:blog:content:`。带动态段（如 `mylab:blog:engagement:{postKey}`）的 key 空间下不得再放固定命名的其他类型 key，须用独立子前缀隔离（已发布索引用 `mylab:blog:engagement:index:published-posts`；曾因与计数 Hash 撞名，post_key 为 `published-posts` 时会 WRONGTYPE）

### 前端硬性规则
1. 优先使用既有技术栈组件（后台用 Ant Design Vue），避免自造轮子
2. `build` 即类型检查，类型错误不许绕过（不用 `@ts-ignore` 掩盖）
3. 后台前端所有资源路径基于 `ADMIN_ROUTE` 生成的 Vite base
4. CSS Modules 会把 `animation` 引用的 keyframes 名一并作用域化：`.module.css` 里引用的 `@keyframes` 必须定义在同一模块内，定义在全局 CSS 里的同名 keyframes 匹配不上（动画静默失效，构建不报错）
5. CSS Modules 里禁用 `.container span` 这类后代裸标签选择器：模块只哈希类名，标签仍是全局的，会命中内部 antd 组件渲染的同名标签（如 Button 的文字 span）导致颜色等样式被意外覆盖；给目标元素加专用类名
6. 可编辑业务标识不得同时用作 React 列表或 Ant Design Collapse 的 `key`；编辑器应维护不参与接口提交的不可变本地 ID，避免输入时组件被卸载重建、面板收起或焦点丢失

### 通用约定
1. 发现经典错误修复后，将原因与对策补充到本文档或 docs/ 相应文档
2. 信息不足时不要猜测，先询问用户
3. 修改了本文档提到的结构、命令或约定时，同步更新本文档

## 7. 文档导航

| 文档 | 路径 | 说明 |
|------|------|------|
| 项目总览 | [README.md](README.md) | 技术栈、快速开始、运维命令、端口表 |
| 后端架构 | [backend-java/ARCHITECTURE.md](backend-java/ARCHITECTURE.md) | 分层职责与依赖规则详解 |
| API 接口文档 | [docs/API接口文档.md](docs/API接口文档.md) | 全部 REST 接口定义 |
| 错误码文档 | [docs/错误码文档.md](docs/错误码文档.md) | ErrorCode 枚举与语义 |
| 数据库设计 | [docs/数据库表结构重设计.md](docs/数据库表结构重设计.md) | 版本化内容系统表设计 |
| 测试工作流 | [docs/测试工作流.md](docs/测试工作流.md) | 测试约定与流程 |
| Redis 公开内容缓存 | [docs/Redis公开内容缓存开发说明.md](docs/Redis公开内容缓存开发说明.md) | 前台公开内容缓存、分布式锁与失效机制 |
| Redis Stream 互动落库 | [docs/Redis Stream互动统计落库改造说明.md](docs/Redis Stream互动统计落库改造说明.md) | 浏览、点赞、访问统计的异步落库、一致性、监控与回滚 |
| Redis Key 命名空间 | [docs/Redis Key命名空间说明.md](docs/Redis Key命名空间说明.md) | `mylab:` 统一顶层、三类子空间与发布迁移注意事项 |
| 部署说明 | [deploy/README.md](deploy/README.md) | Nginx/SSL/域名配置细节 |
| CI 内存耗尽事故复盘 | [docs/事故复盘-CI构建内存耗尽.md](docs/事故复盘-CI构建内存耗尽.md) | 2026-09-07 事故经过、根因、修复与独立 CI 服务器规划 |
| 本地环境变量模板 | [.env.example](.env.example) | 本地一键部署配置 |
| 生产环境变量模板 | [deploy/.env.example](deploy/.env.example) | 生产 CI/CD 配置 |
