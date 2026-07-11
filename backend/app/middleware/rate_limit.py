"""Simple Redis-backed rate limiting middleware."""
from __future__ import annotations

from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import JSONResponse, Response

from app.common.exceptions import RateLimited
from app.core.config import settings
from app.core.redis_client import get_redis


class RateLimitMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next) -> Response:
        client = request.client
        ip = client.host if client else "unknown"
        path = request.url.path
        limit, window, key_prefix = self._resolve_rule(path)
        bucket = int(request.headers.get("x-test-now", "0") or 0)
        redis = get_redis()
        counter_key = f"rate:{key_prefix}:{ip}"
        try:
            count = redis.incr(counter_key)
            if count == 1:
                redis.expire(counter_key, window)
        except Exception:  # noqa: BLE001 - if Redis is down, fail-open
            return await call_next(request)
        if count > limit:
            raise RateLimited("Too many requests")
        response = await call_next(request)
        response.headers["X-RateLimit-Limit"] = str(limit)
        response.headers["X-RateLimit-Remaining"] = str(max(0, limit - count))
        return response

    @staticmethod
    def _resolve_rule(path: str) -> tuple[int, int, str]:
        if path.endswith("/auth/login"):
            return settings.LOGIN_RATE_LIMIT_PER_MINUTE, 60, "login"
        if path.endswith("/files/upload"):
            return 30, 3600, "upload"
        return settings.RATE_LIMIT_PER_MINUTE, 60, "global"