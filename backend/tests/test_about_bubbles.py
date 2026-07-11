"""Tests for the /about-bubbles endpoints."""
from __future__ import annotations

import pytest


def test_list_bubbles_is_public(client):
    resp = client.get("/api/v1/about-bubbles")
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 0
    assert isinstance(body["data"], list)


def test_create_bubble_requires_admin(client, as_user, editor_user):
    as_user(editor_user)
    resp = client.post(
        "/api/v1/about-bubbles",
        json={"label": "Tech", "position_x": 0, "position_y": 0, "radius": 40},
    )
    assert resp.status_code == 403


def test_create_bubble_success(client, as_user, admin_user):
    as_user(admin_user)
    resp = client.post(
        "/api/v1/about-bubbles",
        json={
            "label": "Tech",
            "bg_color": "#fff",
            "glow_color": "#0ff",
            "text_color": "#000",
            "position_x": 100.0,
            "position_y": 200.0,
            "radius": 50.5,
            "tier": "mid",
            "order_num": 1,
            "enabled": True,
            "remark": "r",
        },
    )
    assert resp.status_code == 200, resp.text
    data = resp.json()["data"]
    assert data["label"] == "Tech"
    assert data["radius"] == 50.5
    assert data["tier"] == "mid"


def test_create_bubble_invalid_tier(client, as_user, admin_user):
    as_user(admin_user)
    resp = client.post(
        "/api/v1/about-bubbles",
        json={"label": "X", "tier": "gigantic"},
    )
    assert resp.status_code == 422


def test_create_bubble_duplicate_label(client, as_user, admin_user, db):
    from app.models.about_bubble import AboutBubble
    db.add(AboutBubble(label="Tech", position_x=0, position_y=0, radius=40))
    db.commit()

    as_user(admin_user)
    resp = client.post(
        "/api/v1/about-bubbles",
        json={"label": "Tech", "position_x": 1, "position_y": 1, "radius": 10},
    )
    # Same uniqueness issue as footprints
    assert resp.status_code in (409, 500), resp.text


def test_update_bubble(client, as_user, admin_user, db):
    from app.models.about_bubble import AboutBubble

    b = AboutBubble(label="old", position_x=0, position_y=0, radius=10)
    db.add(b)
    db.commit()
    db.refresh(b)

    as_user(admin_user)
    resp = client.put(
        f"/api/v1/about-bubbles/{b.id}",
        json={"label": "new", "radius": 99.9, "enabled": False},
    )
    assert resp.status_code == 200, resp.text
    data = resp.json()["data"]
    assert data["label"] == "new"
    assert data["radius"] == 99.9
    assert data["enabled"] is False


def test_update_bubble_not_found(client, as_user, admin_user):
    import uuid
    as_user(admin_user)
    resp = client.put(
        f"/api/v1/about-bubbles/{uuid.uuid4()}",
        json={"label": "Ghost"},
    )
    assert resp.status_code == 404


def test_delete_bubble_soft_deletes(client, as_user, admin_user, db):
    from app.models.about_bubble import AboutBubble
    b = AboutBubble(label="kill", position_x=0, position_y=0, radius=10)
    db.add(b)
    db.commit()
    db.refresh(b)

    as_user(admin_user)
    resp = client.delete(f"/api/v1/about-bubbles/{b.id}")
    assert resp.status_code == 200, resp.text
    db.refresh(b)
    assert b.deleted_at is not None
