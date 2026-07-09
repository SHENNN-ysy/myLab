"""Footprint endpoints."""
from __future__ import annotations

import uuid

from fastapi import APIRouter, Depends, Request
from sqlalchemy.orm import Session

from app.api.deps import require_admin
from app.common.response import success
from app.core.database import get_db
from app.models.user import User
from app.schemas.footprint import FootprintCreate, FootprintOut, FootprintUpdate
from app.services.footprint_service import FootprintService

router = APIRouter(prefix="/footprints", tags=["footprints"])


@router.get("")
def list_footprints(db: Session = Depends(get_db)):
    service = FootprintService(db)
    items = service.list_public()
    return success(data=[FootprintOut.model_validate(f).model_dump(mode="json") for f in items])


@router.post("")
async def create_footprint(
    payload: FootprintCreate,
    request: Request,
    db: Session = Depends(get_db),
    user: User = Depends(require_admin),
):
    service = FootprintService(db)
    obj = await service.create(payload, user=user, request=request)
    return success(data=FootprintOut.model_validate(obj).model_dump(mode="json"))


@router.put("/{footprint_id}")
async def update_footprint(
    footprint_id: uuid.UUID,
    payload: FootprintUpdate,
    request: Request,
    db: Session = Depends(get_db),
    user: User = Depends(require_admin),
):
    service = FootprintService(db)
    obj = await service.update(footprint_id, payload, user=user, request=request)
    return success(data=FootprintOut.model_validate(obj).model_dump(mode="json"))


@router.delete("/{footprint_id}")
async def delete_footprint(
    footprint_id: uuid.UUID,
    request: Request,
    db: Session = Depends(get_db),
    user: User = Depends(require_admin),
):
    service = FootprintService(db)
    await service.delete(footprint_id, user=user, request=request)
    return success(message="footprint deleted")
