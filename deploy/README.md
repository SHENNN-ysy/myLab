# MyBlog 生产部署与 OSS/CDN 配置

## 1. 准备 OSS

1. 在轻量应用服务器同地域创建私有 OSS Bucket。
2. 创建专用 RAM 用户，只生成用于服务端的 AccessKey。
3. 为应用 RAM 用户授予业务图片前缀的最小权限，示例中的 Bucket 名需替换：

```json
{
  "Version": "1",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["oss:PutObject", "oss:GetObject", "oss:DeleteObject"],
      "Resource": ["acs:oss:*:*:myblog-production/images/*"]
    }
  ]
}
```

静态资源同步建议使用运维账号或单独的RAM策略访问 `static/*`，不要扩大应用账号权限。

## 2. 配置 CDN

1. 创建图片加速域名，例如 `img.example.com`，源站选择上述OSS Bucket。
2. 为私有Bucket开启CDN回源授权，不向前端暴露OSS源站地址。
3. 配置HTTPS证书并将HTTP重定向到HTTPS。
4. 建议缓存规则：`/images/*` 30天，`/static/*` 180天。
5. 根据实际博客与后台域名设置Referer防盗链；是否允许空Referer按分享需求决定。

## 3. 配置环境变量

复制根目录 `.env.example` 为 `.env`，至少替换：

- PostgreSQL、Redis、JWT和初始管理员密码；
- `BLOG_DOMAIN`、`ADMIN_DOMAIN`；
- `OSS_ENDPOINT`：使用同地域内网Endpoint；
- RAM用户的 `OSS_ACCESS_KEY_ID` 与 `OSS_ACCESS_KEY_SECRET`；
- `OSS_BUCKET`、`OSS_CDN_DOMAIN`、`STATIC_CDN_BASE`。

不要提交 `.env`。应用启动不会创建Bucket，Bucket与CDN必须提前准备完成。

## 4. 同步仓库静态图片

先配置阿里云 `ossutil`，再在仓库根目录执行：

```powershell
.\scripts\upload-static-assets-to-oss.ps1 -Bucket myblog-production
```

脚本把 `myblog/public` 原有目录结构上传到 `oss://<Bucket>/static/`。Nginx会把 `/assets/*` 和 `/game_posters/*` 重定向到 `STATIC_CDN_BASE` 对应路径。

## 5. 启动

```powershell
docker compose config
docker compose up -d --build
docker compose ps
```

生产环境只启动 Nginx、Java API、PostgreSQL 和 Redis。PostgreSQL与Redis没有公网端口映射。

当前Nginx容器监听HTTP 80端口。HTTPS可以在阿里云负载均衡/CDN侧终止，或在服务器外层补充证书挂载与443监听。
