"""Import all models so SQLAlchemy registers them with Base.metadata."""
from app.models.about_bubble import AboutBubble
from app.models.base import Base, BaseModel
from app.models.file_record import FileRecord
from app.models.footprint import Footprint
from app.models.project import Project
from app.models.skill import Skill
from app.models.user import User
from app.models.visit_log import VisitLog

__all__ = [
    "Base",
    "BaseModel",
    "User",
    "Skill",
    "Project",
    "Footprint",
    "AboutBubble",
    "FileRecord",
    "VisitLog",
]  # noqa: F401