"""Project service."""
from __future__ import annotations

import uuid
from typing import List, Optional, Tuple

from sqlalchemy.orm import Session

from app.common.exceptions import NotFound
from app.core.cache import cache_get_or_set
from app.messaging.audit import emit_audit
from app.models.project import Project
from app.models.user import User
from app.repositories.project import ProjectRepository
from app.schemas.project import ProjectCreate, ProjectUpdate


CACHE_PREFIX = "cache:projects:"


class ProjectService:
    def __init__(self, db: Session) -> None:
        self.db = db
        self.repo = ProjectRepository(db)

    def list_public(
        self,
        *,
        page: int,
        page_size: int,
        tag: Optional[str] = None,
        year: Optional[int] = None,
    ) -> Tuple[List[Project], int]:
        key = f"{CACHE_PREFIX}list:{tag}:{year}:{page}:{page_size}:v1"
        return cache_get_or_set(
            key,
            300,
            lambda: self.repo.list_ordered(page=page, page_size=page_size, tag=tag, year=year),
        )

    def get(self, project_id: uuid.UUID) -> Project:
        item = self.repo.get(project_id)
        if not item:
            raise NotFound("Project not found")
        return item

    async def create(self, payload: ProjectCreate, *, user: User, request=None) -> Project:
        obj = Project(**payload.model_dump())
        self.repo.add(obj)
        self.db.commit()
        self.db.refresh(obj)
        await emit_audit(
            action="create",
            resource="project",
            resource_id=obj.id,
            user=user,
            request=request,
            metadata={"title": getattr(obj, "title", None)},
        )
        return obj

    async def update(
        self, project_id: uuid.UUID, payload: ProjectUpdate, *, user: User, request=None
    ) -> Project:
        item = self.get(project_id)
        data = payload.model_dump(exclude_unset=True)
        self.repo.update(item, data)
        self.db.commit()
        self.db.refresh(item)
        await emit_audit(
            action="update",
            resource="project",
            resource_id=item.id,
            user=user,
            request=request,
            metadata={"changed_fields": sorted(data.keys())},
        )
        return item

    async def delete(
        self, project_id: uuid.UUID, *, user: User, request=None
    ) -> None:
        item = self.get(project_id)
        self.repo.soft_delete(item)
        self.db.commit()
        await emit_audit(
            action="delete",
            resource="project",
            resource_id=item.id,
            user=user,
            request=request,
            metadata={"title": getattr(item, "title", None)},
        )