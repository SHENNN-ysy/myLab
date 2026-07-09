"""Publish helpers that build + send each typed payload."""
from __future__ import annotations

import json

from app.messaging.rabbitmq import publish
from app.messaging.schemas import (
    AuditLogPayload,
    EmailNotifyPayload,
    FileCleanupPayload,
    VisitRecordPayload,
)
from app.messaging.topology import RoutingKey


def _send(routing_key: str, payload: BaseModel | dict) -> None:
    data = payload.model_dump(mode="json") if hasattr(payload, "model_dump") else payload
    publish(routing_key, json.dumps(data, default=str).encode("utf-8"))


async def publish_visit_record(payload: VisitRecordPayload) -> None:
    _send(RoutingKey.VISIT_RECORD.value, payload)


async def publish_email_notify(payload: EmailNotifyPayload) -> None:
    _send(RoutingKey.EMAIL_NOTIFY.value, payload)


async def publish_file_cleanup(payload: FileCleanupPayload) -> None:
    _send(RoutingKey.FILE_CLEANUP.value, payload)


async def publish_audit_log(payload: AuditLogPayload) -> None:
    _send(RoutingKey.AUDIT_LOG.value, payload)