"""Asynchronous RabbitMQ connection helper."""
from __future__ import annotations

from contextlib import asynccontextmanager
from typing import AsyncIterator

import aio_pika
from aio_pika.abc import AbstractRobustChannel, AbstractRobustConnection

from app.core.config import settings
from app.messaging.topology import (
    Exchange as ExchangeEnum,
    QUEUE_BINDINGS,
    Queue,
)

_connection: AbstractRobustConnection | None = None


async def get_connection() -> AbstractRobustConnection:
    global _connection
    if _connection is None or _connection.is_closed:
        _connection = await aio_pika.connect_robust(settings.rabbitmq_url)
    return _connection


async def close_connection() -> None:
    global _connection
    if _connection is not None and not _connection.is_closed:
        await _connection.close()
    _connection = None


@asynccontextmanager
async def channel_scope() -> AsyncIterator[AbstractRobustChannel]:
    """Yield a robust channel and declare the standard topology."""
    conn = await get_connection()
    channel = await conn.channel()
    try:
        exchange = await channel.declare_exchange(
            ExchangeEnum.DEFAULT.value,
            aio_pika.ExchangeType.TOPIC,
            durable=True,
        )
        for queue_name, routing_key in QUEUE_BINDINGS.items():
            queue = await channel.declare_queue(queue_name, durable=True)
            await queue.bind(exchange, routing_key=routing_key)
        yield channel
    finally:
        await channel.close()


async def publish(routing_key: str, body: bytes) -> None:
    async with channel_scope() as channel:
        exchange = await channel.get_exchange(ExchangeEnum.DEFAULT.value)
        await exchange.publish(
            aio_pika.Message(
                body=body,
                content_type="application/json",
                delivery_mode=aio_pika.DeliveryMode.PERSISTENT,
            ),
            routing_key=routing_key,
        )