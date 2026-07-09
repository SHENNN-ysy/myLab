"""Auth-related schemas."""
from __future__ import annotations

import uuid
from typing import Optional

from pydantic import BaseModel, EmailStr, Field


class LoginRequest(BaseModel):
    username: str = Field(..., min_length=3, max_length=64)
    password: str = Field(..., min_length=8, max_length=64)


class TokenPair(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    expires_in: int  # seconds for access token


class RefreshRequest(BaseModel):
    refresh_token: str


class PasswordChangeRequest(BaseModel):
    old_password: str = Field(..., min_length=8, max_length=64)
    new_password: str = Field(..., min_length=8, max_length=64)


class CurrentUser(BaseModel):
    id: uuid.UUID
    username: str
    email: EmailStr
    nickname: Optional[str] = None
    role: str
    avatar_url: Optional[str] = None

    class Config:
        from_attributes = True