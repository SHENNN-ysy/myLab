"""MinIO client wrapper."""
from io import BytesIO
from typing import Optional

from minio import Minio
from minio.error import S3Error

from app.core.config import settings

_client: Optional[Minio] = None


def get_minio() -> Minio:
    global _client
    if _client is None:
        _client = Minio(
            settings.MINIO_ENDPOINT,
            access_key=settings.MINIO_ACCESS_KEY,
            secret_key=settings.MINIO_SECRET_KEY,
            secure=settings.MINIO_SECURE,
        )
    return _client


def ensure_bucket() -> None:
    client = get_minio()
    bucket = settings.MINIO_BUCKET
    if not client.bucket_exists(bucket):
        client.make_bucket(bucket)


def upload_file(
    object_key: str,
    data: BytesIO,
    length: int,
    content_type: str = "application/octet-stream",
) -> None:
    client = get_minio()
    ensure_bucket()
    client.put_object(
        settings.MINIO_BUCKET,
        object_key,
        data,
        length=length,
        content_type=content_type,
    )


def delete_file(object_key: str, bucket: str | None = None) -> None:
    client = get_minio()
    target_bucket = bucket or settings.MINIO_BUCKET
    try:
        client.remove_object(target_bucket, object_key)
    except S3Error:
        # swallow errors so the DB soft-delete always succeeds
        pass


def presigned_url(object_key: str) -> str:
    client = get_minio()
    return client.presigned_get_object(
        settings.MINIO_BUCKET,
        object_key,
        expires=settings.MINIO_PRESIGNED_EXPIRE,
    )