"""Footprint repository."""
from __future__ import annotations

from typing import Optional

from sqlalchemy import asc, select

from app.models.footprint import Footprint
from app.repositories.base import BaseRepository


class FootprintRepository(BaseRepository[Footprint]):
    model = Footprint

    def list_ordered(self):
        return self.list(page=1, page_size=1000, order_by=(asc(Footprint.order_num),))

    def get_by_slug(self, slug: str) -> Optional[Footprint]:
        stmt = select(Footprint).where(
            Footprint.slug == slug, Footprint.deleted_at.is_(None)
        )
        return self.db.execute(stmt).scalar_one_or_none()