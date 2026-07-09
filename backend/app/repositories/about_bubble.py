"""About-bubble repository."""
from __future__ import annotations

from typing import Optional

from sqlalchemy import asc, select

from app.models.about_bubble import AboutBubble
from app.repositories.base import BaseRepository


class AboutBubbleRepository(BaseRepository[AboutBubble]):
    model = AboutBubble

    def list_ordered(self):
        return self.list(page=1, page_size=200, order_by=(asc(AboutBubble.order_num),))

    def get_by_label(self, label: str) -> Optional[AboutBubble]:
        stmt = select(AboutBubble).where(
            AboutBubble.label == label, AboutBubble.deleted_at.is_(None)
        )
        return self.db.execute(stmt).scalar_one_or_none()