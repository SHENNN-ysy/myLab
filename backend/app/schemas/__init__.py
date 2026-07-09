"""Import schemas for convenience."""
from app.schemas.about_bubble import (
    AboutBubbleCreate,
    AboutBubbleOut,
    AboutBubbleUpdate,
)
from app.schemas.auth import (
    CurrentUser,
    LoginRequest,
    PasswordChangeRequest,
    RefreshRequest,
    TokenPair,
)
from app.schemas.file import FileOut
from app.schemas.footprint import (
    FootprintCreate,
    FootprintOut,
    FootprintUpdate,
)
from app.schemas.project import (
    ProjectCreate,
    ProjectOut,
    ProjectUpdate,
)
from app.schemas.skill import (
    SkillCreate,
    SkillOut,
    SkillUpdate,
)
from app.schemas.user import UserCreate, UserOut, UserUpdate
from app.schemas.visit import VisitLogOut, VisitStats

__all__ = [
    "LoginRequest",
    "TokenPair",
    "RefreshRequest",
    "PasswordChangeRequest",
    "CurrentUser",
    "UserCreate",
    "UserUpdate",
    "UserOut",
    "SkillCreate",
    "SkillUpdate",
    "SkillOut",
    "ProjectCreate",
    "ProjectUpdate",
    "ProjectOut",
    "FootprintCreate",
    "FootprintUpdate",
    "FootprintOut",
    "AboutBubbleCreate",
    "AboutBubbleUpdate",
    "AboutBubbleOut",
    "FileOut",
    "VisitLogOut",
    "VisitStats",
]  # noqa: F401