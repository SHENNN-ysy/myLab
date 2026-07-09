"""File endpoints."""
from __future__ import annotations

import uuid

from fastapi import APIRouter, Depends, File, Request, UploadFile
from sqlalchemy.orm import Session

from app.api.deps import require_admin
from app.common.response import paginated, success
from app.core.database import get_db
from app.models.user import User
from app.schemas.file import FileOut
from app.services.file_service import FileService

router = APIRouter(prefix="/files", tags=["files"])


def _file_out(record, service: FileService) -> dict:
    data = FileOut.model_validate(record).model_dump(mode="json")
    data["url"] = service.presign(record.id)
    return data


@router.get("")
def list_files(
    page: int = 1,
    page_size: int = 20,
    db: Session = Depends(get_db),
    _: User = Depends(require_admin),
):
    service = FileService(db)
    items, total = service.list(page=page, page_size=page_size)
    return paginated(
        [_file_out(record, service) for record in items],
        page=page,
        page_size=page_size,
        total=total,
    )


@router.post("/upload", response_model=None)
async def upload(
    file: UploadFile = File(...),
    db: Session = Depends(get_db),
    user: User = Depends(require_admin),
):
    service = FileService(db)
    record = await service.upload(file, user)
    return success(data=_file_out(record, service))


@router.get("/presigned/{file_id}")
def presign(
    file_id: uuid.UUID,
    db: Session = Depends(get_db),
    _: User = Depends(require_admin),
):
    service = FileService(db)
    url = service.presign(file_id)
    return success(data={"url": url})


@router.delete("/{file_id}")
async def delete_file(
    file_id: uuid.UUID,
    db: Session = Depends(get_db),
    user: User = Depends(require_admin),
):
    service = FileService(db)
    await service.delete(file_id, user=user)
    return success(message="file deletion queued")
