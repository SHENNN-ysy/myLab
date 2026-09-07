# -*- coding: utf-8 -*-
"""后端分层架构图 v4：卡片式设计，职责说明在卡片内，整体更美观。"""
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

BASE = Path(__file__).parent
FONT_DIR = Path(r"C:\Users\YSY\AppData\Roaming\kimi-desktop\daimon-share\daimon\runtime\python\fonts")
FONT_BOLD = str(FONT_DIR / "NotoSansSC-Bold.ttf")
FONT_REG = str(FONT_DIR / "NotoSansSC-Regular.ttf")

W, H = 2400, 1800
TEXT = "#1a2634"
DESC = "#51606f"

# 层配色（顶条 / 角标）
LAYERS = {
    "controller":     dict(color="#e0607e", role="协议转换层",
                           desc=["只做 HTTP 协议转换：参数校验、", "DTO 组装、统一错误码响应；", "禁止业务逻辑与直接访问存储"]),
    "application":    dict(color="#3fa45b", role="业务核心层",
                           desc=["承载全部业务规则：六大模块服务、", "版本化内容流转、乐观锁并发控制；", "只依赖 repository / port 接口"]),
    "infrastructure": dict(color="#5b7fd4", role="适配实现层",
                           desc=["外部资源的具体实现：持久化、", "Redis、JWT、OSS；实现业务层", "定义的 repository / port 端口"]),
    "common":         dict(color="#c9a227", role="共享契约层",
                           desc=["全局共享契约：Result / ErrorCode、", "异常体系、常量与请求上下文；", "不反向依赖任何层"]),
    "starter":        dict(color="#8b6fd8", role="装配启动层",
                           desc=["装配应用骨架：Security、过滤器链、", "Bean 配置、启动初始化与定时任务"]),
}


def vgradient(w, h, top, bottom):
    """竖直渐变底图。"""
    base = Image.new("RGB", (1, h))
    for y in range(h):
        t = y / (h - 1)
        base.putpixel((0, y), tuple(round(a + (b - a) * t) for a, b in zip(top, bottom)))
    return base.resize((w, h))


def draw_card(bg, x, y, w, h, name, cfg):
    """白卡片：柔和投影 + 彩色顶条（层名 + 角色标签）+ 职责描述。"""
    # 投影
    pad = 60
    shadow = Image.new("RGBA", (w + pad * 2, h + pad * 2), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle([pad, pad + 12, pad + w, pad + h + 12],
                                             radius=30, fill=(30, 60, 110, 60))
    bg.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(24)), (x - pad, y - pad))

    card = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    cd = ImageDraw.Draw(card)
    cd.rounded_rectangle([0, 0, w - 1, h - 1], radius=30, fill=(255, 255, 255, 255))

    # 顶条（先画圆角再补方底边，保持只圆上角）
    strip_h = 118
    cd.rounded_rectangle([0, 0, w - 1, strip_h + 30], radius=30, fill=cfg["color"])
    cd.rectangle([0, strip_h - 4, w - 1, strip_h + 30], fill=cfg["color"])

    f_name = ImageFont.truetype(FONT_BOLD, 52)
    f_role = ImageFont.truetype(FONT_REG, 30)
    cd.text((w / 2, 40), name, font=f_name, anchor="ma", fill="white")
    cd.text((w / 2, 92), cfg["role"], font=f_role, anchor="ma", fill=(255, 255, 255, 215))

    # 职责描述（左对齐 + 彩色小圆点）
    f_desc = ImageFont.truetype(FONT_REG, 31)
    line_h = 62
    y0 = strip_h + 58
    for i, line in enumerate(cfg["desc"]):
        ly = y0 + i * line_h
        cd.ellipse([64, ly + 14, 78, ly + 28], fill=cfg["color"])
        cd.text((100, ly), line, font=f_desc, fill=DESC)

    bg.alpha_composite(card, (x, y))


def draw_arrow(draw, x1, y1, x2, y2, color="#7a8a9a", width=5, dashed=False):
    """带箭头线；dashed 时画虚线。"""
    dx, dy = x2 - x1, y2 - y1
    length = math.hypot(dx, dy)
    ux, uy = dx / length, dy / length
    head = 22
    ex, ey = x2 - ux * head, y2 - uy * head  # 箭头底边中心
    if dashed:
        dash, gap, t = 16, 12, 0.0
        while t < length - head:
            t2 = min(t + dash, length - head)
            draw.line([x1 + ux * t, y1 + uy * t, x1 + ux * t2, y1 + uy * t2],
                      fill=color, width=width)
            t = t2 + gap
    else:
        draw.line([x1, y1, ex, ey], fill=color, width=width)
    # 三角箭头
    px, py = -uy, ux
    draw.polygon([(x2, y2),
                  (ex + px * head * 0.55, ey + py * head * 0.55),
                  (ex - px * head * 0.55, ey - py * head * 0.55)], fill=color)


def main():
    bg = vgradient(W, H, (245, 249, 253), (219, 232, 246)).convert("RGBA")

    # 背景点缀：柔光斑 + 点阵
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse([1750, -350, 2650, 550], fill=(255, 255, 255, 120))
    gd.ellipse([-350, 1250, 550, 2050], fill=(170, 205, 240, 70))
    bg.alpha_composite(glow.filter(ImageFilter.GaussianBlur(140)))
    dd = ImageDraw.Draw(bg)
    for gy in range(80, 260, 42):
        for gx in range(80, 420, 42):
            dd.ellipse([gx, gy, gx + 6, gy + 6], fill=(150, 180, 214, 80))
    for gy in range(1560, 1720, 42):
        for gx in range(2040, 2340, 42):
            dd.ellipse([gx, gy, gx + 6, gy + 6], fill=(150, 180, 214, 80))

    # 标题
    dd.text((W / 2, 96), "后端分层架构", font=ImageFont.truetype(FONT_BOLD, 76),
            anchor="ma", fill=TEXT)
    dd.text((W / 2, 176), "模块化单体 + 端口适配器 · 依赖方向由 ArchUnit 强制约束",
            font=ImageFont.truetype(FONT_REG, 34), anchor="ma", fill="#5a6b7d")

    # 卡片布局
    cw, ch = 660, 470
    y1 = 300                       # 第一排
    x_ctl, x_app, x_inf = 120, 870, 1620
    draw_card(bg, x_ctl, y1, cw, ch, "controller", LAYERS["controller"])
    draw_card(bg, x_app, y1, cw, ch, "application", LAYERS["application"])
    draw_card(bg, x_inf, y1, cw, ch, "infrastructure", LAYERS["infrastructure"])

    y2, ch2 = 890, 400             # common
    draw_card(bg, x_app, y2, cw, ch2, "common", LAYERS["common"])

    y3 = 1410                      # starter
    draw_card(bg, x_app, y3, cw, 340, "starter", LAYERS["starter"])

    # 依赖箭头
    ac = "#7a8a9a"
    draw_arrow(dd, x_ctl + cw, y1 + ch / 2, x_app, y1 + ch / 2, ac)            # controller → application
    draw_arrow(dd, x_inf, y1 + ch / 2, x_app + cw, y1 + ch / 2, ac)            # infrastructure → application
    draw_arrow(dd, x_app + cw / 2 - 40, y1 + ch, x_app + cw / 2 - 40, y2, ac)  # application → common
    draw_arrow(dd, x_ctl + 330, y1 + ch, x_app + 150, y2, ac, dashed=True)     # controller ⇢ common
    draw_arrow(dd, x_inf + 330, y1 + ch, x_app + cw - 150, y2, ac, dashed=True)  # infrastructure ⇢ common
    draw_arrow(dd, x_app + cw / 2 + 40, y3, x_app + cw / 2 + 40, y2 + ch2, ac)  # starter → common

    # 图例
    lx = 120
    ly = 1786
    dd.line([lx, ly - 12, lx + 70, ly - 12], fill=ac, width=5)
    draw_arrow(dd, lx + 45, ly - 12, lx + 70, ly - 12, ac)
    dd.text((lx + 90, ly - 30), "直接依赖", font=ImageFont.truetype(FONT_REG, 26), fill="#5a6b7d")
    draw_arrow(dd, lx + 260, ly - 12, lx + 330, ly - 12, ac, dashed=True)
    dd.text((lx + 350, ly - 30), "辅助依赖（仅使用 common 契约）",
            font=ImageFont.truetype(FONT_REG, 26), fill="#5a6b7d")

    out = BASE / "backend-layers-v4.png"
    bg.convert("RGB").save(out, quality=95)
    print("saved:", out, bg.size)


if __name__ == "__main__":
    main()
