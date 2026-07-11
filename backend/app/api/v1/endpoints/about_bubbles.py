"""About-bubble endpoints."""
from __future__ import annotations

import uuid

from fastapi import APIRouter, Depends, Request
from sqlalchemy.orm import Session

from app.api.deps import require_admin
from app.common.response import success
from app.core.database import get_db
from app.models.user import User
from app.schemas.about_bubble import AboutBubbleCreate, AboutBubbleOut, AboutBubbleUpdate
from app.services.about_bubble_service import AboutBubbleService

router = APIRouter(prefix="/about-bubbles", tags=["about-bubbles"])


@router.get("")
def list_bubbles(db: Session = Depends(get_db)):
    service = AboutBubbleService(db)
    items = service.list_public()
    return success(data=[AboutBubbleOut.model_validate(b).model_dump(mode="json") for b in items])


@router.post("")
async def create_bubble(
    payload: AboutBubbleCreate,
    request: Request,
    db: Session = Depends(get_db),
    user: User = Depends(require_admin),
):
    service = AboutBubbleService(db)
    obj = await service.create(payload, user=user, request=request)
    return success(data=AboutBubbleOut.model_validate(obj).model_dump(mode="json"))


@router.put("/{bubble_id}")
async def update_bubble(
    bubble_id: uuid.UUID,
    payload: AboutBubbleUpdate,
    request: Request,
    db: Session = Depends(get_db),
    user: User = Depends(require_admin),
):
    service = AboutBubbleService(db)
    obj = await service.update(bubble_id, payload, user=user, request=request)
    return success(data=AboutBubbleOut.model_validate(obj).model_dump(mode="json"))


@router.delete("/{bubble_id}")
async def delete_bubble(
    bubble_id: uuid.UUID,
    request: Request,
    db: Session = Depends(get_db),
    user: User = Depends(require_admin),
):
    service = AboutBubbleService(db)
    await service.delete(bubble_id, user=user, request=request)
    return success(message="bubble deleted")
