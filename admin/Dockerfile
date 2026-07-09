# Stage 1: Build the Vue/TS app
FROM node:20-alpine AS builder

WORKDIR /app

# 利用缓存：先只复制 lock 文件
COPY package.json package-lock.json* ./
RUN npm config set registry https://registry.npmmirror.com \
    && npm install --no-audit --no-fund

# 复制源码并构建
COPY . .
RUN npm run build

# Stage 2: Serve via nginx
FROM nginx:1.27-alpine AS runner

# 删除默认站点配置
RUN rm /etc/nginx/conf.d/default.conf

# 复制自定义 nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
