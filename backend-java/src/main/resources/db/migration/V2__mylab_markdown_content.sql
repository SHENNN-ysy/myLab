-- =============================================================================
-- V2__mylab_markdown_content.sql
-- 目的：将 V1 基线中保存在 OSS 的 MyLab Markdown 正文固化到 mylab_cards 表，
--      随后删除正文资源关联及对应约束。
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. 新增正文列
-- -----------------------------------------------------------------------------
ALTER TABLE public.mylab_cards
    ADD COLUMN markdown_content text;


-- -----------------------------------------------------------------------------
-- 2. 根据资源 object_key 把历史 Markdown 内容迁移到卡片
-- -----------------------------------------------------------------------------
WITH legacy_markdown(object_key, markdown_content) AS (
    VALUES
        ('mylab/first-post.md', $md$
# MyLab 开张：为什么单独开一个实验记录页

## 为什么单独开一页

首页适合展示完整作品，而部署踩坑、算法模板和小工具封装等零散记录需要一个可持续积累的位置。MyLab 就是这些内容的实验记录页。

## 记录原则

每条记录包含日期和标签，正文尽量说明当时为什么这样做。长期来看，决策理由比单纯的操作步骤更有价值。
$md$),

        ('mylab/project-gm1.md', $md$
# Moth and Bat：项目研究记录

## 项目说明

这是一款关于夜晚相遇的解谜游戏。玩家扮演一只飞蛾，在月光下寻找答案。

## 技术栈

Unity、C#、Aseprite。
$md$),

        ('mylab/project-gm2.md', $md$
# Naughty Cat：项目研究记录

## 项目说明

一只总想搞破坏的猫与一个不肯关机的扫地机器人，共同构成了一款轻松幽默的平台跳跃游戏。

## 技术栈

Godot、GDScript。
$md$),

        ('mylab/project-gm3.md', $md$
# Naughty Boy：项目研究记录

## 项目说明

这是一个探索规则边界的叙事游戏，也是一次关于儿童行为心理学隐喻的游戏化实验。

## 技术栈

Phaser、JavaScript。
$md$),

        ('mylab/project-gm4.md', $md$
# Ring of Elysium：项目研究记录

## 项目说明

作为玩法设计师参与腾讯北极光工作室《无限法则》的开发，主要关注玩法与系统设计。

## 技术栈

Unreal Engine、C++、Lua。
$md$),

        ('mylab/project-gm5.md', $md$
# Moodlog：项目研究记录

## 项目说明

Moodlog 是一个极简的情绪记录工具，专注输入体验、趋势回顾与长期记录。

## 技术栈

React、TypeScript、Supabase。
$md$),

        ('mylab/project-gm6.md', $md$
# Beat Lab：项目研究记录

## 项目说明

Beat Lab 是一个浏览器内运行的鼓机与音序器，使用 Web Audio API 实时合成声音。

## 技术栈

Vue、Web Audio API、Tone.js。
$md$),

        ('mylab/blog-docker-deploy.md', $md$
# 个人博客 Docker + Nginx 部署全流程记录

## 多阶段构建

构建阶段使用 Node 镜像安装依赖并执行 Vite 构建，运行阶段只保留 Nginx 和 `dist` 产物。这样可以显著减少最终镜像体积。

`.dockerignore` 应排除 `node_modules` 和本地 `dist`，避免 CI 反复上传无关构建上下文。

## Nginx 配置要点

SPA 需要使用 `try_files $uri $uri/ /index.html` 支持非根路由刷新。带内容哈希的静态资源可以设置长期缓存，而 `index.html` 应保持不缓存。

## 部署与回滚

镜像同时保留时间戳标签和 `latest` 标签。出现问题时直接切回上一个时间戳版本，避免临时重新构建。
$md$),

        ('mylab/tailwind-migration.md', $md$
# 项目迁移 Tailwind CSS v4 的坑

## 配置方式变化

Tailwind CSS v4 使用 CSS-first 配置，可以通过 `@theme` 定义设计变量；PostCSS 插件则调整为 `@tailwindcss/postcss`。

## 迁移注意事项

自定义色板需要改成 `--color-*` 变量。Monorepo 中不在默认扫描范围内的目录需要使用 `@source` 明确声明。
$md$),

        ('mylab/vue-composable-mouse-tilt.md', $md$
# 封装一个 useMouseTilt 组合式函数

## 为什么需要节流

`mousemove` 的触发频率高于屏幕刷新率。使用 `requestAnimationFrame` 合并事件后，每帧最多计算一次倾斜角度。

## 封装思路

组合式函数接收元素引用和最大倾斜角，返回 `rotateX` 与 `rotateY`。组件卸载时清理监听和动画帧，透视效果由父容器统一提供。
$md$),

        ('mylab/vue-gsap-hero.md', $md$
# 用 GSAP 给首页 Hero 做电影感动效

## 分镜思路

先把 Hero 拆成开场定格、文字入场、视差拉开和滚动提示等镜头，再把每个镜头编排到同一条时间线上。

## ScrollTrigger 实践

`scrub` 让动画进度与滚动位置绑定，`pin` 则把 Hero 固定在视口中。Vue 组件卸载时必须清理触发器，避免路由切换后残留滚动状态。

## 性能与降级

动画只操作 `transform` 和 `opacity`，并为 `prefers-reduced-motion` 用户直接展示最终状态。
$md$)
)
UPDATE public.mylab_cards AS mc
   SET markdown_content = legacy.markdown_content,
       updated_at = now()
  FROM public.mylab_resources AS mr
  JOIN public.resources AS resource ON resource.id = mr.content_resource_id
  JOIN legacy_markdown AS legacy ON legacy.object_key = resource.object_key
 WHERE mr.card_id = mc.id
   AND mc.markdown_content IS NULL;


-- -----------------------------------------------------------------------------
-- 3. 校验：确保没有遗漏未迁移的 Markdown 正文
-- -----------------------------------------------------------------------------
DO $migration$
BEGIN
    IF EXISTS (
        SELECT 1
          FROM public.mylab_cards mc
          JOIN public.mylab_resources mr ON mr.card_id = mc.id
         WHERE mr.content_resource_id IS NOT NULL
           AND mc.markdown_content IS NULL
    ) THEN
        RAISE EXCEPTION '存在尚未迁移的 MyLab Markdown 正文，拒绝删除 content_resource_id';
    END IF;
END
$migration$;


-- -----------------------------------------------------------------------------
-- 4. 清理：删除无封面图引用的资源关联、索引、外键、列及约束
-- -----------------------------------------------------------------------------
DELETE FROM public.mylab_resources
 WHERE image_resource_id IS NULL;

DROP INDEX IF EXISTS public.idx_mylab_resources_content;

ALTER TABLE public.mylab_resources
    DROP CONSTRAINT IF EXISTS mylab_resources_content_resource_id_fkey,
    DROP CONSTRAINT IF EXISTS ck_mylab_resources_present,
    DROP COLUMN content_resource_id,
    ADD CONSTRAINT ck_mylab_resources_image_present CHECK (image_resource_id IS NOT NULL);
