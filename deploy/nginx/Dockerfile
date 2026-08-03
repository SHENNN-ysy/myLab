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
EXPOSE 80
