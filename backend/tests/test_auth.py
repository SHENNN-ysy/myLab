"""Tests for the auth endpoints (login / refresh / logout / me / password)."""
from __future__ import annotations

import pytest
from fastapi.testclient import TestClient

from app.core.security import create_access_token, create_refresh_token


def _login(client: TestClient, *, username: str, password: str):
    return client.post(
        "/api/v1/auth/login",
        json={"username": username, "password": password},
    )


def test_login_with_valid_credentials_returns_token_pair(client, admin_user):
    resp = _login(client, username=admin_user.username, password="AdminPass1234")
    assert resp.status_code == 200, resp.text
    body = resp.json()
    assert body["code"] == 0
    tokens = body["data"]["tokens"]
    assert tokens["access_token"]
    assert tokens["refresh_token"]
    assert tokens["token_type"] == "bearer"
    assert tokens["expires_in"] > 0
    user = body["data"]["user"]
    assert user["username"] == admin_user.username
    assert user["role"] == "admin"


def test_login_with_wrong_password_returns_401(client, admin_user):
    resp = _login(client, username=admin_user.username, password="wrong-password")
    assert resp.status_code == 401
    body = resp.json()
    assert body["code"] == 10001  # AuthFailed
    assert "Invalid" in body["message"]


def test_login_with_unknown_user_returns_401(client):
    resp = _login(client, username="ghost", password="whatever123")
    assert resp.status_code == 401
    assert resp.json()["code"] == 10001


def test_login_with_disabled_user_returns_401(client, db, admin_user):
    admin_user.is_active = False
    db.commit()
    resp = _login(client, username=admin_user.username, password="AdminPass1234")
    assert resp.status_code == 401


def test_login_payload_validation(client):
    # username too short + password too short
    resp = client.post(
        "/api/v1/auth/login",
        json={"username": "ab", "password": "short"},
    )
    assert resp.status_code == 422
    assert resp.json()["code"] == 10007


def test_login_missing_fields(client):
    resp = client.post("/api/v1/auth/login", json={"username": "alice"})
    assert resp.status_code == 422


def test_login_updates_last_login(client, admin_user, db):
    assert admin_user.last_login_at is None
    resp = _login(client, username=admin_user.username, password="AdminPass1234")
    assert resp.status_code == 200
    db.refresh(admin_user)
    assert admin_user.last_login_at is not None


def test_refresh_token_returns_new_pair(client, admin_user):
    refresh = create_refresh_token(str(admin_user.id), admin_user.role)
    resp = client.post(
        "/api/v1/auth/refresh",
        json={"refresh_token": refresh},
    )
    assert resp.status_code == 200, resp.text
    tokens = resp.json()["data"]
    assert tokens["access_token"]
    assert tokens["refresh_token"]
    assert tokens["refresh_token"] != refresh  # jti should differ


def test_refresh_with_access_token_is_rejected(client, admin_user, admin_headers):
    # Using an *access* token as a refresh should fail.
    access = admin_headers["Authorization"].split(" ", 1)[1]
    resp = client.post("/api/v1/auth/refresh", json={"refresh_token": access})
    assert resp.status_code == 401
    assert resp.json()["code"] == 10002  # TokenExpired (token type mismatch)


def test_refresh_with_garbage_returns_401(client):
    resp = client.post(
        "/api/v1/auth/refresh",
        json={"refresh_token": "totally-not-a-jwt"},
    )
    assert resp.status_code == 401
    assert resp.json()["code"] == 10002


def test_me_returns_current_user(client, as_user, admin_user):
    as_user(admin_user)
    resp = client.get("/api/v1/auth/me")
    assert resp.status_code == 200
    data = resp.json()["data"]
    assert data["username"] == admin_user.username
    assert data["role"] == "admin"
    assert "email" in data


def test_me_without_token_returns_401(client):
    resp = client.get("/api/v1/auth/me")
    assert resp.status_code in (401, 403)
    # The dependency raises HTTPException(401) which goes through Starlette
    # handler and is not reshaped by our exception handler.


def test_me_with_invalid_bearer_returns_401(client):
    resp = client.get(
        "/api/v1/auth/me",
        headers={"Authorization": "Bearer not-a-jwt"},
    )
    assert resp.status_code == 401


def test_logout_returns_success(client, as_user, admin_user):
    as_user(admin_user)
    resp = client.post("/api/v1/auth/logout")
    assert resp.status_code == 200
    assert resp.json()["message"] == "logged out"


def test_logout_token_revokes_jti(client, admin_user):
    # NOTE: do NOT use ``as_user`` here; we need the real JWT verification
    # path (verify_access -> _ensure_not_revoked) to be exercised.
    access = create_access_token(str(admin_user.id), admin_user.role)
    # /me with a valid access token must succeed.
    ok = client.get("/api/v1/auth/me", headers={"Authorization": f"Bearer {access}"})
    assert ok.status_code == 200, ok.text

    # Revoke it.
    resp = client.post(
        "/api/v1/auth/logout-token",
        json={"token": access},
        headers={"Authorization": f"Bearer {access}"},
    )
    assert resp.status_code == 200, resp.text

    # The same token should now be rejected.
    revoked_resp = client.get("/api/v1/auth/me", headers={"Authorization": f"Bearer {access}"})
    assert revoked_resp.status_code == 401
    assert revoked_resp.json()["code"] == 10003  # TokenRevoked
    assert resp.json()["message"] == "token revoked"


def test_logout_token_missing_field_returns_422(client, as_user, admin_user):
    as_user(admin_user)
    resp = client.post("/api/v1/auth/logout-token", json={})
    assert resp.status_code == 422


def test_change_password_with_correct_old(client, as_user, admin_user, db):
    as_user(admin_user)
    resp = client.put(
        "/api/v1/auth/password",
        json={"old_password": "AdminPass1234", "new_password": "NewAdminPass123"},
    )
    assert resp.status_code == 200, resp.text
    db.refresh(admin_user)
    from app.core.security import verify_password

    assert verify_password("NewAdminPass123", admin_user.password_hash)


def test_change_password_with_wrong_old_returns_401(client, as_user, admin_user):
    as_user(admin_user)
    resp = client.put(
        "/api/v1/auth/password",
        json={"old_password": "not-the-old", "new_password": "Whatever12345"},
    )
    assert resp.status_code == 401
    assert resp.json()["code"] == 10001


def test_change_password_validation(client, as_user, admin_user):
    as_user(admin_user)
    resp = client.put(
        "/api/v1/auth/password",
        json={"old_password": "x", "new_password": "y"},
    )
    assert resp.status_code == 422
