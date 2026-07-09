"""About-bubble service."""
from __future__ import annotations

import uuid
from typing import List

from sqlalchemy.orm import Session

from app.common.exceptions import NotFound
from app.core.cache import cache_clear_prefix, cache_get_or_set
from app.messaging.audit import emit_audit
from app.models.about_bubble import AboutBubble
from app.models.user import User
from app.repositories.about_bubble import AboutBubbleRepository
from app.schemas.about_bubble import AboutBubbleCreate, AboutBubbleUpdate


CACHE_PREFIX = "cache:bubbles:"


class AboutBubbleService:
    def __init__(self, db: Session) -> None:
        self.db = db
        self.repo = AboutBubbleRepository(db)

    def list_public(self) -> List[AboutBubble]:
        return cache_get_or_set(
            f"{CACHE_PREFIX}list:v1",
            600,
            lambda: self.repo.list_ordered()[0],
        )

    def get(self, bubble_id: uuid.UUID) -> AboutBubble:
        item = self.repo.get(bubble_id)
        if not item:
            raise NotFound("Bubble not found")
        return item

    async def create(
        self, payload: AboutBubbleCreate, *, user: User, request=None
    ) -> AboutBubble:
        from app.common.exceptions import Conflict

        existing = self.repo.get_by_label(payload.label)
        if existing is not None:
            raise Conflict("Bubble label already exists")
        obj = AboutBubble(**payload.model_dump())
        self.repo.add(obj)
        self.db.commit()
        self.db.refresh(obj)
        cache_clear_prefix(CACHE_PREFIX)
        await emit_audit(
            action="create",
            resource="about_bubble",
            resource_id=obj.id,
            user=user,
            request=request,
        )
        return obj

    async def update(
        self,
        bubble_id: uuid.UUID,
        payload: AboutBubbleUpdate,
        *,
        user: User,
        request=None,
    ) -> AboutBubble:
        item = self.get(bubble_id)
        data = payload.model_dump(exclude_unset=True)
        self.repo.update(item, data)
        self.db.commit()
        self.db.refresh(item)
        cache_clear_prefix(CACHE_PREFIX)
        await emit_audit(
            action="update",
            resource="about_bubble",
            resource_id=item.id,
            user=user,
            request=request,
            metadata={"changed_fields": sorted(data.keys())},
        )
        return item

    async def delete(
        self, bubble_id: uuid.UUID, *, user: User, request=None
    ) -> None:
        item = self.get(bubble_id)
        self.repo.soft_delete(item)
        self.db.commit()
        cache_clear_prefix(CACHE_PREFIX)
        await emit_audit(
            action="delete",
            resource="about_bubble",
            resource_id=item.id,
            user=user,
            request=request,
        )