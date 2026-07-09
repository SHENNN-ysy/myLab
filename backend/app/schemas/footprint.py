"""Footprint-related schemas."""
from __future__ import annotations

import uuid
from typing import Any, Dict, Optional

from pydantic import BaseModel, Field


class FootprintBase(BaseModel):
    name: str = Field(..., max_length=64)
    slug: str = Field(..., max_length=128)
    tag: Optional[str] = Field(default=None, max_length=64)
    position_x: float = 0.0
    position_y: float = 0.0
    is_self: bool = False
    tip_data: Optional[Dict[str, Any]] = None
    order_num: int = 0


class FootprintCreate(FootprintBase):
    pass


class FootprintUpdate(BaseModel):
    name: Optional[str] = Field(default=None, max_length=64)
    slug: Optional[str] = Field(default=None, max_length=128)
    tag: Optional[str] = Field(default=None, max_length=64)
    position_x: Optional[float] = None
    position_y: Optional[float] = None
    is_self: Optional[bool] = None
    tip_data: Optional[Dict[str, Any]] = None
    order_num: Optional[int] = None


class FootprintOut(FootprintBase):
    id: uuid.UUID

    class Config:
        from_attributes = True