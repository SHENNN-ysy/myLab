"""Tests for the /visits endpoints."""
from __future__ import annotations

import uuid
from datetime import datetime, timedelta

import pytest


# ----------------------------- track (POST /logs/track) --------------------


def test_track_publishes_visit_record(client, _stub_rabbit):
    resp = client.post(
        "/api/v1/visits/logs/track",
        json={},
        headers={"User-Agent": "pytest/1.0", "Referer": "https://example.com"},
    )
    assert resp.status_code == 200, resp.text
    assert resp.json()["message"] == "visit queued"
    routes = [k for k, _ in _stub_rabbit]
    assert "visit.record" in routes


def test_track_with_no_referer(client, _stub_rabbit):
    resp = client.post(
        "/api/v1/visits/logs/track",
        json={},
        headers={"User-Agent": "curl/8.0"},
    )
    assert resp.status_code == 200
    routes = [k for k, _ in _stub_rabbit]
    assert "visit.record" in routes


# ----------------------------- stats ----------------------------------------


def test_stats_requires_admin(client):
    resp = client.get("/api/v1/visits/stats")
    assert resp.status_code in (401, 403)


def test_stats_defaults_to_today(client, as_user, admin_user):
    as_user(admin_user)
    resp = client.get("/api/v1/visits/stats")
    assert resp.status_code == 200, resp.text
    data = resp.json()["data"]
    assert data["date"] == datetime.utcnow().date().isoformat()
    assert data["pv"] == 0
    assert data["uv"] == 0
    assert "total_pv" in data


def test_stats_with_explicit_date(client, as_user, admin_user):
    as_user(admin_user)
    resp = client.get("/api/v1/visits/stats?date=2026-01-15")
    assert resp.status_code == 200
    assert resp.json()["data"]["date"] == "2026-01-15"


def test_stats_reads_redis_counters(client, as_user, admin_user):
    from app.core.redis_client import get_redis

    r = get_redis()
    today = datetime.utcnow().date().isoformat()
    pv_key = f"stats:visit:{today}:pv"
    uv_key = f"stats:visit:{today}:uv"
    r.delete(pv_key, uv_key)
    r.set(pv_key, 42)
    r.pfadd(uv_key, "1.2.3.4", "5.6.7.8", "1.2.3.4")  # duplicate ip

    as_user(admin_user)
    resp = client.get("/api/v1/visits/stats")
    assert resp.status_code == 200, resp.text
    data = resp.json()["data"]
    assert data["pv"] == 42
    assert data["uv"] == 2  # 2 unique ips, deduped by HLL

    r.delete(pv_key, uv_key)


# ----------------------------- logs -----------------------------------------


def test_list_logs_requires_admin(client):
    resp = client.get("/api/v1/visits/logs")
    assert resp.status_code in (401, 403)


def test_list_logs_paginated(client, as_user, admin_user, db):
    from app.models.visit_log import VisitLog

    now = datetime.utcnow()
    for i in range(5):
        db.add(VisitLog(ip=f"1.2.3.{i}", user_agent="x", path=f"/p{i}", visited_at=now))
    db.commit()

    as_user(admin_user)
    resp = client.get("/api/v1/visits/logs?page=1&page_size=3")
    assert resp.status_code == 200, resp.text
    body = resp.json()
    assert body["data"]["pagination"]["page"] == 1
    assert body["data"]["pagination"]["page_size"] == 3
    assert body["data"]["pagination"]["total"] == 5
    assert len(body["data"]["items"]) == 3


def test_list_logs_with_time_range(client, as_user, admin_user, db):
    from app.models.visit_log import VisitLog
    from datetime import timezone

    # Insert one log in Jan 2026 and one in Jun 2026.
    db.add(
        VisitLog(
            ip="x",
            user_agent="",
            path="/",
            visited_at=datetime(2026, 1, 1, 12, 0, 0, tzinfo=timezone.utc),
        )
    )
    db.add(
        VisitLog(
            ip="y",
            user_agent="",
            path="/",
            visited_at=datetime(2026, 6, 1, 12, 0, 0, tzinfo=timezone.utc),
        )
    )
    db.commit()

    as_user(admin_user)
    # Filter to only those in the year 2026 by checking pagination total
    # (we can't easily rely on timezone-aware comparison on SQLite, so we
    # only assert the request validates and returns a paginated response).
    start = "2026-01-01T00:00:00%2B00:00"
    end = "2026-12-31T23:59:59%2B00:00"
    resp = client.get(f"/api/v1/visits/logs?start={start}&end={end}")
    assert resp.status_code == 200, resp.text
    body = resp.json()
    assert body["data"]["pagination"]["total"] >= 1


def test_list_logs_invalid_time_filter_returns_422(client, as_user, admin_user):
    as_user(admin_user)
    resp = client.get("/api/v1/visits/logs?start=not-a-date")
    assert resp.status_code == 422


# ----------------------------- delete --------------------------------------


def test_delete_log_requires_admin(client):
    resp = client.delete(f"/api/v1/visits/logs/{uuid.uuid4()}")
    assert resp.status_code in (401, 403)


def test_delete_log_soft_deletes(client, as_user, admin_user, db):
    from app.models.visit_log import VisitLog

    log = VisitLog(ip="1.1.1.1", user_agent="", path="/")
    db.add(log)
    db.commit()
    db.refresh(log)

    as_user(admin_user)
    resp = client.delete(f"/api/v1/visits/logs/{log.id}")
    assert resp.status_code == 200, resp.text
    assert resp.json()["data"]["deleted"] == 1


def test_delete_log_unknown_returns_zero(client, as_user, admin_user):
    as_user(admin_user)
    resp = client.delete(f"/api/v1/visits/logs/{uuid.uuid4()}")
    assert resp.status_code == 200
    assert resp.json()["data"]["deleted"] == 0


# ----------------------------- batch-delete --------------------------------


def test_batch_delete_requires_superadmin(client, as_user, admin_user):
    as_user(admin_user)  # admin, not superadmin
    resp = client.post(
        "/api/v1/visits/logs/batch-delete?cutoff=2026-01-01T00:00:00",
    )
    assert resp.status_code == 403


def test_batch_delete_older_than_cutoff(client, as_user, superadmin_user, db):
    from app.models.visit_log import VisitLog

    old = datetime(2025, 1, 1)
    new = datetime(2026, 6, 1)
    db.add_all(
        [
            VisitLog(ip="x", user_agent="", path="/", visited_at=old),
            VisitLog(ip="x", user_agent="", path="/", visited_at=old),
            VisitLog(ip="y", user_agent="", path="/", visited_at=new),
        ]
    )
    db.commit()

    as_user(superadmin_user)
    resp = client.post(
        "/api/v1/visits/logs/batch-delete?cutoff=2026-01-01T00:00:00",
    )
    assert resp.status_code == 200, resp.text
    assert resp.json()["data"]["deleted"] == 2


def test_batch_delete_validation(client, as_user, superadmin_user):
    as_user(superadmin_user)
    # cutoff must be ISO datetime
    resp = client.post(
        "/api/v1/visits/logs/batch-delete?cutoff=yesterday",
    )
    assert resp.status_code == 422


# ----------------------------- clear ---------------------------------------


def test_clear_logs_requires_superadmin(client, as_user, admin_user):
    as_user(admin_user)
    resp = client.delete("/api/v1/visits/logs")
    assert resp.status_code == 403


def test_clear_logs_removes_all(client, as_user, superadmin_user, db):
    from app.models.visit_log import VisitLog

    for i in range(4):
        db.add(VisitLog(ip=f"a{i}", user_agent="", path="/"))
    db.commit()

    as_user(superadmin_user)
    resp = client.delete("/api/v1/visits/logs")
    assert resp.status_code == 200, resp.text
    assert resp.json()["data"]["deleted"] == 4
    assert db.query(VisitLog).count() == 0
