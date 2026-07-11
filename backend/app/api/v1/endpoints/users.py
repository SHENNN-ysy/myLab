"""User management endpoints."""
from __future__ import annotations

import uuid

from fastapi import APIRouter, Depends, Request
from sqlalchemy.orm import Session

from app.api.deps import get_current_active_user, require_admin, require_superadmin
from app.common.pagination import PaginationQuery
from app.common.response import paginated, success
from app.core.database import get_db
from app.models.user import User
from app.schemas.user import UserCreate, UserOut, UserUpdate
from app.services.user_service import UserService

router = APIRouter(prefix="/users", tags=["users"])


@router.get("")
def list_users(
    page: int = 1,
    page_size: int = 20,
    db: Session = Depends(get_db),
    _: User = Depends(require_admin),
):
    service = UserService(db)
    items, total = service.list(page=page, page_size=page_size)
    return paginated(
        [UserOut.model_validate(u).model_dump(mode="json") for u in items],
        page=page,
        page_size=page_size,
        total=total,
    )


@router.post("")
async def create_user(
    payload: UserCreate,
    request: Request,
    db: Session = Depends(get_db),
    actor: User = Depends(require_superadmin),
):
    service = UserService(db)
    user = await service.create(payload, actor=actor, request=request)
    return success(data=UserOut.model_validate(user).model_dump(mode="json"))


@router.put("/{user_id}")
async def update_user(
    user_id: uuid.UUID,
    payload: UserUpdate,
    request: Request,
    db: Session = Depends(get_db),
    actor: User = Depends(require_admin),
):
    service = UserService(db)
    user = await service.update(user_id, payload, actor=actor, request=request)
    return success(data=UserOut.model_validate(user).model_dump(mode="json"))


@router.delete("/{user_id}")
async def delete_user(
    user_id: uuid.UUID,
    request: Request,
    db: Session = Depends(get_db),
    actor: User = Depends(require_superadmin),
):
    service = UserService(db)
    await service.delete(user_id, actor=actor, request=request)
    return success(message="user deleted")
