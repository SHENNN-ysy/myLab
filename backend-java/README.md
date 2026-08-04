# MyBlog Java 后端

MyBlog 后端已合并为一个 Java 21 / Spring Boot 单体工程。项目只有一个 `pom.xml`、一套 `src` 和一个可部署 JAR，不再拆分 `myblog-common`、`myblog-pojo`、`myblog-server`。

## 目录结构

```text
backend-java/
├── src/main/java/com/myblog/
│   ├── application/              # 应用层：业务模型、用例、仓储与外部能力端口
│   │   ├── model/
│   │   ├── port/
│   │   ├── repository/
│   │   └── service/              # auth/user/content/file/visit/system 等应用服务
│   ├── common/                   # 公共层：异常、响应、安全身份、公共配置
│   ├── controller/               # 接口层：HTTP入口与全局异常处理
│   ├── infrastructure/           # 基础设施层：PostgreSQL、Redis、JWT、OSS
│   ├── starter/                  # 启动配置层：过滤器、Spring配置、初始化装配
│   └── ApplicationLoader.java    # 唯一应用入口
├── src/main/resources/           # application.yml与Flyway脚本
├── src/test/java/                # 单元测试与ArchUnit架构测试
├── Dockerfile
└── pom.xml
```

`controller` 与其他层平级，不属于 `starter`。应用服务按用户看到的具体功能拆分，例如项目接口、项目服务和项目命令分别位于 `controller/ProjectController`、`service/project` 和 `command/project`。详细依赖规则见 [ARCHITECTURE.md](./ARCHITECTURE.md)。

## 本地构建

在 `backend-java` 目录执行：

```powershell
mvn test
mvn package
```

在仓库根目录执行：

```powershell
mvn -f backend-java/pom.xml test
docker compose up -d --build api
```

构建产物为：

```text
backend-java/target/myblog-backend-1.0.0.jar
```

## 内容发布模型

页面内容统一保存在 `content_modules`，发布快照保存在 `content_publications`。每个模块分别维护草稿版本和线上版本，因此管理员保存草稿不会改变访客看到的内容；发布、下线和回滚都由服务端完成并校验数据约束。

- 公共读取：`GET /api/v1/public/content`、`GET /api/v1/public/content/{moduleKey}`。
- 后台草稿：`GET/PUT /api/v1/admin/content/{moduleKey}`。
- 发布控制：`POST /api/v1/admin/content/{moduleKey}/publish|offline|rollback/{version}`。
- 历史版本：`GET /api/v1/admin/content/{moduleKey}/versions`。

模块键仅为 `skills`、`projects`、`footprints`、`hobbies`、`vibe`、`mylab`、`support`。页面标题、描述、关于我、Time 曲线、足迹地图配置、Vibe Coding 左侧主视觉和支持页非统计信息不进入内容模型；支持页面统计使用独立访问会话数、浏览记录总数和后台手动维护的点赞数。

V1 仅建立 `users`、`files`、`visit_logs`、`content_modules`、`content_publications` 五张表；V2 初始化七个模块和七条 v1 发布快照。项目发布依赖已发布的 myLab 记录；删除或停用被线上项目引用的 myLab 记录也会被服务端拒绝。

## API 文档与 Swagger

- Swagger UI：`/swagger-ui.html`
- OpenAPI 3.0 JSON：`/v3/api-docs`
- OpenAPI 3.0 YAML：`/v3/api-docs.yaml`
- 完整接口清单：[../docs/API接口文档.md](../docs/API接口文档.md)
- 错误码规范：[../docs/错误码文档.md](../docs/错误码文档.md)

Swagger UI 支持使用 JWT Bearer access token 在线调试受保护接口。生产环境如不希望公开文档，可通过 `SPRINGDOC_API_DOCS_ENABLED=false` 和 `SPRINGDOC_SWAGGER_UI_ENABLED=false` 关闭。

## OSS/CDN

媒体应用服务只依赖 `ObjectStorage` 端口，阿里云 OSS SDK 位于 `infrastructure/storage/oss`。生产环境需配置 `OSS_ENDPOINT`、`OSS_ACCESS_KEY_ID`、`OSS_ACCESS_KEY_SECRET`、`OSS_BUCKET` 与 `OSS_CDN_DOMAIN`。

建议 ECS 使用同地域 OSS 内网 Endpoint 上传，公开图片通过 CDN 域名返回；数据库仅保存对象键和文件元数据，不保存图片二进制。
