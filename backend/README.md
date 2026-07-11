# MyBlog Backend

FastAPI + PostgreSQL + Redis + RabbitMQ + MinIO 后端服务。

## 架构概览

| 组件 | 作用 |
|------|------|
| FastAPI | HTTP 接口 |
| SQLAlchemy 2.0 | ORM |
| PostgreSQL 16 | 主数据库 |
| Redis 7 | JWT 黑名单、热点缓存、限流计数 |
| RabbitMQ 3.13 | 异步任务（访问日志持久化、邮件、文件清理、审计） |
| MinIO | 对象存储 |

## 项目结构

```
backend/
├── app/
│   ├── api/v1/endpoints/   # 接口路由
│   ├── core/               # 配置、数据库、安全、客户端
│   ├── common/             # 统一响应、异常、枚举
│   ├── models/             # SQLAlchemy 模型
│   ├── schemas/            # Pydantic schemas
│   ├── repositories/       # 数据访问层
│   ├── services/           # 业务逻辑层
│   ├── messaging/          # RabbitMQ 拓扑 / 生产 / 负载
│   ├── tasks/              # 消费者实现
│   └── middleware/         # 安全头、速率限制
├── worker/                 # 独立 Worker 进程入口
├── tests/                  # pytest
└── docker/api.Dockerfile   # 镜像构建
```

## 启动

### 方式一：Docker Compose（一键启动）

```bash
cd D:\MyBlog
docker compose up -d --build
```

服务启动后：

| 端口 | 服务 |
|------|------|
| 8000 | FastAPI |
| 5432 | PostgreSQL |
| 6379 | Redis |
| 5672 | RabbitMQ |
| 15672 | RabbitMQ 管理控制台 |
| 9000/9001 | MinIO API/控制台 |

### 方式二：本地开发

#### 前置：安装 Python 3.12

Windows 推荐使用 winget：

```powershell
winget install Python.Python.3.12 --source winget --accept-package-agreements --accept-source-agreements --silent
```

装完新开 PowerShell 会话生效。

#### 启动服务

```bash
cd backend
python -m venv .venv && .venv\Scripts\activate     # Windows
# source .venv/bin/activate                         # macOS/Linux
pip install -r requirements.txt
cp .env.example .env
# 启动本地 Postgres / Redis / RabbitMQ / MinIO 后执行：
uvicorn app.main:app --reload
```

另起一个终端启动 Worker：

```bash
python -m worker.main
```

## 初始账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| `admin` | `Admin@123456` | superadmin |

> 启动时会自动建表并写入初始 admin（仅在第一次启动时执行）。

## API 文档

- Swagger UI：`http://localhost:8000/api/docs`
- 健康检查：`http://localhost:8000/api/v1/health`

## 认证

所有写接口需要 `Authorization: Bearer <access_token>`，JWT 包含 `sub/role/type/exp/jti`，登出时将 `jti` 加入 Redis 黑名单。

```bash
# 登录
curl -X POST http://localhost:8000/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@123456"}'
```

## RabbitMQ 拓扑

| Exchange | Type | 路由键示例 |
|----------|------|------------|
| `myblog.events` | topic | `visit.record` `email.notify` `file.cleanup` `audit.log` |

Worker 进程会同时消费 4 个持久队列，所有消息以 `delivery_mode=2` 持久化。

## 测试

```bash
cd backend
pytest -q
```

## 常见脚本

```bash
# 手动触发访问日志上报
curl -X POST http://localhost:8000/api/v1/visits/logs/track

# 上传文件
curl -X POST http://localhost:8000/api/v1/files/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F 'file=@./logo.png'
```
