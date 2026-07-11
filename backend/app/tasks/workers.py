"""Concrete RabbitMQ consumer tasks."""
from __future__ import annotations

import asyncio
import json
import logging
from datetime import date
from typing import Awaitable, Callable

import aio_pika
from aio_pika.abc import AbstractIncomingMessage

from app.core.config import settings
from app.core.database import session_scope
from app.core.redis_client import get_redis
from app.messaging.rabbitmq import channel_scope
from app.messaging.topology import RoutingKey
from app.repositories.visit import VisitLogRepository

logger = logging.getLogger("myblog.worker")

TaskHandler = Callable[[dict], Awaitable[None]]


async def _visit_record_handler(payload: dict) -> None:
    visited_at = payload.get("visited_at")
    target_day = visited_at[:10] if isinstance(visited_at, str) else date.today().isoformat()
    r = get_redis()
    r.incr(f"stats:visit:{target_day}:pv")
    r.pfadd(f"stats:visit:{target_day}:uv", payload.get("ip", "unknown"))
    r.expire(f"stats:visit:{target_day}:pv", 60 * 60 * 24 * 90)
    r.expire(f"stats:visit:{target_day}:uv", 60 * 60 * 24 * 90)
    with session_scope() as db:
        repo = VisitLogRepository(db)
        repo.insert(
            ip=payload.get("ip", "unknown"),
            user_agent=payload.get("user_agent", ""),
            path=payload.get("path", "/"),
            referer=payload.get("referer"),
            user_id=payload.get("user_id") or None,
        )
    logger.info("persisted visit %s", payload.get("ip"))


async def _email_notify_handler(payload: dict) -> None:
    """Render and (eventually) send an email.

    The handler is the natural place for SMTP delivery; right now we log a
    structured record so the queue is exercised end-to-end. Plug in your
    SMTP / SES / Resend / SendGrid client here.
    """
    severity = payload.get("severity", "info")
    category = payload.get("category", "general")
    logger.info(
        "[email-stub] severity=%s category=%s to=%s subject=%r template=%s context=%s",
        severity,
        category,
        payload.get("to"),
        payload.get("subject"),
        payload.get("template"),
        payload.get("context"),
    )
    if severity == "critical":
        # Surface critical events at WARNING level so monitoring picks them up.
        logger.warning(
            "CRITICAL email payload: subject=%r to=%s", payload.get("subject"), payload.get("to")
        )


async def _file_cleanup_handler(payload: dict) -> None:
    from minio.error import S3Error

    from app.core.minio_client import delete_file as minio_delete

    object_key = payload.get("object_key")
    if not object_key:
        logger.warning("file_cleanup payload missing object_key: %s", payload)
        return

    bucket = payload.get("bucket") or settings.MINIO_BUCKET
    reason = payload.get("reason", "manual")

    try:
        # Pass through bucket explicitly to avoid relying on env mid-task.
        minio_delete(object_key, bucket=bucket)
    except S3Error:
        # Object may already be gone (idempotent delete). Log and continue.
        logger.info("minio cleanup skipped (already gone?): %s", object_key)
    except Exception:  # noqa: BLE001 - log and requeue so we can retry
        logger.exception("file_cleanup failed for %s; requeue", object_key)
        raise
    logger.info("removed object_key=%s bucket=%s reason=%s", object_key, bucket, reason)


async def _audit_log_handler(payload: dict) -> None:
    logger.info(
        "[audit] user=%s actor=%s action=%s resource=%s resource_id=%s ip=%s meta=%s",
        payload.get("user_id"),
        (payload.get("metadata") or {}).get("actor"),
        payload.get("action"),
        payload.get("resource"),
        payload.get("resource_id"),
        payload.get("ip"),
        {k: v for k, v in (payload.get("metadata") or {}).items() if k != "actor"},
    )


HANDLERS: dict[str, TaskHandler] = {
    RoutingKey.VISIT_RECORD.value: _visit_record_handler,
    RoutingKey.EMAIL_NOTIFY.value: _email_notify_handler,
    RoutingKey.FILE_CLEANUP.value: _file_cleanup_handler,
    RoutingKey.AUDIT_LOG.value: _audit_log_handler,
}


async def _on_message(message: AbstractIncomingMessage) -> None:
    routing_key = message.routing_key or ""
    handler = HANDLERS.get(routing_key)
    if handler is None:
        logger.warning("no handler for routing_key=%s", routing_key)
        await message.ack()
        return
    async with message.process(requeue=False):
        try:
            body = json.loads(message.body.decode("utf-8"))
        except Exception:
            logger.exception("bad json payload")
            return
        await handler(body)


async def consume(queue_name: str) -> None:
    """Consume a single queue until cancelled.

    Holds an event-loop future which makes the awaiting task cancellable
    from outside (FastAPI lifespan, signal handlers, ...).
    """
    loop = asyncio.get_event_loop()
    stop = loop.create_future()
    try:
        async with channel_scope() as channel:
            await channel.set_qos(prefetch_count=settings.RABBITMQ_PREFETCH)
            queue = await channel.get_queue(queue_name)
            logger.info("worker consuming queue=%s", queue_name)
            await queue.consume(_on_message)
            await stop  # wait until cancelled
    finally:
        if not stop.done():
            stop.set_result(None)


async def main() -> None:
    """Standalone consumer entrypoint; delegates to the shared runner."""
    from app.tasks.consumer import consume_queues

    await consume_queues()


if __name__ == "__main__":
    logging.basicConfig(level=settings.LOG_LEVEL)
    asyncio.run(main())