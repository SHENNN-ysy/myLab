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
- `BLOG_DOMAIN`：主站域名（前台在 `/`，后台在 `/admin/` 路径，www 前缀自动附带）；
- `CORS_ORIGINS`：改为 `https://<域名>,https://www.<域名>`；
- `OSS_ENDPOINT`：使用同地域内网Endpoint；
- RAM用户的 `OSS_ACCESS_KEY_ID` 与 `OSS_ACCESS_KEY_SECRET`；
- `OSS_BUCKET`、`OSS_CDN_DOMAIN`。

不要提交 `.env`。应用启动不会创建Bucket，Bucket与CDN必须提前准备完成。

## 4. 同步仓库静态图片

先配置阿里云 `ossutil`，再在仓库根目录执行：

```powershell
.\scripts\upload-static-assets-to-oss.ps1 -Bucket myblog-production
```

脚本可把 `myblog/public` 原有目录结构上传到 `oss://<Bucket>/static/` 作为独立备份；当前 Nginx 直接由前台容器提供 `/assets/*` 和 `/game_posters/*` 静态资源，避免未配置 CDN 时产生循环跳转。

## 5. 启动

```powershell
docker compose config
docker compose up -d --build
docker compose ps
```

生产环境只启动 Nginx、Java API、PostgreSQL 和 Redis。PostgreSQL 与 Redis 只绑定 127.0.0.1，无公网暴露。

Nginx 容器监听 443（HTTPS 服务）与 80（仅 301 跳转 HTTPS）。SSL 证书需手动放置到服务器仓库目录的 `nginx/certs/`（该目录已 gitignore，不会随代码分发）：

```bash
nginx/certs/shennn.top.pem  # 含中间证书的完整链
nginx/certs/shennn.top.key  # 私钥，权限建议 600
```

证书续期后替换这两个文件并执行 `docker compose restart nginx` 即可。
