"""System information endpoints for the admin dashboard."""
from __future__ import annotations

import os
import platform
import shutil
import socket
import time

from fastapi import APIRouter, Depends, Request
from sqlalchemy import text
from sqlalchemy.orm import Session

from app.api.deps import require_admin
from app.common.response import success
from app.core.config import settings
from app.core.database import get_db
from app.models.user import User

router = APIRouter(prefix="/system", tags=["system"])
STARTED_AT = time.time()


@router.get("/static")
def static_info(
    db: Session = Depends(get_db),
    _: User = Depends(require_admin),
):
    disk = shutil.disk_usage("/")
    # Postgres exposes information_schema; SQLite exposes sqlite_master.
    bind = db.get_bind()
    dialect = bind.dialect.name if bind is not None else ""
    if dialect == "sqlite":
        table_count = (
            db.execute(
                text("SELECT count(*) FROM sqlite_master WHERE type='table'")
            ).scalar_one_or_none()
            or 0
        )
    else:
        table_count = db.execute(
            text(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public'"
            ).scalar_one_or_none()
            or 0
        )
    return success(
        data={
            "hostname": socket.gethostname(),
            "os": f"{platform.system()} {platform.release()}",
            "serverIp": socket.gethostbyname(socket.gethostname()),
            "timezone": time.tzname[0] if time.tzname else "local",
            "cpuCore": os.cpu_count() or 0,
            "cpuModel": platform.processor() or platform.machine(),
            "cpuArch": platform.machine(),
            "memoryTotal": 0,
            "swapTotal": 0,
            "diskTotal": disk.total,
            "dbType": "PostgreSQL",
            "dbTables": table_count,
            "appVersion": "1.0.0",
            "storageStatus": "姝ｅ父",
            "emailStatus": "未配置",
        }
    )


@router.get("/dynamic")
def dynamic_info(
    db: Session = Depends(get_db),
    _: User = Depends(require_admin),
):
    disk = shutil.disk_usage("/")
    try:
        load1, load5, load15 = os.getloadavg()
    except (AttributeError, OSError):
        load1 = load5 = load15 = 0.0
    db.execute(text("SELECT 1"))
    return success(
        data={
            "cpuUsage": 0,
            "load1": load1,
            "load5": load5,
            "load15": load15,
            "memoryUsed": 0,
            "memoryAvailable": 0,
            "swapUsed": 0,
            "hostUptime": int(time.time() - STARTED_AT),
            "diskUsed": disk.used,
            "diskFree": disk.free,
            "dbStatus": "姝ｅ父",
            "dbSize": 0,
            "dbConnCount": 0,
        }
    )
