# MyBlog 后端架构

## 架构目标

后端采用单工程、模块化单体和端口适配器模式。代码收敛到 `application`、`common`、`controller`、`infrastructure`、`starter` 五个顶层包，并保持明确依赖方向。

```text
controller ─────> application <──────── infrastructure
    │                  │                       │
    └─────────────────> common <───────────────┘

starter ────────> application / common / infrastructure
```

- `application`：业务模型、命令、应用服务、仓储接口与外部能力端口。
- `common`：各层都可复用的稳定类型，不包含具体业务用例。
- `controller`：REST接口与全局异常处理，只负责协议转换和调用应用服务。
- `infrastructure`：MyBatis、PostgreSQL、Redis、JWT、OSS等端口实现。
- `starter`：Spring Security、过滤器、Bean配置和启动初始化，不放Controller。
- `ApplicationLoader`：唯一 Spring Boot 启动入口。

## 应用层结构

```text
application/
├── model/
│   ├── command/        # 写操作命令
│   ├── dto/            # 输入数据
│   ├── entity/         # 当前业务/持久化模型
│   └── vo/             # 输出视图
├── port/               # OSS、JWT、Redis计数、系统诊断等外部能力端口
├── repository/         # 持久化端口，按实体名称直接查找
└── service/            # 用例实现
    ├── auth/
    ├── user/
    ├── content/
    ├── file/
    ├── visit/
    └── system/
```

## 功能目录

| 目录 | 职责 |
| --- | --- |
| `auth` | 登录、刷新令牌、退出和修改密码 |
| `user` | 后台用户管理 |
| `content` | 七类内容模块的草稿、发布、下线、历史与回滚 |
| `file` | 文件元数据、OSS上传和CDN地址 |
| `visit` | 访问日志、PV与UV统计 |
| `system` | 健康状态与系统运行信息 |

这些名称是项目当前功能的直接映射，不是固定的DDD模板。新增功能时，以“开发者能否从名称直接找到接口、服务和数据”为首要判断；只有功能规模和业务规则明显增长时，再考虑组合成更大的限界上下文。

## 依赖规则

1. `controller` 与其他层平级，只负责HTTP协议转换，不能直接访问Mapper、JDBC、Redis或OSS SDK。
2. 应用服务依赖 `repository` 与 `port` 接口，不能依赖 `infrastructure` 实现。
3. `common` 不能反向依赖其他三层。
4. `infrastructure` 不能依赖 `controller` 或 `starter`。
5. `application` 不能依赖 `controller` 或 `starter`，应用服务也不能接收 `HttpServletRequest`、`MultipartFile` 等Web对象。
6. `starter` 只负责启动和配置，不能依赖 `controller`。
7. 写操作使用明确命令对象，不恢复通用反射CRUD基类。

以上规则由 `LayeredArchitectureTest` 自动检查。

## OSS/CDN边界

```text
FileController
    -> FileService
        -> ObjectStorage（应用端口）
            -> OssObjectStorageAdapter（基础设施实现）
                -> OSS内网上传 / CDN公开读取
```

`FileService` 只接收应用层 `UploadFile` 命令。OSS凭证、Endpoint、Bucket与CDN域名封装在基础设施和配置层，后续替换对象存储实现不影响业务用例。

## 兼容策略

- `/api/v1` 路径、JSON的 `snake_case` 字段与数据库表保持不变。
- 当前模型保留 MyBatis映射注解，以控制本次整体迁移风险；新增复杂领域规则应逐步提取为值对象或聚合行为。
- 只有出现真实跨聚合一致性需求时才引入领域事件，避免为结构而过度设计。
