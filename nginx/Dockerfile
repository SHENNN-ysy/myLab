# ============================================================
# Web 镜像：一个镜像同时构建前台（myblog/）与后台（admin/）两个 Vue 应用，
# 产物由 nginx 按域名分发（配置模板见 nginx/default.conf.template，
# 容器启动时由 envsubst 按 BLOG_DOMAIN 渲染）。
# 基础镜像可通过 build-arg 覆盖，便于从本地镜像仓库拉取。
# ============================================================
ARG NODE_IMAGE=node:20-alpine
ARG NGINX_IMAGE=nginx:1.27-alpine

# ---------- 阶段 1：构建前台（myblog/） ----------
FROM ${NODE_IMAGE} AS blog-builder
WORKDIR /app
# 先复制依赖清单利用 Docker 层缓存，lock 文件不变则跳过 npm ci
COPY myblog/package.json myblog/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY myblog/ ./
# build 内含 vue-tsc 类型检查，类型错误会导致镜像构建失败
RUN npm run build

# ---------- 阶段 2：构建后台（admin/） ----------
FROM ${NODE_IMAGE} AS admin-builder
WORKDIR /app
COPY admin/package.json admin/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY admin/ ./
RUN npm run build

# ---------- 阶段 3：nginx 运行镜像 ----------
FROM ${NGINX_IMAGE}
# 安装时区数据，使 TZ 环境变量生效（容器日志时间与宿主机一致）
RUN apk add --no-cache tzdata
RUN rm /etc/nginx/conf.d/default.conf
# 配置模板放入 templates 目录：官方 nginx 镜像启动时自动 envsubst 渲染到 conf.d
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
# 前台、后台产物分别挂到不同目录，由 nginx 按 server_name 分发
COPY --from=blog-builder /app/dist /usr/share/nginx/blog
COPY --from=admin-builder /app/dist /usr/share/nginx/admin
# 初始内容资源使用站内相对 URL。后台域名也需要能够预览这些资源，
# 因此只复制内容资源目录，不覆盖后台构建生成的 assets 文件。
COPY --from=blog-builder /app/dist/assets/hero /usr/share/nginx/admin/assets/hero
COPY --from=blog-builder /app/dist/assets/404.png /usr/share/nginx/admin/assets/404.png
COPY --from=blog-builder /app/dist/assets/tech-stack-icons /usr/share/nginx/admin/assets/tech-stack-icons
COPY --from=blog-builder /app/dist/mylab /usr/share/nginx/admin/mylab
EXPOSE 80
