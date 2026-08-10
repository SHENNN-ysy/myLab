ARG NODE_IMAGE=node:20-alpine
ARG NGINX_IMAGE=nginx:1.27-alpine

FROM ${NODE_IMAGE} AS blog-builder
WORKDIR /app
COPY myblog/package.json myblog/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY myblog/ ./
RUN npm run build

FROM ${NODE_IMAGE} AS admin-builder
WORKDIR /app
COPY admin/package.json admin/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY admin/ ./
RUN npm run build

FROM ${NGINX_IMAGE}
RUN rm /etc/nginx/conf.d/default.conf
COPY deploy/nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=blog-builder /app/dist /usr/share/nginx/blog
COPY --from=admin-builder /app/dist /usr/share/nginx/admin
# 初始内容资源使用站内相对 URL。后台域名也需要能够预览这些资源，
# 因此只复制内容资源目录，不覆盖后台构建生成的 assets 文件。
COPY --from=blog-builder /app/dist/assets/hero /usr/share/nginx/admin/assets/hero
COPY --from=blog-builder /app/dist/assets/avatar.png /usr/share/nginx/admin/assets/avatar.png
COPY --from=blog-builder /app/dist/assets/tech-stack-icons /usr/share/nginx/admin/assets/tech-stack-icons
COPY --from=blog-builder /app/dist/game_posters /usr/share/nginx/admin/game_posters
COPY --from=blog-builder /app/dist/mylab /usr/share/nginx/admin/mylab
EXPOSE 80
