"""Security-event email notification helper.

Sends email-notify payloads for events worth a real email:
  - repeated failed login attempts
  - password changes
  - account disabled
  - destructive admin actions on the site owner

Best-effort: failures are logged but never block the caller.
"""
from __future__ import annotations

import logging
from typing import Iterable

from app.core.config import settings
from app.messaging.producers import publish_email_notify
from app.messaging.schemas import EmailNotifyPayload

logger = logging.getLogger("myblog.notify")


def _admin_recipients() -> list[str]:
    raw = settings.ADMIN_NOTIFY_EMAILS
    if not raw:
        return []
    return [s.strip() for s in raw.split(",") if s.strip()]


async def notify_security_event(
    *,
    subject: str,
    template: str,
    context: dict,
    severity: str = "warning",
) -> None:
    """Send a security-event notification to configured admin recipients.

    Reads ``ADMIN_NOTIFY_EMAILS`` (comma-separated). If unset, the call
    becomes a no-op so dev environments without email don't crash.
    """
    recipients = _admin_recipients()
    if not recipients:
        logger.debug("no admin recipients configured; skipping notify: %s", subject)
        return
    payload = EmailNotifyPayload(
        to=recipients,
        subject=subject,
        template=template,
        context=context,
        severity=severity,
        category="security",
    )
    try:
        await publish_email_notify(payload)
    except Exception:  # noqa: BLE001 - notifications are best-effort
        logger.exception("failed to publish security notification: %s", subject)


async def notify_user(
    *,
    to: Iterable[str],
    subject: str,
    template: str,
    context: dict | None = None,
    severity: str = "info",
) -> None:
    """Send a non-security email to one or more recipients."""
    payload = EmailNotifyPayload(
        to=list(to),
        subject=subject,
        template=template,
        context=context or {},
        severity=severity,
        category="notification",
    )
    try:
        await publish_email_notify(payload)
    except Exception:  # noqa: BLE001
        logger.exception("failed to publish notify: %s", subject)
