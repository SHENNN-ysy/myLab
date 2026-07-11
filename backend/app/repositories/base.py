"""Generic CRUD repository."""
from __future__ import annotations

import uuid
from datetime import datetime
from typing import Generic, Iterable, Optional, Sequence, Type, TypeVar

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.models.base import BaseModel

T = TypeVar("T", bound=BaseModel)


class BaseRepository(Generic[T]):
    """Generic repository providing soft-delete-aware CRUD."""

    model: Type[T]

    def __init__(self, db: Session) -> None:
        self.db = db

    # ----- query helpers -------------------------------------------------

    def _alive_stmt(self):
        return select(self.model).where(self.model.deleted_at.is_(None))

    def get(self, obj_id: uuid.UUID) -> Optional[T]:
        stmt = self._alive_stmt().where(self.model.id == obj_id)
        return self.db.execute(stmt).scalar_one_or_none()

    def list(
        self,
        *,
        page: int = 1,
        page_size: int = 20,
        order_by: Optional[Sequence] = None,
        filters: Optional[Iterable] = None,
    ) -> tuple[list[T], int]:
        offset = (page - 1) * page_size
        stmt = self._alive_stmt()
        for f in filters or ():
            stmt = stmt.where(f)
        count_stmt = select(func.count()).select_from(stmt.subquery())
        total = self.db.execute(count_stmt).scalar_one()
        if order_by:
            stmt = stmt.order_by(*order_by)
        stmt = stmt.offset(offset).limit(page_size)
        items = list(self.db.execute(stmt).scalars().all())
        return items, total

    def all(self) -> list[T]:
        stmt = self._alive_stmt()
        return list(self.db.execute(stmt).scalars().all())

    # ----- mutations -----------------------------------------------------

    def add(self, obj: T) -> T:
        self.db.add(obj)
        self.db.flush()
        return obj

    def update(self, obj: T, fields: dict) -> T:
        for key, value in fields.items():
            setattr(obj, key, value)
        self.db.flush()
        return obj

    def soft_delete(self, obj: T) -> None:
        obj.deleted_at = datetime.utcnow()
        self.db.flush()

    def hard_delete(self, obj: T) -> None:
        self.db.delete(obj)
        self.db.flush()