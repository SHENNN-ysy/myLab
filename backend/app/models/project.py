"""Project ORM model."""
from __future__ import annotations

from sqlalchemy import Index, Integer, String, Text
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import BaseModel


class Project(BaseModel):
    __tablename__ = "projects"

    title: Mapped[str] = mapped_column(String(128), nullable=False)
    slug: Mapped[str] = mapped_column(String(128), unique=True, index=True, nullable=False)
    description: Mapped[str] = mapped_column(Text, default="", nullable=False)
    content: Mapped[str] = mapped_column(Text, default="", nullable=False)
    tag: Mapped[str | None] = mapped_column(String(64), index=True, nullable=True)
    year: Mapped[int] = mapped_column(Integer, index=True, nullable=False)
    image_url: Mapped[str | None] = mapped_column(String(512), nullable=True)
    project_url: Mapped[str | None] = mapped_column(String(512), nullable=True)
    repo_url: Mapped[str | None] = mapped_column(String(512), nullable=True)
    tech: Mapped[list | None] = mapped_column(JSONB, nullable=True)
    order_num: Mapped[int] = mapped_column(Integer, default=0, nullable=False)

    __table_args__ = (
        Index("ix_projects_year_tag", "year", "tag"),
    )