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
│   │   └── service/              # auth/user/project/skill/footprint/about/file/visit/system
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

## OSS/CDN

媒体应用服务只依赖 `ObjectStorage` 端口，阿里云 OSS SDK 位于 `infrastructure/storage/oss`。生产环境需配置 `OSS_ENDPOINT`、`OSS_ACCESS_KEY_ID`、`OSS_ACCESS_KEY_SECRET`、`OSS_BUCKET` 与 `OSS_CDN_DOMAIN`。

建议 ECS 使用同地域 OSS 内网 Endpoint 上传，公开图片通过 CDN 域名返回；数据库仅保存对象键和文件元数据，不保存图片二进制。
