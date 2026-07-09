"""Redis client singleton."""
from typing import Optional

import redis

from app.core.config import settings

_redis: Optional[redis.Redis] = None


def get_redis() -> redis.Redis:
    """Return a lazily-instantiated Redis client."""
    global _redis
    if _redis is None:
        _redis = redis.Redis(
            host=settings.REDIS_HOST,
            port=settings.REDIS_PORT,
            db=settings.REDIS_DB,
            password=settings.REDIS_PASSWORD or None,
            decode_responses=True,
            socket_timeout=5,
            socket_connect_timeout=5,
        )
    return _redis


def close_redis() -> None:
    global _redis
    if _redis is not None:
        _redis.close()
        _redis = None