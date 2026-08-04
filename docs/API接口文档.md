# MyBlog API 接口文档

## 1. 文档入口

后端启动后提供以下在线文档：

| 用途 | 地址 |
| --- | --- |
| Swagger 在线调试界面 | `/swagger-ui.html` |
| OpenAPI 3.0 JSON | `/v3/api-docs` |
| OpenAPI 3.0 YAML | `/v3/api-docs.yaml` |

通过项目 Nginx 访问时，例如本地环境，可使用：

- `http://localhost/swagger-ui.html`
- `http://localhost/v3/api-docs`
- `http://localhost/v3/api-docs.yaml`

OpenAPI 规范由后端控制器和模型注解实时生成，以下 Markdown 用于快速查阅。若字段模型发生变化，以运行时 `/v3/api-docs` 为准。

## 2. 通用约定

### 2.1 基础路径

业务接口统一使用 `/api/v1` 前缀。请求和响应字符集为 UTF-8，JSON 字段采用 `snake_case`。

### 2.2 认证方式

除公开接口外，接口使用 JWT Bearer 认证：

```http
Authorization: Bearer <access_token>
```

Swagger UI 调试步骤：

1. 调用 `POST /api/v1/auth/login`。
2. 从响应的 `data.tokens.access_token` 复制访问令牌。
3. 点击 Swagger UI 右上角 `Authorize`。
4. 填入访问令牌后，即可调试带锁标识的接口。

### 2.3 统一响应

成功响应：

```json
{
  "code": 0,
  "message": "成功",
  "data": {},
  "request_id": "01J5MYBLOG7P8ABCDEF12345678",
  "timestamp": 1785832213
}
```

失败响应：

```json
{
  "code": 10007,
  "message": "请求参数校验失败",
  "error": "username: 长度必须在 3 到 64 之间",
  "request_id": "01J5MYBLOG7P8ABCDEF12345678",
  "timestamp": 1785832213
}
```

- `code`：稳定业务码，不能只依据 HTTP 状态判断业务结果。
- `message`：稳定中文说明，可直接用于通用提示。
- `data`：成功时的业务数据；错误响应中为 `null` 并因全局配置省略。
- `error`：可安全展示的参数或业务细节，可能为空。
- `request_id`：请求追踪 ID，同时写入响应头 `X-Request-ID`。
- `timestamp`：响应生成时的 Unix 秒时间戳。

分页数据统一使用：

```json
{
  "records": [],
  "total": 0,
  "page": 1,
  "page_size": 20
}
```

完整错误码参见[错误码文档](./错误码文档.md)。

## 3. 接口清单

“认证”列中，“公开”表示无需令牌，“JWT”表示需要管理员 access token。

### 3.1 健康检查

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/v1/health` | 公开 | 查询应用健康状态 |

### 3.2 认证

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/login` | 公开 | 用户名和密码登录 |
| POST | `/api/v1/auth/refresh` | 公开 | 使用 refresh_token 刷新令牌 |
| GET | `/api/v1/auth/me` | JWT | 获取当前管理员信息 |
| POST | `/api/v1/auth/logout` | JWT | 客户端退出登录 |
| POST | `/api/v1/auth/logout-token` | JWT | 吊销指定令牌 |
| PUT | `/api/v1/auth/password` | JWT | 修改当前管理员密码 |

### 3.3 公开内容

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/v1/public/content` | 公开 | 获取全部在线内容模块 |
| GET | `/api/v1/public/content/{moduleKey}` | 公开 | 获取指定在线内容模块 |
| GET | `/api/v1/public/support` | 公开 | 获取支持页三项计算后统计 |

`moduleKey` 允许值：`skills`、`projects`、`footprints`、`hobbies`、`vibe`、`mylab`、`support`。不存在 `about` 模块，也不提供旧版内容 CRUD 兼容接口。

### 3.4 内容管理

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/v1/admin/content` | JWT | 查询全部内容模块草稿 |
| GET | `/api/v1/admin/content/{moduleKey}` | JWT | 查询指定模块草稿和线上快照 |
| PUT | `/api/v1/admin/content/{moduleKey}` | JWT | 保存草稿，不影响当前线上快照 |
| POST | `/api/v1/admin/content/{moduleKey}/publish` | JWT | 校验并发布草稿 |
| POST | `/api/v1/admin/content/{moduleKey}/offline` | JWT | 下线模块 |
| GET | `/api/v1/admin/content/{moduleKey}/versions` | JWT | 查询发布历史 |
| POST | `/api/v1/admin/content/{moduleKey}/rollback/{version}` | JWT | 将历史快照作为新版本发布 |

内容模块响应的重要字段：

| 字段 | 说明 |
| --- | --- |
| `module_key` | 模块标识 |
| `draft_data` | 当前草稿 JSON |
| `published_data` | 最近一次线上快照 JSON |
| `draft_version` | 草稿版本号 |
| `published_version` | 线上版本号 |
| `status` | `draft`、`published` 或 `offline` |
| `published_at` | 最近发布时间 |

项目发布时会校验 `lab_post_id` 是否引用已发布且启用的 myLab 记录；删除或停用被线上项目引用的 myLab 记录会返回 `12005`。

七类模块的草稿根结构如下。未列出的页面标题、描述、布局、关于我、Time 曲线、地图配置和支持页静态信息均由博客前端代码维护，提交到内容接口会被拒绝。

| 模块 | 草稿根字段 | 集合项主要字段 |
| --- | --- | --- |
| `skills` | `items` | `id`、`name`、`percentage`、`level`、`level_text`、`icon`、`bar_style`、`is_new`、`enabled` |
| `projects` | `items` | `id`、`card_title`、`card_summary`、`detail_title`、`detail_summary`、`tag`、`accent`、`year`、`image`、`image_alt`、`paragraphs`、`tech`、`images`、`lab_post_id`、`enabled` |
| `footprints` | `details` | 稳定 `id`、`title`、`summary`、`paragraphs`、`images`、`cta_text`、`cta_url`；城市集合和顺序不由后台管理 |
| `hobbies` | `cards` | `id`、`title`、`description`、`image`、`image_alt`、`enabled` |
| `vibe` | `tools` | `id`、`name`、`percentage`、`description`、`enabled` |
| `mylab` | `tags`、`posts` | 标签；记录的 `id`、日期、标题、摘要、封面、标签、章节、段落和启用状态 |
| `support` | `visit_base`、`like_count`、`page_view_base` | 三个非负整数，无集合项 |

`support` 的公开响应不暴露基数，只返回：

```json
{
  "visit_count": 12847,
  "like_count": 1023,
  "page_view_count": 68921
}
```

其中访问量为 `visit_base + 独立访问统计`，浏览量为 `page_view_base + 访问日志总数`，点赞数完全由后台手动维护。

### 3.5 文件管理

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/v1/files` | JWT | 分页查询媒体文件 |
| POST | `/api/v1/files/upload` | JWT | 使用 `multipart/form-data` 上传文件，字段名为 `file` |
| GET | `/api/v1/files/presigned/{id}` | JWT | 获取 CDN 或临时签名地址 |
| DELETE | `/api/v1/files/{id}` | JWT | 逻辑删除文件并异步删除对象 |

允许的上传类型：PNG、JPEG、WebP、GIF 和 PDF。文件大小由 `OSS_MAX_FILE_SIZE_MB` 控制，默认 10MB。

### 3.6 访问统计

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/v1/visits/logs/track` | 公开 | 记录访问并累计统计 |
| GET | `/api/v1/visits/stats` | JWT | 查询指定日期及累计统计 |
| GET | `/api/v1/visits/logs` | JWT | 分页查询访问日志 |
| DELETE | `/api/v1/visits/logs/{id}` | JWT | 删除单条访问日志 |
| POST | `/api/v1/visits/logs/batch-delete` | JWT | 删除截止时间以前的访问日志 |
| DELETE | `/api/v1/visits/logs` | JWT | 清空访问日志 |

访问记录接口可使用 `X-Page-Path` 传递前台实际页面路径。

### 3.7 管理员账号

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/v1/users` | JWT | 分页查询管理员账号 |
| POST | `/api/v1/users` | JWT / superadmin | 创建管理员账号 |
| PUT | `/api/v1/users/{id}` | JWT | 更新管理员账号 |
| DELETE | `/api/v1/users/{id}` | JWT / superadmin | 删除管理员账号 |

博客没有前台用户注册功能，本组接口仅管理后台管理员账号。

### 3.8 系统信息

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/v1/system/static` | JWT | 查询应用、JVM、操作系统等静态信息 |
| GET | `/api/v1/system/dynamic` | JWT | 查询内存、线程和运行时间等动态信息 |

## 4. HTTP 状态与业务码

| HTTP 状态 | 典型场景 |
| --- | --- |
| 200 | 请求成功，业务码为 0 |
| 400 | JSON 格式、必填查询参数或参数类型错误 |
| 401 | 登录失败或令牌无效 |
| 403 | 管理员权限不足 |
| 404 | 路由、资源、内容模块或历史版本不存在 |
| 409 | 唯一键、资源状态或内容关联关系冲突 |
| 413 | 上传文件超过大小限制 |
| 415 | 请求或文件媒体类型不支持 |
| 422 | 参数或内容数据校验失败 |
| 429 | 请求频率超过限制 |
| 500 | 未预期服务端错误或数据库错误 |
| 503 | 对象存储不可用 |

调用方应同时记录 HTTP 状态、`code` 和 `request_id`，排查问题时优先使用 `request_id` 关联服务端日志。
