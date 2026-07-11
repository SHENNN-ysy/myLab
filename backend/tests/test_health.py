"""Smoke tests for the app without touching external services."""
from __future__ import annotations

import pytest
from fastapi.testclient import TestClient

from app.main import app


@pytest.fixture()
def standalone_client() -> TestClient:
    """A TestClient that does NOT use the conftest dependency_overrides.

    Used to exercise the "raw" app wiring (auth, exception handler, ...).
    The shared conftest fixtures (``client``, ``db``) override more of the
    wiring for the more specific endpoint tests.
    """
    return TestClient(app)


def test_app_has_routes() -> None:
    paths = {route.path for route in app.routes if hasattr(route, "path")}
    expected = {
        "/api/v1/health",
        "/api/v1/auth/login",
        "/api/v1/auth/me",
        "/api/v1/skills",
        "/api/v1/projects",
        "/api/v1/footprints",
        "/api/v1/about-bubbles",
        "/api/v1/files/upload",
        "/api/v1/users",
        "/api/v1/visits/stats",
    }
    assert expected.issubset(paths), f"missing routes: {expected - paths}"


def test_unauthorized_write_returns_4xx(standalone_client: TestClient) -> None:
    # No Authorization header means the dependency raises 401/403/422 before
    # any DB call could be made (validation may fire first).
    resp = standalone_client.post("/api/v1/skills", json={"name": "Python", "percentage": 80})
    assert resp.status_code in (401, 403, 422)


def test_validation_rejects_bad_payload(standalone_client: TestClient) -> None:
    resp = standalone_client.post(
        "/api/v1/auth/login",
        json={"username": "ab", "password": "short"},
    )
    assert resp.status_code == 422
    body = resp.json()
    assert body["code"] == 10007  # ValidationFailed


# ---------------------------------------------------------------------------
# Additional health-endpoint coverage using the shared test client (which
# stubs Redis/MinIO and uses the in-memory SQLite).
# ---------------------------------------------------------------------------


def test_health_all_components_up(client) -> None:
    resp = client.get("/api/v1/health")
    assert resp.status_code == 200, resp.text
    body = resp.json()
    assert body["code"] == 0
    data = body["data"]
    assert data["status"] == "healthy"
    for component in ("database", "redis", "minio"):
        assert data["components"][component] == "up", data


def test_health_degraded_when_redis_down(client, monkeypatch) -> None:
    """If Redis ping raises, the health endpoint should report degraded."""
    from app.core import redis_client as rc

    real_redis = rc.get_redis()

    def _broken_ping(*a, **kw):
        raise RuntimeError("redis is down")

    monkeypatch.setattr(real_redis, "ping", _broken_ping)

    resp = client.get("/api/v1/health")
    assert resp.status_code == 200
    data = resp.json()["data"]
    assert data["status"] == "degraded"
    assert data["components"]["redis"] == "down"
    assert data["components"]["database"] == "up"
    assert data["components"]["minio"] == "up"


def test_health_degraded_when_minio_down(client, monkeypatch) -> None:
    from app.core import minio_client as mc

    stub = mc.get_minio()
    stub.force_fail("list_buckets")

    try:
        resp = client.get("/api/v1/health")
        data = resp.json()["data"]
        assert data["status"] == "degraded"
        assert data["components"]["minio"] == "down"
    finally:
        stub._fail_methods.clear()
