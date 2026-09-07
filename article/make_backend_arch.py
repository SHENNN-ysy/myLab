# -*- coding: utf-8 -*-
"""绘制 MyBlog 后端架构图（分层风格：接口层/装配层/业务层/适配层/公共层）。"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(sys.executable).parent.parent.parent))
from daimon_runtime import setup_plot

setup_plot()

import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch, Rectangle, Ellipse, Arc

OUT = Path(__file__).parent / "images"
OUT.mkdir(parents=True, exist_ok=True)

# 各层配色（容器底色 / 边框色），与系统架构图保持一致
C_IF = ("#d7eef2", "#3a9aad")     # 接口层：青
C_STARTER = ("#dbe7fd", "#5b8def")  # 装配层：蓝
C_BIZ = ("#fdeed3", "#efa53a")    # 业务层：橙
C_ADAPT = ("#ece4fb", "#9b7ede")  # 适配层：紫
C_COMMON = ("#dff3da", "#5cb85c") # 公共层：绿
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


def cylinder(ax, cx, y, w, h, label, ec):
    """数据库圆柱图标：纯白桶身，顶部完整椭圆、底部弧线。"""
    eh = h * 0.12
    ax.add_patch(Rectangle((cx - w / 2, y + eh), w, h - 2 * eh, fc="white", ec="none", zorder=2))
    ax.add_patch(Ellipse((cx, y + eh), w, 2 * eh, fc="white", ec="none", zorder=2))
    ax.add_patch(Arc((cx, y + eh), w, 2 * eh, theta1=180, theta2=360, ec=ec, lw=1.6, zorder=3))
    ax.plot([cx - w / 2, cx - w / 2], [y + eh, y + h - eh], color=ec, lw=1.6, zorder=3)
    ax.plot([cx + w / 2, cx + w / 2], [y + eh, y + h - eh], color=ec, lw=1.6, zorder=3)
    ax.add_patch(Ellipse((cx, y + h - eh), w, 2 * eh, fc="white", ec=ec, lw=1.6, zorder=4))
    ax.text(cx, y + (h - eh) / 2, label, ha="center", va="center",
            fontsize=9.5, color=TEXT, zorder=6)


def main() -> None:
    fig, ax = plt.subplots(figsize=(15, 13.2))
    ax.set_xlim(0, 150)
    ax.set_ylim(0, 132)
    ax.axis("off")

    ax.text(75, 129.5, "MyBlog 后端架构图", ha="center", va="center",
            fontsize=19, fontweight="bold", color="#1a2634")

    layers = [
        ("接口层", 100, 125), ("装配层", 72, 97), ("业务层", 46, 69),
        ("适配层", 22, 43), ("公共层", 2, 19),
    ]
    for name, y0, y1 in layers:
        ax.text(6, (y0 + y1) / 2, name, ha="center", va="center",
                fontsize=17, fontweight="bold", color="#4a5568")

    LX = 14  # 容器区左边界

    # ===== 接口层（controller）=====
    container_with_rows(ax, LX, 100, 40, 25, "公开接口（Public*Controller）",
                        [["内容浏览"], ["互动埋点"], ["健康检查"]], C_IF, inner_fs=10)
    container_with_rows(ax, 58, 100, 52, 25, "管理接口（Admin*Controller）",
                        [["登录认证", "内容管理"], ["文件 / 用户", "标签 / 数据分析"]],
                        C_IF, inner_fs=10)
    container_with_rows(ax, 114, 100, 34, 25, "全局异常处理",
                        [["GlobalExceptionHandler"], ["统一错误码响应"]], C_IF, inner_fs=10)

    # ===== 装配层（starter）=====
    container_with_rows(ax, LX, 72, 40, 25, "安全装配",
                        [["SecurityConfig"], ["JwtFilter 鉴权"], ["BCrypt 密码"]],
                        C_STARTER, inner_fs=10)
    container_with_rows(ax, 58, 72, 52, 25, "过滤器链",
                        [["RateLimitFilter 限流", "TraceIdFilter 追踪"],
                         ["WebFilters 请求上下文"]], C_STARTER, inner_fs=10)
    container_with_rows(ax, 114, 72, 34, 25, "启动与配置",
                        [["StartupInitializer"], ["Async / Scheduling"], ["MyBatis / OpenAPI"]],
                        C_STARTER, inner_fs=10)

    # ===== 业务层（application）=====
    container_with_rows(ax, LX, 46, 84, 23, "业务服务（application/service）",
                        [["auth 认证", "user 用户", "content 内容"],
                         ["file 文件", "engagement 互动", "system 系统"]],
                        C_BIZ, inner_fs=10)
    container_with_rows(ax, 102, 46, 46, 23, "模型与契约（model / port）",
                        [["命令对象 Command", "视图对象 VO"],
                         ["repository 接口", "port 接口"]],
                        C_BIZ, inner_fs=10)

    # ===== 适配层（infrastructure）=====
    container_with_rows(ax, LX, 22, 40, 21, "持久化适配",
                        [["Mapper（MyBatis-Plus）"], ["repository 实现"]], C_ADAPT, inner_fs=10)
    container_with_rows(ax, 58, 22, 52, 21, "安全与存储适配",
                        [["JwtService / JwtUtil", "OssObjectStorageAdapter"]],
                        C_ADAPT, inner_fs=10)
    container_with_rows(ax, 114, 22, 34, 21, "互动计数适配",
                        [["Redis Lua 原子计数"], ["EngagementSnapshotJob"]], C_ADAPT, inner_fs=9.5)

    # ===== 公共层（common + 存储）=====
    container_with_rows(ax, LX, 2, 76, 17, "公共契约（common）",
                        [["Result / ErrorCode", "异常体系"],
                         ["请求上下文", "配置属性 / 常量"]],
                        C_COMMON, inner_fs=10)
    container(ax, 94, 2, 54, 17, "公共存储", *C_COMMON)
    for cx, label in [(106, "PostgreSQL 16"), (121, "Redis 7"), (136, "阿里云 OSS")]:
        cylinder(ax, cx, 4, 12, 11.5, label, C_COMMON[1])

    out = OUT / "backend-arch.png"
    fig.savefig(out, bbox_inches="tight", dpi=150)
    plt.close(fig)
    print("saved:", out)


if __name__ == "__main__":
    main()
