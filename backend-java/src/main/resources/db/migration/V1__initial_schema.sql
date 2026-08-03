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

CREATE TABLE IF NOT EXISTS skills (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(128) NOT NULL,
    category    VARCHAR(64),
    percentage  INTEGER,
    level       VARCHAR(32),
    icon        VARCHAR(255),
    order_num   INTEGER     DEFAULT 0,
    bar_style   VARCHAR(64),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS projects (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title        VARCHAR(255) NOT NULL,
    slug         VARCHAR(128) NOT NULL UNIQUE,
    description  TEXT,
    content      TEXT,
    tag          VARCHAR(64),
    year         INTEGER,
    image_url    VARCHAR(512),
    project_url  VARCHAR(512),
    repo_url     VARCHAR(512),
    tech         JSONB,
    order_num    INTEGER DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at   TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS footprints (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(128) NOT NULL,
    slug        VARCHAR(128) NOT NULL UNIQUE,
    tag         VARCHAR(64),
    position_x  DOUBLE PRECISION,
    position_y  DOUBLE PRECISION,
    is_self     BOOLEAN DEFAULT FALSE,
    tip_data    JSONB,
    order_num   INTEGER DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS about_bubbles (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    label        VARCHAR(128) NOT NULL,
    bg_color     VARCHAR(32),
    glow_color   VARCHAR(32),
    text_color   VARCHAR(32),
    position_x   DOUBLE PRECISION,
    position_y   DOUBLE PRECISION,
    radius       DOUBLE PRECISION,
    tier         VARCHAR(32),
    order_num    INTEGER DEFAULT 0,
    enabled      BOOLEAN DEFAULT TRUE,
    remark       VARCHAR(255),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at   TIMESTAMPTZ
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
