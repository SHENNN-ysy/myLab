# MyBlog Java 后端

Java 21 / Spring Boot 单体服务，使用 PostgreSQL、Redis、MyBatis-Plus、JWT 与 OSS。

## 构建与运行

```powershell
mvn -f backend-java/pom.xml clean test
docker compose up -d --build backend
```

构建产物为 `backend-java/target/myblog-backend-1.0.0.jar`。

## 内容发布模型

七个模块 `home`、`about`、`skills`、`footprints`、`hobbies`、`vibe`、`mylab` 使用关系表保存完整版本。`content_releases` 维护 `DRAFT/PUBLISHED/ARCHIVED/OFFLINE` 状态和版本名称、描述。内容必须先保存为具名草稿才能发布；恢复历史版本时目标记录原地转为草稿，原草稿转为归档记录，不创建新版本。

MyLab 卡片使用 `PROJECT/ARTICLE` 类型统一承载首页项目与文章。全局标签位于 `mylab_tags`，不参与版本；`mylab_card_tags` 使用外键和排序字段保存卡片标签，接口仍以有序 `tag_ids` 数组交互。封面图片通过 `resources` 管理，Markdown 正文保存在 `mylab_cards.markdown_content`，随草稿、发布和历史版本一起复制。

数据库由 Flyway 管理。`V1__baseline.sql` 创建完整基线，后续结构变更使用递增迁移文件；`V2__mylab_markdown_content.sql` 迁移 MyLab 正文，`V3__content_release_metadata.sql` 增加版本名称和描述并调整草稿恢复约束。

接口和表结构详见：

- [API 接口文档](../docs/API接口文档.md)
- [数据库表结构](../docs/数据库表结构重设计.md)
- Swagger UI：`/swagger-ui.html`
- OpenAPI：`/v3/api-docs`
