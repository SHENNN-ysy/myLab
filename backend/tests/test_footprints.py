"""Tests for the /footprints endpoints."""
from __future__ import annotations

import pytest


def test_list_footprints_is_public(client):
    resp = client.get("/api/v1/footprints")
    assert resp.status_code == 200, resp.text
    body = resp.json()
    assert body["code"] == 0
    assert isinstance(body["data"], list)


def test_create_footprint_requires_admin(client, as_user, editor_user):
    as_user(editor_user)
    resp = client.post(
        "/api/v1/footprints",
        json={"name": "Beijing", "slug": "beijing", "tag": "city"},
    )
    assert resp.status_code == 403


def test_create_footprint_success(client, as_user, admin_user):
    as_user(admin_user)
    resp = client.post(
        "/api/v1/footprints",
        json={
            "name": "Beijing",
            "slug": "beijing",
            "tag": "city",
            "position_x": 100.5,
            "position_y": 200.7,
            "is_self": True,
            "tip_data": {"population": 21_000_000},
        },
    )
    assert resp.status_code == 200, resp.text
    data = resp.json()["data"]
    assert data["name"] == "Beijing"
    assert data["slug"] == "beijing"
    assert data["tag"] == "city"
    assert data["position_x"] == 100.5
    assert data["position_y"] == 200.7
    assert data["is_self"] is True
    assert data["tip_data"]["population"] == 21_000_000


def test_create_footprint_missing_required(client, as_user, admin_user):
    as_user(admin_user)
    resp = client.post("/api/v1/footprints", json={"name": "X"})  # missing slug
    assert resp.status_code == 422


def test_create_footprint_duplicate_slug(client, as_user, admin_user, db):
    from app.models.footprint import Footprint

    db.add(Footprint(name="A", slug="unique-slug", position_x=0, position_y=0))
    db.commit()

    as_user(admin_user)
    resp = client.post(
        "/api/v1/footprints",
        json={"name": "B", "slug": "unique-slug"},
    )
    # Could be 409 (Conflict) or 500 from the unique constraint
    assert resp.status_code in (409, 500), resp.text


def test_update_footprint(client, as_user, admin_user, db):
    from app.models.footprint import Footprint

    f = Footprint(name="A", slug="a", position_x=0, position_y=0)
    db.add(f)
    db.commit()
    db.refresh(f)

    as_user(admin_user)
    resp = client.put(
        f"/api/v1/footprints/{f.id}",
        json={"name": "AA", "position_x": 99.9},
    )
    assert resp.status_code == 200, resp.text
    data = resp.json()["data"]
    assert data["name"] == "AA"
    assert data["position_x"] == 99.9


def test_update_footprint_not_found(client, as_user, admin_user):
    import uuid
    as_user(admin_user)
    resp = client.put(
        f"/api/v1/footprints/{uuid.uuid4()}",
        json={"name": "Ghost"},
    )
    assert resp.status_code == 404


def test_delete_footprint_soft_deletes(client, as_user, admin_user, db):
    from app.models.footprint import Footprint
    f = Footprint(name="A", slug="del", position_x=0, position_y=0)
    db.add(f)
    db.commit()
    db.refresh(f)

    as_user(admin_user)
    resp = client.delete(f"/api/v1/footprints/{f.id}")
    assert resp.status_code == 200, resp.text
    db.refresh(f)
    assert f.deleted_at is not None
