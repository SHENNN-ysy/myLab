# CI/CD 持续集成/持续部署 完整指南

> 本文档手把手带你从零搭建 MyBlog 博客系统的 CI/CD 流水线。
> 适用于：对 CI/Docker/Jenkins 零基础的开发者。

---

## 目录

- [1. 基础概念](#1-基础概念)
- [2. 整体架构](#2-整体架构)
- [3. 技术选型](#3-技术选型)
- [4. 项目结构与 Git 仓库](#4-项目结构与-git-仓库)
- [5. Dockerfile 编写](#5-dockerfile-编写)
- [6. docker-compose.yml 编排](#6-docker-composeyml-编排)
- [7. 服务器环境准备](#7-服务器环境准备)
- [8. Jenkins CI Pipeline 配置（Jenkinsfile.ci）](#8-jenkins-ci-pipeline-配置jenkinsfileci)
- [9. Jenkins CD Pipeline 配置（Jenkinsfile.cd）](#9-jenkins-cd-pipeline-配置jenkinsfilecd)
- [10. 敏感信息管理 + 通知优化](#10-敏感信息管理--通知优化)
- [11. 常见问题排查](#11-常见问题排查)
- [12. 文件清单](#12-文件清单)

---

## 1. 基础概念

### CI（Continuous Integration，持续集成）

开发者每次 push 代码后，系统自动执行：**拉取代码 → 编译构建 → 运行测试**。
如果任何一步失败，立即通知开发者修复。目的是**尽早发现问题**，不让 bug 积累。

### CD（Continuous Delivery/Deployment，持续交付/部署）

CI 通过后，系统自动将构建产物**部署到服务器**。
- 持续交付（Delivery）：需要人工点击确认才部署
- 持续部署（Deployment）：全自动

本项目采用**持续交付**模式：CI 每次 push 自动跑，CD 需要手动在 Jenkins 上点"Build with Parameters"触发，部署动作可控、可指定镜像版本、可回滚。

### Docker

Docker 是一种容器化技术，把应用和它的所有依赖打包成一个**镜像（Image）**，
镜像运行起来就是**容器（Container）**。容器就像一个轻量级虚拟机，但启动更快、资源更少。

核心概念：
- **Dockerfile**：菜谱，定义如何构建镜像
- **Image（镜像）**：根据 Dockerfile 构建出的应用包
- **Container（容器）**：镜像运行起来的实例
- **Docker Compose**：编排工具，一条命令启动多个容器

### Jenkins

Jenkins 是最流行的开源 CI/CD 引擎。它监听代码仓库的变化，
自动拉取代码、执行构建脚本、部署应用。

### 生活比喻

```
没有 CI/CD：
  你写完代码 → 手动 SSH 到服务器 → 手动 git pull → 手动 mvn package → 手动重启 → 发现报错 → 再来一遍 😫

有 CI/CD：
  你写完代码 → git push → Jenkins 自动编译+测试（CI）
            → 想发布时在 Jenkins 点一下"构建"（CD）→ 喝杯咖啡 ☕ → 上线了！
```

---

## 2. 整体架构

```
┌─────────────┐     git push      ┌─────────────┐
│   开发者电脑   │ ──────────────→ │    GitHub    │
│  (你的电脑)    │                  │  (代码仓库)   │
└─────────────┘                   └──────┬──────┘
                                         │ 可选 webhook（需 HTTPS 反向代理）
                                         ▼
                                  ┌──────────────┐
                                  │   Jenkins     │
                                  │  (CI/CD 引擎)  │
                                  │              │
                                  │ CI: 编译+测试  │ ← 每次 push 自动触发
                                  │ CD: 构建镜像   │ ← 手动触发，指定 TAG
                                  │     部署到本机  │
                                  └──────┬───────┘
                                         │ docker compose up -d（Jenkins 与部署同机）
                                         ▼
                              ┌──────────────────────┐
                              │    云服务器 (Linux)     │
                              │                      │
                              │  ┌────────────────┐  │
                              │  │  nginx 网关容器  │  │ ← 唯一对外入口（443 HTTPS / 80 跳转）
                              │  │  (:80/:443)    │  │
                              │  ├────────────────┤  │
                              │  │ frontend-web   │  │ ← 前台 SPA 静态托管（容器内 :80，不暴露）
                              │  ├────────────────┤  │
                              │  │ frontend-admin │  │ ← 后台 SPA 静态托管（容器内 :80，不暴露）
                              │  ├────────────────┤  │
                              │  │  backend 容器   │  │ ← Spring Boot（仅 127.0.0.1:8000）
                              │  ├────────────────┤  │
                              │  │ postgres 容器   │  │ ← 数据库（仅 127.0.0.1:15432）
                              │  ├────────────────┤  │
                              │  │  redis 容器     │  │ ← 缓存/会话（仅 127.0.0.1:6379）
                              │  └────────────────┘  │
                              └──────────────────────┘
                                         │
                                         ▼
                              ┌──────────────────────┐
                              │  阿里云 OSS + CDN      │ ← 图片存储与加速（云厂商服务，
                              │  （广州 Bucket）      │    不占服务器资源，无需 MinIO）
                              └──────────────────────┘
```

**请求流转**：

```
https://shennn.top/        → nginx 网关 → frontend-web（前台 SPA）
https://shennn.top${ADMIN_ROUTE}/ → nginx 网关 → frontend-admin（后台 SPA，剥离可配置前缀）
https://shennn.top/api/    → nginx 网关 → backend:8000（Spring Boot）
```

---

## 3. 技术选型

| 组件 | 选择 | 原因 |
|------|------|------|
| 代码仓库 | GitHub | 项目托管地，Jenkins GitHub 集成成熟 |
| CI/CD 引擎 | Jenkins | 行业标准，Pipeline as Code，教程多 |
| 容器化 | Docker + Compose | 部署最规范，环境一致 |
| 前端服务 | Nginx 容器 | 托管 Vue 静态产物 + SPA 路由回退 |
| 入口网关 | 官方 nginx:1.27-alpine | SSL 终止 + 反向代理 + 限流，配置模板化免构建 |
| 后端服务 | Java 21 容器 | 运行 Spring Boot jar |
| 数据库 | PostgreSQL 16 | 项目选型，Flyway 管理 schema 迁移 |
| 对象存储 | 阿里云 OSS + CDN | 图片上云，服务器不扛流量 |
| 镜像仓库 | 服务器本地 | MVP 阶段够用，CD 通过镜像 TAG 管理版本与回滚 |

---

## 4. 项目结构与 Git 仓库

### 4.1 项目结构

```
MyBlog/                        # 单仓库，所有代码在一起
├── myblog/                    # 博客前台（Vue 3 + Vite，本地 dev 端口 5173）
│   ├── Dockerfile             # 前台镜像（构建 + nginx 托管）
│   └── nginx.conf             # 前台容器内 nginx 配置
├── admin/                     # 后台管理（Vue 3 + Vite，本地 dev 端口 5174）
│   ├── Dockerfile             # 后台镜像
│   └── nginx.conf             # 后台容器内 nginx 配置
├── backend-java/              # Spring Boot 后端（Java 21，端口 8000）
│   ├── Dockerfile             # 后端多阶段构建
│   ├── docker/maven-settings.xml  # 构建期 Maven 国内镜像加速
│   └── src/                   # 源码（含 Flyway 迁移脚本 db/migration）
├── nginx/                     # 网关配置（不自建镜像，挂进官方镜像）
│   ├── default.conf.template  # 网关配置模板（envsubst 渲染域名）
│   └── certs/                 # SSL 证书（gitignore，部署时手动放置）
├── docker-compose.yml         # 全服务编排
├── Jenkinsfile.ci             # CI 流水线（push 自动触发）
├── Jenkinsfile.cd             # CD 流水线（手动触发部署）
├── .env.example               # 环境变量模板
└── .env                       # 实际配置（gitignore，不上传）
```

### 4.2 推送到远程仓库

```bash
git init
git remote add origin git@github.com:<你的用户名>/MyBlog.git
git add .
git commit -m "feat(devops): add Docker + CI/CD infrastructure"
git push -u origin master
```

### 4.3 验证

访问 GitHub 仓库页面确认代码已上传，并确认 `.env`、`nginx/certs/` **没有**被提交（已在 .gitignore 中）。

---

## 5. Dockerfile 编写

### 5.1 后端 Dockerfile（backend-java/Dockerfile）

```dockerfile
# 基础镜像可通过 build-arg 覆盖（如离线/内网环境指定私有仓库地址）
ARG MAVEN_IMAGE=maven:3.9-eclipse-temurin-21
ARG JRE_IMAGE=eclipse-temurin:21-jre

# ---------- 阶段 1：编译打包 ----------
FROM ${MAVEN_IMAGE} AS builder
WORKDIR /app

# 先只复制 pom.xml 与 Maven 镜像配置，充分利用 Docker 层缓存
COPY pom.xml ./
COPY docker/maven-settings.xml /root/.m2/settings.xml
RUN mvn -B -q -s /root/.m2/settings.xml dependency:go-offline

# 再复制源码与质量检查配置
COPY src ./src
COPY config ./config
# 构建镜像时执行全部单元测试，测试失败则构建失败、无法部署；
# *IT 集成测试（Testcontainers）不在镜像构建期执行，由 CI 流水线负责
RUN mvn -B -q -s /root/.m2/settings.xml package

# ---------- 阶段 2：精简运行镜像 ----------
FROM ${JRE_IMAGE}
WORKDIR /app

# 非 root 用户运行，降低容器逃逸风险
RUN groupadd --system myblog && useradd --system --gid myblog myblog
COPY --from=builder /app/target/myblog-backend-1.0.0.jar app.jar
USER myblog

EXPOSE 8000
# JVM 内存/GC 参数按 2核4G 服务器调优
ENV JAVA_TOOL_OPTIONS="-Xms256m -Xmx768m -XX:MaxMetaspaceSize=192m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java","-jar","/app/app.jar"]
```

**关键知识点：多阶段构建**

```
┌─────────────────────────────────────────────────────┐
│  第一阶段: builder（构建阶段）                          │
│  FROM maven:3.9-eclipse-temurin-21  (~800MB)        │
│  包含：Maven、JDK21、源码、依赖                         │
│  产出：target/myblog-backend-1.0.0.jar               │
└──────────────────────┬──────────────────────────────┘
                       │ 只复制 jar 包
                       ▼
┌─────────────────────────────────────────────────────┐
│  第二阶段: runtime（运行阶段）                          │
│  FROM eclipse-temurin:21-jre  (~200MB)              │
│  只包含：JRE21 + jar 包 + 非 root 用户                 │
│  最终镜像：安全、轻量                                   │
└─────────────────────────────────────────────────────┘
```

**为什么先 COPY pom.xml 再 COPY 源码？**

Docker 构建是分层缓存的。如果 pom.xml 没变，`RUN mvn dependency:go-offline`
这层缓存可以复用，不需要重新下载几百 MB 的依赖。只有源码变化时才重新编译。
这样每次代码改动后构建速度从几分钟缩短到几十秒。

**测试左移到镜像构建期**：`mvn package` 会执行全部单元测试，测试不过镜像就构建不出来，
从机制上保证"能部署的镜像一定过了单测"。

### 5.2 前端 Dockerfile（myblog/Dockerfile 与 admin/Dockerfile）

两个前端结构相同（以 myblog/ 为例）：

```dockerfile
# ---------- 阶段 1：构建 Vue/TS 应用 ----------
FROM node:20-alpine AS builder
WORKDIR /app

# 利用缓存：先只复制 lock 文件，依赖不变时跳过安装
COPY package.json package-lock.json* ./
RUN npm config set registry https://registry.npmmirror.com \
    && npm install --no-audit --no-fund

# 复制源码并构建（build 内含 vue-tsc 类型检查）
COPY . .
RUN npm run build

# ---------- 阶段 2：nginx 托管静态产物 ----------
FROM nginx:1.27-alpine AS runner
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
```

### 5.3 前端容器内 nginx 配置（myblog/nginx.conf、admin/nginx.conf）

```nginx
server {
    listen 80;
    server_name _;

    root /usr/share/nginx/html;
    index index.html;

    # 静态资源缓存 7 天
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 7d;
        add_header Cache-Control "public, max-age=604800, immutable";
        try_files $uri =404;
    }

    # /api 兜底反代到后端容器（正常流量由网关处理，不走这里）
    # backend 是后端在 docker-compose 中的服务名，不是 localhost！
    location /api/ {
        proxy_pass http://backend:8000;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Vue Router history 模式支持：刷新页面回退到 index.html，否则 404
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 健康检查端点（compose 健康探测用）
    location = /healthz {
        return 200 "ok\n";
    }
}
```

### 5.4 网关配置（nginx/default.conf.template）

网关**没有自定义 Dockerfile**，直接使用官方 `nginx:1.27-alpine` 镜像，
compose 把配置模板挂载到 `/etc/nginx/templates/`，官方镜像启动时自动执行
`envsubst` 把 `${BLOG_DOMAIN}`、`${ADMIN_ROUTE}`、`${JENKINS_ROUTE}` 替换为实际配置后生成正式配置：

```nginx
# HTTP 全量 301 跳转 HTTPS
server {
    listen 80;
    server_name ${BLOG_DOMAIN} www.${BLOG_DOMAIN};
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    http2 on;
    server_name ${BLOG_DOMAIN} www.${BLOG_DOMAIN};

    ssl_certificate     /etc/nginx/certs/shennn.top.pem;
    ssl_certificate_key /etc/nginx/certs/shennn.top.key;
    # ... 限流、安全响应头等略（见仓库文件）

    # Docker 内嵌 DNS 解析上游，容器重建换 IP 后仍能解析
    resolver 127.0.0.11 valid=10s ipv6=off;
    set $api_upstream backend:8000;
    set $web_upstream frontend-web:80;
    set $admin_upstream frontend-admin:80;

    location /api/   { proxy_pass http://$api_upstream;  ... }
    location ${ADMIN_ROUTE}/ { rewrite ^${ADMIN_ROUTE}/?(.*)$ /$1 break; proxy_pass http://$admin_upstream; ... }
    location /       { proxy_pass http://$web_upstream$request_uri; ... }
}
```

**为什么用 `.template` 而不是普通 nginx.conf**：官方镜像的 envsubst 只替换环境中已定义的
变量（`${BLOG_DOMAIN}`、`${ADMIN_ROUTE}`、`${JENKINS_ROUTE}`），nginx 自身的 `$host`、`$request_uri` 等变量原样保留，
域名换环境时只改 `.env` 不用改配置。

### 5.5 .dockerignore 文件

每个服务目录下都有 `.dockerignore`，避免把不必要的文件复制进构建上下文：

**backend-java/.dockerignore：** `target/`、`.idea/`、`*.iml`、`.git/`、`*.log`

**myblog/.dockerignore 和 admin/.dockerignore：** `node_modules/`、`dist/`、`.git/`、`.idea/`、`*.log`

---

## 6. docker-compose.yml 编排

### 6.1 服务总览

完整文件见仓库根目录 `docker-compose.yml`，六个服务：

| 服务名 | 镜像 | 说明 |
|--------|------|------|
| `postgres` | postgres:16-alpine | 数据库，数据持久化到命名卷 `pg_data` |
| `redis` | redis:7-alpine | 缓存/会话，AOF 持久化，卷 `redis_data` |
| `backend` | myblog-api:latest（本地构建） | Spring Boot，Flyway 启动时自动迁移 schema |
| `frontend-web` | myblog-web:latest（本地构建） | 前台 SPA 静态托管 |
| `frontend-admin` | myblog-admin:latest（本地构建） | 后台 SPA 静态托管 |
| `nginx` | nginx:1.27-alpine（官方镜像） | 网关：SSL 终止 + 反向代理 + 限流 |

### 6.2 关键概念解析

| 概念 | 说明 |
|------|------|
| `build` | 从本地 Dockerfile 构建镜像 |
| `image: ${API_IMAGE:-myblog-api:latest}` | 镜像名可被环境变量覆盖，CD 借此指定版本 |
| `ports: "127.0.0.1:8000:8000"` | 绑定 127.0.0.1，只有本机（含 IDE 调试）能直连，公网不可达 |
| `depends_on + condition: service_healthy` | postgres/redis 健康检查通过后 backend 才启动 |
| `healthcheck` | compose 层健康状态，CD 流水线据此判定部署成败 |
| `volumes` | 数据持久化，容器删除后数据不丢失 |
| `networks: blog-net` | 所有服务在同一网络，用服务名互访（如 `backend:8000`） |
| `mem_limit` | 按 2核4G 服务器收紧各容器内存上限 |
| `${VAR}` | 从 `.env` 文件读取变量 |

### 6.3 端口规划

| 服务 | 容器端口 | 主机绑定 | 说明 |
|------|---------|---------|------|
| nginx 网关 | 80/443 | `80:80`、`443:443` | **唯一公网入口** |
| backend | 8000 | `127.0.0.1:8000` | 仅本机调试/健康检查 |
| postgres | 5432 | `127.0.0.1:15432` | 仅本机（避开本机 5432 占用） |
| redis | 6379 | `127.0.0.1:6379` | 仅本机 |
| frontend-web / frontend-admin | 80 | 不映射 | 只在内部网络可达 |
| Jenkins | 8080 | 不映射 | 经 `myblog-jenkins-proxy` 网络由 Nginx HTTPS 代理 |

**安全原则**：公网只暴露 80/443，其余全部绑定 127.0.0.1 或不映射。

### 6.4 环境变量（.env）

复制 `.env.example` 为 `.env` 并修改（生产环境必须用强随机值，`openssl rand -hex 32`）：

```bash
TZ=Asia/Shanghai
POSTGRES_USER=myblog
POSTGRES_PASSWORD=<强随机>
POSTGRES_DB=myblog
REDIS_PASSWORD=<强随机>

# 阿里云 OSS（同地域 ECS 用内网 Endpoint）
OSS_ENDPOINT=https://oss-cn-guangzhou-internal.aliyuncs.com
OSS_ACCESS_KEY_ID=<RAM 用户 AK>
OSS_ACCESS_KEY_SECRET=<RAM 用户 SK>
OSS_BUCKET=<Bucket 名>
OSS_CDN_DOMAIN=                # 未启用 CDN 留空

JWT_SECRET=<openssl rand -hex 32>
ENGAGEMENT_HASH_SECRET=<openssl rand -hex 32>
CORS_ORIGINS=https://shennn.top,https://www.shennn.top
BLOG_DOMAIN=shennn.top
ADMIN_ROUTE=/admin
JENKINS_ROUTE=/jenkins

INIT_ADMIN_USERNAME=admin
INIT_ADMIN_PASSWORD=<强随机，仅首次启动建号时生效>
```

> 生产环境务必复制为 `.env` 并修改密码！`.env` 已在 .gitignore 中，不会被提交。

### 6.5 application.yml 环境变量支持

后端 `application.yml` 全部通过环境变量注入，例如：

```yaml
url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:myblog}
```

冒号后是本地开发默认值；compose 的 `environment` 设置的值会覆盖它。
数据库 schema 由 **Flyway** 管理，backend 启动时自动执行 `db/migration` 下的迁移脚本，
postgres 容器不需要挂载初始化 SQL。

---

## 7. 服务器环境准备

### 7.1 前置条件

- 一台 Linux 云服务器（Ubuntu 20.04+ / CentOS 7+，本项目按 2核4G 规格调优）
- 能 SSH 登录（root 或有 sudo 权限）
- 至少 2GB 内存（Jenkins + Docker 需要一定资源）
- 域名 shennn.top 已解析到服务器 IP（A 记录：`@` 和 `www`）

### 7.2 SSH 连接服务器

```bash
ssh root@你的服务器IP
```

### 7.3 安装 Docker

```bash
# 1. 更新系统包
sudo apt update && sudo apt upgrade -y

# 2. 安装 Docker（官方脚本，最简单的方式）
curl -fsSL https://get.docker.com | sh

# 3. 将当前用户加入 docker 组（免 sudo 执行 docker 命令）
sudo usermod -aG docker $USER

# 4. 启动 Docker 并设置开机自启
sudo systemctl enable docker
sudo systemctl start docker

# 5. 验证安装
docker --version          # 预期输出: Docker version 27.x.x
docker compose version    # 预期输出: Docker Compose version v2.x.x
```

> 💡 如果是 CentOS，把 `apt` 换成 `yum`。

### 7.4 安装 Jenkins（Docker 方式）

```bash
# 1. 创建目录并给 jenkins 用户（UID 1000）权限
sudo mkdir -p /data/jenkins
sudo chown -R 1000:1000 /data/jenkins

# 2. 构建节点镜像（二选一）
cd /data/myblog

# 2-A. 首次安装
docker build -f deploy/jenkins/Dockerfile -t myblog-jenkins:lts .

# 2-B. 已有 Jenkins：复用当前基础镜像，避免替换节点时顺带升级 Jenkins
docker tag "$(docker inspect --format '{{.Image}}' jenkins)" myblog-jenkins-base:current
docker build \
  --build-arg JENKINS_IMAGE=myblog-jenkins-base:current \
  -f deploy/jenkins/Dockerfile \
  -t myblog-jenkins:lts .

# 3. 校验路由、创建专用代理网络，并取得 docker.sock 组 GID
ADMIN_ROUTE=$(sed -n 's/^ADMIN_ROUTE=//p' .env)
JENKINS_ROUTE=$(sed -n 's/^JENKINS_ROUTE=//p' .env)
case "$ADMIN_ROUTE" in
  /*/) echo "ADMIN_ROUTE 不能以 / 结尾" >&2; exit 1 ;;
  /api|/api/*|/) echo "ADMIN_ROUTE 与现有路由冲突" >&2; exit 1 ;;
  /*) ;;
  *) echo "ADMIN_ROUTE 必须以 / 开头" >&2; exit 1 ;;
esac
case "$JENKINS_ROUTE" in
  /*/) echo "JENKINS_ROUTE 不能以 / 结尾" >&2; exit 1 ;;
  /api|/api/*|/) echo "JENKINS_ROUTE 与现有路由冲突" >&2; exit 1 ;;
  /*) ;;
  *) echo "JENKINS_ROUTE 必须以 / 开头" >&2; exit 1 ;;
esac
case "$JENKINS_ROUTE/" in "$ADMIN_ROUTE/"*) echo "ADMIN_ROUTE 与 JENKINS_ROUTE 不能重叠" >&2; exit 1 ;; esac
case "$ADMIN_ROUTE/" in "$JENKINS_ROUTE/"*) echo "ADMIN_ROUTE 与 JENKINS_ROUTE 不能重叠" >&2; exit 1 ;; esac
docker network inspect myblog-jenkins-proxy >/dev/null 2>&1 \
  || docker network create myblog-jenkins-proxy
DOCKER_GID=$(stat -c '%g' /var/run/docker.sock)

# 4. 已有 Jenkins 先停止并删除旧容器；首次安装跳过
docker stop jenkins
docker rm jenkins

# 5. 启动 Jenkins 容器
docker run -d \
  --name jenkins \
  --restart unless-stopped \
  --network myblog-jenkins-proxy \
  -e JENKINS_OPTS="--prefix=${JENKINS_ROUTE}" \
  -v /data/jenkins:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  --group-add "$DOCKER_GID" \
  myblog-jenkins:lts

# 6. 验证节点工具链与 socket 权限
docker exec jenkins docker version
docker exec jenkins docker buildx version
docker exec jenkins docker compose version
docker inspect --format '{{json .NetworkSettings.Ports}}' jenkins
# 预期输出 {}，表示 Jenkins 没有发布任何宿主机端口
```

**参数解析：**

| 参数 | 含义 |
|------|------|
| `-d` | 后台运行 |
| `--name jenkins` | 容器名称 |
| `--restart unless-stopped` | 服务器重启后自动启动 |
| `--network myblog-jenkins-proxy` | 只允许同一专用网络中的 Nginx 访问 Jenkins 8080 |
| `-e JENKINS_OPTS="--prefix=${JENKINS_ROUTE}"` | 让 Jenkins 使用与 Nginx 一致的上下文路径 |
| `-v /data/jenkins:/var/jenkins_home` | 沿用现有 Jenkins Home 映射，保留任务、插件、凭据和构建记录 |
| `-v /var/run/docker.sock:/var/run/docker.sock` | 让 Jenkins 能控制宿主机的 Docker |
| `--group-add "$DOCKER_GID"` | 将 docker.sock 的宿主机组权限传给容器内 Jenkins 用户 |
| `myblog-jenkins:lts` | 内置 Docker CLI、Buildx 与 Compose 插件的项目节点镜像 |

> ⚠️ `-v $(which docker):/usr/bin/docker` 只会挂载 Docker 主程序。Compose v2 与
> Buildx 位于独立的 CLI 插件目录，因此 Jenkins 容器中会出现
> `docker: unknown command: docker compose`。工具链应在 Jenkins 节点镜像构建时安装，
> 不应由 CI/CD 流水线临时下载。

> ⚠️ 不需要修改现有 `/data/jenkins:/var/jenkins_home` 映射。CD 会把容器内的
> `$WORKSPACE` 换算成宿主机 `/data/jenkins/workspace/...`，并通过
> `HOST_PROJECT_DIR` 传给 Compose，确保 bind mount 指向宿主机真实文件。

> ⚠️ 本项目的 CI 需要跑 Testcontainers 集成测试（在 Docker 里再起 PG/Redis 容器），
> 挂载 `docker.sock` 是硬性前提；同时 Jenkins 与部署目标为**同一台机器**，
> CD 直接在本机执行 `docker compose up -d`，无需 SSH 到远程。

已有 Jenkins 容器升级时先备份 `/data/jenkins`，再执行步骤 2-B、3、4、5、6。
删除容器不会删除 bind mount 中的数据，但不要删除宿主机 `/data/jenkins`。

### 7.5 通过域名路由访问 Jenkins

在生产 `.env` 中配置 Jenkins 路由，路径必须以 `/` 开头且不能以 `/` 结尾：

```bash
BLOG_DOMAIN=example.com
JENKINS_ROUTE=/jenkins
```

Nginx 使用 HTTPS 将 `https://${BLOG_DOMAIN}${JENKINS_ROUTE}/` 转发到专用网络中的
`jenkins:8080`。Jenkins 不发布主机端口，公网只能经过 Nginx 访问。

部署配置并验证：

```bash
cd /data/myblog
docker compose up -d nginx
docker compose exec -T nginx getent hosts jenkins
curl -I "https://${BLOG_DOMAIN}${JENKINS_ROUTE}/login"

# 删除旧 Jenkins 端口的公网规则，只保留项目需要的 22、80、443
sudo ufw delete allow 8888/tcp
sudo ufw delete allow 50000/tcp
```

> 路由前缀只是部署配置，不是访问密码。Jenkins 仍须关闭匿名管理权限、使用强密码，
> 并及时更新 Jenkins 核心与插件。

### 7.6 获取 Jenkins 初始密码

```bash
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
# 输出类似: a1b2c3d4e5f67890abcdef1234567890，复制这个密码！
```

### 7.7 初始化 Jenkins

1. 浏览器访问 `https://<BLOG_DOMAIN><JENKINS_ROUTE>/`
2. 粘贴刚才获取的初始密码
3. 选择 **Install suggested plugins**（安装推荐插件）
4. 等待插件安装完成（可能需要几分钟）
5. 创建管理员账号（记住用户名和密码！）
6. Jenkins URL 设置为 `https://<BLOG_DOMAIN><JENKINS_ROUTE>/`，点击完成

### 7.8 安装所需插件

1. **Manage Jenkins** → **Plugins** → **Available plugins**
2. 搜索并安装：
   - `GitHub`（GitHub 集成与 webhook）
   - `Pipeline`（通常默认已装）
   - `Docker Pipeline`（Pipeline 中使用 docker agent，CI 需要）
   - `JUnit`、`JaCoCo`（测试报告与覆盖率展示，CI 需要）
3. 点击 **Install without restart**，等待完成

### 7.9 配置 GitHub 凭据

1. GitHub → Settings → Developer settings → Personal access tokens，生成 token（勾选 `repo`、`admin:repo_hook`）
2. Jenkins → **Manage Jenkins** → **Credentials** → **Global credentials** → **Add Credentials**
   - 拉代码：私有仓库用 `SSH Username with private key` 或 `Username with password`（token 当密码）
   - webhook 鉴权按 GitHub 插件向导配置
3. 在 GitHub 仓库 → **Settings** → **Webhooks** 添加：
   - Payload URL：`https://<BLOG_DOMAIN><JENKINS_ROUTE>/github-webhook/`
   - Content type：`application/json`
   - 事件：`Just the push event`

### 7.10 部署前置：放置机密文件

代码 clone 到服务器后（假设 `/data/MyBlog`），有两类文件不随 git 分发，需手动放置：

```bash
cd /data/MyBlog

# 1. 生产环境变量（按 6.4 节填写）
cp .env.example .env && vim .env
chmod 600 .env

# 2. SSL 证书（从证书提供商下载 shennn.top 的 nginx 格式证书）
mkdir -p nginx/certs
# 放入 shennn.top.pem（含中间证书的完整链）和 shennn.top.key
chmod 600 nginx/certs/shennn.top.key
```

证书续期后替换这两个文件并执行 `docker compose restart nginx` 即可。

---

## 8. Jenkins CI Pipeline 配置（Jenkinsfile.ci）

CI 目标：**每次任意分支 push，自动跑编译 + 静态检查 + 全部测试**，不部署。

### 8.1 创建 Multibranch Pipeline

1. Jenkins 首页 → **New Item**，名称 `myblog-ci`，类型选 **Multibranch Pipeline**
2. **Branch Sources** → 添加 GitHub（或 Git）源：
   - Repository URL：`git@github.com:<你的用户名>/MyBlog.git`
   - Credentials：选 7.9 配置的凭据
   - Behaviours：**Discover Branches**（所有分支）
3. **Build Configuration** → Script Path 填：`Jenkinsfile.ci`
4. 保存。配置 `https://<BLOG_DOMAIN><JENKINS_ROUTE>/github-webhook/` 后，push 自动触发构建。

### 8.2 Jenkinsfile.ci 详解

```groovy
pipeline {
    agent none

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    stages {
        stage('Backend CI') {
            agent {
                docker {
                    // 挂载 docker.sock 供 Testcontainers 起 PG/Redis；
                    // 挂载 ~/.m2 做 Maven 依赖缓存，避免每次全量下载
                    image 'maven:3.9-eclipse-temurin-21'
                    args '-v /var/run/docker.sock:/var/run/docker.sock -v $HOME/.m2:/root/.m2'
                    reuseNode true
                }
            }
            steps {
                dir('backend-java') {
                    // verify = Checkstyle + 单元测试 + *IT 集成测试 + SpotBugs + JaCoCo
                    sh 'mvn -B verify'
                }
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'backend-java/target/surefire-reports/*.xml, backend-java/target/failsafe-reports/*.xml'
                    jacoco execPattern: 'backend-java/target/jacoco.exec',
                           classPattern: 'backend-java/target/classes',
                           sourcePattern: 'backend-java/src/main/java'
                }
            }
        }

        stage('Frontend CI') {
            parallel {
                stage('blog (myblog/)') {
                    agent { docker { image 'node:20-alpine'; reuseNode true } }
                    steps {
                        dir('myblog') {
                            sh 'npm ci --no-audit --no-fund'
                            sh 'npm run lint'
                            sh 'npm run build'   // 内含 vue-tsc 类型检查
                        }
                    }
                }
                stage('admin (admin/)') {
                    agent { docker { image 'node:20-alpine'; reuseNode true } }
                    steps {
                        dir('admin') {
                            sh 'npm ci --no-audit --no-fund'
                            sh 'npm run lint'
                            sh 'npm run build'
                        }
                    }
                }
            }
        }
    }
}
```

**与本项目相关的关键设计：**

| 设计 | 说明 |
|------|------|
| `agent none` + 阶段级 docker agent | 每个阶段在干净的容器里跑，不污染 Jenkins 节点，也不用在节点上装 JDK/Node |
| `docker.sock` 挂载 | 后端 `*IT` 集成测试基于 Testcontainers，需要真实起 PostgreSQL/Redis 容器 |
| `~/.m2` 挂载 | Maven 依赖缓存，CI 从几分钟缩短到几十秒 |
| `mvn -B verify` | 一条命令跑完 Checkstyle + 单测 + 集成测试 + SpotBugs + JaCoCo |
| `parallel` | 前台/后台两个前端并行构建，节省时间 |
| `junit` / `jacoco` | 测试结果与覆盖率在 Jenkins 页面可视化 |

---

## 9. Jenkins CD Pipeline 配置（Jenkinsfile.cd）

CD 目标：**手动触发**，本机构建镜像 → 打版本 TAG → 重建容器 → 健康检查 → 失败可回滚。

### 9.1 创建 CD Pipeline

1. Jenkins 首页 → **New Item**，名称 `myblog-cd`，类型选 **Pipeline**
2. **General** → 勾选 `This project is parameterized`
   - 字符串参数：`IMAGE_TAG`，默认留空（说明：留空则使用构建号 BUILD_NUMBER）
3. **Pipeline** → Definition 选 `Pipeline script from SCM`
   - SCM：Git，Repository URL 同 CI，Branch：`*/master`
   - Script Path：`Jenkinsfile.cd`
4. 保存。发布时点 **Build with Parameters**，可指定版本号（如 `v1.0.0`）或留空用构建号。

### 9.2 Jenkinsfile.cd 四个阶段

```groovy
// 阶段 1：Build Images —— 本机构建三个镜像
//   后端镜像构建期内会执行全部单元测试，测试失败则构建失败
sh 'docker compose build backend frontend-web frontend-admin'

// 阶段 2：Tag Images —— 打上版本标签，保留历史 TAG 便于回滚
sh """
    docker tag myblog-api:latest   myblog-api:${TAG}
    docker tag myblog-web:latest   myblog-web:${TAG}
    docker tag myblog-admin:latest myblog-admin:${TAG}
"""

// 阶段 3：Deploy —— 用环境变量覆盖 compose 默认镜像名，指向本次 TAG 的镜像
sh """
    API_IMAGE=myblog-api:${TAG} \\
    WEB_IMAGE=myblog-web:${TAG} \\
    ADMIN_IMAGE=myblog-admin:${TAG} \\
    docker compose up -d
"""

// 阶段 4：Health Check —— 轮询 compose 健康状态，超时判定部署失败
sh '''
    for svc in backend frontend-web frontend-admin nginx; do
        # 每 5s 查一次 docker inspect 的 Health.Status，最多 30 次
        ...
    done
'''
```

**版本化部署的核心机制**：

```
compose 里写的是  image: ${API_IMAGE:-myblog-api:latest}
                         │
                         └─ CD 部署时注入 API_IMAGE=myblog-api:42
                            → 容器跑的是"42 号构建"这个不可变版本，
                              而不是随时被覆盖的 latest
```

**回滚**：镜像 TAG 都保留在本机，部署失败或线上出问题时，用上一次的 TAG 重新部署即可：

```bash
API_IMAGE=myblog-api:<上一个TAG> \
WEB_IMAGE=myblog-web:<上一个TAG> \
ADMIN_IMAGE=myblog-admin:<上一个TAG> \
docker compose up -d
```

### 9.3 一次完整发布流程

```
开发: git push → CI 自动跑（编译+测试）
                 ↓ 通过
发布: Jenkins → myblog-cd → Build with Parameters（填 TAG 或留空）
                 ↓
      构建镜像（后端构建期跑单测）→ 打 TAG → 重建容器 → 健康检查
                 ↓
      全部 healthy → 发布完成 ✅
      任一失败 → 看上节回滚命令 ↩️
```

### 9.4 数据库变更的发布

schema 由 Flyway 管理：把新的迁移脚本（`V<n>__xxx.sql`）放进
`backend-java/src/main/resources/db/migration/`，随代码提交。
CD 重建 backend 容器后，应用启动时自动执行新迁移——**不需要手动连数据库执行 SQL**。

---

## 10. 敏感信息管理 + 通知优化

### 10.1 敏感信息处理

**原则**：密码、密钥一律不进 Git。

- `.env`（数据库/Redis 密码、JWT 密钥、OSS AK/SK、初始管理员密码）——gitignore，只存在于服务器
- `nginx/certs/`（SSL 私钥）——gitignore，手动放置
- `application.yml` 中用 `${VAR:default}` 引用环境变量，默认值为本地开发专用

### 10.2 Jenkins 凭据管理

不要在 Jenkinsfile 中硬编码密码！本项目的两个 Jenkinsfile 都**不含任何机密**：
机密全在服务器仓库目录的 `.env` 里，compose 自动读取。Jenkins 侧只需要配
GitHub 拉代码的凭据（7.9 节）。

如果以后需要更多凭据（如通知 webhook token），用 Jenkins Credentials：

```groovy
environment {
    NOTIFY_TOKEN = credentials('my-notify-token')
}
```

### 10.3 构建通知（可选）

安装 **Email Extension Plugin** 后，在 Jenkinsfile 的 `post` 部分添加邮件通知：

```groovy
post {
    failure {
        emailext(
            subject: "❌ 构建失败: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
            body: "构建失败，请检查: ${env.BUILD_URL}",
            to: 'your-email@example.com'
        )
    }
}
```

---

## 11. 常见问题排查

### Docker 构建失败

```bash
# 无缓存重新构建
docker compose build --no-cache

# 查看容器日志
docker compose logs backend
docker compose logs frontend-web
docker compose logs nginx

# 进入容器调试
docker exec -it myblog-backend-1 sh
```

### 改了 docker-compose.yml 后报错 "depends on undefined service"

服务改名后必须同步修改**所有引用处**：
`depends_on`、环境变量里的主机名（如 `DB_HOST`）、`nginx/default.conf.template` 的上游、
前端容器 `nginx.conf` 的 `proxy_pass`、Jenkinsfile.cd 里的服务清单。

### 镜像"串味"：本地跑的是旧镜像

`docker compose up -d` **不会**重新构建镜像，只复用本地同名镜像。
代码变更后要 `docker compose build <服务>` 或 `up -d --build`。
可用 `docker history myblog-web:latest` 确认镜像内容是否符合当前 Dockerfile。

### 本地 443 被其他软件抢占

Windows 上 VMware Workstation Server（vmware-hostd.exe）会抢占 443，
导致浏览器访问被 VMware 截走、根本到不了 nginx 网关。排查：

```bash
netstat -ano | findstr :443     # 看占用 443 的 PID
tasklist | findstr <PID>        # 确认进程
# 管理员终端：net stop VMWareHostd
```

### 本地 8000 被 Docker 占用，IDEA 起不来后端

compose 把 backend 映射到 `127.0.0.1:8000`，本地再启动 Spring Boot 会
`BindException: Address already in use`。本地调试时 `docker compose stop backend`
（postgres/redis 保留，本地后端通过 15432/6379 直连）。

### PostgreSQL 连接失败

```bash
# 检查容器健康状态
docker compose ps postgres

# 手动连接测试（密码见服务器 .env）
docker exec -it myblog-postgres-1 psql -U myblog -d myblog -c "SELECT 1"
```

### 前端页面空白 / 刷新 404

- 检查容器内 nginx 配置中 `try_files $uri $uri/ /index.html;` 是否存在
- 检查构建产物是否进镜像：`docker exec -it myblog-frontend-web-1 ls /usr/share/nginx/html`

### 证书问题

- 网关启动报 `cannot load certificate`：`nginx/certs/` 下证书文件缺失或文件名不符
- 浏览器证书警告：本地用 `https://localhost` 访问时证书域名不匹配属正常现象；
  正式访问用 `https://shennn.top`

### 查看 Jenkins 日志

```bash
docker logs jenkins
# 构建日志：Jenkins Web 界面 → 任务 → 构建历史 → Console Output
```

---

## 12. 文件清单

本项目 CI/CD 基础设施涉及的文件：

| 文件路径 | 类型 | 说明 |
|---------|------|------|
| `.gitignore` | 配置 | 排除 .env、certs、node_modules 等 |
| `.env.example` | 模板 | 环境变量模板 |
| `.env` | 机密 | 实际环境变量（不上传） |
| `docker-compose.yml` | 编排 | 六服务编排（postgres/redis/backend/frontend-web/frontend-admin/nginx） |
| `backend-java/Dockerfile` | 构建 | 后端多阶段构建（构建期跑单测） |
| `backend-java/.dockerignore` | 构建 | 后端构建上下文忽略 |
| `backend-java/docker/maven-settings.xml` | 构建 | Maven 国内镜像加速 |
| `myblog/Dockerfile`、`myblog/nginx.conf` | 构建 | 前台镜像 + 容器内 nginx |
| `admin/Dockerfile`、`admin/nginx.conf` | 构建 | 后台镜像 + 容器内 nginx |
| `nginx/default.conf.template` | 网关 | 网关配置模板（envsubst 渲染域名） |
| `nginx/certs/` | 机密 | SSL 证书（手动放置） |
| `Jenkinsfile.ci` | 流水线 | CI：push 触发，编译+静态检查+全部测试 |
| `Jenkinsfile.cd` | 流水线 | CD：手动触发，构建→打 TAG→部署→健康检查→可回滚 |
| `deploy/README.md` | 文档 | OSS/CDN 配置与生产部署细节 |

---

## 快速命令参考

```bash
# === 本地开发 ===
cd myblog && npm run dev                     # 启动前台（5173）
cd admin && npm run dev                      # 启动后台管理（5174）
cd backend-java && mvn spring-boot:run       # 启动后端（8000，注意先停 docker 里的 backend）

# === Docker 本地测试 ===
docker compose config                        # 校验编排配置
docker compose build                         # 构建所有镜像
docker compose up -d                         # 启动所有服务
docker compose ps                            # 查看服务状态与健康度
docker compose logs -f backend               # 查看后端日志
docker compose down                          # 停止所有服务（数据卷保留）

# === 服务器部署 ===
ssh root@服务器IP                             # 连接服务器
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword   # Jenkins 初始密码
# 发布：Jenkins → myblog-cd → Build with Parameters

# === 版本回滚 ===
API_IMAGE=myblog-api:<TAG> WEB_IMAGE=myblog-web:<TAG> ADMIN_IMAGE=myblog-admin:<TAG> \
  docker compose up -d

# === 故障排查 ===
docker logs jenkins                          # Jenkins 日志
docker compose logs backend                  # 后端日志
docker compose ps                            # 各服务健康状态
netstat -tlnp                                # 端口占用（Linux）
```
