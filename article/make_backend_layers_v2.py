# -*- coding: utf-8 -*-
"""后端分层架构图 v2：在 backend-layers.png 基础上为每层添加职责说明。"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(sys.executable).parent.parent.parent))
from daimon_runtime import setup_plot

setup_plot()

import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch, Circle

OUT = Path(__file__).parent
TEXT = "#333333"


def box(ax, x, y, w, h, text, fc, ec="#3b4a5a", fs=11, tc="#1a2634", bold=False):
    """圆角分层框。"""
    ax.add_patch(FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.02,rounding_size=0.06",
                                fc=fc, ec=ec, lw=1.4))
    ax.text(x + w / 2, y + h / 2, text, ha="center", va="center", fontsize=fs,
            color=tc, fontweight="bold" if bold else "normal", linespacing=1.5)


def badge(ax, x, y, num, color):
    """框左上角的编号角标。"""
    ax.add_patch(Circle((x, y), 0.28, fc=color, ec="white", lw=1.5, zorder=6))
    ax.text(x, y, str(num), ha="center", va="center", fontsize=10,
            fontweight="bold", color="white", zorder=7)


def arrow(ax, x1, y1, x2, y2, color="#5a6b7d", style="-|>", lw=1.6, ls="-"):
    ax.add_patch(FancyArrowPatch((x1, y1), (x2, y2), arrowstyle=style,
                                 mutation_scale=14, color=color, lw=lw, linestyle=ls))


# 各层说明：编号、名称、职责描述、框体颜色
LAYERS = [
    (1, "controller", "只做 HTTP 协议转换：参数校验、\nDTO 组装、统一错误码响应；\n禁止业务逻辑与直接访问存储", "#f9d7da", "#c45b6a"),
    (2, "application", "承载全部业务规则：六大模块服务、\n版本化内容流转、乐观锁并发控制；\n只依赖 repository / port 接口", "#d8f0dc", "#4c9a63"),
    (3, "infrastructure", "外部资源的具体实现：持久化、\nRedis、JWT、OSS；实现业务层\n定义的 repository / port 端口", "#dfe9f5", "#5b7fbf"),
    (4, "common", "全局共享契约：Result / ErrorCode、\n异常体系、常量与请求上下文；\n不反向依赖任何层", "#f3f0c9", "#b3a135"),
    (5, "starter", "装配应用骨架：Security、过滤器链、\nBean 配置、启动初始化与定时任务", "#e2dcf3", "#8673c2"),
]


def main() -> None:
    fig, ax = plt.subplots(figsize=(16, 6.8))
    ax.set_xlim(0, 18.9)
    ax.set_ylim(0, 7.2)
    ax.axis("off")

    # ===== 左侧：原有分层图 =====
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
    ax.text(5.5, 6.85, "后端分层架构（模块化单体 + 端口适配器）", fontsize=15,
            fontweight="bold", ha="center", color="#1a2634")

    # 编号角标
    badge(ax, 0.75, 5.85, 1, "#c45b6a")
    badge(ax, 4.15, 5.85, 2, "#4c9a63")
    badge(ax, 7.55, 5.85, 3, "#5b7fbf")
    badge(ax, 4.15, 3.95, 4, "#b3a135")
    badge(ax, 4.15, 2.15, 5, "#8673c2")

    # ===== 右侧：各层职责说明 =====
    ax.text(11.35, 6.85, "各层职责说明", fontsize=13, fontweight="bold",
            ha="left", color="#4a5568")
    ys = [5.55, 4.45, 3.35, 2.25, 1.15]
    for (num, name, desc, fc, ec), y in zip(LAYERS, ys):
        ax.add_patch(Circle((10.9, y + 0.32), 0.26, fc=ec, ec="white", lw=1.5, zorder=6))
        ax.text(10.9, y + 0.32, str(num), ha="center", va="center", fontsize=10,
                fontweight="bold", color="white", zorder=7)
        ax.text(11.35, y + 0.32, name, ha="left", va="center", fontsize=11.5,
                fontweight="bold", color=TEXT)
        ax.text(11.35, y - 0.28, desc, ha="left", va="center", fontsize=9.8,
                color="#4a5568", linespacing=1.55)

    out = OUT / "backend-layers-v2.png"
    fig.savefig(out, bbox_inches="tight", dpi=160)
    plt.close(fig)

    # 裁掉四周近白边（去掉右侧多余空白），保留 16px 安全边距
    from PIL import Image, ImageChops
    im = Image.open(out).convert("RGB")
    diff = ImageChops.difference(im, Image.new("RGB", im.size, (255, 255, 255)))
    bbox = diff.point(lambda p: 255 if p > 12 else 0).getbbox()
    if bbox:
        pad = 16
        left, top, right, bottom = bbox
        im.crop((max(left - pad, 0), max(top - pad, 0),
                 min(right + pad, im.width), min(bottom + pad, im.height))).save(out)
    print("saved:", out)


if __name__ == "__main__":
    main()
