# 事故复盘：CI 构建内存耗尽导致流水线假死（2026-09-07）

## 1. 事件概述

| 项 | 内容 |
|---|---|
| 发生时间 | 2026-09-07 20:49 ～ 21:35（约 46 分钟） |
| 影响范围 | shennn.top-CI 流水线卡死；生产站点未宕机，但整机响应严重变慢 |
| 触发条件 | PR #20（前端 Vue→React 重写）合并到 master，触发全量 CI |
| 根因 | 2 vCPU / 3.4GB 内存 / 无 swap 的单机上，CI 构建与生产栈内存峰值叠加越界 |
| 恢复方式 | 重启服务器；随后实施 swap + JVM 限堆的永久性修复（PR #21） |

## 2. 事件经过

| 时间 | 事件 |
|---|---|
| 20:49:35 | master 收到 push（Merge PR #20），CI 流水线启动 |
| 20:49:49 | Backend CI 阶段在 maven 容器内开始 `mvn -B verify` |
| 20:50:23 | 主代码编译完成（160 个源文件，约 18 秒，尚正常） |
| 20:51:16 | 测试代码编译完成（25 个文件耗时 53 秒，已现资源紧张迹象） |
| 20:54:48 | surefire 打印出 JUnit provider 信息后**再无输出**——正常情况这一步只需几秒，此处已耗时 3 分半钟 |
| 21:02 起 | 系统 journal 被 `systemd-journald: Under memory pressure, flushing caches` 持续刷屏，整机进入内存回收爬行状态 |
| 21:26 前后 | 用户观察到系统内存被打满，流水线仍停在 surefire 阶段无任何报错 |
| 21:35 | 重启服务器，全部容器按 `restart: unless-stopped` 自愈，生产恢复；流水线随重启中断 |
| 21:40 起 | 上机排查取证（journal 历史、dmesg、容器残留、内存分布），定位根因 |
| 当晚 | 实施修复：宿主机 2GB swap + 三处 JVM 限堆，提交 PR #21 |

更早的隐患信号：8 月 25 日、26 日的 dockerd 日志中已出现过 `failed to read oom_kill event` 告警，说明内存吃紧并非首次，只是此前未造成可见故障。

## 3. 现象与误判点

- 流水线**看起来是"卡住"而不是"失败"**：surefire 打印完 provider 检测后再无一行输出，不报错、不退出。
- 关键误导：**没有 OOM killer 日志**。上一次开机的 4947 行内核日志中 0 条 oom 记录——因为机器没有 swap，内存耗尽时内核无法换出匿名页，只能反复回收页缓存（journald 的 "Under memory pressure" 刷屏正是这种挣扎的表现），所有进程一起变慢但不死，形成"假死"。
- 两个 JVM 陷入 **GC 死亡螺旋**：堆接近上限 → 频繁 Full GC → 仅有的 2 个 vCPU 全在 GC → 业务代码几乎不推进。

## 4. 根因分析

### 4.1 内存账本

CI 空闲基线（实测 `docker stats`）：

| 组件 | 常驻内存 |
|---|---|
| Jenkins JVM | ~630 MB |
| 生产后端 JVM | ~619 MB |
| postgres / redis / registry | ~120 MB |
| nginx + 两个前端静态容器 | ~20 MB |
| dockerd + containerd | ~165 MB |
| 云监控/安骑士 agent + 系统 | ~200 MB |
| **合计** | **~1.7 GB**（3.4GB 总内存仅剩 ~1.7GB 余量） |

`mvn verify` 峰值新增：

| 新增进程 | 峰值上限 | 说明 |
|---|---|---|
| Maven 主 JVM | ~850 MB | 未配 MAVEN_OPTS，默认堆上限 = 物理内存 1/4 |
| surefire/failsafe fork 的测试 JVM | ~850 MB | 独立 JVM，默认同样 1/4 物理内存，另带 JaCoCo agent |
| Testcontainers（PG + Redis + Ryuk） | ~200 MB | 通过挂载的 docker.sock 直接起在宿主机 |
| dockerd 起容器瞬时开销 | ~100 MB | — |

峰值合计 ≈ **3.6GB+ > 3.4GB 物理内存**，且无 swap 兜底。流水线恰好冻结在 surefire 单测阶段（failsafe 集成测试尚未开始），说明越界点在双 JVM 叠加，而非测试容器。

### 4.2 结构性原因

1. **CI 与生产同机**：构建负载与生产负载共用 3.4GB，没有任何隔离。
2. **JVM 全部不设防**：Jenkins、Maven、测试 fork JVM 均按"物理内存比例"取默认堆上限，容器没设 memory limit 时 JVM 看到的是宿主机内存。
3. **无 swap**：分钟级的构建内存尖峰没有任何缓冲层，一旦越界直接整机爬行。

## 5. 处理方案（已实施）

### 5.1 宿主机（已生效）

- 新建 2GB swapfile，`/etc/fstab` 持久化，`vm.swappiness=10`（尽量不用 swap，仅作尖峰兜底）
- 清理卡死流水线残留的 5 个 maven 容器

### 5.2 JVM 限堆（PR #21）

| 位置 | 改动 |
|---|---|
| `deploy/jenkins/docker-compose.yml` | `JAVA_OPTS=-Xmx768m -XX:MaxMetaspaceSize=256m`（已重建容器并验证进程参数生效） |
| `Jenkinsfile.ci` | `MAVEN_OPTS="-Xmx512m"` 限制 Maven 主 JVM |
| `backend-java/pom.xml` | surefire/failsafe 配置 `argLine` 为 `@{argLine} -Xmx512m`，限制测试 fork JVM（`@{argLine}` 占位保留 JaCoCo agent 注入） |

治理后的内存预算：峰值从 ~5GB+ 压到 ~3.5GB 以内，另有 2GB swap 兜底。

### 5.3 验证

- 本地 `mvn test`：271 个单测全过，BUILD SUCCESS（50 秒）
- PR #21 合并后的首次全量 CI 即为生产环境实测

## 6. 后续方案：独立 CI/测试服务器 + Harbor 镜像仓库

限堆 + swap 只是让现状可运行，**构建与生产同机的结构性风险仍在**（构建高峰仍会影响生产响应）。规划中的彻底解法：

```text
CI 服务器（新增）                    生产服务器（现有 2C/3.4G）
┌─────────────────────┐            ┌──────────────────────┐
│ Jenkins（迁移过来）   │            │ nginx / web / admin    │
│ Maven / Node 构建     │  push 镜像 │ backend / PG / Redis   │
│ Harbor（镜像仓库）    │ ────────▶  │                        │
└─────────────────────┘  CD 时 pull │ （不再承担任何构建负载） │
                          ◀──────── └──────────────────────┘
```

要点：

- **规格**：CI 服务器建议至少 4C8G——Harbor 自身（portal/core/registry/postgres/redis/jobservice）空转约需 1.5–2GB，2C4G 会让"Jenkins + Harbor + 构建"在新机器上重蹈覆辙。若不用 Harbor、只迁移现有 `registry:2`（25MB），2C4G 亦可接受。
- **Harbor vs registry:2**：Harbor 提供 Web UI、RBAC、漏洞扫描、图形化保留策略，适合作为学习/演进方向；现有 `registry:2` + `scripts/registry-api.sh` + `Jenkinsfile.registry-cleanup` 已满足"保留最近 5 个 tag + 保护当前部署 tag"的需求，单纯解决问题不必强上 Harbor。
- **网络**：生产 CD 需从 Harbor 拉镜像——同 VPC 走内网最佳；走公网必须配 HTTPS + 认证，不能再沿用现在 `127.0.0.1:5000` 的裸 HTTP。
- **迁移清单**：Jenkins 数据目录 `/data/jenkins` 整体搬迁 → `Jenkinsfile.ci` 的 registry 地址/节点假设 → `Jenkinsfile.cd` 改为远程拉取 → `deploy/docker-compose.yml` 镜像名前缀 → Registry 清理逻辑迁移（Harbor 改用其 retention 机制）→ 生产 registry 容器退役。

## 7. 经验教训

1. **无 swap 的机器上，内存耗尽表现为"假死"而非 OOM kill**——排查时不要只找 OOM 日志，journal 的 memory pressure 记录才是关键信号。
2. **容器内 JVM 默认堆按宿主机物理内存计算**，小内存机器上必须显式 `-Xmx`，否则每个 JVM 都以为自己能用 1/4 内存。
3. **CI 峰值要按"基线 + 全部并发进程上限"算账**，而不是看空闲时的 `free`。
4. 监控告警（dockerd 的 oom_kill event 告警）出现时就该处理，不要等到故障发生。
