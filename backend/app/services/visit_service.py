"""Visit-tracking service that publishes events to RabbitMQ."""
from __future__ import annotations

import uuid
from datetime import datetime
from typing import List, Optional, Tuple

from fastapi import Request
from sqlalchemy.orm import Session

from app.messaging import producers as _producers
from app.messaging.schemas import VisitRecordPayload
from app.models.visit_log import VisitLog
from app.repositories.visit import VisitLogRepository
from app.schemas.visit import VisitStats


class VisitService:
    def __init__(self, db: Session) -> None:
        self.db = db
        self.repo = VisitLogRepository(db)

    async def record(self, request: Request, user_id: Optional[uuid.UUID]) -> None:
        client_host = request.client.host if request.client else "unknown"
        payload = VisitRecordPayload(
            ip=client_host,
            user_agent=request.headers.get("user-agent", "")[:512],
            path=request.url.path,
            referer=request.headers.get("referer"),
            user_id=str(user_id) if user_id else None,
        )
        # Use the module attribute rather than a captured local so that
        # monkeypatched producers (in tests) are honoured.
        await _producers.publish_visit_record(payload)

    def list_logs(
        self,
        *,
        page: int,
        page_size: int,
        start: Optional[datetime] = None,
        end: Optional[datetime] = None,
    ) -> Tuple[List[VisitLog], int]:
        return self.repo.list_paginated(page=page, page_size=page_size, start=start, end=end)

    def cleanup(self, cutoff: datetime) -> int:
        deleted = self.repo.delete_older_than(cutoff)
        self.db.commit()
        return deleted

    def delete(self, log_id: uuid.UUID) -> int:
        deleted = self.repo.delete_by_id(log_id)
        self.db.commit()
        return deleted

    def clear(self) -> int:
        deleted = self.repo.delete_all()
        self.db.commit()
        return deleted

    def stats(self, today: str) -> dict:
        # RabbitMQ worker is responsible for hydrating these counters
        from app.core.redis_client import get_redis

        r = get_redis()
        pv = int(r.get(f"stats:visit:{today}:pv") or 0)
        # uv is stored in a HyperLogLog (PFADD/PFCOUNT), not a plain string.
        uv = int(r.pfcount(f"stats:visit:{today}:uv") or 0)
        total = self.repo.total_count()
        total_uv = self.repo.total_uv()
        return {
            "date": today,
            "pv": pv,
            "uv": uv,
            "total_pv": total,
            "total_uv": total_uv,
            "total_visits": total,
        }
