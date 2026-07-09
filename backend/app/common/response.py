"""Unified API response envelope."""
from __future__ import annotations

import time
from typing import Any, Generic, List, Optional, TypeVar

from pydantic import BaseModel, Field

T = TypeVar("T")


class Pagination(BaseModel):
    page: int
    page_size: int
    total: int
    total_pages: int


class PageData(BaseModel, Generic[T]):
    items: List[T]
    pagination: Pagination


class ApiResponse(BaseModel, Generic[T]):
    code: int = 0
    message: str = "success"
    data: Optional[T] = None
    error: Optional[str] = None
    request_id: Optional[str] = None
    timestamp: int = Field(default_factory=lambda: int(time.time()))


def success(data: Any = None, message: str = "success") -> dict:
    return ApiResponse(data=data, message=message).model_dump(mode="json")


def paginated(
    items: List[Any],
    page: int,
    page_size: int,
    total: int,
    message: str = "success",
) -> dict:
    total_pages = (total + page_size - 1) // page_size if page_size else 0
    return success(
        data={
            "items": items,
            "pagination": Pagination(
                page=page,
                page_size=page_size,
                total=total,
                total_pages=total_pages,
            ).model_dump(),
        },
        message=message,
    )


def failure(
    code: int,
    message: str,
    error: Optional[str] = None,
    request_id: Optional[str] = None,
) -> dict:
    return ApiResponse(
        code=code,
        message=message,
        error=error,
        request_id=request_id,
    ).model_dump(mode="json")