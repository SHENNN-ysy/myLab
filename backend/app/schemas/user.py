"""User-related schemas."""
from __future__ import annotations

import uuid
from datetime import datetime
from typing import Optional

from pydantic import BaseModel, EmailStr, Field

from app.common.enums import UserRole


class UserBase(BaseModel):
    username: str = Field(..., min_length=3, max_length=64)
    email: EmailStr
    nickname: Optional[str] = Field(default=None, max_length=64)
    role: UserRole = UserRole.VIEWER


class UserCreate(UserBase):
    password: str = Field(..., min_length=8, max_length=64)


class UserUpdate(BaseModel):
    nickname: Optional[str] = Field(default=None, max_length=64)
    email: Optional[EmailStr] = None
    role: Optional[UserRole] = None
    is_active: Optional[bool] = None
    avatar_url: Optional[str] = None
    password: Optional[str] = Field(default=None, min_length=8, max_length=64)


class UserOut(UserBase):
    id: uuid.UUID
    is_active: bool
    last_login_at: Optional[datetime] = None
    avatar_url: Optional[str] = None
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True