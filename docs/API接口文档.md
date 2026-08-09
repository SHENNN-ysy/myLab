# MyBlog API 接口文档

本文档以当前 Java 后端实现为准。接口基础前缀为 `/api/v1`，OpenAPI JSON 位于 `/v3/api-docs`，Swagger UI 位于 `/swagger-ui.html`。

## 1. 通用约定

### 1.1 认证与权限

除登录、刷新令牌、健康检查和公开内容读取外，其余接口都需要请求头：

```http
Authorization: Bearer <access_token>
```

角色等级从低到高为：`viewer`、`editor`、`admin`、`superadmin`。

| 权限 | 可用角色 |
| --- | --- |
| 公开内容、登录、刷新令牌、健康检查 | 无需登录 |
| 内容、标签、文件、系统信息、用户查询和用户更新 | `admin`、`superadmin` |
| 创建和删除管理员账号 | `superadmin` |

### 1.2 统一响应

成功与失败都使用相同包络：

```json
{
  "code": 0,
  "message": "成功",
  "data": {}
}
```

失败时 `data` 为 `null`，并可能包含可安全展示的 `error`：

```json
{
  "code": 12004,
  "message": "内容数据校验失败",
  "data": null,
  "error": "已发布 PROJECT 必须填写 project_contents"
}
```

客户端应使用 `code` 判断业务结果，优先向用户展示 `error`，没有 `error` 时再展示 `message`。

### 1.3 分页结构

用户与文件列表使用相同分页结构：

```json
{
  "records": [],
  "total": 0,
  "page": 1,
  "page_size": 20
}
```

`page` 从 1 开始。当前接口参数只包含 `page` 和 `page_size`，不提供服务端关键词、类型或状态筛选。

## 2. 认证接口

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/login` | 否 | 用户名密码登录 |
| POST | `/api/v1/auth/refresh` | 否 | 使用刷新令牌获取新令牌对 |
| GET | `/api/v1/auth/me` | 是 | 获取当前用户基本信息 |
| POST | `/api/v1/auth/logout` | 是 | 无状态客户端退出，不在服务端吊销令牌 |
| POST | `/api/v1/auth/logout-token` | 是 | 吊销指定 access 或 refresh token |
| PUT | `/api/v1/auth/password` | 是 | 修改当前用户密码 |

登录请求：

```json
{
  "username": "admin",
  "password": "Admin@123456"
}
```

登录响应中的 `data`：

```json
{
  "tokens": {
    "access_token": "...",
    "refresh_token": "...",
    "token_type": "Bearer",
    "expires_in": 1800
  },
  "user": {
    "id": "uuid",
    "username": "admin",
    "role": "superadmin"
  }
}
```

刷新、吊销和修改密码请求：

```json
{ "refresh_token": "..." }
```

```json
{ "token": "..." }
```

```json
{
  "old_password": "old-password",
  "new_password": "new-password"
}
```

用户名长度为 3～64，登录密码和新旧密码长度为 8～64。

## 3. 公开内容接口

合法 `moduleKey`：`skills`、`footprints`、`hobbies`、`vibe`、`mylab`。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/public/content` | 聚合存在 `PUBLISHED` 版本的内容模块 |
| GET | `/api/v1/public/content/{moduleKey}` | 获取指定模块当前发布内容 |
| GET | `/api/v1/public/mylab/{postKey}` | 获取当前发布版本中的指定 MyLab 卡片 |

公开响应只保留 `enabled = true` 的内容。模块没有发布版本或已下线时，单模块接口返回 `12002`；MyLab 卡片不存在时返回 `10005`。

MyLab 公开卡片包含按关联顺序解析的 `tag_ids` 和 `tags`，还可能包含：

- `image`：封面公开地址。
- `markdown_url`：Markdown 正文地址。
- `project_contents`：首页项目侧边栏正文，仅供 `PROJECT` 使用。

首页项目不使用独立模块，应从 `mylab.cards` 中筛选 `card_type = PROJECT`，再按 `project_show_order` 排序。

## 4. 后台内容管理

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/admin/content` | 查询五个模块的草稿和线上状态 |
| GET | `/api/v1/admin/content/{moduleKey}` | 查询单个模块管理数据 |
| PUT | `/api/v1/admin/content/{moduleKey}` | 完整保存模块草稿 |
| POST | `/api/v1/admin/content/{moduleKey}/publish` | 校验并发布当前草稿 |
| POST | `/api/v1/admin/content/{moduleKey}/offline` | 下线当前发布版本 |
| GET | `/api/v1/admin/content/{moduleKey}/versions` | 查询非草稿版本列表 |
| GET | `/api/v1/admin/content/{moduleKey}/versions/{versionNo}` | 查询指定历史版本完整数据 |
| POST | `/api/v1/admin/content/{moduleKey}/versions/{versionNo}/restore` | 复制历史版本为新草稿 |
| DELETE | `/api/v1/admin/content/{moduleKey}/draft` | 放弃当前草稿 |

模块管理响应 `data`：

```json
{
  "module_key": "skills",
  "draft_release_id": "uuid",
  "published_release_id": "uuid",
  "draft_data": { "items": [] },
  "published_data": { "items": [] },
  "draft_version": 2,
  "published_version": 1,
  "status": "draft",
  "updated_at": "2026-08-09T10:00:00+08:00",
  "published_at": "2026-08-08T10:00:00+08:00"
}
```

`status` 取值为 `draft`、`published`、`offline`。只要存在草稿，状态就返回 `draft`，此时仍可能同时存在 `published_release_id`。

保存草稿请求：

```json
{
  "expected_updated_at": "2026-08-09T10:00:00+08:00",
  "data": {
    "items": []
  }
}
```

当前不存在草稿时 `expected_updated_at` 可以为 `null`；已存在草稿时必须传上次读取到的 `updated_at`。时间戳不匹配返回 HTTP 409、错误码 `12005`。

### 4.1 模块数据结构

所有集合项通用字段为 `row_id`、`enabled`、`sort_order`。新建项可不传 `row_id`，后端生成 UUID；后台保存时按照数组顺序重写 `sort_order`。

| 模块 | 根数组 | 主要字段 |
| --- | --- | --- |
| `skills` | `items` | `skill_key`、`name`、`percentage`、`level_code`、`level_text`、`icon`、`bar_style`、`is_new`、`enabled` |
| `footprints` | `details` | `city_key`、`title`、`summary`、`contents`、`resource_ids`、`enabled` |
| `hobbies` | `cards` | `hobby_key`、`title`、`description`、`resource_id`、`enabled` |
| `vibe` | `tools` | `tool_key`、`name`、`percentage`、`description`、`enabled` |
| `mylab` | `cards` | 见下方 MyLab 卡片结构 |

发布时主要校验：

- 技能和工具的 `percentage` 必须在 0～100。
- 已启用足迹必须填写 `title`、`summary`、`contents`，图片资源必须是图片 MIME。
- 最多启用 5 张爱好卡片；已启用爱好必须填写标题、描述并选择图片。
- 已启用技能必须填写 `name`、`level_code`、`level_text`。
- 已启用 Vibe 工具必须填写 `name`、`description`。

### 4.2 MyLab 卡片

```json
{
  "post_key": "database-redesign",
  "card_title": "数据库重设计",
  "card_summary": "关系模型与版本发布",
  "post_date": "2026-08-09",
  "tag_ids": ["标签 UUID"],
  "enabled": true,
  "sort_order": 0,
  "card_type": "PROJECT",
  "project_show_order": 0,
  "project_contents": "首页项目侧边栏正文",
  "image_resource_id": "图片资源 UUID",
  "content_resource_id": "Markdown 或纯文本资源 UUID"
}
```

- `card_type` 只允许 `PROJECT`、`ARTICLE`。
- `PROJECT` 发布时必须填写非负且不重复的 `project_show_order` 和 `project_contents`。
- `ARTICLE` 的 `project_show_order`、`project_contents` 必须为 `null` 或不传。
- `tag_ids` 按数组顺序保存，不允许重复，只能引用启用且未删除的全局标签。
- 发布已启用卡片时标题、摘要和 `content_resource_id` 必填。
- `image_resource_id` 只能引用图片；`content_resource_id` 只能引用 `text/markdown` 或 `text/plain`。

## 5. MyLab 全局标签

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/admin/mylab/tags` | 查询全部未删除标签，包含停用标签 |
| POST | `/api/v1/admin/mylab/tags` | 创建标签 |
| PUT | `/api/v1/admin/mylab/tags/{id}` | 完整更新标签 |
| DELETE | `/api/v1/admin/mylab/tags/{id}` | 软删除标签 |

创建和更新请求：

```json
{
  "tag_key": "database",
  "name": "数据库",
  "enabled": true,
  "sort_order": 0
}
```

`tag_key`、`name` 必填且在未删除标签中唯一，`sort_order` 不得为负数。标签不参与内容版本管理；重命名、停用和删除会影响当前版本与历史版本的最终显示。

## 6. 管理员账号

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/v1/users?page=1&page_size=20` | admin | 分页查询管理员 |
| POST | `/api/v1/users` | superadmin | 创建管理员 |
| PUT | `/api/v1/users/{id}` | admin | 更新角色、状态或密码 |
| DELETE | `/api/v1/users/{id}` | superadmin | 软删除管理员 |

创建请求：

```json
{
  "username": "editor01",
  "role": "editor",
  "password": "password123"
}
```

更新请求中的字段均可选：

```json
{
  "role": "admin",
  "is_active": true,
  "password": "new-password"
}
```

用户列表、创建和更新接口统一返回专用用户响应 DTO：

```json
{
  "id": "uuid",
  "username": "editor01",
  "role": "editor",
  "is_active": true,
  "last_login_at": null,
  "created_at": "2026-08-09T10:00:00+08:00",
  "updated_at": "2026-08-09T10:00:00+08:00"
}
```

响应不会包含 `password_hash`、`deleted_at` 等内部字段。

## 7. 文件资源

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/files?page=1&page_size=20` | 分页查询未删除资源 |
| POST | `/api/v1/files/upload` | 以 `multipart/form-data` 上传，文件字段名为 `file` |
| GET | `/api/v1/files/presigned/{id}` | 获取公开图片地址或一小时有效的私有签名地址 |
| DELETE | `/api/v1/files/{id}` | 逻辑删除未被任何内容版本引用的资源 |

文件记录字段：

```json
{
  "id": "uuid",
  "object_key": "images/2026/08/example.png",
  "bucket": "myblog",
  "original_name": "example.png",
  "mime_type": "image/png",
  "size": 1024,
  "created_at": "2026-08-09T10:00:00+08:00",
  "url": "https://..."
}
```

允许的 MIME：`image/png`、`image/jpeg`、`image/jpg`、`image/webp`、`image/gif`、`application/pdf`、`text/markdown`、`text/plain`。图片列表记录可直接包含 `url`；非图片应调用签名地址接口。资源仍被草稿、线上或历史版本引用时删除返回 `10006`。

## 8. 系统接口

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/v1/health` | 否 | 应用、PostgreSQL、Redis、OSS 基础状态 |
| GET | `/api/v1/system/static` | admin | 主机、系统、容量、数据库和应用版本信息 |
| GET | `/api/v1/system/dynamic` | admin | 运行时间、内存、磁盘和数据库实时信息 |

系统接口的 `data` 当前由 `Map` 生成，字段使用 camelCase，例如 `serverIp`、`memoryTotal`、`hostUptime`、`dbConnCount`。

## 9. 当前不提供的接口

当前后端没有访问日志、访问量、访客数、点赞、操作日志、站点设置、通知设置、独立 `projects` 或 `support` 接口。后台不得使用本地模拟数据伪装为服务端功能。

错误码、HTTP 状态与客户端处理方式见《错误码文档》。
