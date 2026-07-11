"""Skill service with caching."""
from __future__ import annotations

import uuid
from typing import List, Tuple

from sqlalchemy.orm import Session

from app.common.exceptions import NotFound
from app.core.cache import cache_clear_prefix, cache_get_or_set
from app.messaging.audit import emit_audit
from app.models.skill import Skill
from app.models.user import User
from app.repositories.skill import SkillRepository
from app.schemas.skill import SkillCreate, SkillUpdate


CACHE_PREFIX = "cache:skills:"


class SkillService:
    def __init__(self, db: Session) -> None:
        self.db = db
        self.repo = SkillRepository(db)

    def list_public(self) -> List[Skill]:
        def _load():
            return [s for s in self.repo.list_ordered()[0]]

        cached = cache_get_or_set(f"{CACHE_PREFIX}list:v1", 300, _load)
        return [Skill(**item) for item in cached] if cached and isinstance(cached[0], dict) else _load()

    def list_admin(self, *, page: int, page_size: int) -> Tuple[List[Skill], int]:
        return self.repo.list_ordered(page=page, page_size=page_size)

    def get(self, skill_id: uuid.UUID) -> Skill:
        skill = self.repo.get(skill_id)
        if not skill:
            raise NotFound("Skill not found")
        return skill

    async def create(self, payload: SkillCreate, *, user: User, request=None) -> Skill:
        obj = Skill(**payload.model_dump())
        self.repo.add(obj)
        self.db.commit()
        self.db.refresh(obj)
        cache_clear_prefix(CACHE_PREFIX)
        await emit_audit(
            action="create",
            resource="skill",
            resource_id=obj.id,
            user=user,
            request=request,
            metadata={"name": getattr(obj, "name", None)},
        )
        return obj

    async def update(
        self, skill_id: uuid.UUID, payload: SkillUpdate, *, user: User, request=None
    ) -> Skill:
        skill = self.get(skill_id)
        data = payload.model_dump(exclude_unset=True)
        self.repo.update(skill, data)
        self.db.commit()
        self.db.refresh(skill)
        cache_clear_prefix(CACHE_PREFIX)
        await emit_audit(
            action="update",
            resource="skill",
            resource_id=skill.id,
            user=user,
            request=request,
            metadata={"changed_fields": sorted(data.keys())},
        )
        return skill

    async def delete(self, skill_id: uuid.UUID, *, user: User, request=None) -> None:
        skill = self.get(skill_id)
        self.repo.soft_delete(skill)
        self.db.commit()
        cache_clear_prefix(CACHE_PREFIX)
        await emit_audit(
            action="delete",
            resource="skill",
            resource_id=skill.id,
            user=user,
            request=request,
            metadata={"name": getattr(skill, "name", None)},
        )