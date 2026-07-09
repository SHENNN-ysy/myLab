"""High-level cache helpers backed by Redis."""
from __future__ import annotations

import json
from typing import Any, Callable, Optional

from app.core.redis_client import get_redis


def cache_get(key: str) -> Optional[Any]:
    raw = get_redis().get(key)
    if raw is None:
        return None
    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        return None


def cache_set(key: str, value: Any, ttl: int = 300) -> None:
    get_redis().setex(key, ttl, json.dumps(value, default=str))


def cache_delete(*keys: str) -> None:
    if not keys:
        return
    get_redis().delete(*keys)


def cache_get_or_set(key: str, ttl: int, producer: Callable[[], Any]) -> Any:
    cached = cache_get(key)
    if cached is not None:
        return cached
    value = producer()
    cache_set(key, value, ttl=ttl)
    return value


def cache_clear_prefix(prefix: str) -> int:
    redis = get_redis()
    cursor = 0
    deleted = 0
    while True:
        cursor, keys = redis.scan(cursor=cursor, match=f"{prefix}*", count=200)
        if keys:
            deleted += redis.delete(*keys)
        if cursor == 0:
            break
    return deleted