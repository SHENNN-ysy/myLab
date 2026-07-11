"""About-bubble ORM model."""
from __future__ import annotations

from sqlalchemy import Boolean, Float, Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from app.common.enums import BubbleTier
from app.models.base import BaseModel


class AboutBubble(BaseModel):
    __tablename__ = "about_bubbles"

    label: Mapped[str] = mapped_column(String(64), unique=True, index=True, nullable=False)
    bg_color: Mapped[str] = mapped_column(String(64), default="#ffffff", nullable=False)
    glow_color: Mapped[str | None] = mapped_column(String(64), nullable=True)
    text_color: Mapped[str] = mapped_column(String(64), default="#000000", nullable=False)
    position_x: Mapped[float] = mapped_column(Float, default=0.0, nullable=False)
    position_y: Mapped[float] = mapped_column(Float, default=0.0, nullable=False)
    radius: Mapped[float] = mapped_column(Float, default=40.0, nullable=False)
    tier: Mapped[str] = mapped_column(String(16), default=BubbleTier.SMALL.value, nullable=False)
    order_num: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    enabled: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    remark: Mapped[str | None] = mapped_column(String(256), nullable=True)
