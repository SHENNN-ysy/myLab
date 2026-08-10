# 项目迁移 Tailwind CSS v4 的坑

## 配置方式变化

Tailwind CSS v4 使用 CSS-first 配置，可以通过 `@theme` 定义设计变量；PostCSS 插件则调整为 `@tailwindcss/postcss`。

## 迁移注意事项

自定义色板需要改成 `--color-*` 变量。Monorepo 中不在默认扫描范围内的目录需要使用 `@source` 明确声明。
