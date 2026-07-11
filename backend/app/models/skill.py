"""Skill ORM model."""
from __future__ import annotations

from sqlalchemy import Index, Integer, String
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import BaseModel


class Skill(BaseModel):
    __tablename__ = "skills"

    name: Mapped[str] = mapped_column(String(64), nullable=False)
    category: Mapped[str] = mapped_column(String(64), default="default", index=True, nullable=False)
    percentage: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    level: Mapped[str] = mapped_column(String(32), default="intermediate", nullable=False)
    icon: Mapped[str | None] = mapped_column(String(256), nullable=True)
    order_num: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    bar_style: Mapped[str | None] = mapped_column(String(32), nullable=True)
    extra: Mapped[dict | None] = mapped_column(JSONB, nullable=True)

    __table_args__ = (
        Index("ix_skills_category_order", "category", "order_num"),
    )