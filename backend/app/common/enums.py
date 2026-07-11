"""Global enums used across schemas and models."""
from enum import Enum


class UserRole(str, Enum):
    SUPERADMIN = "superadmin"
    ADMIN = "admin"
    EDITOR = "editor"
    VIEWER = "viewer"


ROLE_HIERARCHY = {
    UserRole.SUPERADMIN: 4,
    UserRole.ADMIN: 3,
    UserRole.EDITOR: 2,
    UserRole.VIEWER: 1,
}


class BubbleTier(str, Enum):
    BIG = "big"
    MID = "mid"
    SMALL = "small"


class SkillLevel(str, Enum):
    BEGINNER = "beginner"
    INTERMEDIATE = "intermediate"
    ADVANCED = "advanced"
    EXPERT = "expert"