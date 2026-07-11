"""Visit / stats endpoints."""
from __future__ import annotations

import uuid
from datetime import datetime
from typing import Optional

from fastapi import APIRouter, Depends, Query, Request
from sqlalchemy.orm import Session

from app.api.deps import require_admin, require_superadmin
from app.common.response import paginated, success
from app.core.database import get_db
from app.models.user import User
from app.schemas.visit import VisitLogOut
from app.services.visit_service import VisitService

router = APIRouter(prefix="/visits", tags=["visits"])


@router.get("/stats")
def stats(
    date: Optional[str] = Query(default=None, description="YYYY-MM-DD, defaults today"),
    db: Session = Depends(get_db),
    _: User = Depends(require_admin),
):
    service = VisitService(db)
    target = date or datetime.utcnow().date().isoformat()
    return success(data=service.stats(target))


@router.get("/logs")
def list_logs(
    page: int = 1,
    page_size: int = 20,
    start: Optional[datetime] = None,
    end: Optional[datetime] = None,
    db: Session = Depends(get_db),
    _: User = Depends(require_admin),
):
    service = VisitService(db)
    items, total = service.list_logs(page=page, page_size=page_size, start=start, end=end)
    return paginated(
        [VisitLogOut.model_validate(v).model_dump(mode="json") for v in items],
        page=page,
        page_size=page_size,
        total=total,
    )


@router.post("/logs/track")
async def track(request: Request, db: Session = Depends(get_db)):
    """Record a visit by publishing to RabbitMQ (the worker persists)."""
    service = VisitService(db)
    await service.record(request, user_id=None)
    return success(message="visit queued")


@router.post("/logs/batch-delete")
def batch_delete(
    cutoff: datetime,
    db: Session = Depends(get_db),
    _: User = Depends(require_superadmin),
):
    service = VisitService(db)
    deleted = service.cleanup(cutoff)
    return success(data={"deleted": deleted})


@router.delete("/logs/{log_id}")
def delete_log(
    log_id: uuid.UUID,
    db: Session = Depends(get_db),
    _: User = Depends(require_admin),
):
    service = VisitService(db)
    deleted = service.delete(log_id)
    return success(data={"deleted": deleted}, message="visit log deleted")


@router.delete("/logs")
def clear_logs(
    db: Session = Depends(get_db),
    _: User = Depends(require_superadmin),
):
    service = VisitService(db)
    deleted = service.clear()
    return success(data={"deleted": deleted}, message="visit logs cleared")
