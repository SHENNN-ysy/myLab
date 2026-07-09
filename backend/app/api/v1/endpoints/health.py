"""Health-check endpoint."""
from __future__ import annotations

from fastapi import APIRouter, Depends
from sqlalchemy import text
from sqlalchemy.orm import Session

from app.common.response import success
from app.core.database import get_db
from app.core.minio_client import get_minio
from app.core.redis_client import get_redis

router = APIRouter(prefix="/health", tags=["health"])


@router.get("")
def health(db: Session = Depends(get_db)):
    components = {}
    try:
        db.execute(text("SELECT 1"))
        components["database"] = "up"
    except Exception:
        components["database"] = "down"
    try:
        get_redis().ping()
        components["redis"] = "up"
    except Exception:
        components["redis"] = "down"
    try:
        client = get_minio()
        client.list_buckets()
        components["minio"] = "up"
    except Exception:
        components["minio"] = "down"
    healthy = all(v == "up" for v in components.values())
    return success(
        data={"status": "healthy" if healthy else "degraded", "components": components}
    )
