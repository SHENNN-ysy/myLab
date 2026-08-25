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
│       ├── infrastructure/  # 适配层：Persistence、Redis、JWT、OSS 实现
│       ├── starter/         # 装配层：Security、过滤器、Bean 配置、启动初始化
│       └── common/          # Result、ErrorCode、常量、属性、上下文
│   └── src/main/resources/
│       ├── application.yml        # 主配置（占位符走环境变量）
│       ├── application-dev.yml    # 本地开发配置（含敏感值，已 gitignore，需自建）
│       └── db/migration/          # Flyway 迁移脚本（V1__baseline.sql 起）
├── nginx/                   # 本地 HTTP 网关配置
├── deploy/                  # 生产 Compose、HTTPS Nginx、Registry、Jenkins
├── docs/                    # API 文档、数据库设计、测试工作流
├── scripts/                 # 运维脚本（OSS 静态资源上传、库基线校验）
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
1. `controller` 只做 HTTP 协议转换，禁止业务逻辑，禁止直接访问 Mapper/JDBC/Redis/OSS
2. 应用服务只依赖 `repository`/`port` 接口，禁止依赖 `infrastructure` 实现
3. 应用服务禁止接收 `HttpServletRequest`、`MultipartFile` 等 Web 对象（文件上传走 `UploadFile` 命令）
4. `common` 不得反向依赖其他层；`infrastructure` 不得依赖 `controller`/`starter`
5. 写操作使用明确的命令对象（`model/command/`），不用通用反射 CRUD

### 业务模块（application/service/）
| 模块 | 职责 |
|------|------|
| `auth` | 登录、刷新令牌、退出、修改密码 |
| `user` | 后台用户管理（创建/删除限 superadmin） |
| `content` | 七个内容模块（home/about/skills/footprints/hobbies/vibe/mylab）的草稿、发布、下线、历史版本与恢复 |
| `file` | 文件元数据、OSS 上传、预签名 URL |
| `engagement` | 浏览/点赞计数（Redis Lua 原子操作，定时快照落 PG，Redis 故障降级读快照） |
| `system` | 健康状态与系统信息 |

### 版本化内容系统
- 每个内容模块同一时刻至多一个 DRAFT 草稿和一个 PUBLISHED 线上版本，发布即生成不可变历史版本
- 发布内容只读；历史版本可恢复到草稿；线上版本须先下线才能删除
- 草稿保存用 `expected_updated_at` 乐观锁防并发覆盖；写操作对模块加行锁串行化

### 认证与安全
- JWT 双令牌（access + refresh），退出登录把 jti 写入 Redis 黑名单吊销
- 密码 BCrypt（强度 12）；初始管理员仅在系统无用户时创建一次（`INIT_ADMIN_*`）
- 限流走 Redis：登录接口独立（更严）阈值 + 全局限流；Redis 故障 fail-open
- 访客标识经 HMAC 哈希（`ENGAGEMENT_HASH_SECRET`），互动明细只存 Redis（72h TTL）不落库

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
- **Jenkinsfile.ci**：任意分支执行后端 `mvn verify` 与前端质量门；仅 master 封装已验证产物并推送三个镜像
- **Jenkinsfile.cd**：必填 `IMAGE_TAG`，先检查 Registry 与三镜像，再拉取并通过 `deploy/docker-compose.yml` 执行 `up -d --no-build`
- **Jenkinsfile.registry-cleanup**：每天 03:30 通过 Registry V2 HTTP API 删除非保留 manifest，再执行 garbage collection
- Jenkins 容器使用 `deploy/jenkins/Dockerfile` 安装 Docker CLI、Buildx、Compose、`curl`、`jq` 与 `flock`
- Jenkins 使用 `deploy/.env` 的 `JENKINS_ROUTE` 作为控制器 `--prefix`，并通过外部网络 `myblog-jenkins-proxy` 由生产 Nginx 提供 HTTPS 与 GitHub Webhook 入口
- 回滚：从 Registry 最近保留版本中选择 tag，重新触发 CD

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

### 前端硬性规则
1. 优先使用既有技术栈组件（后台用 Ant Design Vue），避免自造轮子
2. `build` 即类型检查，类型错误不许绕过（不用 `@ts-ignore` 掩盖）
3. 后台前端所有资源路径基于 `ADMIN_ROUTE` 生成的 Vite base

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
| 部署说明 | [deploy/README.md](deploy/README.md) | Nginx/SSL/域名配置细节 |
| 本地环境变量模板 | [.env.example](.env.example) | 本地一键部署配置 |
| 生产环境变量模板 | [deploy/.env.example](deploy/.env.example) | 生产 CI/CD 配置 |
