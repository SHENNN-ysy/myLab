# ============================================================
# 后台管理前端（admin/）镜像：vite 构建 + nginx 托管静态产物。
# 本地和生产 Nginx 网关均按 ADMIN_ROUTE 反代到本服务，
# 本服务不直接暴露主机端口；也可单独构建用于本地调试。
# ============================================================

# ---------- 阶段 1：构建 Vue/TS 应用 ----------
FROM node:20-alpine AS builder

WORKDIR /app

# Compose 或 CI 通过 build arg 注入路径，供 Vite 生成正确的资源基础路径。
ARG ADMIN_ROUTE=/admin
ENV VITE_ADMIN_ROUTE=$ADMIN_ROUTE

# 利用缓存：先只复制 lock 文件，依赖不变时跳过安装
COPY package.json package-lock.json* ./
RUN npm config set registry https://registry.npmmirror.com \
    && npm install --no-audit --no-fund

# 复制源码并构建（build 内含 vue-tsc 类型检查）
COPY . .
RUN npm run build

# ---------- 阶段 2：nginx 托管静态产物 ----------
FROM nginx:1.27-alpine AS runner

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
