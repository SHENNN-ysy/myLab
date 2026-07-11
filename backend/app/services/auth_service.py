"""Authentication, token, and authorization services."""
from __future__ import annotations

import uuid
from datetime import datetime, timezone
from typing import Any, Tuple

from fastapi import BackgroundTasks
from sqlalchemy.orm import Session

from app.common.enums import ROLE_HIERARCHY, UserRole
from app.common.exceptions import (
    AuthFailed,
    NotFound,
    PermissionDenied,
    TokenExpired,
    TokenRevoked,
)
from app.core.config import settings
from app.core.redis_client import get_redis
from app.core.security import (
    TOKEN_TYPE_ACCESS,
    TOKEN_TYPE_REFRESH,
    create_access_token,
    create_refresh_token,
    decode_token,
    hash_password,
    verify_password,
)
from app.messaging.notify import notify_security_event
from app.models.user import User
from app.repositories.user import UserRepository
from app.schemas.auth import TokenPair


class AuthService:
    def __init__(self, db: Session) -> None:
        self.db = db
        self.users = UserRepository(db)

    # ---------- credentials -------------------------------------------

    def authenticate(
        self,
        username: str,
        password: str,
        *,
        request=None,
        background: BackgroundTasks | None = None,
    ) -> User:
        user = self.users.get_by_username(username)
        if user is None:
            self._enqueue_login_failure(
                background, username=username, reason="unknown_user"
            )
            raise AuthFailed("Invalid username or password")
        if not user.is_active:
            self._enqueue_login_failure(
                background, username=username, reason="user_disabled"
            )
            raise AuthFailed("User is disabled")
        if not verify_password(password, user.password_hash):
            self._enqueue_login_failure(
                background, username=username, reason="bad_password"
            )
            raise AuthFailed("Invalid username or password")
        user.last_login_at = datetime.now(timezone.utc)
        self.db.flush()
        return user

    @staticmethod
    def _enqueue_login_failure(
        background: BackgroundTasks | None, *, username: str, reason: str
    ) -> None:
        """Notify admins of a login failure via RabbitMQ.

        We use ``BackgroundTasks`` when an event loop is available (the
        FastAPI request flow); we fall back to a fire-and-forget asyncio
        task when called from a sync context with a loop running.
        """
        if background is not None:
            background.add_task(
                notify_security_event,
                subject=f"[MyBlog] Failed login attempt: {username}",
                template="security/login_failed",
                context={"username": username, "reason": reason},
                severity="warning",
            )
        else:
            # Synchronous fallback: just log so we lose nothing.
            import logging

            logging.getLogger("myblog.notify").warning(
                "login failure: username=%s reason=%s (no background tasks; "
                "client-side mail not delivered)",
                username,
                reason,
            )

    # ---------- tokens ------------------------------------------------

    def create_token_pair(self, user: User) -> TokenPair:
        sub = str(user.id)
        access = create_access_token(sub, user.role)
        refresh = create_refresh_token(sub, user.role)
        return TokenPair(
            access_token=access,
            refresh_token=refresh,
            expires_in=settings.JWT_ACCESS_EXPIRE_MINUTES * 60,
        )

    def refresh_tokens(self, refresh_token: str) -> TokenPair:
        try:
            payload = decode_token(refresh_token)
        except Exception as exc:  # PyJWT raises specific subclasses
            raise TokenExpired("Refresh token expired or invalid") from exc
        if payload.get("type") != TOKEN_TYPE_REFRESH:
            raise TokenExpired("Token type mismatch")
        self._ensure_not_revoked(payload.get("jti"))
        user_id = uuid.UUID(payload["sub"])
        user = self.users.get_by_id(user_id)
        if not user or not user.is_active:
            raise AuthFailed("User is no longer valid")
        return self.create_token_pair(user)

    def revoke_jti(self, jti: str, exp: int) -> None:
        ttl = max(1, exp - int(datetime.now(timezone.utc).timestamp()))
        get_redis().setex(f"jwt:blacklist:{jti}", ttl, "1")

    def verify_access(self, token: str) -> dict[str, Any]:
        try:
            payload = decode_token(token)
        except Exception as exc:
            raise TokenExpired("Access token expired or invalid") from exc
        if payload.get("type") != TOKEN_TYPE_ACCESS:
            raise TokenExpired("Token type mismatch")
        self._ensure_not_revoked(payload.get("jti"))
        return payload

    def _ensure_not_revoked(self, jti: str) -> None:
        if not jti:
            return
        if get_redis().exists(f"jwt:blacklist:{jti}"):
            raise TokenRevoked("Token has been revoked")

    # ---------- lookup -------------------------------------------------

    def get_user_by_payload(self, payload: dict[str, Any]) -> User:
        user_id = uuid.UUID(payload["sub"])
        user = self.users.get_by_id(user_id)
        if user is None:
            raise NotFound("User not found")
        if not user.is_active:
            raise AuthFailed("User is disabled")
        return user

    # ---------- passwords --------------------------------------------

    def change_password(
        self,
        user: User,
        old_password: str,
        new_password: str,
        *,
        background: BackgroundTasks | None = None,
    ) -> None:
        if not verify_password(old_password, user.password_hash):
            raise AuthFailed("Old password is incorrect")
        user.password_hash = hash_password(new_password)
        self.db.flush()
        if background is not None:
            background.add_task(
                notify_security_event,
                subject=f"[MyBlog] Password changed for {user.username}",
                template="security/password_changed",
                context={
                    "user_id": str(user.id),
                    "username": user.username,
                },
                severity="warning",
            )


def role_at_least(role: str, required: UserRole) -> bool:
    return ROLE_HIERARCHY.get(UserRole(role), 0) >= ROLE_HIERARCHY.get(required, 0)


def require_role(user: User, required: UserRole) -> None:
    if not role_at_least(user.role, required):
        raise PermissionDenied(f"Requires role {required.value}")