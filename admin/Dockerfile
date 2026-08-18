# ============================================================
# 后台管理前端（admin/）独立构建镜像
# 注意：docker-compose.yml 的 web 服务使用根目录 nginx/Dockerfile
# 同时构建前台+后台（推荐的生产部署方式）；本文件仅用于单独构建/调试后台。
# 基础镜像固定从本地镜像仓库 localhost:5000 拉取（离线/加速），
# 需先执行根目录 sync-images-to-local-registry.ps1 同步基础镜像。
# ============================================================

# ---------- 阶段 1：构建 Vue/TS 应用 ----------
FROM localhost:5000/node:20-alpine AS builder

WORKDIR /app

# 利用缓存：先只复制 lock 文件，依赖不变时跳过安装
COPY package.json package-lock.json* ./
RUN npm config set registry https://registry.npmmirror.com \
    && npm install --no-audit --no-fund

# 复制源码并构建（build 内含 vue-tsc 类型检查）
COPY . .
RUN npm run build

# ---------- 阶段 2：nginx 托管静态产物 ----------
FROM localhost:5000/nginx:1.27-alpine AS runner

# 删除默认站点配置，替换为本项目的配置（含 /api 反向代理，见 admin/nginx.conf）
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

# 每 30s 探测一次站点可用性，连续 3 次失败标记 unhealthy
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
