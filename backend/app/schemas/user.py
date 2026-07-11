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
    website: Optional[str] = Field(default=None, max_length=256)
    bio: Optional[str] = Field(default=None, max_length=2000)
    password: Optional[str] = Field(default=None, min_length=8, max_length=64)


class UserOut(BaseModel):
    """Response payload for a user.

    ``UserOut`` deliberately re-declares fields from ``UserBase`` with ``str``
    for ``email`` instead of ``EmailStr``. Email-validator (used by Pydantic's
    ``EmailStr``) rejects RFC 6761/6762 reserved names (``*.local``,
    ``*.localhost``, ``*.test``...) which would 500 any admin endpoint trying
    to serialize a seeded admin with such an email. The format is already
    enforced on the write path in ``UserCreate`` / ``UserUpdate``; re-validating
    on read has no security value (the DB row is already trusted).
    """

    id: uuid.UUID
    username: str
    email: str
    nickname: Optional[str] = None
    role: UserRole
    avatar_url: Optional[str] = None
    website: Optional[str] = None
    bio: Optional[str] = None
    is_active: bool
    last_login_at: Optional[datetime] = None
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True