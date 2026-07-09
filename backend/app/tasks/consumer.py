"""Background queue consumer runner.

Used both by the FastAPI lifespan (in-process) and the standalone
``worker.main`` entrypoint (out-of-process). Defines which queues the
application actually wants to consume.
"""
from __future__ import annotations

import asyncio
import logging

from app.messaging.topology import Queue
from app.tasks import workers

logger = logging.getLogger("myblog.consumer")


# Queues this process will consume. Keeping it centralised makes it easy
# to opt in/out additional background tasks without touching call sites.
DEFAULT_QUEUES = [
    Queue.VISIT_RECORD.value,
    Queue.EMAIL_NOTIFY.value,
    Queue.FILE_CLEANUP.value,
    Queue.AUDIT_LOG.value,
]


async def consume_queues(queues: list[str] | None = None) -> None:
    """Run consumer tasks for the given queues until cancelled.

    Spawns one task per queue so a slow handler on one queue does not
    block the others. Returns when the surrounding task is cancelled,
    e.g. during FastAPI shutdown.
    """
    targets = queues or DEFAULT_QUEUES
    logger.info("starting in-process consumers for queues=%s", targets)
    tasks = [asyncio.create_task(workers.consume(q), name=f"consumer:{q}") for q in targets]
    try:
        await asyncio.gather(*tasks)
    except asyncio.CancelledError:
        logger.info("consumers cancelled, cancelling tasks...")
        for t in tasks:
            t.cancel()
        await asyncio.gather(*tasks, return_exceptions=True)
        raise
