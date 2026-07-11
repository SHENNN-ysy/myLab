"""Footprint service."""
from __future__ import annotations

import uuid
from typing import List

from sqlalchemy.orm import Session

from app.common.exceptions import NotFound
from app.messaging.audit import emit_audit
from app.models.footprint import Footprint
from app.models.user import User
from app.repositories.footprint import FootprintRepository
from app.schemas.footprint import FootprintCreate, FootprintUpdate


class FootprintService:
    def __init__(self, db: Session) -> None:
        self.db = db
        self.repo = FootprintRepository(db)

    def list_public(self) -> List[Footprint]:
        items, _ = self.repo.list_ordered()
        return items

    def get(self, foot_id: uuid.UUID) -> Footprint:
        item = self.repo.get(foot_id)
        if not item:
            raise NotFound("Footprint not found")
        return item

    async def create(self, payload: FootprintCreate, *, user: User, request=None) -> Footprint:
        from app.common.exceptions import Conflict

        existing = self.repo.get_by_slug(payload.slug)
        if existing is not None:
            raise Conflict("Footprint slug already exists")
        obj = Footprint(**payload.model_dump())
        self.repo.add(obj)
        self.db.commit()
        self.db.refresh(obj)
        await emit_audit(
            action="create",
            resource="footprint",
            resource_id=obj.id,
            user=user,
            request=request,
        )
        return obj

    async def update(
        self, foot_id: uuid.UUID, payload: FootprintUpdate, *, user: User, request=None
    ) -> Footprint:
        item = self.get(foot_id)
        data = payload.model_dump(exclude_unset=True)
        self.repo.update(item, data)
        self.db.commit()
        self.db.refresh(item)
        await emit_audit(
            action="update",
            resource="footprint",
            resource_id=item.id,
            user=user,
            request=request,
            metadata={"changed_fields": sorted(data.keys())},
        )
        return item

    async def delete(self, foot_id: uuid.UUID, *, user: User, request=None) -> None:
        item = self.get(foot_id)
        self.repo.soft_delete(item)
        self.db.commit()
        await emit_audit(
            action="delete",
            resource="footprint",
            resource_id=item.id,
            user=user,
            request=request,
        )