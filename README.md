# MyBlog 个人博客

MyBlog 包含访客博客、管理后台和 Spring Boot API，支持本地 Docker 一键部署，也支持 Jenkins + 私有 Registry 的生产 CI/CD。

## 技术栈

| 模块 | 技术 |
|---|---|
| 博客前台 | Vue 3、TypeScript、Vite、GSAP、Tailwind CSS 4 |
| 管理后台 | Vue 3、TypeScript、Vite、Ant Design Vue 4、ECharts 5 |
| 后端 | Spring Boot 3.5、Java 21、MyBatis-Plus |
| 数据与缓存 | PostgreSQL 16、Redis 7、Flyway |
| 部署 | Docker、Docker Compose、Nginx、Jenkins |

## 两种部署方式

| 场景 | 入口 | 构建方式 | Nginx | Registry |
|---|---|---|---|---|
| 本地一键部署 | `docker-compose.yml` | 从当前源码构建 | HTTP `:80` | 不需要 |
| 生产 CI/CD | `deploy/docker-compose.yml` | CI 构建，CD 只拉镜像 | HTTPS `:443` + Jenkins 代理 | `127.0.0.1:5000` |

两套 Compose 使用不同项目名和 Nginx 配置。根目录方案不依赖 Jenkins、域名、TLS 证书或私有镜像仓库；`deploy/` 下的文件只用于生产环境。

## 本地一键部署

### 1. 准备配置

```bash
cp .env.example .env
```

修改 `.env` 中的数据库密码、Redis 密码、JWT 密钥、初始管理员和 OSS 配置。后台路径由 `ADMIN_ROUTE` 控制，默认 `/admin`。

### 2. 构建并启动

```bash
docker compose up -d --build
docker compose ps
```

本地一键部署仅编译并打包应用，不执行后端单元测试或 Testcontainers 集成测试；完整质量门由 CI 执行。

首次启动由 Flyway 自动建表。访问地址：

- 博客前台：`http://localhost/`
- 管理后台：`http://localhost/admin/`
- 后端健康检查：`http://localhost/api/v1/health`

本地 Nginx 使用 `nginx/default.conf.template`，只监听 HTTP，不读取生产证书，也不代理 Jenkins。

### 3. 常用命令

```bash
docker compose logs -f backend
docker compose up -d --build backend frontend-web frontend-admin
docker compose down
```

`docker compose down --volumes` 会删除本地 PostgreSQL 和 Redis 数据，请先备份。

## 本地开发

只启动依赖容器：

```bash
docker compose up -d postgres redis
```

分别启动应用：

```bash
# 后端
cd backend-java
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 博客前台
cd myblog
npm ci
npm run dev

# 管理后台
cd admin
npm ci
npm run dev
```

端口为后端 `8000`、前台 `5173`、后台 `5174`、PostgreSQL `15432`、Redis `6379`。

## 测试

```bash
cd backend-java
mvn test
mvn verify

cd ../myblog
npm ci && npm run lint && npm run build

cd ../admin
npm ci && npm run lint && npm run build
```

`mvn verify` 包含单元测试、Testcontainers 集成测试、Checkstyle、SpotBugs、JaCoCo 和 ArchUnit。

## 生产 CI/CD

生产环境不执行根目录的一键部署命令。完整流程为：

```text
GitHub PR 合并到 master
  → CI 质量门与集成测试
  → 构建三个镜像
  → 推送本机 Registry
  → CD 校验并拉取指定 tag
  → deploy/docker-compose.yml 无构建部署
```

从空白服务器开始的 Docker、Registry、Jenkins、域名、证书、Webhook、Job 和首次发布步骤见 [生产部署指南](deploy/README.md)。流水线行为和保留策略见 [CI/CD 设计](docs/CI-CD.md)。

## 项目结构

```text
MyBlog/
├── myblog/                         # 博客前台
├── admin/                          # 管理后台
├── backend-java/                   # Spring Boot 后端
├── nginx/
│   └── default.conf.template       # 本地 HTTP 网关
├── deploy/
│   ├── docker-compose.yml          # 生产 image-only 编排
│   ├── .env.example                # 生产配置模板
│   ├── nginx/                      # 生产 HTTPS 网关
│   ├── registry/                   # 私有 Registry
│   ├── jenkins/                    # Jenkins 镜像与编排
│   └── images/                     # CI 运行镜像 Dockerfile
├── docker-compose.yml              # 本地一键部署
├── Jenkinsfile.ci
├── Jenkinsfile.cd
└── Jenkinsfile.registry-cleanup
```

## 文档

| 文档 | 说明 |
|---|---|
| [deploy/README.md](deploy/README.md) | 从空白服务器开始的生产部署指南 |
| [docs/CI-CD.md](docs/CI-CD.md) | CI/CD 阶段、版本、校验、清理与回滚 |
| [docs/CI-CD学习实验记录.md](docs/CI-CD学习实验记录.md) | 从手工部署演进到 Jenkins CI/CD 的学习与实践记录 |
| [docs/测试工作流.md](docs/测试工作流.md) | 本地与流水线测试要求 |
| [backend-java/ARCHITECTURE.md](backend-java/ARCHITECTURE.md) | 后端分层架构 |
| [docs/API接口文档.md](docs/API接口文档.md) | REST API |
| [.env.example](.env.example) | 本地一键部署配置模板 |
| [deploy/.env.example](deploy/.env.example) | 生产 CI/CD 配置模板 |
