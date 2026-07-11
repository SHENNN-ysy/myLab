"""Tests for the /files endpoints."""
from __future__ import annotations

import io
import uuid

import pytest


# ----------------------------- list -----------------------------------------


def test_list_files_requires_admin(client):
    resp = client.get("/api/v1/files")
    assert resp.status_code in (401, 403)


def test_list_files_empty(client, as_user, admin_user):
    as_user(admin_user)
    resp = client.get("/api/v1/files")
    assert resp.status_code == 200, resp.text
    body = resp.json()
    assert body["code"] == 0
    assert body["data"]["items"] == []


# ----------------------------- upload ---------------------------------------


def test_upload_requires_admin(client):
    files = {"file": ("hello.txt", io.BytesIO(b"hi"), "text/plain")}
    resp = client.post("/api/v1/files/upload", files=files)
    assert resp.status_code in (401, 403)


def test_upload_rejects_disallowed_type(client, as_user, admin_user):
    as_user(admin_user)
    files = {"file": ("script.exe", io.BytesIO(b"MZ"), "application/octet-stream")}
    resp = client.post("/api/v1/files/upload", files=files)
    assert resp.status_code == 422, resp.text
    assert resp.json()["code"] == 10007


def test_upload_png_success(client, as_user, admin_user, db):
    as_user(admin_user)
    # 1x1 transparent PNG
    png_bytes = (
        b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR"
        b"\x00\x00\x00\x01\x00\x00\x00\x01\x08\x06\x00\x00\x00\x1f\x15\xc4\x89"
        b"\x00\x00\x00\rIDATx\x9cc\xf8\xff\xff?\x00\x05\xfe\x02\xfe\xa3W\xbd\xf9"
        b"\x00\x00\x00\x00IEND\xaeB`\x82"
    )
    files = {"file": ("pixel.png", io.BytesIO(png_bytes), "image/png")}
    resp = client.post("/api/v1/files/upload", files=files)
    assert resp.status_code == 200, resp.text
    data = resp.json()["data"]
    assert data["original_name"] == "pixel.png"
    assert data["mime_type"] == "image/png"
    assert data["size"] == len(png_bytes)
    assert "url" in data
    assert "id" in data

    from app.models.file_record import FileRecord

    rec = db.get(FileRecord, uuid.UUID(data["id"]))
    assert rec is not None
    assert rec.is_deleted is False


def test_upload_pdf_success(client, as_user, admin_user):
    as_user(admin_user)
    pdf_bytes = b"%PDF-1.4\n%fake\n%%EOF"
    files = {"file": ("doc.pdf", io.BytesIO(pdf_bytes), "application/pdf")}
    resp = client.post("/api/v1/files/upload", files=files)
    assert resp.status_code == 200, resp.text
    assert resp.json()["data"]["mime_type"] == "application/pdf"


def test_upload_oversize_rejected(client, as_user, admin_user):
    as_user(admin_user)
    # MAX_BYTES is computed at class-import time. Build a payload that is
    # larger than the configured cap (10 MB by default) to make sure the
    # guard fires.
    big_payload = b"\x89PNG" + b"x" * (10 * 1024 * 1024 + 1)
    files = {"file": ("big.png", io.BytesIO(big_payload), "image/png")}
    resp = client.post("/api/v1/files/upload", files=files)
    assert resp.status_code == 422, resp.text
    assert "size" in resp.text.lower()


# ----------------------------- presigned ------------------------------------


def test_presign_returns_url(client, as_user, admin_user):
    as_user(admin_user)
    # upload first
    png_bytes = b"\x89PNG\r\n\x1a\nfake"
    files = {"file": ("a.png", io.BytesIO(png_bytes), "image/png")}
    upload_resp = client.post("/api/v1/files/upload", files=files)
    file_id = upload_resp.json()["data"]["id"]

    resp = client.get(f"/api/v1/files/presigned/{file_id}")
    assert resp.status_code == 200, resp.text
    assert resp.json()["data"]["url"].startswith("http://stub")


def test_presign_unknown_returns_404(client, as_user, admin_user):
    as_user(admin_user)
    resp = client.get(f"/api/v1/files/presigned/{uuid.uuid4()}")
    assert resp.status_code == 404


# ----------------------------- delete ---------------------------------------


def test_delete_file_soft_deletes_and_publishes(client, as_user, admin_user, db, _stub_rabbit):
    as_user(admin_user)
    # upload
    files = {"file": ("del.png", io.BytesIO(b"\x89PNGxxx"), "image/png")}
    upload_resp = client.post("/api/v1/files/upload", files=files)
    file_id = upload_resp.json()["data"]["id"]

    # delete
    resp = client.delete(f"/api/v1/files/{file_id}")
    assert resp.status_code == 200, resp.text
    assert "queued" in resp.json()["message"]

    # DB row is soft-deleted
    from app.models.file_record import FileRecord
    rec = db.get(FileRecord, uuid.UUID(file_id))
    assert rec.is_deleted is True

    # file_cleanup message was published
    routes = [k for k, _ in _stub_rabbit]
    assert "file.cleanup" in routes


def test_delete_unknown_file_returns_404(client, as_user, admin_user):
    as_user(admin_user)
    resp = client.delete(f"/api/v1/files/{uuid.uuid4()}")
    assert resp.status_code == 404
