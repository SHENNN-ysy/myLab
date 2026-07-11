"""Aggregate /api/v1 router."""
from fastapi import APIRouter

from app.api.v1.endpoints import (
    about_bubbles,
    auth,
    files,
    footprints,
    health,
    projects,
    skills,
    system,
    users,
    visits,
)

api_router = APIRouter(prefix="/api/v1")
api_router.include_router(health.router)
api_router.include_router(auth.router)
api_router.include_router(users.router)
api_router.include_router(skills.router)
api_router.include_router(projects.router)
api_router.include_router(footprints.router)
api_router.include_router(about_bubbles.router)
api_router.include_router(files.router)
api_router.include_router(visits.router)
api_router.include_router(system.router)
