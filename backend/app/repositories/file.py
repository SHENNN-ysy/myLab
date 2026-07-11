"""File repository."""
from __future__ import annotations

import uuid
from typing import Optional

from sqlalchemy import func, select

from app.models.file_record import FileRecord
from app.repositories.base import BaseRepository


class FileRepository(BaseRepository[FileRecord]):
    model = FileRecord

    def get_by_id(self, file_id: uuid.UUID) -> Optional[FileRecord]:
        stmt = select(FileRecord).where(
            FileRecord.id == file_id, FileRecord.is_deleted.is_(False)
        )
        return self.db.execute(stmt).scalar_one_or_none()

    def list_paginated(self, *, page: int, page_size: int):
        stmt = select(FileRecord).where(FileRecord.is_deleted.is_(False))
        count_stmt = select(func.count()).select_from(FileRecord).where(FileRecord.is_deleted.is_(False))
        total = self.db.execute(count_stmt).scalar_one()
        stmt = stmt.order_by(FileRecord.created_at.desc()).offset((page - 1) * page_size).limit(page_size)
        return list(self.db.execute(stmt).scalars().all()), total

    def soft_delete(self, record: FileRecord) -> None:
        record.is_deleted = True
        self.db.flush()
