# MyBlog 生产环境 CI/CD 部署指南

本文从一台空白 Ubuntu 服务器开始，部署 Docker、私有 Registry、Jenkins、生产应用和 Nginx。根目录 `docker-compose.yml` 是本地一键部署入口，不用于生产环境。

## 1. 生产架构

```text
GitHub PR 合并到 master
  → Jenkins CI：测试、检查、构建产物
  → 构建并推送三个不可变镜像
  → 127.0.0.1:5000 私有 Registry
  → Jenkins CD：预检、拉取、无构建部署、健康检查

Internet
  → Nginx :443
      ├── /                    → 博客前台
      ├── ${ADMIN_ROUTE}/      → 管理后台
      ├── /api/                → Spring Boot
      └── ${JENKINS_ROUTE}/    → Jenkins
```

- Registry 只绑定 `127.0.0.1:5000`，不开放公网。
- Jenkins 正式运行时不映射宿主机端口，只通过 Nginx 暴露 HTTPS 路径。
- 生产 Compose 不含 `build`，CD 日志中不应出现 `docker build`。
- PostgreSQL、Redis 和后端调试端口只绑定回环地址。

## 2. 前置条件

- Ubuntu 22.04/24.04 服务器；
- 已解析到服务器公网 IP 的域名；
- 域名 TLS 完整证书链和私钥；
- 服务器可以访问 GitHub 和 Docker Hub；
- 安全组仅放行 `22`、`80`、`443`，不放行 `5000`、`8080`、`8888`；
- 使用 root，或具有 `sudo` 权限的用户。

## 3. 安装 Docker

使用 Docker 官方脚本安装 Docker Engine、Buildx 和 Compose：

```bash
sudo -i
curl -fsSL https://get.docker.com | sh
exit
```

把当前用户加入 Docker 用户组：

```bash
sudo usermod -aG docker "$USER"
```

重新登录 SSH，然后验证：

```bash
docker version
docker buildx version
docker compose version
```

## 4. 获取项目并准备目录

手动拉取项目到固定目录 `/opt/myblog`：

```bash
sudo mkdir -p /opt/myblog /data/jenkins /data/registry /data/myblog/logs
sudo chown -R "$USER":"$USER" /opt/myblog /data/registry /data/myblog
sudo chown -R 1000:1000 /data/jenkins

git clone https://github.com/SHENNN-ysy/myLab.git /opt/myblog
cd /opt/myblog
cp deploy/.env.example deploy/.env
```

编辑 `deploy/.env`，至少修改：

- `BLOG_DOMAIN`、`CORS_ORIGINS`；
- `ADMIN_ROUTE`、`JENKINS_ROUTE`，二者必须以 `/` 开头且不能冲突；
- PostgreSQL、Redis、JWT、访客哈希和初始管理员密码；
- OSS/CDN 参数；
- `TLS_CERT_FILE`、`TLS_KEY_FILE`；
- `DOCKER_GID`。

写入 Docker Socket 的实际组 ID：

```bash
DOCKER_GID=$(stat -c '%g' /var/run/docker.sock)
sed -i "s/^DOCKER_GID=.*/DOCKER_GID=$DOCKER_GID/" deploy/.env
```

CI 会在已分配 Jenkins 节点的 `Prepare` 阶段再次校验该值，再将实际 GID 传入 Maven 容器。不要在 `agent none` 流水线的顶层 `environment` 中使用 `sh` 读取 GID，否则 Jenkins 尚无 workspace，会直接报 `MissingContextVariableException: hudson.FilePath is missing`。

默认配置目录保持一致：

```text
DEPLOY_CONFIG_DIR=/opt/myblog/deploy
```

Jenkins Compose 将该目录挂载到容器内相同路径：

```yaml
- ${DEPLOY_CONFIG_DIR:-/opt/myblog/deploy}:/opt/myblog/deploy:ro
```

因此宿主机和 Jenkins 容器均通过 `/opt/myblog/deploy/.env` 读取生产配置。

## 5. 配置 TLS

将证书放到 `deploy/nginx/certs/`，文件名与 `deploy/.env` 一致：

```bash
mkdir -p deploy/nginx/certs
cp /path/to/fullchain.pem deploy/nginx/certs/fullchain.pem
cp /path/to/privkey.pem deploy/nginx/certs/privkey.pem
chmod 600 deploy/nginx/certs/privkey.pem
```

生产 Nginx 使用 `deploy/nginx/default.conf.template`：

- `80` 端口只跳转 HTTPS；
- `443` 提供博客、后台、API 与 Jenkins；
- Jenkins 路径必须与 `JENKINS_OPTS --prefix` 一致；
- `/healthz` 由网关直接返回。

## 6. 启动私有 Registry

本机 Registry 使用 HTTP。若 `/etc/docker/daemon.json` 已存在，合并配置，不要覆盖其他字段：

```json
{
  "insecure-registries": ["127.0.0.1:5000"]
}
```

应用配置并启动独立 Registry：

```bash
sudo systemctl restart docker
docker network create myblog-cicd
docker network create myblog-jenkins-proxy

cd /opt/myblog
docker compose --env-file deploy/.env \
  -f deploy/registry/docker-compose.yml up -d

docker inspect --format '{{.State.Health.Status}}' myblog-registry
curl -f http://127.0.0.1:5000/v2/
```

Registry 数据保存在 `/data/registry`，不随博客应用重启或删除。

## 7. 启动 Jenkins

Jenkins 工具镜像会在首次 `apt-get update` 前将 Debian 主源和安全更新源固定切换到中科大镜像，Docker CE 源默认使用中科大镜像。相关 Release、Docker GPG 与 amd64 软件包索引已在替换时验证可访问；如 Docker CE 镜像线路不同，可修改 `deploy/.env` 中的 `DOCKER_APT_MIRROR` 后重新构建。

### 7.1 首次初始化

首次发布前生产 Nginx 尚未运行，因此临时把 Jenkins 绑定到服务器回环地址：

```bash
cd /opt/myblog
docker compose --env-file deploy/.env \
  -f deploy/jenkins/docker-compose.yml \
  -f deploy/jenkins/docker-compose.bootstrap.yml \
  up -d --build

docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

在自己的电脑建立 SSH 隧道：

```bash
ssh -L 8888:127.0.0.1:8888 <user>@<server-ip>
```

浏览器打开 `http://localhost:8888<JENKINS_ROUTE>/`，例如 `http://localhost:8888/jenkins/`。`8888` 只监听服务器回环地址，公网无法直接访问。

完成向导并确认安装：

- Pipeline、Git、GitHub Branch Source；
- Docker Pipeline；
- JUnit、Coverage。

在 Jenkins 系统配置中把 Jenkins URL 设置为：

```text
https://<BLOG_DOMAIN><JENKINS_ROUTE>/
```

### 7.2 创建流水线

Jenkins 只在 PR 合并到 `master` 后运行 CI。合并操作会更新 `master`，GitHub 随即发送 push Webhook；功能分支 push 和 PR 创建事件不触发 Jenkins。

创建三个 Job，仓库均指向本项目的 `master`：

| Job | 类型 | Script Path | 触发方式 |
|---|---|---|---|
| CI | Multibranch Pipeline | `Jenkinsfile.ci` | PR 合并到 `master` 后自动触发 |
| CD | Pipeline from SCM | `Jenkinsfile.cd` | 手动参数化 |
| Registry Cleanup | Pipeline from SCM | `Jenkinsfile.registry-cleanup` | 每天 03:30 |

#### 7.2.1 创建 CI Job

1. 点击 `New Item`，名称填写 `myblog-CI`，选择 `Multibranch Pipeline`。
2. 在 `Branch Sources` 中添加 `GitHub`，填写仓库所有者、仓库名和访问凭据；公开仓库可不填凭据。
3. 在 `Behaviors` 中添加 `Filter by name (with regular expression)`，正则填写：

   ```text
   ^master$
   ```

4. 不添加 `Discover pull requests`，避免 PR 创建或更新时提前执行 CI。
5. 在 `Build Configuration` 中选择 `by Jenkinsfile`，`Script Path` 填写 `Jenkinsfile.ci`。
6. 不启用 `Poll SCM` 或周期性分支扫描，后续只由 GitHub Webhook 通知变更。
7. 保存后点击 `Scan Multibranch Pipeline Now`，确认只生成 `master` 子任务。

#### 7.2.2 创建 CD Job

1. 点击 `New Item`，名称填写 `myblog-CD`，选择 `Pipeline`。
2. `Definition` 选择 `Pipeline script from SCM`，SCM 选择 `Git`。
3. 填写仓库地址和凭据，`Branch Specifier` 填写 `*/master`。
4. `Script Path` 填写 `Jenkinsfile.cd`，不要配置任何自动触发器。
5. 保存后手动运行；部署时必须填写 CI 输出的 `IMAGE_TAG`。

#### 7.2.3 创建 Registry Cleanup Job

1. 点击 `New Item`，名称填写 `myblog-registry-cleanup`，选择 `Pipeline`。
2. 与 CD Job 相同，使用 `Pipeline script from SCM`、`*/master` 和相同仓库凭据。
3. `Script Path` 填写 `Jenkinsfile.registry-cleanup`。
4. 保存后手动运行一次，使 Jenkins 注册文件中的北京时间 `03:30` 定时规则；首次保持 `DRY_RUN=true`。

#### 7.2.4 配置 master 合并规则

在 GitHub 仓库的分支 Ruleset 或 Branch protection 中保护 `master`：

1. 要求所有变更通过 Pull Request 合并；
2. 禁止直接 push、force push 和删除 `master`；
3. 按团队需要设置审批人数；
4. 不把该 Jenkins CI 设为合并前必需检查，因为本方案在合并完成后才运行 CI。

日常发布流程为：功能分支提交 → 创建 PR → 审核并合并到 `master` → Jenkins 自动运行 CI → 使用 CI 输出的 tag 手动运行 CD。

#### 7.2.5 配置 GitHub Webhook

首次生产部署完成、域名入口可访问后，在 GitHub 仓库 `Settings` → `Webhooks` 中添加：

```text
Payload URL: https://<BLOG_DOMAIN><JENKINS_ROUTE>/github-webhook/
Content type: application/json
Events: Just the push event
Active: true
```

Webhook 不需要订阅 Pull requests 事件。PR 合并后 GitHub 会产生一次针对 `master` 的 push 事件，Jenkins 收到后扫描仓库并只构建 `master`。保存 Webhook 后先确认自动发送的 `ping` 返回成功；再合并一个测试 PR，确认 push delivery 成功且 `myblog-CI/master` 只触发一次。需要排障时可在 `Recent Deliveries` 中重新投递该 push 事件。

## 8. 首次发布

### 8.1 运行 CI

首次上线时 Nginx 和 Webhook 尚不可用，在 `myblog-CI` 中手动扫描仓库并运行 `master` 子任务。后续版本必须通过 PR 合并到 `master` 自动触发，不直接 push `master`。CI 成功后下载归档的 `release.env`：

```text
RELEASE_TAG=yyyyMMdd-HHmmss-7位GitSHA
```

三个仓库必须同时存在该 tag：

```bash
docker exec jenkins sh -c \
  "curl -fsS http://registry:5000/v2/myblog-api/tags/list | jq -r '.tags[]?'"
docker exec jenkins sh -c \
  "curl -fsS http://registry:5000/v2/myblog-web/tags/list | jq -r '.tags[]?'"
docker exec jenkins sh -c \
  "curl -fsS http://registry:5000/v2/myblog-admin/tags/list | jq -r '.tags[]?'"
```

### 8.2 运行 CD

首次部署包含 `V2__mylab_markdown_content.sql` 的版本时，Flyway 会新增正文列，将现有 MyLab Markdown 正文写入 `mylab_cards.markdown_content`，验证没有遗漏后删除 `mylab_resources.content_resource_id`。迁移失败时后端不会启动，也不得继续后续 CD。

部署成功后可验证正文和旧字段状态：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.yml exec -T postgres \
  sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "
    SELECT COUNT(*) AS missing_markdown
    FROM mylab_cards
    WHERE deleted_at IS NULL AND markdown_content IS NULL;
    SELECT COUNT(*) AS legacy_column
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'mylab_resources'
      AND column_name = 'content_resource_id';"'
```

`missing_markdown` 和 `legacy_column` 均应为 `0`。

执行 CD 的 `Build with Parameters`，将 `IMAGE_TAG` 设置为 CI 输出的 release tag。CD 会：

1. 检查 Registry 容器与 `/v2/`；
2. 校验 tag 和三个 manifest digest；
3. 拉取三个完整镜像；
4. 比较 admin 镜像标签与 `ADMIN_ROUTE`；
5. 执行 `deploy/docker-compose.yml up -d --no-build`；
6. 检查 backend、frontend-web、frontend-admin、nginx；
7. 原子更新当前成功版本文件。

验证：

```bash
curl -I https://<BLOG_DOMAIN>/
curl -f https://<BLOG_DOMAIN>/api/v1/health
curl -I https://<BLOG_DOMAIN><ADMIN_ROUTE>/
cat /data/jenkins/deploy-state/myblog-current-release
```

### 8.3 移除 Jenkins 临时端口

确认 `https://<BLOG_DOMAIN><JENKINS_ROUTE>/` 可访问后，仅使用正式 Compose 重建 Jenkins：

```bash
cd /opt/myblog
docker compose --env-file deploy/.env \
  -f deploy/jenkins/docker-compose.yml up -d --force-recreate

docker port jenkins
```

`docker port jenkins` 应无输出。Jenkins 配置保存在 `/data/jenkins`，重建容器不会丢失。

## 9. Registry 清理

每个仓库保留“最近 5 个 release tag + 当前成功部署 tag”。当前版本位于最近 5 个中时保留 5 个，否则保留 6 个。

生产配置默认 `REGISTRY_CLEANUP_DRY_RUN=true`，因此定时任务也只输出计划。首次手动执行 Registry Cleanup Job 并勾选 `DRY_RUN`，确认保留和删除集合正确后，把 `deploy/.env` 改为：

```bash
REGISTRY_CLEANUP_DRY_RUN=false
```

之后定时任务执行正式清理；临时演练仍可在 Job 中勾选 `DRY_RUN`。清理脚本会在删除前校验：

- 当前版本状态文件存在且格式正确；
- 三个运行容器使用同一个当前 tag；
- 三个仓库都存在当前 tag；
- Registry 健康。

任一校验失败都会安全退出，不删除镜像。清理脚本通过 Registry V2 HTTP API 删除 manifest，随后短暂停止 Registry 执行 garbage collection，再等待 `/v2/` 恢复。

## 10. 回滚与运维

回滚时在 CD Job 中填写保留范围内的旧 tag。CD 会重新执行全部预检和健康检查，不在服务器重新构建。

```bash
# 应用
docker compose --env-file deploy/.env -f deploy/docker-compose.yml ps
docker compose --env-file deploy/.env -f deploy/docker-compose.yml logs --tail=100 backend nginx

# Registry
docker compose --env-file deploy/.env -f deploy/registry/docker-compose.yml ps
docker logs myblog-registry --tail 100
curl -f http://127.0.0.1:5000/v2/

# Jenkins
docker inspect jenkins --format '{{json .NetworkSettings.Networks}}'
docker exec jenkins curl -f http://registry:5000/v2/
docker exec jenkins docker compose version
docker exec jenkins jq --version
```

更新仓库或 Jenkins 镜像：

```bash
cd /opt/myblog
git pull --ff-only
docker compose --env-file deploy/.env \
  -f deploy/jenkins/docker-compose.yml up -d --build
```

不要执行根目录 `docker compose up -d --build` 代替生产 CD；该命令属于本地一键部署，不使用 Registry，也不包含生产 HTTPS/Jenkins 配置。

## 11. 上线验收

- 功能分支 push、PR 创建和 PR 更新不会触发 Jenkins；
- PR 合并到 `master` 后自动触发一次 CI，三个仓库出现相同 release tag；
- Registry 异常、镜像缺失、tag 非法或 `ADMIN_ROUTE` 不一致时，CD 在修改容器前失败；
- CD 不执行镜像构建，部署失败不覆盖当前成功版本文件；
- Jenkins 无公网主机端口，域名路径和 GitHub Webhook 可用；
- 清理 dry-run 保留最近 5 个 tag，并额外保护较旧的当前部署 tag。
