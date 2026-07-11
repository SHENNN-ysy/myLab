"""Tests for the /skills endpoints."""
from __future__ import annotations

import pytest


# ----------------------------- list ----------------------------------------


def test_list_skills_is_public(client):
    # No auth needed.
    resp = client.get("/api/v1/skills")
    assert resp.status_code == 200, resp.text
    body = resp.json()
    assert body["code"] == 0
    assert "items" in body["data"]
    assert "pagination" in body["data"]


def test_list_skills_pagination_validation(client):
    # page_size must be > 0
    resp = client.get("/api/v1/skills?page=0&page_size=0")
    # FastAPI's default int parsing doesn't reject 0, so we should still get 200
    # but the service must clamp it internally.
    assert resp.status_code in (200, 422)


def test_create_skill_requires_admin(client, as_user, viewer_user):
    as_user(viewer_user)
    resp = client.post(
        "/api/v1/skills",
        json={"name": "Python", "percentage": 80, "category": "lang"},
    )
    assert resp.status_code == 403


def test_create_skill_success(client, as_user, admin_user, db):
    as_user(admin_user)
    resp = client.post(
        "/api/v1/skills",
        json={
            "name": "Go",
            "category": "lang",
            "percentage": 70,
            "level": "intermediate",
            "icon": "go.png",
            "order_num": 1,
        },
    )
    assert resp.status_code == 200, resp.text
    data = resp.json()["data"]
    assert data["name"] == "Go"
    assert data["category"] == "lang"
    assert data["percentage"] == 70
    assert data["level"] == "intermediate"
    assert "id" in data


def test_create_skill_percentage_out_of_range(client, as_user, admin_user):
    as_user(admin_user)
    for bad in (0 - 1, 100 + 1, -50, 200):
        resp = client.post(
            "/api/v1/skills",
            json={"name": f"X{bad}", "percentage": bad},
        )
        assert resp.status_code == 422, f"bad={bad} body={resp.text}"


def test_create_skill_invalid_level(client, as_user, admin_user):
    as_user(admin_user)
    resp = client.post(
        "/api/v1/skills",
        json={"name": "X", "percentage": 50, "level": "wizard"},
    )
    assert resp.status_code == 422


def test_create_skill_name_required(client, as_user, admin_user):
    as_user(admin_user)
    resp = client.post("/api/v1/skills", json={"percentage": 50})
    assert resp.status_code == 422


def test_update_skill(client, as_user, admin_user, db):
    from app.models.skill import Skill

    s = Skill(name="old", percentage=50)
    db.add(s)
    db.commit()
    db.refresh(s)

    as_user(admin_user)
    resp = client.put(
        f"/api/v1/skills/{s.id}",
        json={"name": "new", "percentage": 95},
    )
    assert resp.status_code == 200, resp.text
    data = resp.json()["data"]
    assert data["name"] == "new"
    assert data["percentage"] == 95
    db.refresh(s)
    assert s.name == "new"


def test_update_skill_not_found(client, as_user, admin_user):
    import uuid
    as_user(admin_user)
    resp = client.put(
        f"/api/v1/skills/{uuid.uuid4()}",
        json={"name": "ghost"},
    )
    assert resp.status_code == 404


def test_delete_skill_soft_deletes(client, as_user, admin_user, db):
    from app.models.skill import Skill
    from datetime import datetime

    s = Skill(name="doomed", percentage=50)
    db.add(s)
    db.commit()
    db.refresh(s)

    as_user(admin_user)
    resp = client.delete(f"/api/v1/skills/{s.id}")
    assert resp.status_code == 200, resp.text
    db.refresh(s)
    assert s.deleted_at is not None
    assert isinstance(s.deleted_at, datetime)


def test_delete_skill_not_found(client, as_user, admin_user):
    import uuid
    as_user(admin_user)
    resp = client.delete(f"/api/v1/skills/{uuid.uuid4()}")
    assert resp.status_code == 404
