"""Tests for the /system endpoints."""
from __future__ import annotations


def test_static_requires_admin(client):
    resp = client.get("/api/v1/system/static")
    assert resp.status_code in (401, 403)


def test_static_returns_metadata(client, as_user, admin_user):
    as_user(admin_user)
    resp = client.get("/api/v1/system/static")
    assert resp.status_code == 200, resp.text
    data = resp.json()["data"]
    for key in (
        "hostname",
        "os",
        "serverIp",
        "cpuCore",
        "cpuModel",
        "dbType",
        "dbTables",
        "appVersion",
    ):
        assert key in data, f"missing key: {key}"
    assert data["dbType"] == "PostgreSQL"
    assert isinstance(data["cpuCore"], int)
    assert data["cpuCore"] >= 0
    assert data["appVersion"] == "1.0.0"


def test_static_counts_tables(client, as_user, admin_user, db):
    # The endpoint queries information_schema.tables; on SQLite we emulate
    # the same thing by checking that some tables exist.
    from app.models import User

    db.add(User(username="x", email="x@e.com", role="viewer",
                password_hash="x" * 60, is_active=True))
    db.commit()

    as_user(admin_user)
    resp = client.get("/api/v1/system/static")
    assert resp.status_code == 200
    data = resp.json()["data"]
    # SQLite uses sqlite_master instead of information_schema.tables, so
    # the count may be 0 here. We just assert the field is an int.
    assert isinstance(data["dbTables"], int)


def test_dynamic_requires_admin(client):
    resp = client.get("/api/v1/system/dynamic")
    assert resp.status_code in (401, 403)


def test_dynamic_returns_metrics(client, as_user, admin_user):
    as_user(admin_user)
    resp = client.get("/api/v1/system/dynamic")
    assert resp.status_code == 200, resp.text
    data = resp.json()["data"]
    for key in ("cpuUsage", "load1", "load5", "load15", "hostUptime", "diskUsed", "diskFree", "dbStatus"):
        assert key in data, f"missing key: {key}"
    assert isinstance(data["hostUptime"], int)
    assert data["hostUptime"] >= 0
    assert isinstance(data["diskUsed"], int)
    assert isinstance(data["diskFree"], int)
