# MyLab 学习实验记录 ：从 git pull 到 CI/CD 流水线

> 这不是一篇部署手册，而是一次**学习实验**的记录。实验对象是我的个人博客 MyBlog；实验目标很明确：通过一次真实改造，掌握 Jenkinsfile 的语法和流水线搭建方法，配合代码仓库的 Webhook 和私有镜像仓库 Registry，完成 CI 与 CD 的解耦，把项目从「手工发布」带入「生产级部署」。
>
> 下文按「背景 → 疑问 → 设计 → 步骤  → 验收 → 结论」的实验脉络展开，重点讲清每一层为什么改、解决了什么问题、以及新方案还存在哪些边界。

## 一、实验背景与学习目标

### 1.1 实验对象

MyBlog 是一个前后端分离的个人博客系统，包含三个业务构建单元：

- Spring Boot 3.5 + Java 21 后端；
- React 18 + TypeScript 博客前台；
- React 18 + TypeScript 管理后台。

运行时还依赖 PostgreSQL、Redis 和 Nginx。项目早期规模不大时，我采用最直接的部署方式：服务器上保留一份 Git 工作区，更新时执行 `git pull`，再让 Docker Compose 从当前源码重新构建并启动所有服务：

```bash
cd /path/to/myblog
git pull origin master
docker compose up -d --build
docker compose ps
```

### 1.2 学习目标

- 理解 Jenkinsfile（Declarative Pipeline）的阶段、agent、stash 等语法；
- 搭建完整的 CI/CD 流水线，并理解 CI（持续集成）与 CD（持续交付/部署）各自的职责；
- 学会用 GitHub 分支保护 + Webhook 控制代码入口和构建触发；
- 建立可回滚、可验收的生产发布流程。

## 二、起点：旧部署方式回顾

旧方案的概念少、搭建快，一个 Compose 文件就编排了数据库、缓存、后端、前端和网关，在项目早期确实很省心。但当我想给项目加 HTTPS、让后台走独立路由、引入回滚能力时，逐渐发现「能部署」和「能可控地发布」是两回事。

![图 1　旧方案发布流程：源码、构建、部署全部耦合在生产服务器上](https://cdn.shennn.top/mylab/0965d172-75bd-4870-8588-a97f8be2a542.png)

*图 1　旧方案发布流程：源码、构建、部署全部耦合在生产服务器上*

这套流程的核心特征是：**源码、构建、部署和运行环境全部耦合在生产服务器上**。服务器既运行业务，又承担发布时的全部构建工作，整个链路没有任何机器可验证的检查点。

## 三、驱动实验的三个疑问

旧方案没有绝对的错误，但它留下了三个我想通过这次实验亲手回答的问题。

### 3.1 疑问一：流水线的作用是什么？

问：原先的动手操作简单直接，但是有大量的重复性操作，把这部分内容做成流水线自动操作能大大减小运维工作量。那我写个脚本不就行了吗？CI/CD和自动化脚本的区别是什么？

答：脚本是“把命令打包跑一次”；CI/CD 是“在干净环境里、由事件触发、按门禁判定成败、留下制品与记录、可重复发布”的完整体系。区别核心：脚本管怎么跑，流水线还管何时跑、在哪跑、什么算过、失败挡谁、产物去哪、能不能回滚。所以脚本像一把扳手，CI/CD 像带红绿灯、传送带和安检门的生产线。

### 3.2 疑问二：测试检查能否自动化？

问：在AI的帮助下，项目其实已经具备一定的检查能力：后端有单元测试、Testcontainers 集成测试、Checkstyle、SpotBugs、JaCoCo 和 ArchUnit；两个前端有 ESLint、TypeScript 类型检查和 Vite 构建。旧流程中，「拉取成功」与「可以发布」之间没有机器可验证的契约，于是我就想能不能在CI或CD环节自动的执行这个测试？

答：可以。这些检查适合固化成 CI 流水线，让「拉取成功 → 可以发布」变成机器可验证的契约：PR/push/tag 触发后自动跑后端单测、Testcontainers、Checkstyle、SpotBugs、JaCoCo、ArchUnit，以及前端 ESLint、TS 类型检查、Vite build；全绿才允许合并/打 tag/发布，红灯即阻断。

### 3.3 疑问三：CI 和 CD 的区分点在哪？怎么解耦？

问：按照最初我对CI（持续集成）与 CD（持续交付/部署）的这个理解，我可以通过git push就自动执行项目的构建与部署，但是这样直接将代码改动推到生产环境真的好吗？哪怕只有一些小改动也直接重新构建生产环境的项目这真的合适吗？如果部署失败了怎么回滚呢？

答：解耦点：CI 只管从源码做出“可晋级制品”，CD 不再碰源码，只挑已验证制品发布到环境。
做法：CI 成功后把 jar/image 推到制品库并打版本；部署流水线按 dev/stage/prod 逐环境“晋级”，配置用环境变量/配置中心分离；生产发布用审批、定时窗、金丝雀/蓝绿触发，而不是 git push 直接重建生产。
好处是回滚简单：不重新构建，直接重发上一稳定制品；DB 迁移保持可逆/前向兼容，健康检查失败自动切回。

## 四、实验设计：六条规则与两套 Compose

带着这三个疑问的答案去实践，我给这次改造设定了六条规则，作为整个实验的设计约束：
  
1. 生产 Compose 和 CD 阶段不再从源码构建业务镜像；
2. 只有通过质量门的产物才能进入镜像；
3. 前台、后台、后端必须共享同一个不可变 release tag；
4. CD 只部署指定 tag，不接收模糊的 `latest`；
5. 部署成功必须经过健康检查，并记录最后一次成功版本；
6. `master` 通过 GitHub PR 保护，构建、部署和镜像清理各自有独立流程。

同时，项目保留两套 Compose 文件，避免把本地开发的便利性和生产发布的纪律混在一起：

| 使用场景 | Compose 文件 | 镜像来源 | 网关 | 依赖 Jenkins/Registry |
|---|---|---|---|---|
| 本地一键部署 | `docker-compose.yml` | 当前源码构建 | HTTP | 否 |
| 生产 CI/CD | `deploy/docker-compose.yml` | CI 推送的不可变镜像 | HTTPS | 是 |

生产 Compose 完全没有 `build` 字段，三个业务镜像必须由 CD 通过环境变量 `API_IMAGE`、`WEB_IMAGE`、`ADMIN_IMAGE` 注入。这是一条重要的环境边界：

> **本地方案优化开发效率，生产方案优化可追溯性和可恢复性。**

## 五、新方案整体架构

![图 2　新方案整体架构](https://cdn.shennn.top/mylab/a6135a2a-471c-478e-83ac-b693e5695782.png)

*图 2　新方案整体架构：GitHub → Jenkins CI → 私有 Registry → 手动 CD → 生产环境*

新流程把一次发布拆成了三个可以单独理解的动作，这也是本实验对 CI/CD 解耦的核心体会：

- **GitHub 决定什么代码可以进入 master**（代码入口）；
- **CI 决定什么 commit 可以成为发布镜像**（质量门 + 制品生产）；
- **CD 决定哪个已经验证的版本在什么时候上线**（发布决策）。

## 六、步骤一：用 GitHub 管住代码入口

CI/CD 的起点不是 Jenkins，而是代码仓库。如果 `master` 可以被任意直接推送，后面的流水线再严格，也只能验证一个缺少审核过程的结果。

### 6.1 分支保护规则

在 GitHub Branch protection / Ruleset 中对 `master` 设置五条规则：

1. 所有变更必须通过 Pull Request 合并；
2. 禁止直接 push `master`；
3. 禁止 force push 和删除 `master`；
4. 根据协作人数设置审批数量；
5. 功能分支提交和 PR 更新不触发生产 CI，只有 PR 合并形成的 `master` push 才触发。

日常代码路径因此变成：

```text
feature/fix 分支 → commit → push → PR → review → merge → master
```

GitHub 只负责变更入口和审核记录，Jenkins 不需要向仓库写代码的权限——私有仓库配置只读 PAT 或 SSH 凭据即可，公开仓库甚至可以匿名拉取。生产密钥不写入 GitHub，而是保存在服务器的 `deploy/.env` 和 Jenkins 运行环境中。

### 6.2 Webhook 触发方式

Jenkins 通过生产 Nginx 的 HTTPS 路径对外暴露，Webhook 地址形如：

```text
https://<BLOG_DOMAIN><JENKINS_ROUTE>/github-webhook/
```

Webhook 只订阅 push 事件；生产 Multibranch Pipeline 用正则 `^master$` 过滤分支，不启用 PR discovery，也不启用 Poll SCM，从而避免功能分支 push、PR 创建和更新重复触发生产构建。

## 七、步骤二：Jenkins CI——先验证，再封装镜像

![图 3　CI](https://cdn.shennn.top/mylab/b2668534-2216-4cf0-9721-77548c03850f.png)

CI 的实现位于 `Jenkinsfile.ci`。顶层使用 `agent none`，每个阶段按需申请节点或容器，避免整条流水线长期占用同一种执行环境。

### 7.1 流水线阶段总览

| 阶段 | 主要工作 | 失败后的结果 |
|---|---|---|
| Prepare | 校验 Docker Socket GID，读取并校验 `ADMIN_ROUTE` | 不进入构建 |
| Backend CI | 执行 `mvn -B verify`，归档 JUnit 与 JaCoCo 报告 | 不产生后端发布物 |
| Frontend CI | 前台、后台并行执行安装、lint、类型检查和构建 | 不产生前端发布物 |
| Build Images | 只在 `master` 封装三个已验证的产物 | 不推送镜像 |
| Push Images | 加锁推送、读取三个 digest、归档 `release.env` | 不宣布可部署版本 |

### 7.2 后端质量门：mvn verify 做了什么

后端在 `maven:3.9-eclipse-temurin-21` 容器中运行 `mvn -B verify`。这条命令不是简单编译，它串联了七项检查：

- JUnit 单元测试；
- Failsafe `*IT` 集成测试；
- Testcontainers 启动真实 PostgreSQL 和 Redis；
- Checkstyle 代码规范检查；
- SpotBugs 静态分析；
- JaCoCo 覆盖率报告；
- ArchUnit 分层依赖约束。

Jenkins 会收集 Surefire/Failsafe XML 报告并发布 JaCoCo 结果；后端产出的 JAR 用 `stash` 暂存，后续镜像阶段只拿这份「已经验证的 JAR」。由于 Testcontainers 需要启动容器，Maven 容器必须挂载宿主机 `/var/run/docker.sock`。

### 7.3 前端并行质量门

博客前台和管理后台在两个 `node:20-alpine` 执行环境中并行运行：

```bash
npm ci --no-audit --no-fund
npm run lint
npm run build
```

项目的 `build` 脚本内部包含 `vue-tsc`，因此 TypeScript 类型错误会直接让流水线失败。管理后台构建还会注入 `VITE_ADMIN_ROUTE`，确保 Vite base 与生产 Nginx 的后台路径一致。两个 `dist` 同样通过 `stash` 交给镜像阶段。并行执行减少了串行等待，又保持前后台结果互不掩盖。

### 7.4 「构建一次，封装一次」

CI 不在业务 Dockerfile 里重新执行 Maven 或 npm，而是先完成全部验证，再用 `deploy/images/` 下的运行时 Dockerfile 封装结果：

- `myblog-api`：JRE 21 + 已验证 JAR；
- `myblog-web`：Nginx + 已验证前台 `dist`；
- `myblog-admin`：Nginx + 已验证后台 `dist`。

这样保证「通过测试的产物」和「放进镜像的产物」是同一份，而不是在镜像阶段重新编译出另一份结果。

### 7.5 不可变 release tag：commit 如何变成「可部署版本」

三个镜像共用同一个 tag，格式为「日期时间-7 位 Git SHA」：

```text
yyyyMMdd-HHmmss-7位GitSHA

127.0.0.1:5000/myblog-api:20260826-153000-a1b2c3d
127.0.0.1:5000/myblog-web:20260826-153000-a1b2c3d
127.0.0.1:5000/myblog-admin:20260826-153000-a1b2c3d
```

项目不创建 `latest`。镜像还通过 OCI Label 记录 version、完整 Git SHA 和 UTC 构建时间，管理后台镜像额外记录 `com.myblog.admin-route` 标签。只有三个镜像全部推送成功、并且 Registry API 能读取到合法的 `sha256` digest 后，Jenkins 才归档 `release.env`：

```dotenv
RELEASE_TAG=20260826-153000-a1b2c3d
RELEASE_REVISION=<完整GitSHA>
RELEASE_CREATED=<UTC时间>
```

到这一步，一个 Git commit 才真正变成**可部署版本**——这是 CI 阶段的终点，也是 CD 阶段的起点。

## 八、步骤三：私有 Registry——把镜像当作发布记录

Registry 用独立的 `deploy/registry/docker-compose.yml` 管理，与博客应用生命周期解耦。关键约束：

- 使用官方 `registry:2` 镜像；
- 数据持久化到 `/data/registry`；
- 只绑定 `127.0.0.1:5000`，不向公网开放；
- Jenkins 通过外部网络 `myblog-cicd` 访问 `registry:5000`；
- 开启 manifest 删除，为后续垃圾回收做准备。

这是本机 HTTP Registry：宿主机 Docker 走回环地址访问，Jenkins 容器走隔离网络访问，均不暴露公网。若未来改为跨主机 Registry，必须增加 TLS、认证和独立备份，不能原样暴露当前 HTTP 端口。

**备注：镜像仓库的生命周期管理。** 镜像仓库不能只管推送、不管增长。项目用 `Jenkinsfile.registry-cleanup` 每天北京时间 03:30 调用 `scripts/registry-cleanup.sh`，保留规则是「最近 5 个合法 release tag + 当前成功部署 tag」（最多 6 个）：当前版本已在最近 5 个里就保留 5 个；线上仍运行较旧版本时额外保护它。

## 九、步骤四：Jenkins CD——部署的是版本，不是工作区

![图 4　CD](https://cdn.shennn.top/mylab/e880b801-c116-4b48-a27c-a65e7447c2f3.png)

CD 的实现位于 `Jenkinsfile.cd`，被故意设计为手动参数化 Job：操作者必须填写 CI 生成的 `IMAGE_TAG`，由人来决定哪个已验证的版本、在什么时候上线。项目把「生成可发布版本」（CI 自动完成）和「让版本上线」（CD 人工触发）分开——对单机个人项目，这比每次合并都立即上线更稳妥，也保留了发布窗口和人工确认；回滚与普通部署走同一条路径。

CD 在修改运行中容器之前，先做七项预检：

1. `IMAGE_TAG` 符合 release tag 格式；
2. Registry 容器状态为 `healthy`；
3. Jenkins 能访问 Registry 的 `/v2/` 接口；
4. 三个仓库都存在这个 tag；
5. 三个 manifest digest 都合法；
6. 三个完整镜像都能拉取；
7. admin 镜像中的 `ADMIN_ROUTE` 与生产配置一致。

任一预检失败，CD 都不会执行 `docker compose up`，线上容器保持原状。其中 `ADMIN_ROUTE` 检查尤其重要：管理后台的 Vite base 在构建时就写进了静态文件，而 Nginx 路径来自部署配置，两者不一致时页面可能正常打开、但 JS/CSS 和前端路由全部 404——把构建路径写入镜像 Label、再在 CD 中比对，可以把这类问题挡在部署之前。

预检通过后，CD 注入三个完整镜像地址并执行无构建部署：

```bash
docker compose \
  --env-file /opt/myblog/deploy/.env \
  -f deploy/docker-compose.yml \
  up -d --no-build
```

`--no-build` 参数与生产 Compose 中不存在 `build` 字段构成双重约束：服务器部署阶段只做镜像拉取和容器编排，不再受源码工作区和构建缓存影响。

部署后，CD 轮询 backend、frontend-web、frontend-admin、nginx 四个服务的 Docker health status；全部健康后，才用「临时文件 + 原子移动」的方式更新当前成功版本记录 `/data/jenkins/deploy-state/myblog-current-release`。如果部署失败，状态文件保持旧值；流水线保留失败容器现场并打印状态、日志和手动回滚命令，但不自动回滚——自动回滚可能覆盖最有价值的失败现场，且健康检查失败不意味着旧容器可以无条件恢复。

回滚因此不需要切 Git 分支，也不需要重新构建：只要在 CD Job 中填写仍被保留的旧 tag，就会重新执行同样的预检、拉取、部署和健康检查。

> **发布和回滚终于成为同一种操作：选择一个已经验证过的版本并部署。**

## 十、如何验收这套流水线

实验的验收分为四组，每组都是可以实际操作的检查项。

### 10.1 代码入口

- 功能分支 push 不触发生产 CI；
- PR 创建和更新不触发生产 CI；
- `master` 不能直接 push 或 force push；
- PR 合并后的 `master` push 只触发一次 CI。

### 10.2 CI

- 后端单测、集成测试或静态检查失败时不构建发布镜像；
- 任一前端 lint、类型检查或构建失败时不构建发布镜像；
- 三个仓库生成相同 release tag，且都能读取合法 digest；

### 10.3 CD

- 空 tag、非法 tag 和不存在的 tag 在部署前失败；
- Registry 故障时不修改运行中容器；`ADMIN_ROUTE` 不匹配时不部署；
- CD 日志中不出现 `docker build`；
- 四个服务全部健康后才更新当前版本状态文件；部署失败时状态文件保持旧值。

### 10.4 回滚与清理

- 选择旧 tag 可以走同一 CD 流程恢复；
- 当前线上 tag 即使不在最近 5 个中也不会被清理；
- 三个运行容器版本不一致时清理任务直接退出；
- Dry Run 只输出计划不删除；Cleanup 持锁时，CI 推送和 CD 拉取会等待。

## 十一、新旧方案对比与实验结论

![图 5　新旧方案链路对比](https://cdn.shennn.top/mylab/a96e668f-47ad-4f3e-840c-845efb9698ec.png)

*图 4　新旧方案链路对比：短链路耦合 vs 长链路分层*

### 11.1 这次实验真正学到的东西

最初我把 CI/CD 理解成「代码 push 后自动执行几条命令」。做完这次改造后，更准确的理解是：

> **CI/CD 的核心不仅仅只是自动化命令数量，还有建立从代码到运行版本的可信链路。**

### 11.2 后续演进方向

- CI优化，将为Jenkins和镜像仓库迁移到独立服务器，为跨主机 Registry 增加 TLS、认证、备份和容量监控；
- 增加 Jenkins 配置即代码（JCasC）和凭据备份；
- 把健康检查扩展为真实业务 smoke test；
- CD优化，CD过程中停机窗口不可接受时，引入滚动、蓝绿或双机部署；
- 增加发布通知、耗时趋势和失败原因统计。

这次改造没有追求一次性搭建「最复杂」的平台，而是从项目真实痛点出发，把手工发布逐步替换成一条可以解释、验证和回滚的工程链路。对个人项目来说，这种演进比单纯堆叠工具更有学习价值。

### 11.3 参考实现

- CI 流水线：`Jenkinsfile.ci`
- CD 流水线：`Jenkinsfile.cd`
- Registry 清理流水线：`Jenkinsfile.registry-cleanup`
- 生产镜像编排：`deploy/docker-compose.yml`
- Jenkins 编排：`deploy/jenkins/docker-compose.yml`
- Registry 编排：`deploy/registry/docker-compose.yml`
- Registry 清理脚本：`scripts/registry-cleanup.sh`
- CI/CD 设计约束：`docs/CI-CD.md`；生产部署指南：`deploy/README.md`
