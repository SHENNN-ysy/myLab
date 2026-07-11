"""Tests for the /projects endpoints."""
from __future__ import annotations

import pytest


def test_list_projects_is_public(client):
    resp = client.get("/api/v1/projects")
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 0
    assert "items" in body["data"]


def test_list_projects_filter_by_tag_and_year(client, db):
    from app.models.project import Project

    db.add_all(
        [
            Project(
                title="A", slug="a", year=2024, tag="web", description="", content="", tech=[]
            ),
            Project(
                title="B", slug="b", year=2023, tag="cli", description="", content="", tech=[]
            ),
            Project(
                title="C", slug="c", year=2024, tag="web", description="", content="", tech=[]
            ),
        ]
    )
    db.commit()

    # Filter by tag
    r1 = client.get("/api/v1/projects?tag=web")
    assert r1.status_code == 200
    titles = [it["title"] for it in r1.json()["data"]["items"]]
    assert set(titles) == {"A", "C"}

    # Filter by year
    r2 = client.get("/api/v1/projects?year=2024")
    assert r2.status_code == 200
    titles = [it["title"] for it in r2.json()["data"]["items"]]
    assert set(titles) == {"A", "C"}


def test_create_project_requires_admin(client, as_user, editor_user):
    as_user(editor_user)
    resp = client.post(
        "/api/v1/projects",
        json={"title": "X", "slug": "x", "year": 2024},
    )
    assert resp.status_code == 403


def test_create_project_success(client, as_user, admin_user):
    as_user(admin_user)
    resp = client.post(
        "/api/v1/projects",
        json={
            "title": "My Project",
            "slug": "my-project",
            "year": 2024,
            "description": "Cool project",
            "content": "Long content...",
            "tag": "web",
            "tech": ["Python", "FastAPI"],
            "order_num": 0,
        },
    )
    assert resp.status_code == 200, resp.text
    data = resp.json()["data"]
    assert data["title"] == "My Project"
    assert data["slug"] == "my-project"
    assert data["year"] == 2024
    assert data["tech"] == ["Python", "FastAPI"]


def test_create_project_missing_required(client, as_user, admin_user):
    as_user(admin_user)
    resp = client.post("/api/v1/projects", json={"title": "X"})  # missing slug, year
    assert resp.status_code == 422


def test_update_project(client, as_user, admin_user, db):
    from app.models.project import Project

    p = Project(title="old", slug="o", year=2020, description="", content="", tech=[])
    db.add(p)
    db.commit()
    db.refresh(p)

    as_user(admin_user)
    resp = client.put(
        f"/api/v1/projects/{p.id}",
        json={"title": "new", "year": 2025, "tech": ["Go"]},
    )
    assert resp.status_code == 200, resp.text
    data = resp.json()["data"]
    assert data["title"] == "new"
    assert data["year"] == 2025
    assert data["tech"] == ["Go"]


def test_update_project_not_found(client, as_user, admin_user):
    import uuid
    as_user(admin_user)
    resp = client.put(
        f"/api/v1/projects/{uuid.uuid4()}",
        json={"title": "ghost"},
    )
    assert resp.status_code == 404


def test_delete_project_soft_deletes(client, as_user, admin_user, db):
    from app.models.project import Project
    p = Project(title="kill", slug="k", year=2020, description="", content="", tech=[])
    db.add(p)
    db.commit()
    db.refresh(p)

    as_user(admin_user)
    resp = client.delete(f"/api/v1/projects/{p.id}")
    assert resp.status_code == 200, resp.text
    db.refresh(p)
    assert p.deleted_at is not None


def test_delete_project_not_found(client, as_user, admin_user):
    import uuid
    as_user(admin_user)
    resp = client.delete(f"/api/v1/projects/{uuid.uuid4()}")
    assert resp.status_code == 404
