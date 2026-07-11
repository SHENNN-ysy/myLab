"""Project repository."""
from __future__ import annotations

from typing import Optional

from sqlalchemy import asc, desc

from app.models.project import Project
from app.repositories.base import BaseRepository


class ProjectRepository(BaseRepository[Project]):
    model = Project

    def list_ordered(
        self,
        *,
        page: int = 1,
        page_size: int = 20,
        tag: Optional[str] = None,
        year: Optional[int] = None,
    ):
        filters = []
        if tag:
            filters.append(Project.tag == tag)
        if year:
            filters.append(Project.year == year)
        return self.list(
            page=page,
            page_size=page_size,
            order_by=(desc(Project.year), asc(Project.order_num)),
            filters=filters,
        )