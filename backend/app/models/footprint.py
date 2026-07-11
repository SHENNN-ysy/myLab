"""Footprint ORM model."""
from __future__ import annotations

from sqlalchemy import Boolean, Float, Index, String
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import BaseModel


class Footprint(BaseModel):
    __tablename__ = "footprints"

    name: Mapped[str] = mapped_column(String(64), nullable=False)
    slug: Mapped[str] = mapped_column(String(128), unique=True, index=True, nullable=False)
    tag: Mapped[str | None] = mapped_column(String(64), index=True, nullable=True)
    position_x: Mapped[float] = mapped_column(Float, default=0.0, nullable=False)
    position_y: Mapped[float] = mapped_column(Float, default=0.0, nullable=False)
    is_self: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    tip_data: Mapped[dict | None] = mapped_column(JSONB, nullable=True)
    order_num: Mapped[int] = mapped_column(default=0, nullable=False)

    __table_args__ = (
        Index("ix_footprints_tag_order", "tag", "order_num"),
    )