# -*- coding: utf-8 -*-
"""绘制 MyBlog 前端架构图（分层风格：页面层/组件层/逻辑层/数据层/接口层，前台与后台双列）。"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(sys.executable).parent.parent.parent))
from daimon_runtime import setup_plot

setup_plot()

import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch

OUT = Path(__file__).parent / "images"
OUT.mkdir(parents=True, exist_ok=True)

# 各层配色（容器底色 / 边框色），与系统架构图保持一致
C_PAGE = ("#d7eef2", "#3a9aad")   # 页面层：青
C_COMP = ("#dbe7fd", "#5b8def")   # 组件层：蓝
C_LOGIC = ("#ece4fb", "#9b7ede")  # 逻辑层：紫
C_DATA = ("#fdeed3", "#efa53a")   # 数据层：橙
C_API = ("#dff3da", "#5cb85c")    # 接口层：绿
TEXT = "#333333"


def container(ax, x, y, w, h, title, fc, ec, title_fs=12.5):
    """圆角容器 + 顶部居中标题。"""
    ax.add_patch(FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.15,rounding_size=0.8",
                                fc=fc, ec=ec, lw=1.8))
    ax.text(x + w / 2, y + h - 2.6, title, ha="center", va="center",
            fontsize=title_fs, fontweight="bold", color=TEXT)


def inner(ax, x, y, w, h, text, ec, fs=10.5):
    """容器内的白底功能块。"""
    ax.add_patch(FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.1,rounding_size=0.5",
                                fc="white", ec=ec, lw=1.4))
    ax.text(x + w / 2, y + h / 2, text, ha="center", va="center", fontsize=fs, color=TEXT)


def container_with_rows(ax, x, y, w, h, title, rows, colors, inner_fs=10.5, title_fs=12.5):
    """容器 + 若干行等宽功能块。"""
    fc, ec = colors
    container(ax, x, y, w, h, title, fc, ec, title_fs)
    pad_x, top_off, bot_off, row_gap, col_gap = 2.5, 5.2, 1.8, 1.6, 1.8
    ix, iw = x + pad_x, w - 2 * pad_x
    iy0, iy1 = y + bot_off, y + h - top_off
    n = len(rows)
    rh = (iy1 - iy0 - (n - 1) * row_gap) / n
    for i, row in enumerate(rows):
        ry = iy1 - (i + 1) * rh - i * row_gap
        m = len(row)
        cw = (iw - (m - 1) * col_gap) / m
        for j, label in enumerate(row):
            inner(ax, ix + j * (cw + col_gap), ry, cw, rh, label, ec, inner_fs)


def main() -> None:
    fig, ax = plt.subplots(figsize=(15, 13.2))
    ax.set_xlim(0, 150)
    ax.set_ylim(0, 132)
    ax.axis("off")

    ax.text(75, 129.5, "MyBlog 前端架构图", ha="center", va="center",
            fontsize=19, fontweight="bold", color="#1a2634")

    layers = [
        ("页面层", 100, 125), ("组件层", 66, 97), ("逻辑层", 40, 63),
        ("数据层", 16, 37), ("接口层", 2, 13),
    ]
    for name, y0, y1 in layers:
        ax.text(6, (y0 + y1) / 2, name, ha="center", va="center",
                fontsize=17, fontweight="bold", color="#4a5568")

    LX, CW, GAP = 14, 64, 4       # 左列 x、列宽、列间距
    RX = LX + CW + GAP            # 右列 x = 82

    # ===== 页面层 =====
    container_with_rows(ax, LX, 100, CW, 25, "博客前台页面（views/）",
                        [["HomeView 单页（七大模块）", "MyLabView 实验室"],
                         ["MyLabPostView 文章详情", "NotFoundView 404"]],
                        C_PAGE, inner_fs=9.5)
    container_with_rows(ax, RX, 100, CW, 25, "管理后台页面（views/）",
                        [["Login 登录", "Dashboard 仪表盘"],
                         ["内容管理 ×7 模块页", "系统管理 ×4 页"]],
                        C_PAGE, inner_fs=9.5)

    # ===== 组件层 =====
    container_with_rows(ax, LX, 66, CW, 31, "前台组件（components/）",
                        [["Navigation", "Hero 视差", "ScrollSphere"],
                         ["Skills / Footstep", "Hobbies / Vibe", "Projects / MyLab"],
                         ["About / Contact / Footer", "UI 动效组件 ×6"]],
                        C_COMP, inner_fs=9.5)
    container_with_rows(ax, RX, 66, CW, 31, "后台组件（components/）",
                        [["AdminLayout / Sidebar", "StaticModuleShell"],
                         ["CollectionHeader", "ContentVersionItem"],
                         ["VersionHistoryModal", "OssImageResourcePicker"]],
                        C_COMP, inner_fs=9.5)

    # ===== 逻辑层 =====
    container_with_rows(ax, LX, 40, CW, 23, "前台逻辑（composables/ + utils/）",
                        [["usePublicContent / useLabPosts", "useEngagement / 站点统计"],
                         ["useScrollReveal / 鼠标视差", "markdown / cdn 工具"]],
                        C_LOGIC, inner_fs=9)
    container_with_rows(ax, RX, 40, CW, 23, "后台逻辑（composables/ + utils/）",
                        [["useAuth 令牌管理", "useStaticModule 版本编辑"],
                         ["request 请求拦截", "listEditing / 版本元数据"]],
                        C_LOGIC, inner_fs=9)

    # ===== 数据层 =====
    container_with_rows(ax, LX, 16, CW, 21, "前台数据（公开接口 + 兜底）",
                        [["公开只读内容接口", "浏览 / 点赞 / 统计接口"],
                         ["data/ 静态兜底数据"]],
                        C_DATA, inner_fs=9.5)
    container_with_rows(ax, RX, 16, CW, 21, "后台数据（api/）",
                        [["auth / user / system", "content / mylabTag"],
                         ["file / analytics", "adapter 字段映射"]],
                        C_DATA, inner_fs=9.5)

    # ===== 接口层 =====
    container_with_rows(ax, LX, 2, 134, 11, "后端 REST API（/api/v1，经 Nginx 网关反代）",
                        [["公开接口（只读）", "互动接口", "管理接口（JWT 鉴权）"]],
                        C_API, inner_fs=10.5)

    out = OUT / "frontend-arch.png"
    fig.savefig(out, bbox_inches="tight", dpi=150)
    plt.close(fig)
    print("saved:", out)


if __name__ == "__main__":
    main()
