# MyBlog API 接口文档

本文档是博客前台 `myblog`、博客后台 `admin` 与 Java 后端联调时共同遵守的目标契约。页面已经确定的内容字段和交互规则是本轮后端改造的依据。接口基础前缀为 `/api/v1`，OpenAPI JSON 位于 `/v3/api-docs`，Swagger UI 位于 `/swagger-ui.html`。

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

## 3. 内容模块总览

合法 `moduleKey` 共 7 个：`home`、`about`、`skills`、`footprints`、`hobbies`、`vibe`、`mylab`。

| 模块 | 页面用途 | 版本化 |
| --- | --- | --- |
| `home` | WELCOME 区域固定六张背景图 | 是 |
| `about` | 头像、个人简介、固定三条简介条目和成分气泡 | 是 |
| `skills` | 技术栈卡片，最多启用 8 条 | 是 |
| `footprints` | 足迹详情和照片墙，最多启用 6 条 | 是 |
| `hobbies` | 爱好卡片及 Time 面板，爱好卡片和 Time 标签各最多启用 5 条 | 是 |
| `vibe` | Vibe Coding 工具，最多启用 6 条 | 是 |
| `mylab` | 项目与文章卡片 | 是 |

MyLab 全局标签不属于版本快照，通过独立标签接口管理。

## 4. 公开内容接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/public/content` | 聚合全部存在 `PUBLISHED` 版本的模块 |
| GET | `/api/v1/public/content/{moduleKey}` | 获取指定模块当前发布内容 |
| GET | `/api/v1/public/mylab/{postKey}` | 获取当前发布版本中的指定 MyLab 卡片详情 |

聚合接口的 `data` 使用模块名作为属性；没有发布版本或已经下线的模块不出现在聚合结果中：

```json
{
  "home": {},
  "about": {},
  "skills": {},
  "footprints": {},
  "hobbies": {},
  "vibe": {},
  "mylab": {}
}
```

公开响应只返回已启用内容，并把资源 ID 转换为可访问 URL。单模块没有发布版本或已经下线时返回 `12002`；MyLab 卡片不存在时返回 `10005`。

首页项目不使用独立模块。前台从 `mylab.cards` 筛选 `card_type = PROJECT`，再按 `project_show_order ASC` 排序。

## 5. 后台内容管理

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/admin/content` | 查询 7 个模块的草稿和线上状态 |
| GET | `/api/v1/admin/content/{moduleKey}` | 查询单个模块的当前内容和草稿内容 |
| PUT | `/api/v1/admin/content/{moduleKey}` | 完整保存模块草稿 |
| POST | `/api/v1/admin/content/{moduleKey}/publish` | 校验并发布当前草稿 |
| POST | `/api/v1/admin/content/{moduleKey}/offline` | 下线当前发布版本 |
| GET | `/api/v1/admin/content/{moduleKey}/versions` | 查询非草稿版本列表 |
| GET | `/api/v1/admin/content/{moduleKey}/versions/{versionNo}` | 查询指定历史版本完整数据 |
| POST | `/api/v1/admin/content/{moduleKey}/versions/{versionNo}/restore` | 提取历史版本内容覆盖当前草稿（无草稿时新建草稿） |
| DELETE | `/api/v1/admin/content/{moduleKey}/versions/{versionNo}` | 软删除指定历史版本并解除其资源引用 |
| DELETE | `/api/v1/admin/content/{moduleKey}/draft` | 放弃当前草稿 |

`DELETE .../versions/{versionNo}` 为软删除：版本本体及其在各模块子表中的数据行统一打 `deleted_at` 标记，版本号不会复用。版本不存在或为草稿返回 `10005`；`PUBLISHED` 线上版本不可删除，返回 `10006`，需先下线或发布新版本。删除后其独占引用的文件资源解除引用，可在文件管理中删除。

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

`status` 取值为 `draft`、`published`、`offline`。只要存在草稿，状态就返回 `draft`，此时仍可同时存在只读的 `published_data`。

保存草稿请求：

```json
{
  "expected_updated_at": "2026-08-09T10:00:00+08:00",
  "data": {}
}
```

当前不存在草稿时 `expected_updated_at` 可以为 `null`；已存在草稿时必须传上次读取到的 `updated_at`。时间戳不匹配返回 HTTP 409、错误码 `12005`。保存采用完整替换语义，数组顺序决定 `sort_order`。

### 5.1 通用集合约定

- 集合项使用 `row_id` 表示版本内数据库行 UUID；新建项可以省略，后端负责生成。
- `*_key` 是跨版本稳定业务键，不使用 `row_id` 代替。
- 管理接口同时返回资源 ID 和资源展示 URL；保存草稿只信任资源 ID，不接受任意 URL 充当资源关联。
- 草稿可以暂时缺少发布必填项；发布时执行完整校验。
- 已发布版本只读，继续编辑时复制为新的草稿版本。

### 5.2 `home`

```json
{
  "images": [
    {
      "row_id": "uuid",
      "image_resource_id": "uuid",
      "image_url": "https://img.example.com/hero-1.webp",
      "alt": "香港太平山城市远景",
      "object_position": "50% 35%",
      "sort_order": 0
    }
  ]
}
```

发布时必须恰好包含 6 张图片；`image_resource_id` 必须引用未删除的图片资源；`alt` 必填；公开接口按 `sort_order ASC` 返回。

### 5.3 `about`

```json
{
  "profile": {
    "title": "关于我",
    "avatar_resource_id": "uuid",
    "avatar_url": "https://img.example.com/avatar.png",
    "avatar_alt": "DNSamuel",
    "intro": "你好，我是 SHENNN……",
    "bullets": ["条目一", "条目二", "条目三"],
    "outro": "努力成长……"
  },
  "ingredients": {
    "title": "我的成分",
    "description": "面板说明"
  },
  "bubbles": [
    {
      "row_id": "uuid",
      "text": "技术探索者",
      "size": "mid",
      "background_color": "#5BA4E6",
      "text_color": "#81D4FA",
      "glow_color": "#5BA4E6",
      "sort_order": 0
    }
  ]
}
```

发布时头像必须引用图片资源；简介条目必须恰好 3 条且均非空；`size` 只允许 `big`、`mid`；三种颜色统一使用 `#RRGGBB`。

### 5.4 `skills`

```json
{
  "items": [
    {
      "row_id": "uuid",
      "skill_key": "java-spring-boot",
      "name": "Java / Spring Boot",
      "percentage": 80,
      "level_code": "proficient",
      "level_text": "熟练",
      "icon_resource_id": "uuid",
      "icon_url": "https://img.example.com/java.svg",
      "bar_style": "coral",
      "is_new": false,
      "enabled": true,
      "sort_order": 0
    }
  ]
}
```

最多启用 8 条；`percentage` 为 0～100；`level_code` 只允许 `novice`、`competent`、`proficient`；已启用记录的名称、等级文字和图片类型图标资源必填。

### 5.5 `footprints`

```json
{
  "details": [
    {
      "row_id": "uuid",
      "city_key": "photo",
      "title": "胶片摄影 · 西安城墙",
      "summary": "摘要",
      "contents": "第一段\n\n第二段",
      "resource_ids": ["图片资源 UUID"],
      "resources": [{ "id": "uuid", "url": "https://img.example.com/xian.webp" }],
      "enabled": true,
      "sort_order": 0
    }
  ]
}
```

最多启用 6 条；已启用记录的标题、摘要和正文必填；照片允许多张并按 `resource_ids` 顺序保存，资源不得重复且必须是图片。

### 5.6 `hobbies`

```json
{
  "cards": [
    {
      "row_id": "uuid",
      "hobby_key": "counter-strike-2",
      "title": "Counter-Strike 2",
      "description": "卡片说明",
      "image_resource_id": "uuid",
      "image_url": "https://img.example.com/cs2.webp",
      "enabled": true,
      "sort_order": 0
    }
  ],
  "time_tags": [
    {
      "row_id": "uuid",
      "data_key": "爱好1",
      "name": "Study",
      "color": "#93C5FD",
      "label_x": 110,
      "label_y": 240,
      "label_scale": 1.5,
      "enabled": true,
      "sort_order": 0
    }
  ],
  "time_points": [
    {
      "age": -1,
      "values": {
        "爱好1": 0,
        "爱好2": 0,
        "爱好3": 0,
        "爱好4": 0,
        "爱好5": 10
      }
    }
  ]
}
```

- 爱好卡片最多启用 5 条；已启用卡片的标题、描述和图片资源必填。
- Time 标签最多启用 5 条，`data_key` 只允许 `爱好1`、`爱好2`、`爱好3`、`爱好4`、`爱好5` 且不得重复。
- 标签坐标范围为 X 0～500、Y 0～300，`label_scale` 为 0.5～3，颜色使用 `#RRGGBB`。
- `time_points` 必须完整覆盖 -1～27 共 29 个年龄；每个 `values` 必须包含五个数据键，每项为 0～10且合计为 10。

### 5.7 `vibe`

```json
{
  "tools": [
    {
      "row_id": "uuid",
      "tool_key": "codex",
      "name": "Codex",
      "percentage": 80,
      "description": "工具说明",
      "enabled": true,
      "sort_order": 0
    }
  ]
}
```

最多启用 6 条；`percentage` 为 0～100；已启用记录的名称和说明必填。

### 5.8 `mylab`

模块草稿只提交卡片；标签通过全局标签接口维护：

```json
{
  "cards": [
    {
      "row_id": "uuid",
      "post_key": "database-redesign",
      "card_title": "数据库重设计",
      "card_summary": "关系模型与版本发布",
      "post_date": "2026-08-09",
      "tag_ids": ["标签 UUID"],
      "card_type": "PROJECT",
      "project_show_order": 0,
      "project_contents": "首页项目侧边栏正文",
      "image_resource_id": "图片资源 UUID",
      "content_resource_id": "Markdown 或纯文本资源 UUID",
      "image_url": "https://img.example.com/cover.webp",
      "markdown_url": "https://img.example.com/post.md",
      "enabled": true,
      "sort_order": 0
    }
  ],
  "tags": []
}
```

- `card_type` 只允许 `PROJECT`、`ARTICLE`。
- `PROJECT` 发布时必须填写非负且不重复的 `project_show_order` 和 `project_contents`。
- `ARTICLE` 的 `project_show_order`、`project_contents` 必须为 `null` 或不传。
- `tag_ids` 按数组顺序保存，不允许重复，只能引用启用且未删除的全局标签。
- 发布已启用卡片时标题、摘要和 `content_resource_id` 必填。
- `image_resource_id` 只能引用图片；`content_resource_id` 只能引用 `text/markdown` 或 `text/plain`。
- 公开接口在 `tags` 中返回有效标签对象，并在卡片中同时返回解析后的标签名称数组。

## 6. MyLab 全局标签

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

## 7. 管理员账号

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

## 8. 文件资源

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/files?page=1&page_size=20&directory=hero` | 分页查询未删除资源，可按目录筛选 |
| POST | `/api/v1/files/upload` | 以 `multipart/form-data` 上传，字段为 `file` 和 `directory` |
| GET | `/api/v1/files/presigned/{id}` | 获取公开图片地址或一小时有效的私有签名地址 |
| GET | `/api/v1/files/{id}/references` | 查询资源被哪些内容版本引用（模块、版本号、版本状态、用途），供删除确认展示 |
| DELETE | `/api/v1/files/{id}` | 逻辑删除未被任何内容版本引用的资源 |

文件记录字段：

```json
{
  "id": "uuid",
  "object_key": "hero/2026/08/example.png",
  "directory": "hero",
  "bucket": "ysy-myblog",
  "original_name": "example.png",
  "mime_type": "image/png",
  "size": 1024,
  "created_at": "2026-08-09T10:00:00+08:00",
  "url": "https://..."
}
```

`directory` 只允许 `footstep`、`hero`、`hobbies`、`icon`、`mylab`、`mylab-post`。`mylab` 只保存 PDF、Markdown 和纯文本正文，其余目录只保存图片；MyLab 卡片封面固定使用 `mylab-post`。CDN 域名为空时图片与文档均使用 OSS 签名地址，配置 CDN 域名后图片自动改用 CDN 地址。资源仍被草稿、线上或历史版本引用时删除返回 `10006`。

## 9. 系统接口

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/v1/health` | 否 | 应用、PostgreSQL、Redis、OSS 基础状态 |
| GET | `/api/v1/system/static` | admin | 主机、系统、容量、数据库和应用版本信息 |
| GET | `/api/v1/system/dynamic` | admin | 运行时间、内存、磁盘和数据库实时信息 |

系统接口的 `data` 当前由 `Map` 生成，字段使用 camelCase，例如 `serverIp`、`memoryTotal`、`hostUptime`、`dbConnCount`。

## 10. 当前不提供的接口

当前后端没有访问日志、访问量、访客数、点赞、操作日志、站点设置、通知设置、独立 `projects` 或 `support` 接口。后台不得使用本地模拟数据伪装为服务端功能。

错误码、HTTP 状态与客户端处理方式见《错误码文档》。
