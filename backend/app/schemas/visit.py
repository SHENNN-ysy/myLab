"""Visit-log schemas."""
from __future__ import annotations

import uuid
from datetime import datetime
from typing import Optional

from pydantic import BaseModel


class VisitLogOut(BaseModel):
    id: uuid.UUID
    ip: str
    user_agent: str
    path: str
    referer: Optional[str] = None
    user_id: Optional[uuid.UUID] = None
    visited_at: datetime

    class Config:
        from_attributes = True


class VisitStats(BaseModel):
    date: str
    pv: int
    uv: int