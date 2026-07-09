"""Visit log repository."""
from __future__ import annotations

import uuid
from datetime import datetime
from typing import Optional

from sqlalchemy import func, select

from app.models.visit_log import VisitLog
from app.repositories.base import BaseRepository


class VisitLogRepository(BaseRepository):
    model = VisitLog

    def insert(
        self,
        *,
        ip: str,
        user_agent: str,
        path: str,
        referer: Optional[str] = None,
        user_id: Optional[uuid.UUID] = None,
    ) -> VisitLog:
        record = VisitLog(
            ip=ip,
            user_agent=user_agent,
            path=path,
            referer=referer,
            user_id=user_id,
        )
        self.db.add(record)
        self.db.flush()
        return record

    def list_paginated(
        self,
        *,
        page: int,
        page_size: int,
        start: Optional[datetime] = None,
        end: Optional[datetime] = None,
    ):
        stmt = select(VisitLog)
        count_stmt = select(func.count()).select_from(VisitLog)
        if start:
            stmt = stmt.where(VisitLog.visited_at >= start)
            count_stmt = count_stmt.where(VisitLog.visited_at >= start)
        if end:
            stmt = stmt.where(VisitLog.visited_at <= end)
            count_stmt = count_stmt.where(VisitLog.visited_at <= end)
        total = self.db.execute(count_stmt).scalar_one()
        stmt = stmt.order_by(VisitLog.visited_at.desc()).offset((page - 1) * page_size).limit(page_size)
        items = list(self.db.execute(stmt).scalars().all())
        return items, total

    def total_count(self) -> int:
        return self.db.execute(select(func.count()).select_from(VisitLog)).scalar_one()

    def total_uv(self) -> int:
        return self.db.execute(select(func.count(func.distinct(VisitLog.ip)))).scalar_one()

    def delete_by_id(self, log_id: uuid.UUID) -> int:
        result = self.db.execute(VisitLog.__table__.delete().where(VisitLog.id == log_id))
        return result.rowcount or 0

    def delete_all(self) -> int:
        result = self.db.execute(VisitLog.__table__.delete())
        return result.rowcount or 0

    def delete_older_than(self, cutoff: datetime) -> int:
        result = self.db.execute(
            VisitLog.__table__.delete().where(VisitLog.visited_at < cutoff)
        )
        return result.rowcount or 0
