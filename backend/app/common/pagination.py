"""Reusable pagination query schema."""
from typing import Optional

from pydantic import BaseModel, Field


class PaginationQuery(BaseModel):
    page: int = Field(1, ge=1)
    page_size: int = Field(20, ge=1, le=100)
    search: Optional[str] = None
    sort: Optional[str] = Field(default=None, description="field:asc|desc")

    @property
    def offset(self) -> int:
        return (self.page - 1) * self.page_size