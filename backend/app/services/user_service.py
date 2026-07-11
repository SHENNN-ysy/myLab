"""User CRUD service."""
from __future__ import annotations

import uuid
from typing import List, Tuple

from sqlalchemy.orm import Session

from app.common.enums import UserRole
from app.common.exceptions import Conflict, NotFound
from app.core.security import hash_password
from app.messaging.audit import emit_audit
from app.models.user import User
from app.repositories.user import UserRepository
from app.schemas.user import UserCreate, UserUpdate


class UserService:
    def __init__(self, db: Session) -> None:
        self.db = db
        self.users = UserRepository(db)

    def list(self, *, page: int, page_size: int) -> Tuple[List[User], int]:
        return self.users.list(page=page, page_size=page_size)

    def get(self, user_id: uuid.UUID) -> User:
        user = self.users.get_by_id(user_id)
        if not user:
            raise NotFound("User not found")
        return user

    async def create(
        self, payload: UserCreate, *, actor: User, request=None
    ) -> User:
        if self.users.get_by_username(payload.username):
            raise Conflict("Username already exists")
        if self.users.get_by_email(payload.email):
            raise Conflict("Email already exists")
        user = User(
            username=payload.username,
            email=payload.email,
            nickname=payload.nickname,
            role=payload.role.value,
            password_hash=hash_password(payload.password),
            is_active=True,
        )
        self.users.add(user)
        self.db.commit()
        self.db.refresh(user)
        await emit_audit(
            action="create",
            resource="user",
            resource_id=user.id,
            user=actor,
            request=request,
            metadata={
                "new_username": user.username,
                "new_role": user.role,
                "new_email": user.email,
            },
        )
        return user

    async def update(
        self,
        user_id: uuid.UUID,
        payload: UserUpdate,
        *,
        actor: User,
        request=None,
    ) -> User:
        user = self.get(user_id)
        data = payload.model_dump(exclude_unset=True)
        password_changed = "password" in data and data["password"]
        if password_changed:
            data["password_hash"] = hash_password(data.pop("password"))
        if "role" in data and data["role"] is not None:
            data["role"] = data["role"].value if hasattr(data["role"], "value") else data["role"]
        self.users.update(user, data)
        self.db.commit()
        self.db.refresh(user)
        await emit_audit(
            action="update",
            resource="user",
            resource_id=user.id,
            user=actor,
            request=request,
            metadata={
                "changed_fields": sorted(
                    k for k in data.keys() if k != "password_hash"
                ),
                "password_changed": password_changed,
                "is_active": user.is_active,
            },
        )
        return user

    async def delete(
        self, user_id: uuid.UUID, *, actor: User, request=None
    ) -> None:
        user = self.get(user_id)
        snapshot = {"username": user.username, "role": user.role}
        self.users.soft_delete(user)
        self.db.commit()
        await emit_audit(
            action="delete",
            resource="user",
            resource_id=user_id,
            user=actor,
            request=request,
            metadata=snapshot,
        )