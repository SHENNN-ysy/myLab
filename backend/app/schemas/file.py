"""File-related schemas."""
from __future__ import annotations

import uuid
from datetime import datetime

from pydantic import BaseModel


class FileOut(BaseModel):
    id: uuid.UUID
    object_key: str
    bucket: str
    original_name: str
    mime_type: str
    size: int
    url: str | None = None
    created_at: datetime

    class Config:
        from_attributes = True