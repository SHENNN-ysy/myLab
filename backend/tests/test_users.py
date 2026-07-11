"""Tests for the /users endpoints."""
from __future__ import annotations

import pytest


# ----------------------------- list_users ----------------------------------


def test_list_users_requires_admin(client, as_user, viewer_user):
    as_user(viewer_user)
    resp = client.get("/api/v1/users")
    assert resp.status_code == 403
    assert resp.json()["code"] == 10004  # PermissionDenied


def test_list_users_requires_authentication(client):
    resp = client.get("/api/v1/users")
    assert resp.status_code in (401, 403)


def test_list_users_returns_paginated(client, as_user, admin_user, superadmin_user, db):
    # db fixture already has superadmin_user, admin_user; add two more
    from app.core.security import hash_password
    from app.models.user import User

    for i in range(3):
        db.add(
            User(
                username=f"user{i}",
                email=f"u{i}@e.com",
                role="viewer",
                password_hash=hash_password("ViewerPass1234"),
                is_active=True,
            )
        )
    db.commit()

    as_user(admin_user)
    resp = client.get("/api/v1/users?page=1&page_size=10")
    assert resp.status_code == 200, resp.text
    body = resp.json()
    assert body["code"] == 0
    pag = body["data"]["pagination"]
    assert pag["page"] == 1
    assert pag["page_size"] == 10
    assert pag["total"] >= 5
    assert len(body["data"]["items"]) >= 5
    for item in body["data"]["items"]:
        assert "password_hash" not in item
        assert {"id", "username", "email", "role", "is_active"} <= set(item)


# ----------------------------- create_user ---------------------------------


def test_create_user_requires_superadmin(client, as_user, admin_user):
    as_user(admin_user)
    resp = client.post(
        "/api/v1/users",
        json={
            "username": "newby",
            "email": "newby@example.com",
            "role": "viewer",
            "password": "ViewerPass1234",
        },
    )
    assert resp.status_code == 403


def test_create_user_success(client, as_user, superadmin_user):
    as_user(superadmin_user)
    resp = client.post(
        "/api/v1/users",
        json={
            "username": "alice",
            "email": "alice@example.com",
            "nickname": "Alice",
            "role": "editor",
            "password": "AlicePass1234",
        },
    )
    assert resp.status_code == 200, resp.text
    data = resp.json()["data"]
    assert data["username"] == "alice"
    assert data["role"] == "editor"
    assert data["is_active"] is True
    assert data["id"]


def test_create_user_duplicate_username_returns_409(client, as_user, superadmin_user, admin_user):
    as_user(superadmin_user)
    resp = client.post(
        "/api/v1/users",
        json={
            "username": admin_user.username,  # collision
            "email": "totally-different@example.com",
            "role": "viewer",
            "password": "Whatever12345",
        },
    )
    assert resp.status_code == 409
    assert resp.json()["code"] == 10006  # Conflict


def test_create_user_duplicate_email_returns_409(client, as_user, superadmin_user, admin_user):
    as_user(superadmin_user)
    resp = client.post(
        "/api/v1/users",
        json={
            "username": "totally-different",
            "email": admin_user.email,  # collision
            "role": "viewer",
            "password": "Whatever12345",
        },
    )
    assert resp.status_code == 409


def test_create_user_password_too_short_returns_422(client, as_user, superadmin_user):
    as_user(superadmin_user)
    resp = client.post(
        "/api/v1/users",
        json={
            "username": "shorty",
            "email": "s@e.com",
            "role": "viewer",
            "password": "short",  # min_length=8
        },
    )
    assert resp.status_code == 422


def test_create_user_invalid_email_returns_422(client, as_user, superadmin_user):
    as_user(superadmin_user)
    resp = client.post(
        "/api/v1/users",
        json={
            "username": "invalidmail",
            "email": "not-an-email",
            "role": "viewer",
            "password": "ValidPass1234",
        },
    )
    assert resp.status_code == 422


# ----------------------------- update_user ---------------------------------


def test_update_user_requires_admin(client, as_user, viewer_user, editor_user):
    as_user(viewer_user)
    resp = client.put(
        f"/api/v1/users/{editor_user.id}",
        json={"nickname": "nope"},
    )
    assert resp.status_code == 403


def test_update_user_nickname_and_email(client, as_user, admin_user, editor_user, db):
    as_user(admin_user)
    resp = client.put(
        f"/api/v1/users/{editor_user.id}",
        json={"nickname": "Eddy", "email": "eddy@example.com"},
    )
    assert resp.status_code == 200, resp.text
    data = resp.json()["data"]
    assert data["nickname"] == "Eddy"
    assert data["email"] == "eddy@example.com"
    db.refresh(editor_user)
    assert editor_user.nickname == "Eddy"


def test_update_user_password_changes_hash(client, as_user, admin_user, editor_user, db):
    from app.core.security import verify_password

    as_user(admin_user)
    old_hash = editor_user.password_hash
    resp = client.put(
        f"/api/v1/users/{editor_user.id}",
        json={"password": "NewEditorPass1234"},
    )
    assert resp.status_code == 200, resp.text
    db.refresh(editor_user)
    assert editor_user.password_hash != old_hash
    assert verify_password("NewEditorPass1234", editor_user.password_hash)


def test_update_user_deactivate(client, as_user, admin_user, editor_user, db):
    as_user(admin_user)
    resp = client.put(
        f"/api/v1/users/{editor_user.id}",
        json={"is_active": False},
    )
    assert resp.status_code == 200
    db.refresh(editor_user)
    assert editor_user.is_active is False


def test_update_user_not_found_returns_404(client, as_user, admin_user):
    import uuid

    as_user(admin_user)
    resp = client.put(
        f"/api/v1/users/{uuid.uuid4()}",
        json={"nickname": "Ghost"},
    )
    assert resp.status_code == 404
    assert resp.json()["code"] == 10005


# ----------------------------- delete_user ---------------------------------


def test_delete_user_requires_superadmin(client, as_user, admin_user, viewer_user):
    as_user(admin_user)
    resp = client.delete(f"/api/v1/users/{viewer_user.id}")
    assert resp.status_code == 403


def test_delete_user_soft_deletes(client, as_user, superadmin_user, viewer_user, db):
    from datetime import datetime

    as_user(superadmin_user)
    resp = client.delete(f"/api/v1/users/{viewer_user.id}")
    assert resp.status_code == 200
    db.refresh(viewer_user)
    # Soft delete only - the row still exists
    assert viewer_user.deleted_at is not None
    assert isinstance(viewer_user.deleted_at, datetime)


def test_deleted_user_cannot_log_in(client, as_user, superadmin_user, viewer_user):
    as_user(superadmin_user)
    client.delete(f"/api/v1/users/{viewer_user.id}")
    resp = client.post(
        "/api/v1/auth/login",
        json={"username": viewer_user.username, "password": "ViewerPass1234"},
    )
    assert resp.status_code == 401


def test_delete_user_not_found_returns_404(client, as_user, superadmin_user):
    import uuid

    as_user(superadmin_user)
    resp = client.delete(f"/api/v1/users/{uuid.uuid4()}")
    assert resp.status_code == 404
