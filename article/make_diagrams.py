# -*- coding: utf-8 -*-
"""生成 MyBlog 博客文章的架构配图。"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(sys.executable).parent.parent.parent))
from daimon_runtime import setup_plot

setup_plot()

import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch

OUT = Path(__file__).parent / "images"
OUT.mkdir(parents=True, exist_ok=True)


def box(ax, x, y, w, h, text, fc, ec="#3b4a5a", fs=11, tc="#1a2634", bold=False):
    ax.add_patch(FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.02,rounding_size=0.06",
                                fc=fc, ec=ec, lw=1.4))
    ax.text(x + w / 2, y + h / 2, text, ha="center", va="center", fontsize=fs,
            color=tc, fontweight="bold" if bold else "normal", linespacing=1.5)


def arrow(ax, x1, y1, x2, y2, color="#5a6b7d", style="-|>", lw=1.6, ls="-"):
    ax.add_patch(FancyArrowPatch((x1, y1), (x2, y2), arrowstyle=style,
                                 mutation_scale=14, color=color, lw=lw, linestyle=ls))


# ============ 图 1：整体部署架构 ============
fig, ax = plt.subplots(figsize=(11, 7.2))
ax.set_xlim(0, 11)
ax.set_ylim(0, 7.2)
ax.axis("off")

box(ax, 4.35, 6.35, 2.3, 0.7, "访客 / 管理员", "#dfe9f5", fs=12, bold=True)
box(ax, 3.85, 4.95, 3.3, 0.95, "Nginx 网关\n(443 HTTPS · 唯一对外入口)", "#ffe3c2", fs=11.5, bold=True)

box(ax, 0.5, 3.15, 2.9, 1.0, "frontend-web\n博客前台静态站点\n(Vue 3 + GSAP)", "#d8f0dc", fs=10.5)
box(ax, 4.05, 3.15, 2.9, 1.0, "frontend-admin\n管理后台静态站点\n(Ant Design Vue 4)", "#d8f0dc", fs=10.5)
box(ax, 7.6, 3.15, 2.9, 1.0, "backend\nSpring Boot 3.5 API\n(Java 21 · 仅内网)", "#f9d7da", fs=10.5)

box(ax, 6.6, 1.15, 2.3, 0.95, "PostgreSQL 16\n(Flyway 迁移)", "#e2dcf3", fs=10.5)
box(ax, 9.15, 1.15, 1.7, 0.95, "Redis 7\n(缓存/限流)", "#e2dcf3", fs=10.5)
box(ax, 0.5, 1.15, 2.9, 0.95, "Jenkins CI/CD\n+ 私有 Registry\n(镜像构建与发布)", "#f3f0c9", fs=10.5)

arrow(ax, 5.5, 6.35, 5.5, 5.92)
arrow(ax, 4.6, 4.95, 1.95, 4.17)
arrow(ax, 5.5, 4.95, 5.5, 4.17)
arrow(ax, 6.4, 4.95, 9.05, 4.17)
arrow(ax, 8.6, 3.15, 7.75, 2.12)
arrow(ax, 9.5, 3.15, 10.0, 2.12)
arrow(ax, 3.4, 1.62, 6.6, 1.62, style="-|>", ls="--")
ax.text(5.0, 1.78, "推送镜像 / 拉取部署", fontsize=9, color="#6b7a89", ha="center")

ax.text(5.5, 0.35, "互联网流量统一经 Nginx 分发：/ → 前台，/admin → 后台，/api → 后端；数据库与缓存仅监听内网",
        fontsize=9.5, color="#6b7a89", ha="center")
ax.set_title("MyBlog 整体部署架构", fontsize=15, fontweight="bold", pad=12)
fig.savefig(OUT / "deploy-arch.png", bbox_inches="tight", dpi=160)
plt.close(fig)

# ============ 图 2：后端分层架构 ============
fig, ax = plt.subplots(figsize=(10.5, 6.4))
ax.set_xlim(0, 10.5)
ax.set_ylim(0, 6.4)
ax.axis("off")

box(ax, 0.6, 4.9, 3.0, 1.0, "controller\nREST 接口 · 全局异常处理\n(只做协议转换)", "#f9d7da", fs=10.5)
box(ax, 4.0, 4.9, 3.0, 1.0, "application\n业务层：service / model\nport / repository 接口", "#d8f0dc", fs=10.5, bold=True)
box(ax, 7.4, 4.9, 2.7, 1.0, "infrastructure\n适配层：Persistence\nRedis / JWT / OSS", "#dfe9f5", fs=10.5)
box(ax, 4.0, 3.1, 3.0, 0.9, "common\nResult / ErrorCode / 常量", "#f3f0c9", fs=10.5)
box(ax, 4.0, 1.3, 3.0, 0.9, "starter\n装配层：Security / 过滤器\nBean 配置 / 启动初始化", "#e2dcf3", fs=10.5)

arrow(ax, 3.6, 5.4, 4.0, 5.4)
arrow(ax, 7.4, 5.4, 7.0, 5.4)
arrow(ax, 2.1, 4.9, 4.6, 4.0, ls="--")
arrow(ax, 8.75, 4.9, 6.4, 4.0, ls="--")
arrow(ax, 5.5, 4.9, 5.5, 4.0)
arrow(ax, 5.5, 2.2, 5.5, 3.1)
arrow(ax, 5.5, 2.2, 5.5, 4.9, style="-|>", ls="--")

ax.text(5.5, 0.55, "依赖方向由 ArchUnit 强制：controller → application ← infrastructure，common 不反向依赖，starter 负责装配",
        fontsize=9.5, color="#6b7a89", ha="center")
ax.set_title("后端分层架构（模块化单体 + 端口适配器）", fontsize=15, fontweight="bold", pad=12)
fig.savefig(OUT / "backend-layers.png", bbox_inches="tight", dpi=160)
plt.close(fig)

print("done:", list(OUT.iterdir()))
