# -*- coding: utf-8 -*-
"""绘制 MyBlog 系统架构图（分层风格：展现层/接入层/应用层/领域层/基建层）。"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(sys.executable).parent.parent.parent))
from daimon_runtime import setup_plot

setup_plot()

import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch, Rectangle, Ellipse, Arc

OUT = Path(__file__).parent / "images"
OUT.mkdir(parents=True, exist_ok=True)

# 各层配色（容器底色 / 边框色）
C_VIEW = ("#d7eef2", "#3a9aad")   # 展现层：青
C_EDGE = ("#dbe7fd", "#5b8def")   # 接入层：蓝
C_APP = ("#fdeed3", "#efa53a")    # 应用层：橙
C_DOMAIN = ("#ece4fb", "#9b7ede") # 领域层：紫
C_INFRA = ("#dff3da", "#5cb85c")  # 基建层：绿
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
    """数据库圆柱图标：纯白桶身，顶部完整椭圆、底部弧线（与参考图一致）。"""
    eh = h * 0.12  # 椭圆半高
    # 桶身与底部先铺白色，避免容器底色沁入
    ax.add_patch(Rectangle((cx - w / 2, y + eh), w, h - 2 * eh, fc="white", ec="none", zorder=2))
    ax.add_patch(Ellipse((cx, y + eh), w, 2 * eh, fc="white", ec="none", zorder=2))
    # 底部只画可见的下半弧
    ax.add_patch(Arc((cx, y + eh), w, 2 * eh, theta1=180, theta2=360, ec=ec, lw=1.6, zorder=3))
    # 两侧竖线
    ax.plot([cx - w / 2, cx - w / 2], [y + eh, y + h - eh], color=ec, lw=1.6, zorder=3)
    ax.plot([cx + w / 2, cx + w / 2], [y + eh, y + h - eh], color=ec, lw=1.6, zorder=3)
    # 顶部完整椭圆
    ax.add_patch(Ellipse((cx, y + h - eh), w, 2 * eh, fc="white", ec=ec, lw=1.6, zorder=4))
    ax.text(cx, y + (h - eh) / 2, label, ha="center", va="center",
            fontsize=9.5, color=TEXT, zorder=6)


def main() -> None:
    fig, ax = plt.subplots(figsize=(15, 12.8))
    ax.set_xlim(0, 150)
    ax.set_ylim(0, 131)
    ax.axis("off")

    ax.text(75, 128.5, "MyBlog 系统架构图", ha="center", va="center",
            fontsize=19, fontweight="bold", color="#1a2634")

    layers = [
        ("界面层", 104, 124), ("接入层", 76, 100), ("应用层", 46, 72),
        ("领域层", 28, 44), ("基建层", 3, 26),
    ]
    for name, y0, y1 in layers:
        ax.text(6, (y0 + y1) / 2, name, ha="center", va="center",
                fontsize=17, fontweight="bold", color="#4a5568")

    LX, LW = 14, 134  # 容器区左右边界

    # ===== 界面层 =====
    container_with_rows(ax, LX, 104, 65, 20, "博客前台（Vue 3 + TypeScript）",
                        [["GSAP 动效", "Tailwind CSS 4"], ["七大内容模块展示", "MyLab 文章"]],
                        C_VIEW)
    container_with_rows(ax, 83, 104, 65, 20, "管理后台（Vue 3 + TypeScript）",
                        [["Ant Design Vue 4", "ECharts 5"], ["版本化内容管理", "文件 / 用户管理"]],
                        C_VIEW)

    # ===== 接入层 =====
    container_with_rows(ax, LX, 88.5, LW, 11.5, "Nginx 网关（唯一对外入口）",
                        [["路由分发", "HTTPS 终止", "静态站点托管", "Jenkins 反向代理"]],
                        C_EDGE, inner_fs=10)
    container_with_rows(ax, LX, 76, LW, 10.5, "安全过滤链",
                        [["JWT 鉴权", "Redis 限流", "TraceId 全链追踪", "全局异常处理"]],
                        C_EDGE, inner_fs=10)

    # ===== 应用层 =====
    container_with_rows(ax, LX, 46, 40, 26, "认证与用户",
                        [["登录认证", "刷新令牌"], ["用户管理", "账号安全"]], C_APP)
    container_with_rows(ax, 58, 46, 52, 26, "内容中心（版本化：草稿 / 发布 / 历史版本）",
                        [["首页图片", "关于我", "技术栈", "足迹"], ["爱好", "Vibe Coding", "MyLab"]],
                        C_APP, inner_fs=9.5)
    container_with_rows(ax, 114, 46, 34, 26, "支撑服务",
                        [["文件管理（OSS）"], ["互动计数"], ["系统信息"]], C_APP)

    # ===== 领域层 =====
    container_with_rows(ax, LX, 28, 40, 16, "内容版本模型",
                        [["草稿 / 发布 / 下线"], ["历史版本恢复"]], C_DOMAIN, inner_fs=10)
    container_with_rows(ax, 58, 28, 52, 16, "文件与互动模型",
                        [["文件元数据", "浏览 / 点赞计数"], ["命令对象（Command）", "乐观锁版本戳"]],
                        C_DOMAIN, inner_fs=10)
    container_with_rows(ax, 114, 28, 34, 16, "公共契约",
                        [["Result / ErrorCode"], ["repository / port 接口"]], C_DOMAIN, inner_fs=10)

    # ===== 基建层 =====
    container_with_rows(ax, LX, 3, 80, 23, "基础设施适配器",
                        [["Persistence（MyBatis-Plus）", "JWT / BCrypt 安全"],
                         ["OSS 适配器", "Redis Lua 计数"],
                         ["Flyway 迁移", "HikariCP 连接池"]],
                        C_INFRA, inner_fs=10)
    # 公共存储容器 + 圆柱
    container(ax, 98, 3, 50, 23, "公共存储", *C_INFRA)
    for cx, label in [(110.5, "PostgreSQL 16"), (123.5, "Redis 7"), (136.5, "阿里云 OSS")]:
        cylinder(ax, cx, 5, 11, 14, label, C_INFRA[1])

    out = OUT / "system-arch.png"
    fig.savefig(out, bbox_inches="tight", dpi=150)
    plt.close(fig)
    print("saved:", out)


if __name__ == "__main__":
    main()
