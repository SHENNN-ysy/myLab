# CI 已完成前端构建，本镜像只封装已验证的 dist，并记录构建路径。
FROM nginx:1.27-alpine

ARG OCI_VERSION
ARG OCI_REVISION
ARG OCI_CREATED
ARG ADMIN_ROUTE
LABEL org.opencontainers.image.title="myblog-admin" \
      org.opencontainers.image.version="$OCI_VERSION" \
      org.opencontainers.image.revision="$OCI_REVISION" \
      org.opencontainers.image.created="$OCI_CREATED" \
      com.myblog.admin-route="$ADMIN_ROUTE"

RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY dist /usr/share/nginx/html

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost/ || exit 1
CMD ["nginx", "-g", "daemon off;"]
