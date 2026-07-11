# MyBlog Admin

MyBlog 博客系统的后台管理前端，基于 Vue 3 + Element Plus 构建。

## 功能特性

- **仪表盘** - 统计数据卡片、技能雷达图、项目年份分布图、操作日志
- **技术栈管理** - 增删改查，支持熟练度滑块、等级标签、进度条风格、实时预览
- **项目管理** - 增删改查，支持分类筛选、封面图预览、技术栈标签组
- **足迹管理** - 增删改查，集成地图预览，支持坐标微调
- **系统设置** - 基本信息、联系方式、主题配置、数据导入导出重置

## 技术栈

- Vue 3 + Composition API
- Vite
- TypeScript
- Element Plus
- VueUse
- ECharts 5
- SCSS
- RemixIcon
- localStorage（数据持久化）

## 项目结构

```
admin/
├── src/
│   ├── api/              # API 接口层
│   │   ├── auth.ts      # 认证
│   │   ├── skill.ts     # 技术栈
│   │   ├── project.ts   # 项目
│   │   ├── footprint.ts  # 足迹
│   │   └── log.ts       # 操作日志
│   ├── components/
│   │   └── layout/      # 布局组件
│   ├── composables/     # 组合式函数
│   ├── router/          # 路由配置
│   ├── styles/          # 全局样式
│   ├── types/           # TypeScript 类型
│   ├── utils/           # 工具函数
│   └── views/           # 页面视图
│       ├── dashboard/
│       ├── content/
│       └── system/
├── package.json
└── vite.config.ts
```

## 开发

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build
```

## 演示账号

- 用户名: `admin`
- 密码: `admin123`

## 数据说明

当前版本使用 localStorage 存储数据，可在浏览器开发者工具中清除。后续可接入后端 API 实现真正的数据持久化。

## 界面预览

与 FlecBlog Admin 后台保持一致的设计风格：

- 侧边栏: `#304156` 深灰蓝色
- 顶栏: 白色背景
- 内容区: `#f0f2f5` 浅灰背景
- 主题色: Element Plus 默认蓝 `#409EFF`
