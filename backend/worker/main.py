"""Standalone worker entry point.

Run with:
    python -m worker.main
"""
from __future__ import annotations

import asyncio
import logging
import signal
from typing import Optional

from app.core.config import settings
from app.tasks import workers


_stop_event: Optional[asyncio.Event] = None


async def run() -> None:
    global _stop_event
    _stop_event = asyncio.Event()
    loop = asyncio.get_running_loop()
    for sig in (signal.SIGINT, signal.SIGTERM):
        try:
            loop.add_signal_handler(sig, _stop_event.set)
        except NotImplementedError:
            pass
    worker_task = asyncio.create_task(workers.main())
    await _stop_event.wait()
    worker_task.cancel()


def main() -> None:
    logging.basicConfig(level=settings.LOG_LEVEL, format="%(asctime)s %(levelname)s %(name)s %(message)s")
    asyncio.run(run())


if __name__ == "__main__":
    main()