"""Auth endpoints."""
from __future__ import annotations

from fastapi import APIRouter, BackgroundTasks, Depends, Request
from sqlalchemy.orm import Session

from app.api.deps import get_current_active_user
from app.common.response import success
from app.core.database import get_db
from app.models.user import User
from app.schemas.auth import (
    CurrentUser,
    LoginRequest,
    PasswordChangeRequest,
    RefreshRequest,
    TokenPair,
)
from app.services.auth_service import AuthService

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/login")
def login(
    payload: LoginRequest,
    request: Request,
    background: BackgroundTasks,
    db: Session = Depends(get_db),
):
    service = AuthService(db)
    user = service.authenticate(
        payload.username,
        payload.password,
        request=request,
        background=background,
    )
    tokens = service.create_token_pair(user)
    return success(
        data={
            "tokens": tokens.model_dump(),
            "user": CurrentUser.model_validate(user).model_dump(mode="json"),
        }
    )


@router.post("/refresh")
def refresh(payload: RefreshRequest, db: Session = Depends(get_db)):
    service = AuthService(db)
    tokens = service.refresh_tokens(payload.refresh_token)
    return success(data=tokens.model_dump())


@router.post("/logout")
def logout(current=Depends(get_current_active_user), db: Session = Depends(get_db)):
    # Without access to the raw JWT we can't revoke that jti here.
    # Caller should normally hit /auth/logout with body containing the token.
    return success(message="logged out")


@router.post("/logout-token")
def logout_token(
    token_payload: dict,
    current: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Revoke a specific token via body {token: '...'}."""
    token = token_payload.get("token") if isinstance(token_payload, dict) else None
    if not token:
        from app.common.exceptions import ValidationFailed

        raise ValidationFailed("token is required")
    service = AuthService(db)
    payload = service.verify_access(token)
    service.revoke_jti(payload["jti"], int(payload["exp"]))
    return success(message="token revoked")


@router.get("/me")
def me(current: User = Depends(get_current_active_user)):
    return success(data=CurrentUser.model_validate(current).model_dump(mode="json"))


@router.put("/password")
def change_password(
    payload: PasswordChangeRequest,
    background: BackgroundTasks,
    current: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    service = AuthService(db)
    service.change_password(
        current, payload.old_password, payload.new_password, background=background
    )
    db.commit()
    return success(message="password updated")
