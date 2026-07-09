"""Diagnostic script: verify connectivity to Postgres, Redis, RabbitMQ, MinIO.

Run with:
    python scripts/diagnose.py
"""
from __future__ import annotations

import asyncio
import socket
import sys

from app.core.config import settings


def _check_tcp(host: str, port: int, name: str) -> bool:
    try:
        with socket.create_connection((host, port), timeout=3):
            print(f"[OK ] {name:10s} {host}:{port}")
            return True
    except OSError as exc:
        print(f"[FAIL] {name:10s} {host}:{port}  -> {exc}")
        return False


def check_postgres() -> bool:
    return _check_tcp(settings.DB_HOST, settings.DB_PORT, "postgres")


def check_redis() -> bool:
    return _check_tcp(settings.REDIS_HOST, settings.REDIS_PORT, "redis")


def check_rabbitmq() -> bool:
    return _check_tcp(settings.RABBITMQ_HOST, settings.RABBITMQ_PORT, "rabbitmq")


def check_minio() -> bool:
    host, _, port = settings.MINIO_ENDPOINT.partition(":")
    return _check_tcp(host, int(port or 9000), "minio")


async def check_rabbitmq_amqp() -> bool:
    try:
        import aio_pika

        conn = await aio_pika.connect_robust(settings.rabbitmq_url, timeout=5)
        await conn.close()
        print("[OK ] rabbitmq amqp handshake")
        return True
    except Exception as exc:  # noqa: BLE001
        print(f"[FAIL] rabbitmq amqp -> {exc}")
        return False


def main() -> int:
    print(f"MyBlog diagnose @ APP_ENV={settings.APP_ENV}")
    results = [
        check_postgres(),
        check_redis(),
        check_rabbitmq(),
        check_minio(),
    ]
    try:
        results.append(asyncio.run(check_rabbitmq_amqp()))
    except Exception as exc:  # noqa: BLE001
        print(f"[FAIL] rabbitmq amqp -> {exc}")
        results.append(False)
    return 0 if all(results) else 1


if __name__ == "__main__":
    sys.exit(main())