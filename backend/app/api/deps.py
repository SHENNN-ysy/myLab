"""FastAPI dependency providers."""
from __future__ import annotations

import uuid
from typing import Annotated

from fastapi import Depends, Header, HTTPException, status
from sqlalchemy.orm import Session

from app.common.enums import UserRole
from app.common.exceptions import AuthFailed, PermissionDenied
from app.core.database import get_db
from app.models.user import User
from app.services.auth_service import AuthService, require_role


def _extract_token(authorization: str | None) -> str:
    if not authorization or not authorization.lower().startswith("bearer "):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing bearer token",
            headers={"WWW-Authenticate": "Bearer"},
        )
    return authorization.split(" ", 1)[1].strip()


def get_current_user(
    authorization: Annotated[str | None, Header()] = None,
    db: Session = Depends(get_db),
) -> User:
    token = _extract_token(authorization)
    auth = AuthService(db)
    payload = auth.verify_access(token)
    return auth.get_user_by_payload(payload)


def get_current_active_user(
    user: User = Depends(get_current_user),
) -> User:
    if not user.is_active:
        raise AuthFailed("User is disabled")
    return user


def require_admin(
    user: User = Depends(get_current_active_user),
) -> User:
    require_role(user, UserRole.ADMIN)
    return user


def require_superadmin(
    user: User = Depends(get_current_active_user),
) -> User:
    require_role(user, UserRole.SUPERADMIN)
    return user