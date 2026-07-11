"""Skill endpoints."""
from __future__ import annotations

import uuid

from fastapi import APIRouter, Depends, Request
from sqlalchemy.orm import Session

from app.api.deps import require_admin
from app.common.response import paginated, success
from app.core.database import get_db
from app.models.user import User
from app.schemas.skill import SkillCreate, SkillOut, SkillUpdate
from app.services.skill_service import SkillService

router = APIRouter(prefix="/skills", tags=["skills"])


@router.get("")
def list_skills(
    page: int = 1,
    page_size: int = 100,
    db: Session = Depends(get_db),
):
    service = SkillService(db)
    items, total = service.list_admin(page=page, page_size=page_size)
    return paginated(
        [SkillOut.model_validate(s).model_dump(mode="json") for s in items],
        page=page,
        page_size=page_size,
        total=total,
    )


@router.post("")
async def create_skill(
    payload: SkillCreate,
    request: Request,
    db: Session = Depends(get_db),
    user: User = Depends(require_admin),
):
    service = SkillService(db)
    obj = await service.create(payload, user=user, request=request)
    return success(data=SkillOut.model_validate(obj).model_dump(mode="json"))


@router.put("/{skill_id}")
async def update_skill(
    skill_id: uuid.UUID,
    payload: SkillUpdate,
    request: Request,
    db: Session = Depends(get_db),
    user: User = Depends(require_admin),
):
    service = SkillService(db)
    obj = await service.update(skill_id, payload, user=user, request=request)
    return success(data=SkillOut.model_validate(obj).model_dump(mode="json"))


@router.delete("/{skill_id}")
async def delete_skill(
    skill_id: uuid.UUID,
    request: Request,
    db: Session = Depends(get_db),
    user: User = Depends(require_admin),
):
    service = SkillService(db)
    await service.delete(skill_id, user=user, request=request)
    return success(message="skill deleted")
