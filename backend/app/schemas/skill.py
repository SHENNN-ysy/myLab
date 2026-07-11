"""Skill-related schemas."""
from __future__ import annotations

import uuid
from typing import Optional

from pydantic import BaseModel, Field

from app.common.enums import SkillLevel


class SkillBase(BaseModel):
    name: str = Field(..., max_length=64)
    category: str = Field(default="default", max_length=64)
    percentage: int = Field(default=0, ge=0, le=100)
    level: SkillLevel = SkillLevel.INTERMEDIATE
    icon: Optional[str] = Field(default=None, max_length=256)
    order_num: int = 0
    bar_style: Optional[str] = Field(default=None, max_length=32)


class SkillCreate(SkillBase):
    pass


class SkillUpdate(BaseModel):
    name: Optional[str] = Field(default=None, max_length=64)
    category: Optional[str] = Field(default=None, max_length=64)
    percentage: Optional[int] = Field(default=None, ge=0, le=100)
    level: Optional[SkillLevel] = None
    icon: Optional[str] = Field(default=None, max_length=256)
    order_num: Optional[int] = None
    bar_style: Optional[str] = Field(default=None, max_length=32)


class SkillOut(SkillBase):
    id: uuid.UUID

    class Config:
        from_attributes = True