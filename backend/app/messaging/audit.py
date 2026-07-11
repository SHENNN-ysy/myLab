"""Audit-log emission helper.

Wraps ``publish_audit_log`` so business services can call a single
``await emit_audit(...)`` instead of building the payload each time.

Failures are swallowed because audit delivery MUST NOT block the main
business workflow (audit is best-effort).
"""
from __future__ import annotations

import logging
import uuid
from typing import Any, Dict, Optional

from fastapi import Request

from app.messaging.producers import publish_audit_log
from app.messaging.schemas import AuditLogPayload
from app.models.user import User

logger = logging.getLogger("myblog.audit")


async def emit_audit(
    *,
    action: str,
    resource: str,
    resource_id: str | uuid.UUID | None = None,
    user: User | None = None,
    request: Request | None = None,
    metadata: Optional[Dict[str, Any]] = None,
) -> None:
    """Emit an audit log entry asynchronously. Never raises.

    Parameters mirror the ``AuditLogPayload`` fields plus a few
    convenience extras:

    - ``request`` adds ``ip`` and ``request_id`` automatically.
    - ``user`` adds ``user_id`` (and ``actor`` nickname for readability).
    """
    ip: str | None = None
    request_id: str | None = None
    if request is not None:
        ip = request.client.host if request.client else None
        request_id = getattr(request.state, "request_id", None)

    actor_name = None
    user_id_str: str | None = None
    if user is not None:
        user_id_str = str(user.id)
        actor_name = getattr(user, "nickname", None) or getattr(user, "username", None)

    payload = AuditLogPayload(
        user_id=user_id_str,
        action=action,
        resource=resource,
        resource_id=str(resource_id) if resource_id is not None else None,
        ip=ip,
        metadata={
            **(metadata or {}),
            **({"actor": actor_name} if actor_name else {}),
            **({"request_id": request_id} if request_id else {}),
        },
    )

    # Synchronous console mirror so we don't lose entries when RabbitMQ
    # is unavailable or audit consumer is down.
    logger.info(
        "audit action=%s resource=%s resource_id=%s user_id=%s actor=%s ip=%s meta=%s",
        action,
        resource,
        payload.resource_id,
        user_id_str,
        actor_name,
        ip,
        payload.metadata,
    )

    try:
        await publish_audit_log(payload)
    except Exception:  # noqa: BLE001 - audit is best-effort
        logger.exception("failed to publish audit log for %s/%s", resource, action)
