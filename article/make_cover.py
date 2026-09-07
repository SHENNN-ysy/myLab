# -*- coding: utf-8 -*-
"""生成 MyBlog MyLab 详情页头图海报：简约风格，含三张项目截图。

设计约束：
- 头图面板为 aspect-ratio: 16/8 且 object-fit: cover，故画布严格 2:1，避免裁切/变形；
- 技术栈标签完全位于左侧文字区，不与右侧截图卡片重叠。
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

BASE = Path(__file__).parent
FONT_DIR = Path(r"C:\Users\YSY\AppData\Roaming\kimi-desktop\daimon-share\daimon\runtime\python\fonts")
FONT_BOLD = str(FONT_DIR / "NotoSansSC-Bold.ttf")
FONT_REG = str(FONT_DIR / "NotoSansSC-Regular.ttf")

W, H = 2400, 1200  # 严格 2:1，与 MyLab 详情页头图面板一致


def rounded(im: Image.Image, radius: int) -> Image.Image:
    """给图片加圆角，返回 RGBA。"""
    im = im.convert("RGBA")
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, *im.size], radius=radius, fill=255)
    im.putalpha(mask)
    return im


def card(im: Image.Image, w: int, angle: float, frame: int = 14) -> Image.Image:
    """截图缩放到指定宽度，加白边框、圆角、柔和投影，并旋转，返回带透明通道的卡片。"""
    h = round(im.height * w / im.width)
    shot = rounded(im.resize((w, h), Image.LANCZOS), 18)
    cw, ch = w + frame * 2, h + frame * 2
    face = Image.new("RGBA", (cw, ch), (0, 0, 0, 0))
    ImageDraw.Draw(face).rounded_rectangle([0, 0, cw - 1, ch - 1], radius=26, fill=(255, 255, 255, 255))
    face.paste(shot, (frame, frame), shot)

    # 投影：模糊的黑圆角矩形
    pad = 90
    canvas = Image.new("RGBA", (cw + pad * 2, ch + pad * 2), (0, 0, 0, 0))
    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle(
        [pad, pad + 14, pad + cw, pad + ch + 14], radius=26, fill=(30, 60, 110, 90))
    shadow = shadow.filter(ImageFilter.GaussianBlur(28))
    canvas.alpha_composite(shadow)
    canvas.alpha_composite(face, (pad, pad))

    return canvas.rotate(angle, expand=True, resample=Image.BICUBIC)


def vgradient(w: int, h: int, top, bottom) -> Image.Image:
    """竖直渐变底图。"""
    base = Image.new("RGB", (1, h))
    for y in range(h):
        t = y / (h - 1)
        base.putpixel((0, y), tuple(round(a + (b - a) * t) for a, b in zip(top, bottom)))
    return base.resize((w, h))


def decorate(bg: Image.Image) -> None:
    """背景装饰：柔光斑、点阵、线框圆、十字标记与标题小装饰，营造简约设计感。"""
    # 1) 柔光斑：右后大白光 + 左下淡蓝光
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse([1400, -260, 2500, 640], fill=(255, 255, 255, 110))
    gd.ellipse([-300, 820, 560, 1420], fill=(170, 205, 240, 70))
    bg.alpha_composite(glow.filter(ImageFilter.GaussianBlur(120)))

    draw = ImageDraw.Draw(bg)

    # 2) 点阵：左上文字区后方 + 右下角，低存在感点缀
    dot_color = (150, 180, 214, 90)
    for region in [(70, 60, 560, 300), (2050, 940, 2360, 1150)]:
        x0, y0, x1, y1 = region
        for gy in range(y0, y1, 46):
            for gx in range(x0, x1, 46):
                draw.ellipse([gx, gy, gx + 7, gy + 7], fill=dot_color)

    # 3) 线框圆：左下一个大圆弧 + 右上小圆
    draw.arc([-160, 760, 560, 1480], start=270, end=90, fill=(120, 160, 205, 130), width=4)
    draw.ellipse([2180, 60, 2300, 180], outline=(120, 160, 205, 120), width=4)

    # 4) 十字标记：散落三处
    cross_color = (110, 150, 200, 150)
    for cx, cy, s in [(640, 180, 16), (1150, 1050, 14), (2280, 860, 16)]:
        draw.line([cx - s, cy, cx + s, cy], fill=cross_color, width=5)
        draw.line([cx, cy - s, cx, cy + s], fill=cross_color, width=5)

    # 5) 标题上方小装饰条：短粗横线 + 小圆点
    draw.rounded_rectangle([154, 268, 244, 280], radius=6, fill=(44, 78, 120))
    draw.ellipse([258, 266, 282, 290], fill=(91, 164, 230))


def main() -> None:
    bg = vgradient(W, H, (238, 245, 252), (210, 228, 245)).convert("RGBA")
    decorate(bg)

    # 三张截图卡片：后左 MyLab、后右仪表盘、前景首页（全部位于右侧，不压左侧文字区）
    shots = [Image.open(BASE / "assets" / f"shot{i}.png") for i in (1, 2, 3)]
    bg.alpha_composite(card(shots[2], 880, -5), (1050, 80))    # MyLab，左后
    bg.alpha_composite(card(shots[0], 840, 4), (1410, 140))    # 仪表盘，右后
    bg.alpha_composite(card(shots[1], 1000, 0), (1180, 390))   # 首页，前景

    # 左侧文案
    draw = ImageDraw.Draw(bg)
    x = 150
    draw.text((x, 320), "MyBlog", font=ImageFont.truetype(FONT_BOLD, 150), fill=(23, 43, 77))
    draw.text((x + 6, 520), "个人博客系统全栈实践", font=ImageFont.truetype(FONT_BOLD, 64),
              fill=(52, 84, 130))
    draw.text((x + 6, 650), "一个前后端分离、Docker 化部署的个人博客系统",
              font=ImageFont.truetype(FONT_REG, 40), fill=(96, 122, 152))

    # 技术栈标签，两行排列（右侧图片卡片左缘约 x=1270，标签整体限制在左侧区域内）
    tags = [["Vue 3", "Spring Boot", "PostgreSQL"], ["Redis", "Docker", "Jenkins"]]
    font_tag = ImageFont.truetype(FONT_REG, 32)
    for row, tag_row in enumerate(tags):
        tx = x
        ty = 800 + row * 88
        for tag in tag_row:
            tw = draw.textlength(tag, font=font_tag)
            draw.rounded_rectangle([tx, ty, tx + tw + 52, ty + 64], radius=32,
                                   fill=(255, 255, 255, 220), outline=(163, 191, 222), width=2)
            draw.text((tx + 26, ty + 11), tag, font=font_tag, fill=(44, 78, 120))
            tx += tw + 52 + 22
        assert tx - 22 <= 1200, f"标签行超出左侧安全区：{tx}"

    out = BASE / "images" / "cover.png"
    bg.convert("RGB").save(out, quality=95)
    print("saved:", out, bg.size)


if __name__ == "__main__":
    main()
