# 个人博客 Docker + Nginx 部署全流程记录

## 多阶段构建

构建阶段使用 Node 镜像安装依赖并执行 Vite 构建，运行阶段只保留 Nginx 和 `dist` 产物。这样可以显著减少最终镜像体积。

`.dockerignore` 应排除 `node_modules` 和本地 `dist`，避免 CI 反复上传无关构建上下文。

## Nginx 配置要点

SPA 需要使用 `try_files $uri $uri/ /index.html` 支持非根路由刷新。带内容哈希的静态资源可以设置长期缓存，而 `index.html` 应保持不缓存。

## 部署与回滚

镜像同时保留时间戳标签和 `latest` 标签。出现问题时直接切回上一个时间戳版本，避免临时重新构建。
