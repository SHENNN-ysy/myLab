# -*- coding: utf-8 -*-
"""后端分层架构图 v3：职责说明直接写进分层框内。"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(sys.executable).parent.parent.parent))
from daimon_runtime import setup_plot

setup_plot()

import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch

OUT = Path(__file__).parent
TEXT = "#333333"
DESC = "#3d4a5a"


def layer_box(ax, x, y, w, h, title, desc, fc, ec="#3b4a5a"):
    """分层框：框内上方为层名（加粗），下方为职责说明。"""
    ax.add_patch(FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.02,rounding_size=0.08",
                                fc=fc, ec=ec, lw=1.5))
    ax.text(x + w / 2, y + h - 0.5, title, ha="center", va="center",
            fontsize=12.5, fontweight="bold", color="#1a2634")
    ax.text(x + w / 2, y + (h - 0.85) / 2, desc, ha="center", va="center",
            fontsize=9.8, color=DESC, linespacing=1.6)


def arrow(ax, x1, y1, x2, y2, color="#5a6b7d", style="-|>", lw=1.6, ls="-"):
    ax.add_patch(FancyArrowPatch((x1, y1), (x2, y2), arrowstyle=style,
                                 mutation_scale=14, color=color, lw=lw, linestyle=ls))


def main() -> None:
    fig, ax = plt.subplots(figsize=(13.5, 10))
    ax.set_xlim(0, 13.5)
    ax.set_ylim(0, 10.4)
    ax.axis("off")

    ax.text(6.75, 10.05, "后端分层架构（模块化单体 + 端口适配器）", fontsize=15,
            fontweight="bold", ha="center", color="#1a2634")

    # ===== 第一行：controller / application / infrastructure =====
    layer_box(ax, 0.5, 7.4, 4.0, 2.1, "controller",
              "只做 HTTP 协议转换：参数校验、\nDTO 组装、统一错误码响应；\n禁止业务逻辑与直接访问存储",
              "#f9d7da")
    layer_box(ax, 4.75, 7.4, 4.0, 2.1, "application",
              "承载全部业务规则：六大模块服务、\n版本化内容流转、乐观锁并发控制；\n只依赖 repository / port 接口",
              "#d8f0dc")
    layer_box(ax, 9.0, 7.4, 4.0, 2.1, "infrastructure",
              "外部资源的具体实现：持久化、\nRedis、JWT、OSS；实现业务层\n定义的 repository / port 端口",
              "#dfe9f5")

    # ===== 第二行：common =====
    layer_box(ax, 4.75, 4.5, 4.0, 1.9, "common",
              "全局共享契约：Result / ErrorCode、\n异常体系、常量与请求上下文；\n不反向依赖任何层",
              "#f3f0c9")

    # ===== 第三行：starter =====
    layer_box(ax, 4.75, 1.8, 4.0, 1.9, "starter",
              "装配应用骨架：Security、过滤器链、\nBean 配置、启动初始化与定时任务",
              "#e2dcf3")

    # 依赖箭头（与依赖方向一致）
    arrow(ax, 4.5, 8.45, 4.75, 8.45)                      # controller → application
    arrow(ax, 9.0, 8.45, 8.75, 8.45)                      # infrastructure → application
    arrow(ax, 6.75, 7.4, 6.75, 6.4)                       # application → common
    arrow(ax, 2.5, 7.4, 5.4, 6.4, ls="--")                # controller ⇢ common
    arrow(ax, 11.0, 7.4, 8.1, 6.4, ls="--")               # infrastructure ⇢ common
    arrow(ax, 6.75, 3.7, 6.75, 4.5)                       # starter → common
    # starter 装配全部层，不再单独画 starter→application 虚线，避免穿过 common 框

    ax.text(6.75, 0.9,
            "依赖方向由 ArchUnit 强制：controller → application ← infrastructure；"
            "common 不反向依赖任何层；starter 负责装配各层（虚线为辅助依赖）",
            fontsize=9.5, color="#6b7a89", ha="center")

    out = OUT / "backend-layers-v3.png"
    fig.savefig(out, bbox_inches="tight", dpi=160)
    plt.close(fig)
    print("saved:", out)


if __name__ == "__main__":
    main()
