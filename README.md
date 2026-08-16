# MyBlog 个人博客

个人博客全栈项目：Vue 3 前台 + Vue 3 管理后台 + Java 21 / Spring Boot 后端，PostgreSQL 持久化，Redis 做互动计数与令牌黑名单，阿里云 OSS 托管图片资源，Docker Compose 一键部署。

## 功能特性

- **版本化内容发布**：home / about / skills / footprints / hobbies / vibe / mylab 七个内容模块按 `DRAFT / PUBLISHED / ARCHIVED / OFFLINE` 状态流转，发布内容只读，支持历史版本恢复
- **管理后台**：内容编辑、图片裁剪上传、文件素材库、用户管理、访问统计面板
- **互动统计**：浏览 / 点赞 / 访问实时计数走 Redis（Lua 脚本保证原子性），定时快照落 PG，Redis 故障时读接口自动降级到 PG 快照
- **认证安全**：JWT 双令牌（access + refresh），Redis jti 黑名单支持吊销，退出登录即失效
- **匿名访客隐私**：访客标识经 HMAC 哈希，明细只存 Redis（72h TTL），不落库

## 技术栈

| 端 | 技术 |
| --- | --- |
| 前台 `myblog/` | Vue 3、Vite、GSAP、TypeScript |
| 后台 `admin/` | Vue 3、Vite、Ant Design Vue、cropperjs |
| 后端 `backend-java/` | Java 21、Spring Boot 3、MyBatis-Plus、JWT |
| 数据 | PostgreSQL 16、Redis 7、阿里云 OSS |
| 部署 | Nginx、Docker Compose |

## 目录结构

```
├── myblog/          # 博客前台
├── admin/           # 管理后台
├── backend-java/    # 后端服务（含数据库迁移脚本）
├── deploy/nginx/    # Nginx 镜像与站点配置
├── docs/            # API 文档、表结构设计、测试工作流
└── docker-compose.yml
```

## 快速开始

```bash
cp .env.example .env   # 填写数据库、Redis、JWT、OSS 等配置
docker compose up -d --build
```

- 博客前台：http://localhost（`BLOG_DOMAIN`）
- 管理后台：http://localhost（`ADMIN_DOMAIN`）
- 后端 API：`/api/v1`，Swagger UI 见 `/swagger-ui.html`

本地开发：

```bash
# 后端（Java 21 + Maven）
mvn -f backend-java/pom.xml test
mvn -f backend-java/pom.xml spring-boot:run

# 前台 / 后台（Node 20+）
cd myblog && npm ci && npm run dev    # :5173
cd admin && npm ci && npm run dev     # :5174，/api 代理到 :8000
```

## 文档

- [API 接口文档](docs/API接口文档.md)
- [数据库表结构](docs/数据库表结构重设计.md)
- [后端架构说明](backend-java/ARCHITECTURE.md)
- [部署说明](deploy/README.md)
