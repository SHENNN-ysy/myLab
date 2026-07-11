"""Custom exception hierarchy mapped to unified responses."""
from __future__ import annotations

from typing import Any, Optional


class BizException(Exception):
    """Base business exception."""

    http_status: int = 400
    code: int = 10000

    def __init__(
        self,
        message: str = "Business error",
        *,
        error: Optional[str] = None,
        extra: Optional[dict] = None,
    ) -> None:
        super().__init__(message)
        self.message = message
        self.error = error
        self.extra = extra or {}


class AuthFailed(BizException):
    http_status = 401
    code = 10001

    def __init__(self, message: str = "Authentication failed", **kwargs: Any) -> None:
        super().__init__(message, **kwargs)


class TokenExpired(BizException):
    http_status = 401
    code = 10002

    def __init__(self, message: str = "Token expired", **kwargs: Any) -> None:
        super().__init__(message, **kwargs)


class TokenRevoked(BizException):
    http_status = 401
    code = 10003

    def __init__(self, message: str = "Token revoked", **kwargs: Any) -> None:
        super().__init__(message, **kwargs)


class PermissionDenied(BizException):
    http_status = 403
    code = 10004

    def __init__(self, message: str = "Permission denied", **kwargs: Any) -> None:
        super().__init__(message, **kwargs)


class NotFound(BizException):
    http_status = 404
    code = 10005

    def __init__(self, message: str = "Resource not found", **kwargs: Any) -> None:
        super().__init__(message, **kwargs)


class Conflict(BizException):
    http_status = 409
    code = 10006

    def __init__(self, message: str = "Resource conflict", **kwargs: Any) -> None:
        super().__init__(message, **kwargs)


class ValidationFailed(BizException):
    http_status = 422
    code = 10007

    def __init__(self, message: str = "Validation failed", **kwargs: Any) -> None:
        super().__init__(message, **kwargs)


class RateLimited(BizException):
    http_status = 429
    code = 10008

    def __init__(self, message: str = "Too many requests", **kwargs: Any) -> None:
        super().__init__(message, **kwargs)


class ServerError(BizException):
    http_status = 500
    code = 20001

    def __init__(self, message: str = "Internal server error", **kwargs: Any) -> None:
        super().__init__(message, **kwargs)