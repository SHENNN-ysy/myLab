# MyBlog 个人博客

全栈个人博客系统：面向访客的前台博客 + 面向管理员的后台管理系统，前后端分离架构，Docker 容器化部署，Jenkins CI/CD 流水线。
---

## 技术栈

| 层级 | 技术 |
|------|------|
| **博客前台** | Vue 3 + TypeScript + Vite + GSAP + Tailwind CSS 4 |
| **博客后台** | Vue 3 + TypeScript + Vite + Ant Design Vue 4 + ECharts 5 |
| **后端** | Spring Boot 3.5 + Java 21 + MyBatis-Plus |
| **数据库** | PostgreSQL 16（Flyway 版本化迁移） |
| **缓存** | Redis 7（互动计数、令牌黑名单、限流） |
| **对象存储** | 阿里云 OSS + CDN |
| **反向代理** | Nginx（HTTPS 入口、静态资源、API 反代） |
| **质量保障** | JUnit 5 + Mockito（240+ 单测）、Checkstyle、SpotBugs、JaCoCo、ArchUnit |
| **容器化** | Docker + Docker Compose |

---

## 架构

```
Internet
   │
   ▼
Nginx (:443)                     ← HTTPS 入口、静态资源、反向代理（:80 仅 301 跳转）
   ├── /             → myblog 静态站点（Vue SPA，前台）
   ├── /admin/**     → admin 静态站点（Vue SPA，后台）
   └── /api/**       → myblog-api   (:8000, Spring Boot)
                          ├── PostgreSQL  (:5432)
                          ├── Redis       (:6379)
                          └── 阿里云 OSS  （图片/文件，CDN 回源）
```


---

## 项目结构

```
MyBlog/
├── myblog/                      # 博客前台（Vue 3 + GSAP 动效）
│   └── src/
│       ├── views/               # 页面组件
│       ├── components/          # 可复用组件
│       ├── composables/         # 组合式函数
│       ├── router/              # 路由
│       └── utils/               # 工具函数
├── admin/                       # 管理后台（Vue 3 + Ant Design Vue）
│   └── src/
│       ├── api/                 # 后台 API 封装（/api/v1/admin/**）
│       ├── views/               # 登录、仪表盘、内容/文件/用户管理
│       ├── components/          # 内容编辑器、图片裁剪等
│       └── router/              # 路由 + 守卫
├── backend-java/                # Spring Boot 后端（DDD 风格模块单体）
│   └── src/main/java/com/myblog/
│       ├── controller/          # Web 层（公开接口 + /admin 管理接口）
│       ├── application/         # 业务层（service / port / repository 接口 / model）
│       ├── infrastructure/      # 适配层（Persistence、Redis、OSS、安全）
│       ├── starter/             # 装配层（Security、限流、异步等配置）
│       └── common/              # Result、ErrorCode、常量、属性
│   └── src/main/resources/
│       ├── application.yml           # 主配置（占位符走环境变量）
│       ├── application-dev.yml       # 本地开发配置（含敏感值，已 gitignore）
│       └── db/migration/             # Flyway 迁移脚本（V1__baseline.sql 起）
├── nginx/                       # Nginx 反向代理 + SSL + 限流配置
├── scripts/                     # 运维脚本（OSS 静态资源上传、库基线校验）
├── docs/                        # API 文档、数据库设计、测试工作流
├── docker-compose.yml           # 容器编排（db / redis / api / web）
└── .env.example                 # 环境变量模板
```

---

## 准备工作

### 1. 环境变量

```bash
cp .env.example .env
# 编辑 .env 填写实际值（.env 已在 .gitignore 中，不会提交）
```

| 变量 | 说明 | 必填 |
|------|------|------|
| `POSTGRES_USER` / `POSTGRES_PASSWORD` / `POSTGRES_DB` | 数据库账号 | 是 |
| `REDIS_PASSWORD` | Redis 密码（生产必须强密码） | 是 |
| `JWT_SECRET` | JWT 签名密钥（`openssl rand -hex 32` 生成） | 是 |
| `ENGAGEMENT_HASH_SECRET` | 访客标识 HMAC 密钥，不设置则沿用 `JWT_SECRET` | 建议 |
| `INIT_ADMIN_USERNAME` / `INIT_ADMIN_PASSWORD` | 初始管理员（仅在系统无任何用户时创建） | 是 |
| `OSS_ENDPOINT` / `OSS_ACCESS_KEY_ID` / `OSS_ACCESS_KEY_SECRET` / `OSS_BUCKET` | 阿里云 OSS | 是 |
| `OSS_CDN_DOMAIN` | CDN 域名，留空则直连 OSS | 按需 |
| `CORS_ORIGINS` | 跨域来源（浏览器访问的源，逗号分隔） | 是 |
| `BLOG_DOMAIN` | 主站域名（前台在 `/`，后台在 `/admin/` 路径；www 前缀自动附带） | 是 |

### 2. 环境要求

| 依赖 | 版本 | 说明 |
|------|------|------|
| Java | 21+ | 后端编译/运行（Docker 中内置） |
| Maven | 3.9+ | 后端构建（Docker 中内置） |
| Node.js | 20+ | 前端构建（Docker 多阶段构建内置） |
| Docker | 24+ | 容器运行时 |
| Docker Compose | v2 | 容器编排 |

---

## 快速开始

### 本地开发

数据库与 Redis 直接用 Docker 容器（不必整套 compose 启动）：

```bash
# 1. 启动依赖容器（复用 docker-compose 里的 db / redis 即可）
docker compose up -d db redis
#    PostgreSQL 映射到本机 5432，Redis 映射到本机 6379

# 2. 后端（Java 21 + Maven）：dev profile 自动连上述容器
#    敏感配置在 backend-java/src/main/resources/application-dev.yml（已 gitignore，需自行创建）
cd backend-java
mvn spring-boot:run -Dspring-boot.run.profiles=dev
#    启动时 Flyway 自动执行 db/migration 下的迁移完成建表与初始数据
#    首次启动自动创建初始管理员（默认 admin / admin123，见 application-dev.yml）

# 3. 前台（端口 5173）
cd myblog && npm ci && npm run dev

# 4. 管理后台（端口 5174，/api 自动代理到 :8000）
cd admin && npm ci && npm run dev
```

> IDEA 直跑后端：Run Configuration 的 Active profiles 填 `dev` 即可；本地调试前注意停掉 compose 的 api 容器（8000 端口冲突）。

### 测试与质量检查

```bash
cd backend-java
mvn test        # 单元测试（240+，纯单测不依赖 DB/Redis）
mvn verify      # 完整质量门：单测 + Checkstyle + SpotBugs + JaCoCo 覆盖率
```

覆盖率报告：`backend-java/target/site/jacoco/index.html`。核心业务层（`application.service`）行覆盖率 ≥ 99%，分层依赖约束由 ArchUnit 守护。

---

## 服务器部署

### Docker Compose 一键部署（推荐）

```bash
# 1. 拉取项目
cd /data && git clone <repo-url> myblog && cd myblog

# 2. 配置环境变量
cp .env.example .env && vim .env

# 3. 构建并启动全部服务
docker compose up -d --build
```

- **构建即质量门**：后端镜像构建阶段会执行全部单元测试 + Checkstyle，测试不通过则构建失败，问题代码不会进入线上
- **数据库初始化**：无需手工执行 SQL。首次启动时后端 Flyway 自动执行 `V1__baseline.sql` 完成建表与初始数据；存量库自动以 V1 为基线接管，后续变更追加 `V2__xxx.sql` 即可
- 镜像加速：离线/内网环境可先跑 `sync-images-to-local-registry.ps1` 把基础镜像同步到本地 registry（localhost:5000）

### 验证部署

```bash
curl http://localhost:8000/actuator/health     # 后端健康检查
curl http://localhost/api/v1/...               # 公开内容接口
```

浏览器访问：

| 地址 | 服务 |
|------|------|
| `https://<域名>` | 博客前台 |
| `https://<域名>/admin/` | 管理后台 |
| `https://<域名>/swagger-ui.html` | API 文档（Swagger UI） |

---

## 运维命令

```bash
# 服务管理
docker compose ps                        # 查看服务状态
docker compose logs -f api               # 跟踪后端日志
docker compose restart api               # 重启后端

# 仅更新业务服务（不重建 db / redis）
docker compose up -d --no-deps --build api web

# 数据库
docker exec -it myblog-db-1 psql -U myblog -d myblog           # 进入数据库
docker exec myblog-db-1 pg_dump -U myblog -d myblog \
  --no-owner --no-privileges --inserts > backup.sql            # 逻辑备份
docker exec myblog-db-1 psql -U myblog -d myblog \
  -c "SELECT version, description, success FROM flyway_schema_history;"  # 迁移状态

# 静态资源上云（图片同步 OSS，配合 CDN）
pwsh scripts/upload-static-assets-to-oss.ps1 -Bucket <bucket>

# 空间维护
docker image prune -f                    # 清理无用镜像
docker compose down --volumes            # 停止并删除数据卷（危险，先备份）
```

---

## 端口速查

| 端口 | 服务 | 用途 |
|------|------|------|
| 443 | Nginx (web) | HTTPS 入口（前台 `/`，后台 `/admin/`） |
| 80 | Nginx (web) | 仅 301 跳转 HTTPS |
| 8000 | myblog-api | Spring Boot API（仅绑定 127.0.0.1） |
| 15432 | PostgreSQL | 数据库（容器内 5432，避开本机已占用的 5432） |
| 6379 | Redis | 缓存（仅绑定 127.0.0.1） |
| 5173 / 5174 | Vite Dev Server | 本地开发前台 / 后台 |

---

## 核心功能

### 博客前台
- 七个内容模块展示：首页轮播、关于我（气泡/要点）、技能图谱、足迹地图、爱好时间轴、Vibe 工具、MyLab 帖子
- 互动：浏览 / 点赞 / 访问实时计数走 Redis（Lua 保证原子性），定时快照落 PG，Redis 故障自动降级读快照
- 匿名访客隐私：访客标识经 HMAC 哈希，明细只存 Redis（72h TTL），不落库

### 管理后台
- 版本化内容管理：草稿编辑 → 发布 → 归档/下线，发布内容只读，历史版本可恢复；草稿保存带乐观锁防并发覆盖
- 图片裁剪上传（cropperjs）、文件素材库（OSS 预签名 URL 分发）
- 用户管理（创建/停用/重置密码）、访问统计仪表盘（ECharts）
- JWT 双令牌认证（access + refresh），Redis jti 黑名单支持吊销，退出即失效；登录与全局双级限流

---

## 文档导航

| 文档 | 路径 | 说明 |
|------|------|------|
| API 接口文档 | [docs/API接口文档.md](docs/API接口文档.md) | 全部 REST 接口定义 |
| 后端架构说明 | [backend-java/ARCHITECTURE.md](backend-java/ARCHITECTURE.md) | 分层架构与模块职责 |
| 数据库设计 | [docs/数据库表结构重设计.md](docs/数据库表结构重设计.md) | 版本化内容系统的表设计实践 |
| 部署说明 | [deploy/README.md](deploy/README.md) | Nginx 配置与部署细节 |
| 测试工作流 | [docs/测试工作流.md](docs/测试工作流.md) | 测试约定与流程 |
| 环境变量模板 | [.env.example](.env.example) | 全部配置项说明 |
