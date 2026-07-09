"""About-bubble schemas."""
from __future__ import annotations

import uuid
from typing import Optional

from pydantic import BaseModel, Field

from app.common.enums import BubbleTier


class AboutBubbleBase(BaseModel):
    label: str = Field(..., max_length=64)
    bg_color: str = Field(default="#ffffff", max_length=64)
    glow_color: Optional[str] = Field(default=None, max_length=64)
    text_color: str = Field(default="#000000", max_length=64)
    position_x: float = 0.0
    position_y: float = 0.0
    radius: float = 40.0
    tier: BubbleTier = BubbleTier.SMALL
    order_num: int = 0
    enabled: bool = True
    remark: Optional[str] = Field(default=None, max_length=256)


class AboutBubbleCreate(AboutBubbleBase):
    pass


class AboutBubbleUpdate(BaseModel):
    label: Optional[str] = Field(default=None, max_length=64)
    bg_color: Optional[str] = Field(default=None, max_length=64)
    glow_color: Optional[str] = Field(default=None, max_length=64)
    text_color: Optional[str] = Field(default=None, max_length=64)
    position_x: Optional[float] = None
    position_y: Optional[float] = None
    radius: Optional[float] = None
    tier: Optional[BubbleTier] = None
    order_num: Optional[int] = None
    enabled: Optional[bool] = None
    remark: Optional[str] = Field(default=None, max_length=256)


class AboutBubbleOut(AboutBubbleBase):
    id: uuid.UUID

    class Config:
        from_attributes = True
