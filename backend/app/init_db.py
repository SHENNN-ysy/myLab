"""Initialize DB schema and seed initial data."""
from __future__ import annotations

import logging

from app.common.enums import UserRole
from app.core.config import settings
from app.core.database import Base, engine, session_scope
from app.core.minio_client import ensure_bucket
from app.core.security import hash_password
from app.models import Skill, User  # noqa: F401 - register models
from app.models.about_bubble import AboutBubble  # noqa: F401
from app.models.footprint import Footprint  # noqa: F401
from app.models.project import Project  # noqa: F401
from app.repositories.user import UserRepository
from sqlalchemy import text

logger = logging.getLogger(__name__)


def init_schema() -> None:
    """Create tables if absent and the MinIO bucket."""
    from app.models import (  # noqa: F401
        AboutBubble,
        FileRecord,
        Footprint,
        Project,
        Skill,
        User,
        VisitLog,
    )

    Base.metadata.create_all(bind=engine)
    with engine.begin() as conn:
        conn.execute(text("ALTER TABLE IF EXISTS about_bubbles ALTER COLUMN bg_color TYPE VARCHAR(64)"))
        conn.execute(text("ALTER TABLE IF EXISTS about_bubbles ALTER COLUMN glow_color TYPE VARCHAR(64)"))
        conn.execute(text("ALTER TABLE IF EXISTS about_bubbles ALTER COLUMN text_color TYPE VARCHAR(64)"))
        conn.execute(text("ALTER TABLE IF EXISTS about_bubbles ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE"))
        conn.execute(text("ALTER TABLE IF EXISTS about_bubbles ADD COLUMN IF NOT EXISTS remark VARCHAR(256)"))
    ensure_bucket()
    logger.info("Database schema and MinIO bucket ensured")


def seed_admin() -> None:
    with session_scope() as db:
        repo = UserRepository(db)
        if repo.get_by_username(settings.INIT_ADMIN_USERNAME):
            return
        user = User(
            username=settings.INIT_ADMIN_USERNAME,
            email=settings.INIT_ADMIN_EMAIL,
            nickname="Administrator",
            role=UserRole.SUPERADMIN.value,
            password_hash=hash_password(settings.INIT_ADMIN_PASSWORD),
            is_active=True,
        )
        db.add(user)
        logger.info("Seeded initial admin user")


def run() -> None:
    init_schema()
    seed_admin()
