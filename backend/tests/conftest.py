"""Shared pytest fixtures.

Strategy
--------
- Replace Postgres with a single shared SQLite in-memory database.
- Replace Redis with fakeredis (no real network).
- Stub RabbitMQ producers (no broker connection).
- Stub MinIO client (no real object storage).
- Override FastAPI dependencies for ``get_db`` and the auth guards.
- Build tables once per session on the models' ``Base`` (the app's
  ``app.core.database.Base`` is *not* the one models register on; see
  ``app.models.base.Base``).
- The JSONB Postgres type is compiled to ``TEXT`` for SQLite via a
  type-compiler hook so we can keep the production models untouched.
"""
from __future__ import annotations

import os

# ---------------------------------------------------------------------------
# Environment must be configured BEFORE the app modules are imported.
# ---------------------------------------------------------------------------
os.environ.setdefault("APP_ENV", "test")
os.environ.setdefault("APP_DEBUG", "false")
os.environ.setdefault("DB_HOST", "127.0.0.1")
os.environ.setdefault("REDIS_HOST", "127.0.0.1")
os.environ.setdefault("RABBITMQ_HOST", "127.0.0.1")
os.environ.setdefault("MINIO_ENDPOINT", "127.0.0.1:9000")
os.environ.setdefault("JWT_SECRET", "test-secret-for-pytest-only-do-not-use-in-prod")
os.environ.setdefault("CORS_ORIGINS", "http://localhost:5173")

import fakeredis
import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, event
from sqlalchemy.dialects import sqlite as sqlite_dialect
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

# ---------------------------------------------------------------------------
# Compile JSONB as TEXT for SQLite so production models work without edits.
# ---------------------------------------------------------------------------
from sqlalchemy.ext.compiler import compiles  # noqa: E402


@compiles(JSONB, "sqlite")
def _compile_jsonb_sqlite(_type, compiler, **_kw):
    return "TEXT"


# ---------------------------------------------------------------------------
# Make the model-side Base the one used by tests, so the tables it declares
# are exactly the ones the engine will create.
# ---------------------------------------------------------------------------
from app.core import config as app_config  # noqa: E402
from app.core import database as app_database  # noqa: E402
from app.core import redis_client as app_redis_client  # noqa: E402
from app.core.security import create_access_token  # noqa: E402
from app.models import base as models_base  # noqa: E402,F401  (imports register models)
from app.models.user import User  # noqa: E402
from app.repositories.user import UserRepository  # noqa: E402

# Force a single in-memory SQLite DB shared across connections.
# StaticPool + check_same_thread=False is required so multiple FastAPI
# dependency-injected sessions see the same schema/data.
TEST_ENGINE = create_engine(
    "sqlite://",
    connect_args={"check_same_thread": False},
    poolclass=StaticPool,
    future=True,
)


@event.listens_for(TEST_ENGINE, "connect")
def _enable_sqlite_fk(dbapi_conn, _):
    cur = dbapi_conn.cursor()
    cur.execute("PRAGMA foreign_keys=ON")
    cur.close()


# Build the schema once.
models_base.Base.metadata.create_all(bind=TEST_ENGINE)

# Re-point the application's SessionLocal to the test engine so any code
# path that uses sessionmaker() (rare) still works. The endpoint code uses
# ``get_db`` which is overridden below.
TestSessionLocal = sessionmaker(bind=TEST_ENGINE, autoflush=False, autocommit=False, expire_on_commit=False)


# ---------------------------------------------------------------------------
# Stub MinIO: replace the four functions used by file_service.
# ---------------------------------------------------------------------------
from app.core import minio_client as app_minio_client  # noqa: E402


class _StubMinio:
    def __init__(self):
        self._fail_methods: set[str] = set()

    def presigned_get_object(self, bucket, key, expires):
        return f"http://stub-minio.local/{bucket}/{key}?expires={expires}"

    def list_buckets(self):
        if "list_buckets" in self._fail_methods:
            raise RuntimeError("minio unreachable")
        return []

    def bucket_exists(self, bucket):
        return True

    def make_bucket(self, bucket):
        return None

    def put_object(self, bucket, key, data, length, content_type=None):
        return None

    def remove_object(self, bucket, key):
        return None

    def force_fail(self, *names: str) -> None:
        self._fail_methods.update(names)


# Single shared instance so per-test monkeypatching of its methods persists
# for the lifetime of one request.
_shared_stub_minio = _StubMinio()


@pytest.fixture(autouse=True)
def _stub_minio(monkeypatch):
    """Replace every MinIO entry-point used by the application.

    Some endpoint modules import ``get_minio`` and friends by name and end
    up with their own module-level reference. ``monkeypatch.setattr`` on the
    source module does NOT update those references, so we have to walk
    every loaded module and patch the local names too.
    """
    import sys

    targets = ("get_minio", "ensure_bucket", "upload_file", "delete_file", "presigned_url")

    def _make_stub(name):
        if name == "get_minio":
            return lambda: _shared_stub_minio
        if name == "ensure_bucket":
            return lambda: None
        if name == "upload_file":
            return lambda *a, **kw: None
        if name == "delete_file":
            return lambda *a, **kw: None
        if name == "presigned_url":
            return lambda key, bucket=None: f"http://stub/{key}"
        return lambda *a, **kw: None

    # Patch the source module.
    for name in targets:
        monkeypatch.setattr(app_minio_client, name, _make_stub(name))

    # Patch every already-imported module that carries one of those names.
    # ``file_service`` historically captured these by ``import as``; we have
    # to handle the legacy aliases too in case any module still does.
    for name in ("minio_delete", "minio_upload", "presigned_url"):
        try:
            mod = sys.modules.get("app.services.file_service")
            if mod is not None and hasattr(mod, name):
                setattr(mod, name, _make_stub(
                    name.removeprefix("minio_") if name != "presigned_url" else "presigned_url"
                ))
        except Exception:
            pass

    for mod_name, mod in list(sys.modules.items()):
        if mod is None or not (mod_name or "").startswith("app."):
            continue
        for name in targets:
            if hasattr(mod, name) and getattr(mod, name).__module__ == app_minio_client.__name__:
                monkeypatch.setattr(mod, name, _make_stub(name))
    yield


# ---------------------------------------------------------------------------
# Stub RabbitMQ producers: every publish becomes a no-op that records the
# call so tests can assert side effects.
# ---------------------------------------------------------------------------
from app.messaging import producers as app_producers  # noqa: E402

# Module-level list shared across all tests. Each test starts with a fresh
# empty list (cleared in the fixture's teardown).
_PUBLISHED: list = []


@pytest.fixture(autouse=True)
def _stub_rabbit(monkeypatch):
    """Replace every async publish_xxx with a no-op coroutine that records
    the call in a module-level list."""
    _PUBLISHED.clear()

    async def _record(routing_key, payload):
        try:
            import json

            body = json.loads(payload.decode("utf-8")) if isinstance(payload, (bytes, bytearray)) else payload
        except Exception:
            body = {"_raw": repr(payload)}
        _PUBLISHED.append((routing_key, body))

    from app.messaging import rabbitmq as app_rabbitmq
    from app.messaging.topology import RoutingKey

    async def _publish(routing_key, body):
        await _record(routing_key, body)

    # The async producers in app.messaging.producers take a single Pydantic
    # payload and forward to publish() under the hood. We override them with
    # a 1-arg signature that simply records what was published.
    async def _pub_visit(payload):
        await _record(RoutingKey.VISIT_RECORD.value, payload)

    async def _pub_email(payload):
        await _record(RoutingKey.EMAIL_NOTIFY.value, payload)

    async def _pub_cleanup(payload):
        await _record(RoutingKey.FILE_CLEANUP.value, payload)

    async def _pub_audit(payload):
        await _record(RoutingKey.AUDIT_LOG.value, payload)

    monkeypatch.setattr(app_producers, "publish_visit_record", _pub_visit)
    monkeypatch.setattr(app_producers, "publish_email_notify", _pub_email)
    monkeypatch.setattr(app_producers, "publish_file_cleanup", _pub_cleanup)
    monkeypatch.setattr(app_producers, "publish_audit_log", _pub_audit)
    # The producers call publish() under the hood; stub that too so any
    # direct caller (e.g. tests, future code) is covered.
    monkeypatch.setattr(app_rabbitmq, "publish", _publish)

    # Also stub the audit/notify wrappers that call them internally.
    from app.messaging import audit as app_audit
    from app.messaging import notify as app_notify

    async def _no_audit(*a, **kw):
        return None

    async def _no_notify(*a, **kw):
        return None

    monkeypatch.setattr(app_audit, "emit_audit", _no_audit)
    monkeypatch.setattr(app_notify, "notify_security_event", _no_notify)
    monkeypatch.setattr(app_notify, "notify_user", _no_notify)

    return _PUBLISHED


# ---------------------------------------------------------------------------
# Replace the real Redis client with fakeredis.
# ---------------------------------------------------------------------------
_fake_redis = fakeredis.FakeRedis(decode_responses=True)


@pytest.fixture(autouse=True)
def _stub_redis(monkeypatch):
    """Replace the real Redis client with fakeredis.

    As with the MinIO stub, endpoint modules may have already imported
    ``get_redis`` by name. We patch the source module and every other
    ``app.*`` module that captured a reference.
    """
    import sys

    monkeypatch.setattr(app_redis_client, "get_redis", lambda: _fake_redis)
    for mod_name, mod in list(sys.modules.items()):
        if mod is None or not (mod_name or "").startswith("app."):
            continue
        if hasattr(mod, "get_redis") and getattr(mod, "get_redis").__module__ == app_redis_client.__name__:
            monkeypatch.setattr(mod, "get_redis", lambda: _fake_redis)
    yield
    _fake_redis.flushall()


# ---------------------------------------------------------------------------
# FastAPI: per-function SQLite session + DB schema reset, dependency
# overrides for ``get_db`` and the auth guards.
# ---------------------------------------------------------------------------
@pytest.fixture()
def db():
    # Truncate all tables before each test for isolation. The SQLite engine
    # itself is shared; we wipe rows but keep the schema.
    from app.models import (
        AboutBubble,
        FileRecord,
        Footprint,
        Project,
        Skill,
        User,
        VisitLog,
    )

    session = TestSessionLocal()
    try:
        for model in (VisitLog, FileRecord, AboutBubble, Footprint, Project, Skill, User):
            session.query(model).delete()
        session.commit()
        yield session
    finally:
        session.close()


@pytest.fixture()
def client(db):
    # Override get_db to use the same session for the whole request.
    def _get_db_override():
        try:
            yield db
        finally:
            # We deliberately don't close db here; the fixture owns its lifecycle.
            pass

    # Import the FastAPI app only after our monkey-patches are in place so
    # the lifespan handler sees a test environment.
    from app.api import deps as app_deps
    from app.main import app

    app.dependency_overrides[app_deps.get_db] = _get_db_override

    # Default auth override: require_admin / require_superadmin both return
    # the "current user" supplied via the per-test ``current_user`` fixture.
    # Tests can override these on a per-test basis by patching the override.
    test_app = TestClient(app)
    try:
        with test_app:
            yield test_app
    finally:
        app.dependency_overrides.clear()


# ---------------------------------------------------------------------------
# Pre-seeded users.
# ---------------------------------------------------------------------------
def _make_user(db, *, username: str, email: str, password: str, role: str) -> User:
    from app.core.security import hash_password

    user = User(
        username=username,
        email=email,
        nickname=username.title(),
        role=role,
        password_hash=hash_password(password),
        is_active=True,
    )
    db.add(user)
    db.flush()
    return user


@pytest.fixture()
def superadmin_user(db) -> User:
    return _make_user(
        db,
        username="root",
        email="root@example.com",
        password="RootPass1234",
        role="superadmin",
    )


@pytest.fixture()
def admin_user(db) -> User:
    return _make_user(
        db,
        username="admin",
        email="admin@example.com",
        password="AdminPass1234",
        role="admin",
    )


@pytest.fixture()
def editor_user(db) -> User:
    return _make_user(
        db,
        username="editor",
        email="editor@example.com",
        password="EditorPass1234",
        role="editor",
    )


@pytest.fixture()
def viewer_user(db) -> User:
    return _make_user(
        db,
        username="viewer",
        email="viewer@example.com",
        password="ViewerPass1234",
        role="viewer",
    )


def _auth_for(user: User) -> dict[str, str]:
    token = create_access_token(str(user.id), user.role)
    return {"Authorization": f"Bearer {token}"}


@pytest.fixture()
def superadmin_headers(superadmin_user) -> dict[str, str]:
    return _auth_for(superadmin_user)


@pytest.fixture()
def admin_headers(admin_user) -> dict[str, str]:
    return _auth_for(admin_user)


@pytest.fixture()
def editor_headers(editor_user) -> dict[str, str]:
    return _auth_for(editor_user)


@pytest.fixture()
def viewer_headers(viewer_user) -> dict[str, str]:
    return _auth_for(viewer_user)


# ---------------------------------------------------------------------------
# Convenience: an ``as_user`` fixture that overrides get_current_active_user
# so tests can choose which user the request should look like.
# ---------------------------------------------------------------------------
@pytest.fixture()
def as_user(client):
    """Return a callable that sets the impersonated current user.

    Usage::

        def test_x(as_user, admin_user):
            as_user(admin_user)
            r = client.get("/api/v1/users")
    """
    from app.api import deps as app_deps
    from app.main import app

    def _set(user: User) -> None:
        def _override():
            return user

        app.dependency_overrides[app_deps.get_current_active_user] = _override
        app.dependency_overrides[app_deps.get_current_user] = _override

    return _set


# ---------------------------------------------------------------------------
# Make sure the lifespan handler does not try to call init_app() and our
# test settings are honoured even after the app module is loaded.
# ---------------------------------------------------------------------------
@pytest.fixture(autouse=True)
def _lock_test_env(monkeypatch):
    monkeypatch.setattr(app_config.settings, "APP_ENV", "test", raising=False)
    yield
