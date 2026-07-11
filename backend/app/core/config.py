"""Application settings loaded via Pydantic Settings."""
from functools import lru_cache
from typing import List

from pydantic import Field, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    # Application
    APP_NAME: str = "MyBlog"
    APP_ENV: str = "development"
    APP_DEBUG: bool = True
    APP_HOST: str = "0.0.0.0"
    APP_PORT: int = 8000
    LOG_LEVEL: str = "INFO"

    # Database
    DB_HOST: str = "localhost"
    DB_PORT: int = 5432
    DB_USER: str = "myblog"
    DB_PASSWORD: str = "myblog_secret"
    DB_NAME: str = "myblog"
    DB_POOL_SIZE: int = 10
    DB_MAX_OVERFLOW: int = 20

    # Redis
    REDIS_HOST: str = "127.0.0.1"
    REDIS_PORT: int = 6379
    REDIS_DB: int = 0
    REDIS_PASSWORD: str = ""

    # RabbitMQ
    RABBITMQ_HOST: str = "localhost"
    RABBITMQ_PORT: int = 5672
    RABBITMQ_USER: str = "myblog"
    RABBITMQ_PASSWORD: str = "myblog_secret"
    RABBITMQ_VHOST: str = "/"
    RABBITMQ_PREFETCH: int = 10
    # When True, the API process spawns RabbitMQ consumers in its own
    # asyncio loop (B-form: no separate worker container). Set to False
    # to run a dedicated worker process or to disable background tasks.
    MQ_INPROCESS_CONSUMER: bool = True

    # JWT
    JWT_SECRET: str = "change-this-in-production-please"
    JWT_ALGORITHM: str = "HS256"
    JWT_ACCESS_EXPIRE_MINUTES: int = 30
    JWT_REFRESH_EXPIRE_DAYS: int = 14

    # MinIO
    MINIO_ENDPOINT: str = "localhost:9000"
    MINIO_ACCESS_KEY: str = "minioadmin"
    MINIO_SECRET_KEY: str = "minioadmin"
    MINIO_BUCKET: str = "myblog"
    MINIO_SECURE: bool = False
    MINIO_PRESIGNED_EXPIRE: int = 3600
    MINIO_MAX_FILE_SIZE_MB: int = 10

    # CORS
    CORS_ORIGINS: str = "http://localhost:5173,http://localhost:5174"

    # Rate Limit
    RATE_LIMIT_PER_MINUTE: int = 60
    LOGIN_RATE_LIMIT_PER_MINUTE: int = 5

    # Initial Admin
    INIT_ADMIN_USERNAME: str = "admin"
    INIT_ADMIN_PASSWORD: str = "Admin@123456"
    INIT_ADMIN_EMAIL: str = "admin@myblog.local"

    # Email notifications
    # Comma-separated list of recipient addresses that should receive
    # security-event notifications (failed logins, password changes, ...).
    # Leave empty in dev to disable security emails entirely.
    ADMIN_NOTIFY_EMAILS: str = ""

    @field_validator("CORS_ORIGINS")
    @classmethod
    def _split_cors(cls, value: str) -> List[str]:
        return [item.strip() for item in value.split(",") if item.strip()]

    @property
    def database_url(self) -> str:
        return (
            f"postgresql+psycopg2://{self.DB_USER}:{self.DB_PASSWORD}"
            f"@{self.DB_HOST}:{self.DB_PORT}/{self.DB_NAME}"
        )

    @property
    def redis_url(self) -> str:
        auth = f":{self.REDIS_PASSWORD}@" if self.REDIS_PASSWORD else ""
        return f"redis://{auth}{self.REDIS_HOST}:{self.REDIS_PORT}/{self.REDIS_DB}"

    @property
    def rabbitmq_url(self) -> str:
        return (
            f"amqp://{self.RABBITMQ_USER}:{self.RABBITMQ_PASSWORD}"
            f"@{self.RABBITMQ_HOST}:{self.RABBITMQ_PORT}{self.RABBITMQ_VHOST}"
        )


@lru_cache
def get_settings() -> Settings:
    return Settings()


settings = get_settings()