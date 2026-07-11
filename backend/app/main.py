"""FastAPI application entry point."""
from __future__ import annotations

import asyncio
import logging
import time
import uuid
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

from app.api.v1.router import api_router
from app.common.exceptions import BizException
from app.common.response import failure
from app.core.config import settings
from app.init_db import run as init_app
from app.messaging.rabbitmq import close_connection
from app.middleware.rate_limit import RateLimitMiddleware
from app.middleware.security_headers import SecurityHeadersMiddleware
from app.tasks.consumer import consume_queues

logging.basicConfig(level=settings.LOG_LEVEL)
logger = logging.getLogger("myblog")


@asynccontextmanager
async def lifespan(app: FastAPI):
    if settings.APP_ENV != "test":
        try:
            init_app()
        except Exception:  # noqa: BLE001 - never block startup
            logger.exception("init_app failed; continuing without seeding")

    # Run RabbitMQ consumers in the SAME process as the API (B-form
    # deployment: single API container, no separate worker). This keeps
    # the message-queue topology while avoiding a second API container.
    consumer_task: asyncio.Task | None = None
    if settings.MQ_INPROCESS_CONSUMER:
        consumer_task = asyncio.create_task(
            consume_queues(), name="myblog.consumer"
        )
        logger.info("in-process RabbitMQ consumer started")

    try:
        yield
    finally:
        if consumer_task is not None:
            consumer_task.cancel()
            try:
                await consumer_task
            except asyncio.CancelledError:
                pass
            except Exception:  # noqa: BLE001
                logger.exception("consumer task exited with error")
        try:
            await close_connection()
        except Exception:  # noqa: BLE001
            logger.exception("failed to close RabbitMQ connection")


app = FastAPI(
    title=settings.APP_NAME,
    version="1.0.0",
    docs_url="/api/docs" if settings.APP_DEBUG else None,
    redoc_url="/api/redoc" if settings.APP_DEBUG else None,
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type"],
    max_age=3600,
)
app.add_middleware(SecurityHeadersMiddleware)
app.add_middleware(RateLimitMiddleware)


@app.middleware("http")
async def attach_request_id(request: Request, call_next):
    rid = request.headers.get("x-request-id") or uuid.uuid4().hex
    request.state.request_id = rid
    start = time.perf_counter()
    response = await call_next(request)
    response.headers["X-Request-ID"] = rid
    response.headers["X-Response-Time-ms"] = str(int((time.perf_counter() - start) * 1000))
    return response


@app.exception_handler(BizException)
async def biz_exception_handler(request: Request, exc: BizException):
    logger.warning("biz error: %s code=%s", exc.message, exc.code)
    return JSONResponse(
        status_code=exc.http_status,
        content=failure(
            code=exc.code,
            message=exc.message,
            error=str(exc.error) if exc.error else None,
            request_id=getattr(request.state, "request_id", None),
        ),
    )


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    return JSONResponse(
        status_code=422,
        content=failure(
            code=10007,
            message="Validation failed",
            error=str(exc.errors()),
            request_id=getattr(request.state, "request_id", None),
        ),
    )


@app.exception_handler(StarletteHTTPException)
async def http_exception_handler(request: Request, exc: StarletteHTTPException):
    return JSONResponse(
        status_code=exc.status_code,
        content=failure(
            code=exc.status_code * 100,
            message=exc.detail if isinstance(exc.detail, str) else "HTTP error",
            request_id=getattr(request.state, "request_id", None),
        ),
    )


@app.exception_handler(Exception)
async def unhandled_exception_handler(request: Request, exc: Exception):
    logger.exception("unhandled error")
    return JSONResponse(
        status_code=500,
        content=failure(
            code=20001,
            message="Internal server error",
            error=str(exc),
            request_id=getattr(request.state, "request_id", None),
        ),
    )


app.include_router(api_router)


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "app.main:app",
        host=settings.APP_HOST,
        port=settings.APP_PORT,
        reload=settings.APP_DEBUG,
    )