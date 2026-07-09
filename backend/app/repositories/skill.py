"""Skill repository."""
from __future__ import annotations

from sqlalchemy import asc

from app.models.skill import Skill
from app.repositories.base import BaseRepository


class SkillRepository(BaseRepository[Skill]):
    model = Skill

    def list_ordered(self, *, page: int = 1, page_size: int = 100):
        return self.list(
            page=page,
            page_size=page_size,
            order_by=(asc(Skill.order_num), asc(Skill.name)),
        )