# MyLab：个人博客系统全栈实践

> 项目地址：<https://github.com/SHENNN-ysy/myLab>

myLab 是我的个人作品集 + 博客系统，包含**访客博客前台**、**管理后台**和 **Spring Boot API** 三大部分。它支持本地 Docker 一键部署，也支持基于 Jenkins + 私有 Registry 的生产 CI/CD。这篇文章简单介绍一下这个项目的整体结构、架构设计和主要功能。

注：随着学习实践的进行，该项目的架构和功能还会继续变化。本文依据 2026 年 9 月的当前仓库状态整理。

## 技术栈一览

| 模块 | 技术选型 |
| --- | --- |
| 博客前台 | React 18、TypeScript、Vite、React Router、Zustand、GSAP、Tailwind CSS 4 |
| 管理后台 | React 18、TypeScript、Vite、React Router、Zustand、Ant Design 5、ECharts 5 |
| 后端 | Spring Boot 3.5.16、Java 21、MyBatis-Plus、Redis 会话、OSS |
| 数据与缓存 | PostgreSQL 16、Redis 7、Flyway、Redis Cache-Aside |
| 测试与质量 | JUnit、Testcontainers、ArchUnit、Checkstyle、SpotBugs、JaCoCo |
| 部署 | Docker、Docker Compose、Nginx、Jenkins |

## 整体架构

![整体架构](https://cdn.shennn.top/mylab/7a40f445-2308-4aad-9897-cac03832538b.png)

系统采用前后端分离部署架构：两个 React SPA 通过 Nginx 网关访问统一的 REST API，后端再对接 PostgreSQL、Redis 和对象存储（OSS + CDN）。

所有流量统一经过 **Nginx 网关**（生产环境 443 HTTPS，唯一对外入口）：

- `/` → 博客前台静态站点（`frontend-web` 容器）
- `${ADMIN_ROUTE}/` → 管理后台静态站点（`frontend-admin` 容器，默认 `/admin/`）
- `/api/` → Spring Boot 后端（公网不直接暴露，主机侧仅绑定 `127.0.0.1:8000`）

后端向下连接 **PostgreSQL 16**（Flyway 管理表结构迁移）和 **Redis 7**（缓存、限流、互动计数）。两个前端容器不发布主机端口；后端、PostgreSQL 和 Redis 只绑定宿主机回环地址，公网唯一入口仍是 Nginx 的 80/443 端口。

项目提供两套编排：

- **本地一键部署**：根目录 `docker-compose.yml`，从源码构建，仅 HTTP，适合开发调试；
- **生产部署**：`deploy/docker-compose.yml`，纯镜像部署（image-only），由 CI 构建镜像推送到私有 Registry，CD 按不可变 tag 拉取发布。

## 项目结构

仓库采用单仓多模块（monorepo）组织，各模块职责清晰：

```text
MyBlog/
├── myblog/                  # 博客前台（React SPA）
├── admin/                   # 管理后台前端（React SPA）
├── backend-java/            # Spring Boot 后端
│   └── src/main/java/com/myblog/
│       ├── controller/      # REST 接口 + 全局异常处理
│       ├── application/     # 业务层（service / model / port）
│       ├── infrastructure/  # 适配层（Persistence / Redis 会话 / OSS）
│       ├── starter/         # 装配层（Security / 配置 / 初始化）
│       └── common/          # Result / ErrorCode / 常量
├── nginx/                   # 本地 HTTP 网关配置
├── deploy/                  # 生产 Compose、HTTPS Nginx、Registry、Jenkins
├── docs/                    # API 与错误码文档
├── scripts/                # Registry 查询与清理脚本
├── docker-compose.yml       # 本地一键部署
├── Jenkinsfile.ci           # 质量门、镜像构建与推送
├── Jenkinsfile.cd           # 指定不可变 tag 部署
└── Jenkinsfile.registry-cleanup # Registry 定时清理
```

- **myblog/**：访客看到的 React 应用，目前包含首页、MyLab 列表和文章详情等视图，使用 React Router 管理路由、Zustand 管理状态，并用 GSAP 编排动效。
- **admin/**：站长使用的 React 管理端，基于 Ant Design 5，包含登录、仪表盘、版本化内容编辑和文件管理。
- **backend-java/**：后端主体，采用 Java 21 和 Spring Boot，是仓库中体量最大的模块。

## 后端架构：模块化单体 + 端口适配器

![后端分层架构](https://cdn.shennn.top/mylab/72036d87-8320-48ca-8386-239c4d8a3402.png)

后端采用**单工程、模块化单体**设计，代码收敛到五个顶层包，并保持严格的依赖方向：

| 包 | 职责 |
| --- | --- |
| `controller` | REST 接口与全局异常处理，只做 HTTP 协议转换，禁止业务逻辑，禁止直接碰 Mapper/Redis/OSS |
| `application` | 业务核心，不感知基础设施实现；业务模块按域划分：`auth`（认证）、`user`（用户管理）、`content`（内容）、`file`（文件）、`engagement`（互动计数）、`system`（系统信息） |
| `infrastructure` | 基础设施适配层，MyBatis、PostgreSQL、Redis 会话、OSS 等端口实现 |
| `common` | 公共契约层，各层复用的稳定类型（异常、枚举、结果包装等），不反向依赖任何层 |
| `starter` | 装配层，负责 Security、过滤器、Bean 配置与启动初始化 |

几个值得一提的设计点：

- **依赖规则由测试兜底**：分层约束（如 controller 不能碰 Mapper、应用服务不依赖 Web 对象）由 ArchUnit 的 `LayeredArchitectureTest` 自动检查，违规直接构建失败。
- **OSS/CDN 边界清晰**：文件上传走 `FileService → ObjectStorage 端口 → OSS 适配器`，凭证和域名都封装在基础设施层，以后换存储厂商不影响业务代码。
- **数据库版本化**：用 Flyway 管理表结构，全新数据库执行 `V1__baseline.sql`，后续变更通过 V2～V4 增量迁移演进。
- **公开内容缓存**：聚合公开内容和 MyLab 单篇详情使用 Redis Cache-Aside，缓存未命中时回源 PostgreSQL，发布或下线后主动失效缓存。

## 主要功能

### 1. 版本化内容系统（核心）

七个内容模块（首页、关于、技能、足迹、爱好、Vibe、MyLab）共享同一套版本化机制：

- 每个模块同一时刻**至多一个草稿 + 一个线上版本**；
- 草稿必须填写版本名称和描述，保存时通过 `expected_updated_at` 做乐观锁校验，避免并发覆盖；
- **发布即生成不可变历史版本**，历史版本可原地恢复为草稿，原草稿转为归档；
- 当前草稿也可以手动归档，不影响线上内容，也不会自动创建新草稿；
- 线上版本只读，当前线上版本不可删除，须先下线为历史版本才能删除；
- MyLab 用 `PROJECT/ARTICLE` 两种卡片统一承载首页项目和文章；`project_show_order` 为空时，项目仍在 MyLab 列表展示，但不会出现在首页项目区；
- MyLab 的 Markdown 正文存入数据库并参与版本复制，公开列表不返回正文，单篇详情按 `post_key` 获取。

### 2. 认证与安全

- 管理后台使用随机 UUID Bearer Token，Redis 只保存 SHA-256 摘要和会话 Hash，空闲 8 小时滑动过期；
- 用户会话通过 ZSet 反向索引，修改用户名、密码、角色、启用状态或删除用户时会踢出该用户全部会话；登录与敏感写操作使用 PostgreSQL 行锁规避并发竞态；
- Redis 会话服务不可用时登录和后台受保护接口返回 503，公开博客与访客 HMAC 互动不受影响；
- 密码 BCrypt 加密（强度 12），初始管理员仅在系统无用户时创建一次；
- 登录接口独立更严的限流阈值 + 全局限流，均基于 Redis，故障时 fail-open；

### 3. 访客互动

- 访客可以浏览内容、点赞互动；系统记录访问日志并统计 PV/UV，后台用 ECharts 展示访问分析；
- 浏览量、点赞数通过 **Redis Lua 脚本原子操作**实现，定时快照落库到 PostgreSQL；
- Redis 故障时自动降级读快照，保证页面数据始终可用；
- 访客标识经 HMAC 哈希，互动明细只存 Redis（72h TTL）不落库，保护访客隐私。

### 4. 文件管理

文件元数据存入 PostgreSQL，实体文件上传 OSS；公开图片通过 OSS/CDN URL 访问，其他文件可以获取限时预签名 URL。管理后台支持上传、选择和删除文件，新上传对象使用“业务目录/UUID.扩展名”的稳定 key。

## 部署与 CI/CD

项目提供**两套互不干扰的部署轨道**：

| 场景 | 入口 | 特点 |
| --- | --- | --- |
| 本地一键部署 | `docker-compose.yml` | 从源码构建，HTTP 访问，不依赖 Jenkins/域名/证书 |
| 生产 CI/CD | `deploy/docker-compose.yml` | CI 构建镜像推送私有 Registry，CD 只拉镜像，HTTPS + Jenkins |

本地体验非常简单：

```bash
cp .env.example .env      # 修改密码、访客哈希密钥、OSS 配置
docker compose up -d --build
```

生产流水线则是：`PR 合并到 master → CI 质量门（单测 + Testcontainers 集成测试 + Checkstyle + SpotBugs + JaCoCo + ArchUnit）→ 构建三个应用镜像 → 推送私有 Registry → 手动选择不可变 tag 触发 CD → 拉取并部署`。生产 Compose 不包含 `build`，部署阶段不会临时编译源码；Registry 为三个应用仓库分别保留最近 5 个 tag，并额外保护当前成功部署版本。

## 总结

这个博客项目从开始到现在前前后后经过了两个月多吧，代码的编写基本都是AI来完成，原来最初的博客版本因为页面的设计AI味太浓被放弃了，因为我想做的这不仅是一个博客还想做的是我的个人网站，希望能有点个人特色，我还能清晰的记得当我看到我的博客和别人的Vibe coding项目页面设计的风格一模一样时的心情，都是一个模型甚至可能都是一个前端的skills做出来的东西，越看越不是滋味，为了让我的博客避免变成“风味博客”，我兜兜转转，然后自己再不断的翻看别人的博客，去翻看一些设计网站，去参考借鉴他们的风格，最后靠AI神力将一些优秀的设计弄到我的项目上（感谢AI感谢大家🙏），然后再加上因为一些其他事情暂时搁置了项目的开发，总的算下来前期的页面设计这一环节就花了一半以上的时间。

我这个项目的名字叫mylab，顾名思义这个项目也是我的实验室，也是我为后续更深层次的学习实践搭建的一个场景，这也是前面我说的该项目的架构和功能等可能会发生变化的原因。当然后面的所有的变化我都会写出“实验记录”出来作为这个博客的文章，希望以此来激励自己继续学下去，这也是搭建这个项目的一点初心。

对我而言，这不只是一个博客，还是一个介绍自己的个人网站，更是以后学习实践的载体。目前项目已经走过博客前台原型设计、React 前端重构、管理后台与 Spring Boot 后端开发，再到 Docker 部署和 Jenkins 流水线搭建，这个过程覆盖了个人项目从开发到上线的全链路。后续我也会继续写文章，作为学习过程的归纳总结。项目代码已经放在 GitHub 上持续更新，最后特此感谢所有做出开源贡献的人。如果你也想搭一个属于自己的博客系统，欢迎参考或直接 Star ⭐

> 项目地址：<https://github.com/SHENNN-ysy/myLab>
