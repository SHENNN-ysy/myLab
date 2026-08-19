# MyBlog Java 后端

Java 21 / Spring Boot 单体服务，使用 PostgreSQL、Redis、MyBatis-Plus、JWT 与 OSS。

## 构建与运行

```powershell
mvn -f backend-java/pom.xml clean test
docker compose up -d --build backend
```

构建产物为 `backend-java/target/myblog-backend-1.0.0.jar`。

## 内容发布模型

七个模块 `home`、`about`、`skills`、`footprints`、`hobbies`、`vibe`、`mylab` 使用关系表保存完整版本。`content_releases` 维护 `DRAFT/PUBLISHED/ARCHIVED/OFFLINE` 状态；发布内容只读，继续编辑或恢复历史会创建更高版本号的新草稿。

MyLab 卡片使用 `PROJECT/ARTICLE` 类型统一承载首页项目与文章。全局标签位于 `mylab_tags`，不参与版本；`mylab_card_tags` 使用外键和排序字段保存卡片标签，接口仍以有序 `tag_ids` 数组交互。图片及 Markdown 正文均通过 `resources` 和三个资源关联表管理。

数据库最终结构共 19 张业务表。V1 直接创建完整表结构，V2 创建七个初始发布版本、全局标签及对应资源元数据，V3 起补充互动与站点流量聚合表。项目不启用 Flyway；Docker 只在 PostgreSQL 空数据卷首次初始化时执行 V1/V2。

接口和表结构详见：

- [API 接口文档](../docs/API接口文档.md)
- [数据库表结构](../docs/数据库表结构重设计.md)
- Swagger UI：`/swagger-ui.html`
- OpenAPI：`/v3/api-docs`
