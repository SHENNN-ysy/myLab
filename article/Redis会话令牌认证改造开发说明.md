# Redis 会话令牌认证改造开发说明

## 1. 改造目标与范围

本次改造将管理后台原有的 JWT access/refresh 双令牌认证替换为 Redis 不透明会话令牌，消除 refresh token 刷新、401 请求重放和 JWT 黑名单维护，同时支持按用户批量吊销全部登录会话。

改造范围：

- 管理后台登录、身份认证、退出登录和当前账号修改。
- 后台用户角色、启用状态、密码修改与用户删除。
- 后端 Spring Security 认证过滤器、Redis 会话服务和用户仓储行锁查询。
- 管理后台 Token 存储、Axios 错误处理和登录失效跳转。
- 本地及生产环境配置、Compose 变量和认证相关文档。

不受影响的范围：

- `/api/v1/public/**` 博客前台公开接口。
- 访客浏览、点赞等基于 HMAC 的互动接口。
- PostgreSQL 数据库表结构和 Flyway 迁移脚本。
- Bearer Token 的客户端存储位置仍为 `localStorage`，不改为 Cookie，因此不新增 CSRF 流程。

本次改造没有为 `users` 增加 `auth_version`，也没有新增任何数据库字段或表。登录与敏感账号写操作的并发一致性由 PostgreSQL 用户行锁保证。

## 2. 会话结构

| Redis Key | 类型 | 内容 | TTL |
|---|---|---|---|
| `auth:session:{token_sha256}` | Hash | `user_id`、`created_at`、`last_seen_at` | 空闲 8 小时，认证成功后续期 |
| `auth:user-sessions:{user_id}` | ZSet | member 为 Token SHA-256，score 为会话过期时间戳（毫秒） | 8 小时，创建或认证会话后续期 |

客户端只持有标准小写 UUID 原文，例如：

```text
6cf6ceff-9838-45e8-b196-e83f4dd69327
```

Redis Key 和用户会话索引只保存该 UUID 的 SHA-256 十六进制摘要，不保存可直接用于请求的 Bearer Token。即使 Redis Key 被读取，也不能直接还原客户端凭证。

会话 Hash 只保存用户 ID 和时间字段，不缓存用户名、角色或启用状态。每次认证成功后仍从 PostgreSQL 读取用户的最新状态，避免角色变更或账号停用后继续使用旧权限。

用户 ZSet 是会话反向索引，用于在修改密码、角色、启用状态或删除用户时定位并删除该用户的全部会话。过期成员会在创建或认证会话时通过 score 清理；会话 Hash 自身由 Redis TTL 自动淘汰。

## 3. 登录与认证流程

### 3.1 登录流程

登录在数据库事务中执行：

1. 根据用户名执行 `SELECT ... FOR UPDATE`，锁定目标用户行。
2. 在锁内重新检查用户是否存在、是否启用以及 BCrypt 密码是否匹配。
3. 更新 `last_login_at`。
4. 生成随机 UUID Token，并计算 SHA-256 摘要。
5. 使用 Lua 脚本原子写入会话 Hash 和用户 ZSet，同时设置 8 小时 TTL。
6. 返回单个 access token 和当前用户信息。
7. 数据库事务提交后释放用户行锁。

登录响应保留原有 `tokens` 外层结构，仅删除 `refresh_token`：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "tokens": {
      "access_token": "6cf6ceff-9838-45e8-b196-e83f4dd69327",
      "token_type": "bearer",
      "expires_in": 28800
    },
    "user": {
      "id": "b7b1a013-fc83-579a-a1e3-bb1cc0483bac",
      "username": "admin",
      "role": "superadmin"
    }
  },
  "error": null
}
```

### 3.2 受保护请求认证

管理后台请求继续使用标准请求头：

```http
Authorization: Bearer <uuid-token>
```

认证过滤器按以下顺序处理：

1. 提取并规范校验 UUID Bearer Token。
2. 计算 Token SHA-256 摘要。
3. 执行 Lua 脚本读取 `user_id`、更新 `last_seen_at`，并同时续期会话 Hash 与用户 ZSet。
4. 根据 `user_id` 从 PostgreSQL 读取最新用户记录。
5. 用户存在且启用时，使用最新用户名和角色建立 Spring Security 身份。
6. 用户不存在或已停用时，吊销该用户全部会话，并按未认证请求返回 401。

Token 不存在、已经过期、格式非法或会话身份损坏时，不建立 SecurityContext，最终由 Spring Security 返回统一的 401 响应。

### 3.3 公开接口旁路

会话认证过滤器直接跳过以下路径：

- `POST /api/v1/auth/login`
- `/api/v1/health` 与 Actuator 健康检查
- OpenAPI 和 Swagger UI
- `/api/v1/public` 与 `/api/v1/public/**`

因此 Redis 会话服务故障不会触发博客前台公开接口的会话读取。公开内容和访客互动仍按原有安全规则执行。

## 4. 会话创建、续期与吊销

会话操作均通过 Redis Lua 脚本在服务端原子执行，避免 Hash 与 ZSet 之间出现部分更新。

### 4.1 创建会话 `issue(User)`

- 写入会话 Hash 的 `user_id`、`created_at` 和 `last_seen_at`。
- 设置会话 Hash 的空闲超时。
- 清除用户 ZSet 中 score 已过期的成员。
- 将当前摘要写入用户 ZSet，score 设置为当前时间加空闲超时。
- 刷新用户 ZSet 的 TTL。

### 4.2 认证与滑动续期 `authenticate(token)`

- 会话 Hash 不存在时返回认证失败。
- 会话存在时更新 `last_seen_at`。
- 刷新会话 Hash 的 TTL。
- 清除对应用户 ZSet 中的过期成员。
- 更新当前摘要的过期 score，并刷新 ZSet TTL。

滑动 TTL 表示 `expires_in=28800` 是空闲超时时间，不是从首次登录开始计算的固定绝对有效期。每次成功认证都会重新获得完整的 8 小时空闲时间。

### 4.3 单会话注销 `revoke(token)`

- 读取会话 Hash 中的 `user_id`。
- 删除当前会话 Hash。
- 从用户 ZSet 移除当前 Token 摘要。
- ZSet 已无成员时删除整个索引 Key。
- 会话已经不存在时保持幂等。

`POST /api/v1/auth/logout` 不再接收请求体，直接注销 Authorization 请求头中的当前会话，不影响同一用户在其他设备上的登录。

### 4.4 批量注销 `revokeAll(userId)`

- 从用户 ZSet 读取全部 Token 摘要。
- 删除每个摘要对应的会话 Hash。
- 删除该用户的 ZSet 反向索引。

以下操作成功时会调用 `revokeAll`：

- 当前用户修改密码。
- 当前用户修改账号名称，或同时修改密码。
- 后台修改用户角色、启用状态或密码。
- 后台删除用户。

## 5. 并发一致性

### 5.1 用户行锁

`UserRepository` 新增两个锁定查询：

- `findByUsernameForUpdate(username)`：登录时按用户名锁定用户行。
- `findByIdForUpdate(id)`：账号修改、用户更新和删除时按 ID 锁定用户行。

`UserMapper` 使用 MyBatis-Plus 查询条件追加 PostgreSQL `FOR UPDATE`。锁只用于登录和敏感账号写操作，普通后台读取不加锁。

### 5.2 登录与账号修改竞态

两个并发操作锁定同一用户行，因此结果由取得锁的先后顺序确定：

1. 登录先取得锁：登录在旧账号状态下创建会话；随后账号修改取得锁并执行 `revokeAll`，刚创建的会话会被删除。
2. 修改先取得锁：登录等待事务提交，随后重新读取更新后的密码、角色或启用状态，并按新数据完成校验。

这种处理不依赖数据库版本字段，也不会遗留使用旧密码或旧权限创建的有效会话。

### 5.3 数据库与 Redis 故障边界

Redis 会话操作不是 PostgreSQL 事务资源，两者无法组成单一 ACID 事务，因此采用安全优先的故障语义：

- 登录创建会话失败：抛出 503，登录事务回滚。
- 敏感账号修改时 `revokeAll` 失败：抛出 503，数据库修改回滚，避免账号已变更但旧会话仍有效。
- Redis 吊销成功但数据库提交失败：会话可能被提前踢出，但账号数据不会产生权限安全不一致；用户重新登录即可恢复。

## 6. 异常处理

### 6.1 Redis 会话不可用

`RedisSessionService` 将 Spring Data Redis 的 `DataAccessException` 统一转换为 `AuthenticationUnavailableException`：

| code | HTTP | 枚举 | 含义 |
|---|---:|---|---|
| `10014` | 503 | `AUTHENTICATION_UNAVAILABLE` | 认证服务暂不可用 |

登录和受保护接口遇到 Redis 故障时返回 503。管理后台不会把 503 当成登录过期，不会删除本地 Token，也不会跳转登录页，待 Redis 恢复后可继续使用原会话。

### 6.2 认证失败

普通认证失败仍返回 HTTP 401。管理后台收到非登录请求的 401 时：

- 只清理一次本地 access token 和用户信息。
- 多个并发 401 只触发一次登录页跳转。
- 不调用 refresh 接口。
- 不重放原始请求。

错误码 `10002` 和 `10003` 保留原编号以避免历史文档或客户端产生编号歧义，但已经标记为废弃。新增代码不再使用 JWT 过期或 JWT 吊销语义。

## 7. 代码改动

### 7.1 Application Port 与响应模型

#### `backend-java/src/main/java/com/myblog/application/port/SessionService.java`

- 新增管理会话端口，定义 `issue()`、`authenticate()`、`revoke()` 和 `revokeAll()`。
- Application 层只依赖会话能力，不直接依赖 `StringRedisTemplate` 或 Lua 实现。

#### `backend-java/src/main/java/com/myblog/application/port/SessionIdentity.java`

- 会话身份只包含 `userId`。
- 用户名、角色和启用状态不信任 Redis 缓存，每次认证从数据库重新读取。

#### `backend-java/src/main/java/com/myblog/application/model/vo/AccessTokenVO.java`

- 保留 `access_token`、`token_type` 和 `expires_in`。
- 删除 `refresh_token` 及 refresh token 过期时间。

#### 删除的 JWT 类型

- 删除 `TokenService`、`TokenClaims` 和 `TokenPairVO`。
- 删除 JWT 过期、吊销相关异常类型。

### 7.2 Application Service 与用户仓储

#### `backend-java/src/main/java/com/myblog/application/service/auth/AuthServiceImpl.java`

- `login()` 改为锁定用户行后校验凭证并签发 Redis 会话。
- `change()` 修改密码后吊销当前用户全部会话。
- `updateAccount()` 修改用户名或密码后吊销全部会话。
- 敏感操作与会话吊销位于同一个数据库事务方法中。

#### `backend-java/src/main/java/com/myblog/application/service/user/UserServiceImpl.java`

- 更新用户前使用 `findByIdForUpdate()` 锁定目标行。
- 角色、启用状态或密码实际发生请求更新时调用 `revokeAll()`。
- 删除用户前锁定目标行，删除后调用 `revokeAll()`。

#### `backend-java/src/main/java/com/myblog/application/repository/UserRepository.java`

- 新增 `findByUsernameForUpdate()` 和 `findByIdForUpdate()` 仓储方法。
- 不改变已有用户实体和数据库结构。

#### `backend-java/src/main/java/com/myblog/infrastructure/persistence/mapper/user/UserMapper.java`

- 使用 PostgreSQL `SELECT ... FOR UPDATE` 实现两个锁定查询。

### 7.3 Redis Adapter 与认证过滤器

#### `backend-java/src/main/java/com/myblog/infrastructure/security/RedisSessionService.java`

- 使用 UUID 生成客户端不透明 Token，并在服务端计算 SHA-256 摘要。
- 定义会话 Hash 与用户 ZSet Key 前缀。
- 使用四段 Lua 脚本原子完成创建、认证续期、当前会话注销和全部会话注销。
- 统一把 Redis 数据访问异常转换为 503 认证服务异常。

#### `backend-java/src/main/java/com/myblog/infrastructure/security/SessionAuthenticationFilter.java`

- 替换原 `JwtFilter`。
- 从 Bearer Token 认证 Redis 会话，并从数据库加载最新用户状态与权限。
- 用户不存在或停用时吊销全部会话。
- 公开接口、登录、健康检查和 Swagger 路径不访问管理会话 Redis。
- Filter 位于 MVC 之前，Redis 故障时直接输出统一 `Result` 格式的 503 响应。

#### 删除的 JWT 实现

- 删除 `JwtService`、`JwtUtil` 和 `JwtFilter`。
- 从 Maven 依赖中删除 JJWT API、实现和 Jackson 模块。
- 删除 JWT 黑名单与 refresh token 流程。

### 7.4 Controller 与安全配置

#### `backend-java/src/main/java/com/myblog/controller/AuthController.java`

- 登录返回单个 Redis 会话 Token。
- `/auth/logout` 不再接收请求体，注销 Authorization 中的当前 Token。
- 删除 `/auth/refresh` 和 `/auth/logout-token`。
- 删除 refresh 与 logout-token 相关 DTO。

#### `backend-java/src/main/java/com/myblog/starter/config/SecurityConfig.java`

- 将 `SessionAuthenticationFilter` 注册到 Spring Security 过滤链。
- 继续使用无状态 SecurityContext，不创建 Servlet Session。
- 保留公开接口白名单和统一 401/403 JSON 响应。

#### `backend-java/src/main/java/com/myblog/common/enumeration/ErrorCode.java`

- 新增 `AUTHENTICATION_UNAVAILABLE`，code 为 `10014`，HTTP 状态为 503。
- 保留并废弃旧的 JWT 过期和 Token 吊销错误码编号。

### 7.5 管理后台

#### `admin/src/utils/request.ts`

- 请求拦截器只注入单个 access token。
- 删除 refresh 请求、刷新队列、`_retry` 标记和 401 请求重放。
- 非登录请求返回 401 时清理本地会话，并确保并发请求只跳转一次。
- 503 走普通错误提示流程，保留本地登录状态。

#### `admin/src/api/auth.ts`

- 登录只读取 `tokens.access_token`。
- 退出接口不再发送 Token 请求体。
- 无论服务端退出是否成功，用户主动退出时都会清理本地状态。

#### `admin/src/utils/storage.ts`

- 删除 `REFRESH_TOKEN` 存储 Key。
- 初始化时清除浏览器中遗留的 `myblog_admin_refresh_token`。

#### `admin/src/stores/authStore.ts`

- 新增 `clearSession()`，用于服务端已经批量吊销会话后的纯本地清理。

#### `SystemSettings.tsx` 与 `UserManage.tsx`

- 当前账号信息、密码、角色或启用状态更新成功后，直接清理本地状态并跳转登录页。
- 不再使用已经失效的 Token 请求 logout。

### 7.6 配置注册与部署传递

#### `backend-java/src/main/java/com/myblog/common/properties/SessionProperties.java`

- 新增 `app.session.idle-timeout` 配置绑定。
- 校验空闲超时必须大于 0，并转换为 Redis `EXPIRE` 使用的秒数。

#### `backend-java/src/main/resources/application.yml`

- 新增 `app.session.idle-timeout: ${SESSION_IDLE_TIMEOUT:8h}`。
- `ENGAGEMENT_HASH_SECRET` 改为独立必填，不再回退使用 `JWT_SECRET`。
- 删除新程序对 JWT 密钥和 JWT 有效期配置的绑定。

#### `.env.example` 与 `deploy/.env.example`

- 新增会话空闲时间配置。
- 保留旧 `JWT_SECRET` 仅供回滚观察期使用。
- 将访客互动哈希密钥配置为独立值。

#### `docker-compose.yml` 与 `deploy/docker-compose.yml`

- 将 `SESSION_IDLE_TIMEOUT` 传入 backend 容器。
- 继续传递旧 `JWT_SECRET` 以支持旧镜像回滚，但新应用不读取该变量。

当前配置如下：

```env
SESSION_IDLE_TIMEOUT=8h
JWT_SECRET=change-this-jwt-secret
ENGAGEMENT_HASH_SECRET=change-this-engagement-hash-secret
```

`JWT_SECRET` 与 `ENGAGEMENT_HASH_SECRET` 必须使用不同的随机值。稳定观察期结束并确认不再回滚旧 JWT 镜像后，可以从服务器环境和 Compose 中删除 `JWT_SECRET`。

### 7.7 测试构建支持

#### `backend-java/pom.xml`

- 删除 JJWT 依赖。
- Surefire 与 Failsafe 将 `java.io.tmpdir` 指向 Maven `target`，避免 Windows 上 JUnit 临时目录受系统目录锁影响而清理失败。

## 8. 测试代码位置

### `backend-java/src/test/java/com/myblog/AuthApiIT.java`

- 验证登录响应只包含 UUID access token，不包含 refresh token。
- 验证真实 Redis 会话 Hash、用户 ZSet、SHA-256 Key 和 8 小时 TTL。
- 验证 `/auth/me` 成功认证并触发滑动续期。
- 验证退出当前会话不影响同用户其他会话。
- 验证账号修改批量吊销全部会话。
- 验证登录与修改密码并发执行后不会遗留旧凭证会话。
- 验证缺失、非法和已注销 Token 返回 401。

### `backend-java/src/test/java/com/myblog/UserApiIT.java`

- 验证后台修改用户角色后目标用户全部会话失效。
- 验证删除用户后目标用户全部会话失效。

### `backend-java/src/test/java/com/myblog/infrastructure/security/SessionAuthenticationFilterTest.java`

- 验证公开接口完全不调用管理会话 Redis。
- 验证 Redis 故障返回 HTTP 503 和 code `10014`，不会被转换为 401。

### `AuthServiceImplTest` 与 `UserServiceImplTest`

- 验证登录使用锁定查询并创建会话。
- 验证修改密码、账号、角色、状态和删除用户时调用 `revokeAll()`。

### 删除的测试

- 删除只针对 JWT 签名、解析、刷新和黑名单行为的 `JwtServiceTest`。

## 9. 测试结果

### 后端完整质量门

执行：

```bash
cd backend-java
mvn verify
```

实际结果：

- 单元测试：270 个，全部通过。
- Testcontainers 集成测试：35 个，全部通过。
- Checkstyle：通过。
- ArchUnit：通过。
- SpotBugs：0 个问题。
- JaCoCo 报告：生成成功。
- Maven：`BUILD SUCCESS`。

### 管理后台

执行：

```bash
cd admin
npm run lint
npm run build
```

实际结果：

- ESLint：0 个错误；保留 3 个与本次认证改造无关的既有警告。
- TypeScript 类型检查：通过。
- Vite 生产构建：成功。
- 构建仍有既有的大体积 chunk 提示，不影响构建结果。

## 10. 上线与回滚

backend 与 admin 必须使用同一版本发布。若只更新一侧，旧后台可能继续调用已经删除的 refresh 接口，或新后台无法识别旧 JWT 双令牌响应。

上线影响：

- 现有 JWT access token 和 refresh token 不会被新版本识别。
- 所有后台用户上线后需要重新登录一次。
- 浏览器初始化时会自动清除遗留 refresh token。
- 博客前台公开访问和访客互动不需要重新认证。

回滚观察期：

- 服务器暂时保留旧 `JWT_SECRET`，供旧镜像回滚使用。
- 新版本不读取 `JWT_SECRET`。
- 回滚旧版本后，新 Redis UUID 会话不会被旧 JWT 程序识别，后台用户同样需要重新登录。
- 稳定后删除旧 JWT 密钥，避免长期保留废弃凭据。

## 11. 上线检查

### 11.1 API 验收

1. 登录后确认响应只有 `access_token`、`token_type` 和 `expires_in`，没有 `refresh_token`。
2. 调用 `/api/v1/auth/me`，确认用户信息正常返回。
3. 在浏览器 Network 中确认不再出现 `/api/v1/auth/refresh` 或 `/api/v1/auth/logout-token`。
4. 同一用户登录两个浏览器，退出其中一个，确认另一个会话仍可使用。
5. 修改用户密码、角色或启用状态，确认该用户所有浏览器均在下一次请求时返回 401。
6. 暂停 Redis 后访问公开博客，确认公开接口仍可用；访问后台受保护接口应返回 503。

### 11.2 Redis 检查

登录后可在 Redis 容器中检查会话：

```bash
docker compose exec redis redis-cli SCAN 0 MATCH 'auth:session:*' COUNT 100
docker compose exec redis redis-cli SCAN 0 MATCH 'auth:user-sessions:*' COUNT 100
docker compose exec redis redis-cli HGETALL auth:session:<token_sha256>
docker compose exec redis redis-cli TTL auth:session:<token_sha256>
docker compose exec redis redis-cli ZRANGE auth:user-sessions:<user_id> 0 -1 WITHSCORES
docker compose exec redis redis-cli TTL auth:user-sessions:<user_id>
```

Redis 中不应出现客户端 UUID 原文、密码、用户名或角色。会话 Hash Key 的摘要部分应为 64 位小写十六进制字符串。

### 11.3 故障语义检查

- Redis 正常、Token 无效：返回 401，后台清理本地状态并跳转登录。
- Redis 故障、Token 原本有效：返回 503，后台保留本地状态并提示认证服务暂不可用。
- 公开接口访问：无论管理会话 Redis 是否可用，均不应因为后台认证过滤器失败。
- 敏感账号修改时 Redis 吊销失败：接口返回 503，数据库修改应回滚。
