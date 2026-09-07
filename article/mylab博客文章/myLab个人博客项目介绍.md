# MyBlog：个人博客系统全栈实践

![封面](images/cover.png)

> 项目地址：<https://github.com/SHENNN-ysy/myLab>

myLab 是我的个人作品集 + 博客系统，包含**访客博客前台**、**管理后台**和 **Spring Boot API** 三大部分。它支持本地 Docker 一键部署，也支持基于 Jenkins + 私有 Registry 的生产级 CI/CD。这篇文章简单介绍一下这个项目的整体结构、架构设计和主要功能。

注：随着学习实践的进行，该项目的架构和功能等可能会发生变化，本篇文章基于当前项目情况编写，版本号暂且记录为1.0吧

## 技术栈一览

| 模块 | 技术选型 |
| --- | --- |
| 博客前台 | Vue 3、TypeScript、Vite、GSAP、Tailwind CSS 4 |
| 管理后台 | Vue 3、TypeScript、Vite、Ant Design Vue 4、ECharts 5 |
| 后端 | Spring Boot 3.5、Java 21、MyBatis-Plus |
| 数据与缓存 | PostgreSQL 16、Redis 7、Flyway |
| 部署 | Docker、Docker Compose、Nginx、Jenkins |

## 整体架构

![整体架构](https://ysy-myblog.oss-cn-guangzhou.aliyuncs.com/mylab-post/2026/08/409d9c747df5459c9439f1efe281f83e.png?Expires=1788195478&OSSAccessKeyId=LTAI5t7JhA6sGRZKP3xDM5UC&Signature=z8TVVIYSY3nRwxLwUnnmdMByDmg%3D)

系统是典型的前后端分离三层结构：两个前端应用通过 Nginx 网关访问统一的 REST API，后端再对接 PostgreSQL、Redis 和对象存储（OSS + CDN）。

所有流量统一经过 **Nginx 网关**（生产环境 443 HTTPS，唯一对外入口）：

- `/` → 博客前台静态站点（`frontend-web` 容器）
- `/admin/` → 管理后台静态站点（`frontend-admin` 容器，路径前缀可配置）
- `/api/` → Spring Boot 后端（仅监听内网，不直接暴露）

后端向下连接 **PostgreSQL 16**（Flyway 管理表结构迁移）和 **Redis 7**（缓存、限流、互动计数）。前端两个静态站点和后端都不暴露主机端口，只能在内网被 Nginx 访问，攻击面最小化。

项目提供两套编排：

- **本地一键部署**：根目录 `docker-compose.yml`，从源码构建，仅 HTTP，适合开发调试；
- **生产部署**：`deploy/docker-compose.yml`，纯镜像部署（image-only），由 CI 构建镜像推送到私有 Registry，CD 按不可变 tag 拉取发布。

## 项目结构

仓库采用单仓多模块（monorepo）组织，各模块职责清晰：

```text
MyBlog/
├── myblog/                  # 博客前台（Vue SPA）
├── admin/                   # 管理后台前端（Vue SPA）
├── backend-java/            # Spring Boot 后端
│   └── src/main/java/com/myblog/
│       ├── controller/      # REST 接口 + 全局异常处理
│       ├── application/     # 业务层（service / model / port）
│       ├── infrastructure/  # 适配层（Persistence / Redis / JWT / OSS）
│       ├── starter/         # 装配层（Security / 配置 / 初始化）
│       └── common/          # Result / ErrorCode / 常量
├── nginx/                   # 本地 HTTP 网关配置
├── deploy/                  # 生产 Compose、HTTPS Nginx、Registry、Jenkins
├── docs/                    # API 文档、数据库设计、CI/CD 设计
├── scripts/                # Registry 清理、OSS 上传等运维脚本
├── docker-compose.yml       # 本地一键部署
└── Jenkinsfile.*            # CI / CD / Registry 清理
```

- **myblog/**：访客看到的页面，目前包含首页、MyLab 列表、文章详情等视图，用 GSAP 做动效。
- **admin/**：站长使用的管理端，带登录页和数据统计图表。
- **backend-java/**：后端主体，约 180 个 Java 文件，是仓库中体量最大的模块。

## 后端架构：模块化单体 + 端口适配器

![后端分层架构](images/backend-layers.png)

后端采用**单工程、模块化单体**设计，代码收敛到五个顶层包，并保持严格的依赖方向：

| 包 | 职责 |
| --- | --- |
| `controller` | REST 接口与全局异常处理，只做 HTTP 协议转换，禁止业务逻辑，禁止直接碰 Mapper/Redis/OSS |
| `application` | 业务核心，不感知基础设施实现；业务模块按域划分：`auth`（认证）、`user`（用户管理）、`content`（内容）、`file`（文件）、`engagement`（互动计数）、`system`（系统信息） |
| `infrastructure` | 基础设施适配层，MyBatis、PostgreSQL、Redis、JWT、OSS 等端口实现 |
| `common` | 公共契约层，各层复用的稳定类型（异常、枚举、结果包装等），不反向依赖任何层 |
| `starter` | 装配层，负责 Security、过滤器、Bean 配置与启动初始化。 |

几个值得一提的设计点：

- **依赖规则由测试兜底**：分层约束（如 controller 不能碰 Mapper、应用服务不依赖 Web 对象）由 ArchUnit 的 `LayeredArchitectureTest` 自动检查，违规直接构建失败。
- **OSS/CDN 边界清晰**：文件上传走 `FileService → ObjectStorage 端口 → OSS 适配器`，凭证和域名都封装在基础设施层，以后换存储厂商不影响业务代码。
- **数据库版本化**：用 Flyway 管理表结构（baseline + 增量迁移），首次启动自动建表。

## 主要功能

### 1. 版本化内容系统（核心）

七个内容模块（首页、MyLab 文章、关于、足迹、爱好、技能、Vibe）共享同一套版本化机制：

- 每个模块同一时刻**至多一个草稿 + 一个线上版本**；
- **发布即生成不可变历史版本**，历史版本内容可随时恢复到草稿继续编辑；
- 线上版本只读，当前线上版本不可删除，须先下线为历史版本才能删除；
- MyLab 模块支持 Markdown 正文，公开列表不返回正文，单篇详情按需获取。

### 2. 认证与安全

- JWT **双令牌**（access + refresh），退出登录把 jti 写入 Redis 黑名单吊销；
- 密码 BCrypt 加密（强度 12），初始管理员仅在系统无用户时创建一次；
- 登录接口独立更严的限流阈值 + 全局限流，均基于 Redis，故障时 fail-open；

### 3. 访客互动

- 访客可以浏览内容、点赞互动；系统记录访问日志并统计 PV/UV，后台用 ECharts 展示访问分析；
- 浏览量、点赞数通过 **Redis Lua 脚本原子操作**实现，定时快照落库到 PostgreSQL；
- Redis 故障时自动降级读快照，保证页面数据始终可用；
- 访客标识经 HMAC 哈希，互动明细只存 Redis（72h TTL）不落库，保护访客隐私。

### 4. 文件管理

文件元数据入库，实体文件上传 OSS，支持预签名 URL；管理后台可在线管理文件的上传与删除。

## 部署与 CI/CD

项目提供**两套互不干扰的部署轨道**：

| 场景 | 入口 | 特点 |
| --- | --- | --- |
| 本地一键部署 | `docker-compose.yml` | 从源码构建，HTTP 访问，不依赖 Jenkins/域名/证书 |
| 生产 CI/CD | `deploy/docker-compose.yml` | CI 构建镜像推送私有 Registry，CD 只拉镜像，HTTPS + Jenkins |

本地体验非常简单：

```bash
cp .env.example .env      # 修改密码、JWT 密钥、OSS 配置
docker compose up -d --build
```

生产流水线则是：`PR 合并 → CI 质量门（单测 + Testcontainers 集成测试 + Checkstyle + SpotBugs + JaCoCo + ArchUnit）→ 构建镜像 → 推送私有 Registry → CD 校验并拉取指定 tag 部署`，全程无需在服务器上构建源码。

## 总结

这个博客项目从开始到现在前前后后经过了两个月多吧，代码的编写基本都是AI来完成，原来最初的博客版本因为页面的设计AI味太浓被放弃了，因为我想做的这不仅是一个博客还是一个个人网站，希望能有点个人特色，你能想象得到当我看到我的博客和别人的Vibe coding项目页面设计的风格一模一样时的心情吗？为了让我的博客避免变成“风味博客”，我兜兜转转，然后自己再不断的翻看别人的博客，去翻看一些设计网站，去参考借鉴他们的风格，最后靠AI神力将一些优秀的设计落地到我的项目上，再加上因为一些其他事情暂时搁置了项目的开发，总的算下来前期的页面设计这一环节就花了一半以上的时间。

我这个项目的名字叫mylab，顾名思义这个项目也是我的实验室，也是我为后续更深层次的学习实践搭建的一个场景，这也是前面我说的该项目的架构和功能等可能会发生变化的原因。当然后面的所有的变化我都会写出一篇“实验记录”出来作为这个博客的文章，希望以此来激励自己继续学下去，这也是搭建这个项目的一点初心。

这对我而言这不只是一个博客，还是一个介绍自己的个人网站，更是以后学习实践的载体，目前该项目从 Vue3 博客前台后台双前端、Spring Boot 后端，到 Docker 双轨部署和 Jenkins 流水线，覆盖了个人项目从开发到上线的全链路。最后特此感谢所有做出开源贡献的人，本项目最终也会开源，如果你也想搭一个属于自己的博客系统，欢迎参考或直接 Star ⭐

> 项目地址：<https://github.com/SHENNN-ysy/myLab>
