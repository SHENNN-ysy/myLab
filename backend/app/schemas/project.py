"""Project-related schemas."""
from __future__ import annotations

import uuid
from typing import List, Optional

from pydantic import BaseModel, Field


class ProjectBase(BaseModel):
    title: str = Field(..., max_length=128)
    slug: str = Field(..., max_length=128)
    description: str = ""
    content: str = ""
    tag: Optional[str] = Field(default=None, max_length=64)
    year: int
    image_url: Optional[str] = Field(default=None, max_length=512)
    project_url: Optional[str] = Field(default=None, max_length=512)
    repo_url: Optional[str] = Field(default=None, max_length=512)
    tech: List[str] = Field(default_factory=list)
    order_num: int = 0


class ProjectCreate(ProjectBase):
    pass


class ProjectUpdate(BaseModel):
    title: Optional[str] = Field(default=None, max_length=128)
    slug: Optional[str] = Field(default=None, max_length=128)
    description: Optional[str] = None
    content: Optional[str] = None
    tag: Optional[str] = Field(default=None, max_length=64)
    year: Optional[int] = None
    image_url: Optional[str] = Field(default=None, max_length=512)
    project_url: Optional[str] = Field(default=None, max_length=512)
    repo_url: Optional[str] = Field(default=None, max_length=512)
    tech: Optional[List[str]] = None
    order_num: Optional[int] = None


class ProjectOut(ProjectBase):
    id: uuid.UUID

    class Config:
        from_attributes = True