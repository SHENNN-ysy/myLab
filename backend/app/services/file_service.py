"""File service: upload, listing, presigned, delete."""
from __future__ import annotations

import uuid
from datetime import date
from io import BytesIO
from typing import List, Tuple

from fastapi import UploadFile
from sqlalchemy.orm import Session

from app.common.exceptions import NotFound, ValidationFailed
from app.core import minio_client as _minio
from app.core.config import settings
from app.messaging import producers as _producers
from app.messaging.schemas import FileCleanupPayload
from app.models.file_record import FileRecord
from app.models.user import User
from app.repositories.file import FileRepository

ALLOWED_TYPES = {
    "image/png",
    "image/jpeg",
    "image/jpg",
    "image/webp",
    "image/gif",
    "image/svg+xml",
    "application/pdf",
}


class FileService:
    MAX_BYTES = settings.MINIO_MAX_FILE_SIZE_MB * 1024 * 1024

    def __init__(self, db: Session) -> None:
        self.db = db
        self.repo = FileRepository(db)

    async def upload(self, file: UploadFile, user: User) -> FileRecord:
        if file.content_type not in ALLOWED_TYPES:
            raise ValidationFailed(f"Unsupported content type: {file.content_type}")
        data = await file.read()
        if len(data) > self.MAX_BYTES:
            raise ValidationFailed("File exceeds maximum allowed size")
        ext = ""
        if file.filename and "." in file.filename:
            ext = file.filename.rsplit(".", 1)[-1]
        key = f"{date.today().isoformat()}/{uuid.uuid4().hex}.{ext}" if ext else f"{date.today().isoformat()}/{uuid.uuid4().hex}"
        _minio.upload_file(key, BytesIO(data), length=len(data), content_type=file.content_type)
        record = FileRecord(
            object_key=key,
            bucket=settings.MINIO_BUCKET,
            original_name=file.filename or key,
            mime_type=file.content_type,
            size=len(data),
            uploaded_by=user.id,
        )
        self.repo.add(record)
        self.db.commit()
        self.db.refresh(record)
        return record

    def list(self, *, page: int, page_size: int) -> Tuple[List[FileRecord], int]:
        return self.repo.list_paginated(page=page, page_size=page_size)

    def presign(self, file_id: uuid.UUID) -> str:
        record = self.repo.get_by_id(file_id)
        if not record:
            raise NotFound("File not found")
        return _minio.presigned_url(record.object_key)

    async def delete(self, file_id: uuid.UUID, *, user: User | None = None) -> None:
        """Soft-delete the DB row synchronously, remove the MinIO object
        asynchronously via RabbitMQ.

        Splitting the two phases means:
          * the API responds immediately (no MinIO round-trip on the hot path)
          * a transient MinIO failure does NOT break the user-facing delete
          * if the API dies between DB soft-delete and broker enqueue, the
            MinIO object leaks, but the inverse (MinIO deleted, DB row
            still visible) is the worse failure mode.
        """
        record = self.repo.get_by_id(file_id)
        if not record:
            raise NotFound("File not found")
        object_key = record.object_key
        bucket = record.bucket
        # Commit the soft-delete first so subsequent list/presign queries
        # stop returning the row, even if the consumer is delayed.
        self.repo.soft_delete(record)
        self.db.commit()
        await _producers.publish_file_cleanup(
            FileCleanupPayload(
                object_key=object_key,
                bucket=bucket,
                reason="user_delete",
                file_id=str(record.id),
                requested_by=str(user.id) if user else None,
            )
        )
