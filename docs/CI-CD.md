# MyBlog CI/CD 设计

本文说明流水线契约与安全规则。空白服务器的逐步安装和首次上线命令统一维护在 [deploy/README.md](../deploy/README.md)，不在两处重复。

## 1. 环境边界

| 环境 | Compose | Nginx | 镜像来源 |
|---|---|---|---|
| 本地一键部署 | `docker-compose.yml` | `nginx/default.conf.template`，HTTP | 当前源码构建 |
| 生产 CI/CD | `deploy/docker-compose.yml` | `deploy/nginx/default.conf.template`，HTTPS | 私有 Registry |

本地环境不运行 Jenkins 和 Registry。生产应用 Compose 不包含 `build`，CD 只能使用 CI 发布的不可变镜像。

## 2. 流水线

```text
任意分支
  → Backend: mvn verify
  → Frontend Web: npm ci + lint + build
  → Frontend Admin: npm ci + lint + build

仅 master
  → Build Images
  → Push Images
  → 输出 release.env

手动 CD
  → Registry Preflight & Pull
  → Deploy
  → Health Check
  → Record Release
```

CI 中的 Testcontainers 集成测试早于镜像构建。镜像阶段仅包装已经通过验证的 JAR 和两个 `dist`，不重复 Maven、npm 或测试。
Jenkins 与 Maven 构建容器都使用生产配置中的 `DOCKER_GID` 访问宿主机 `docker.sock`。

## 3. 版本契约

三个镜像共用同一个 tag：

```text
127.0.0.1:5000/myblog-api:yyyyMMdd-HHmmss-7位GitSHA
127.0.0.1:5000/myblog-web:yyyyMMdd-HHmmss-7位GitSHA
127.0.0.1:5000/myblog-admin:yyyyMMdd-HHmmss-7位GitSHA
```

- 不创建 `latest` 等浮动 tag；
- 镜像记录 OCI version、完整 Git SHA 和 UTC 构建时间；
- admin 镜像额外记录 `com.myblog.admin-route`；
- 只有三个镜像全部推送且 digest 可读取后，CI 才输出可发布 tag；
- `ADMIN_ROUTE` 统一读取生产 `deploy/.env`，避免构建路径和部署路径不一致。

## 4. CD 安全顺序

CD 在执行 `docker compose up` 前完成以下检查：

1. `myblog-registry` 容器状态为 `healthy`；
2. Jenkins 从 `myblog-cicd` 网络访问 `http://registry:5000/v2/` 返回 200；
3. `IMAGE_TAG` 符合 release tag 格式；
4. 三个仓库均存在该 tag，且 digest 格式有效；
5. 三个完整镜像均拉取成功；
6. admin 镜像标签与生产 `ADMIN_ROUTE` 一致。

任一步失败都不修改运行中容器。检查通过后，CD 注入三个完整镜像地址并执行：

```bash
docker compose --env-file /opt/myblog/deploy/.env \
  -f deploy/docker-compose.yml up -d --no-build
```

backend、frontend-web、frontend-admin、nginx 全部健康后，才原子更新：

```text
/data/jenkins/deploy-state/myblog-current-release
```

失败部署保留容器现场和原状态文件，流水线打印状态、日志和手工回滚命令，不自动回滚。

## 5. Registry 与并发

Registry 使用独立 `deploy/registry/docker-compose.yml`：

- 镜像为 `registry:2`；
- 数据目录默认为 `/data/registry`；
- 只绑定 `127.0.0.1:5000`；
- 开启 manifest 删除；
- 加入外部网络 `myblog-cicd`。

CI 推送、CD 拉取和清理共用文件锁：

```text
/data/jenkins/locks/myblog-registry.lock
```

因此垃圾回收期间不会同时推送或部署。

## 6. Tag 清理

`Jenkinsfile.registry-cleanup` 每天北京时间 03:30 执行。每个仓库的保留集合为：

```text
最近 5 个合法 release tag + 当前成功部署 tag
```

- 当前版本位于最近 5 个中时保留 5 个；
- 当前版本较旧时保留 6 个；
- 状态文件缺失、格式错误、仓库缺少当前 tag，或三个运行容器版本不一致时，整个任务退出且不删除任何内容；
- 先用 `reg rm` 删除旧 manifest，再停止 Registry 执行 garbage collection，最后重启并等待健康；
- 生产配置默认 `REGISTRY_CLEANUP_DRY_RUN=true`；确认演练后改为 `false` 才启用定时删除，Job 的 `DRY_RUN` 参数可随时强制演练。

## 7. 回滚

在 CD Job 中重新选择仍被保留的旧 tag。回滚与普通部署使用相同的 Registry 检查、镜像拉取、`ADMIN_ROUTE` 校验和健康检查。

不要手工改写当前版本状态文件，也不要用根目录本地 Compose 覆盖生产容器。

## 8. 验收

- 功能分支没有 `Build Images` 和 `Push Images`；
- `master` 的三个仓库拥有相同 tag；
- Registry 故障发生时，CD 在部署前结束；
- CD 日志没有 `docker build`；
- 成功部署更新状态文件，失败部署不更新；
- 当前 tag 较旧时，清理后仍存在；
- 清理期间其他 Registry 操作等待公共锁；
- 生产 Jenkins 只有域名 HTTPS 路径，无公网宿主机端口。
