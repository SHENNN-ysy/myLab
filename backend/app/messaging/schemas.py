"""Message payload schemas exchanged via RabbitMQ."""
from __future__ import annotations

from datetime import datetime
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field


class VisitRecordPayload(BaseModel):
    ip: str
    user_agent: str
    path: str
    referer: Optional[str] = None
    user_id: Optional[str] = None
    visited_at: datetime = Field(default_factory=datetime.utcnow)


class EmailNotifyPayload(BaseModel):
    to: List[str]
    subject: str
    template: str
    context: Dict[str, Any] = Field(default_factory=dict)
    severity: str = "info"  # info | warning | critical
    category: str = "general"  # security | notification | general


class FileCleanupPayload(BaseModel):
    object_key: str
    bucket: str
    reason: str = "manual"
    file_id: Optional[str] = None
    requested_by: Optional[str] = None


class AuditLogPayload(BaseModel):
    user_id: Optional[str] = None
    action: str
    resource: str
    resource_id: Optional[str] = None
    ip: Optional[str] = None
    metadata: Dict[str, Any] = Field(default_factory=dict)
    occurred_at: datetime = Field(default_factory=datetime.utcnow)