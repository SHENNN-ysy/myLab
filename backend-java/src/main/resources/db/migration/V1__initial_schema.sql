CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS users (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username        VARCHAR(64)  NOT NULL UNIQUE,
    email           VARCHAR(128) NOT NULL UNIQUE,
    nickname        VARCHAR(128),
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(32)  NOT NULL DEFAULT 'viewer',
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at   TIMESTAMPTZ,
    avatar_url      VARCHAR(512),
    website         VARCHAR(255),
    bio             TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS files (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    object_key     VARCHAR(512) NOT NULL,
    bucket         VARCHAR(128) NOT NULL,
    original_name  VARCHAR(255),
    mime_type      VARCHAR(128),
    size           BIGINT,
    uploaded_by    UUID,
    is_deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS visit_logs (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ip           VARCHAR(64),
    user_agent   VARCHAR(512),
    path         VARCHAR(512),
    referer      VARCHAR(512),
    user_id      UUID,
    visited_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_visit_logs_visited_at ON visit_logs(visited_at DESC);

CREATE TABLE IF NOT EXISTS content_modules (
    module_key          VARCHAR(32) PRIMARY KEY,
    draft_data          JSONB       NOT NULL DEFAULT '{}'::jsonb,
    published_data      JSONB,
    draft_version       INTEGER     NOT NULL DEFAULT 1,
    published_version   INTEGER     NOT NULL DEFAULT 0,
    status              VARCHAR(16) NOT NULL DEFAULT 'draft',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at        TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS content_publications (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    module_key      VARCHAR(32) NOT NULL REFERENCES content_modules(module_key) ON DELETE CASCADE,
    version         INTEGER     NOT NULL,
    data            JSONB       NOT NULL,
    published_by    UUID,
    published_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (module_key, version)
);

CREATE INDEX IF NOT EXISTS idx_content_publications_module_version
    ON content_publications(module_key, version DESC);
