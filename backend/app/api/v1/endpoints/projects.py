"""Project endpoints."""
from __future__ import annotations

import uuid
from typing import Optional

from fastapi import APIRouter, Depends, Request
from sqlalchemy.orm import Session

from app.api.deps import require_admin
from app.common.response import paginated, success
from app.core.database import get_db
from app.models.user import User
from app.schemas.project import ProjectCreate, ProjectOut, ProjectUpdate
from app.services.project_service import ProjectService

router = APIRouter(prefix="/projects", tags=["projects"])


@router.get("")
def list_projects(
    page: int = 1,
    page_size: int = 20,
    tag: Optional[str] = None,
    year: Optional[int] = None,
    db: Session = Depends(get_db),
):
    service = ProjectService(db)
    items, total = service.list_public(page=page, page_size=page_size, tag=tag, year=year)
    return paginated(
        [ProjectOut.model_validate(p).model_dump(mode="json") for p in items],
        page=page,
        page_size=page_size,
        total=total,
    )


@router.post("")
async def create_project(
    payload: ProjectCreate,
    request: Request,
    db: Session = Depends(get_db),
    user: User = Depends(require_admin),
):
    service = ProjectService(db)
    obj = await service.create(payload, user=user, request=request)
    return success(data=ProjectOut.model_validate(obj).model_dump(mode="json"))


@router.put("/{project_id}")
async def update_project(
    project_id: uuid.UUID,
    payload: ProjectUpdate,
    request: Request,
    db: Session = Depends(get_db),
    user: User = Depends(require_admin),
):
    service = ProjectService(db)
    obj = await service.update(project_id, payload, user=user, request=request)
    return success(data=ProjectOut.model_validate(obj).model_dump(mode="json"))


@router.delete("/{project_id}")
async def delete_project(
    project_id: uuid.UUID,
    request: Request,
    db: Session = Depends(get_db),
    user: User = Depends(require_admin),
):
    service = ProjectService(db)
    await service.delete(project_id, user=user, request=request)
    return success(message="project deleted")
