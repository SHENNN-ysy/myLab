-- 内容互动与全站流量聚合表。
-- PostgreSQL 只保存聚合数，不保存 Cookie、访客哈希、IP、User-Agent 或点赞关系。

CREATE TABLE IF NOT EXISTS mylab_engagement_stats (
    post_key   VARCHAR(96) PRIMARY KEY,
    view_count BIGINT      NOT NULL DEFAULT 0 CHECK (view_count >= 0),
    like_count BIGINT      NOT NULL DEFAULT 0 CHECK (like_count >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS site_traffic_stats (
    id               SMALLINT    PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    visit_count      BIGINT      NOT NULL DEFAULT 0 CHECK (visit_count >= 0),
    total_view_count BIGINT      NOT NULL DEFAULT 0 CHECK (total_view_count >= 0),
    total_like_count BIGINT      NOT NULL DEFAULT 0 CHECK (total_like_count >= 0),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO site_traffic_stats (id) VALUES (1)
ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS site_daily_stats (
    stat_date   DATE        PRIMARY KEY,
    visit_count BIGINT      NOT NULL DEFAULT 0 CHECK (visit_count >= 0),
    view_count  BIGINT      NOT NULL DEFAULT 0 CHECK (view_count >= 0),
    like_count  BIGINT      NOT NULL DEFAULT 0 CHECK (like_count >= 0),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
