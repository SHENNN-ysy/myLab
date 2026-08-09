CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username      VARCHAR(64)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(32)  NOT NULL DEFAULT 'viewer'
                  CHECK (role IN ('viewer', 'editor', 'admin', 'superadmin')),
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMPTZ
);

CREATE TABLE resources (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    object_key    VARCHAR(512) NOT NULL UNIQUE,
    bucket        VARCHAR(128) NOT NULL,
    original_name VARCHAR(255),
    mime_type     VARCHAR(128) NOT NULL,
    size          BIGINT       NOT NULL CHECK (size >= 0),
    uploaded_by   UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMPTZ
);

CREATE TABLE content_releases (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    module_key        VARCHAR(32) NOT NULL
                      CHECK (module_key IN ('footprints', 'hobbies', 'mylab', 'skills', 'vibe')),
    version_no        INTEGER     NOT NULL CHECK (version_no > 0),
    state             VARCHAR(16) NOT NULL
                      CHECK (state IN ('DRAFT', 'PUBLISHED', 'ARCHIVED', 'OFFLINE')),
    published_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    source_release_id UUID REFERENCES content_releases(id) ON DELETE RESTRICT,
    published_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at        TIMESTAMPTZ,
    CONSTRAINT uq_content_release_version UNIQUE (module_key, version_no),
    CONSTRAINT ck_content_release_publication CHECK (
        state = 'DRAFT'
        OR (published_by IS NOT NULL AND published_at IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uq_content_release_draft
    ON content_releases(module_key)
    WHERE state = 'DRAFT' AND deleted_at IS NULL;
CREATE UNIQUE INDEX uq_content_release_current
    ON content_releases(module_key)
    WHERE state IN ('PUBLISHED', 'OFFLINE') AND deleted_at IS NULL;
CREATE INDEX idx_content_release_history
    ON content_releases(module_key, version_no DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE footprints (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    release_id UUID         NOT NULL REFERENCES content_releases(id) ON DELETE CASCADE,
    city_key   VARCHAR(64)  NOT NULL,
    title      VARCHAR(200),
    summary    TEXT,
    contents   TEXT,
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order INTEGER      NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_footprints_release_key
    ON footprints(release_id, city_key) WHERE deleted_at IS NULL;
CREATE INDEX idx_footprints_release_order
    ON footprints(release_id, sort_order, city_key) WHERE deleted_at IS NULL;

CREATE TABLE hobbies (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    release_id  UUID         NOT NULL REFERENCES content_releases(id) ON DELETE CASCADE,
    hobby_key   VARCHAR(64)  NOT NULL,
    title       VARCHAR(160),
    description TEXT,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order  INTEGER      NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_hobbies_release_key
    ON hobbies(release_id, hobby_key) WHERE deleted_at IS NULL;
CREATE INDEX idx_hobbies_release_order
    ON hobbies(release_id, sort_order, hobby_key) WHERE deleted_at IS NULL;

CREATE TABLE skills (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    release_id  UUID         NOT NULL REFERENCES content_releases(id) ON DELETE CASCADE,
    skill_key   VARCHAR(64)  NOT NULL,
    name        VARCHAR(128),
    percentage  SMALLINT     NOT NULL DEFAULT 0 CHECK (percentage BETWEEN 0 AND 100),
    level_code  VARCHAR(32)  CHECK (level_code IS NULL OR level_code IN ('novice', 'competent', 'proficient')),
    level_text  VARCHAR(32),
    icon        VARCHAR(64),
    bar_style   VARCHAR(32),
    is_new      BOOLEAN      NOT NULL DEFAULT FALSE,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order  INTEGER      NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_skills_release_key
    ON skills(release_id, skill_key) WHERE deleted_at IS NULL;
CREATE INDEX idx_skills_release_order
    ON skills(release_id, sort_order, skill_key) WHERE deleted_at IS NULL;

CREATE TABLE vibe_tools (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    release_id  UUID         NOT NULL REFERENCES content_releases(id) ON DELETE CASCADE,
    tool_key    VARCHAR(64)  NOT NULL,
    name        VARCHAR(128),
    percentage  SMALLINT     NOT NULL DEFAULT 0 CHECK (percentage BETWEEN 0 AND 100),
    description TEXT,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order  INTEGER      NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_vibe_tools_release_key
    ON vibe_tools(release_id, tool_key) WHERE deleted_at IS NULL;
CREATE INDEX idx_vibe_tools_release_order
    ON vibe_tools(release_id, sort_order, tool_key) WHERE deleted_at IS NULL;

CREATE TABLE mylab_tags (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tag_key    VARCHAR(64) NOT NULL,
    name       VARCHAR(96) NOT NULL,
    enabled    BOOLEAN     NOT NULL DEFAULT TRUE,
    sort_order INTEGER     NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_mylab_tags_key
    ON mylab_tags(tag_key) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_mylab_tags_name
    ON mylab_tags(name) WHERE deleted_at IS NULL;
CREATE INDEX idx_mylab_tags_order
    ON mylab_tags(sort_order, tag_key) WHERE deleted_at IS NULL;

CREATE TABLE mylab_cards (
    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    release_id         UUID         NOT NULL REFERENCES content_releases(id) ON DELETE CASCADE,
    post_key           VARCHAR(96)  NOT NULL,
    card_title         VARCHAR(240),
    card_summary       TEXT,
    post_date          DATE,
    enabled            BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order         INTEGER      NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    card_type          VARCHAR(16)  NOT NULL CHECK (card_type IN ('PROJECT', 'ARTICLE')),
    project_show_order INTEGER      CHECK (project_show_order IS NULL OR project_show_order >= 0),
    project_contents   TEXT,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at         TIMESTAMPTZ,
    CONSTRAINT ck_mylab_card_project_fields CHECK (
        card_type = 'PROJECT'
        OR (card_type = 'ARTICLE' AND project_show_order IS NULL AND project_contents IS NULL)
    )
);
CREATE UNIQUE INDEX uq_mylab_cards_release_key
    ON mylab_cards(release_id, post_key) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_mylab_cards_project_order
    ON mylab_cards(release_id, project_show_order)
    WHERE card_type = 'PROJECT' AND deleted_at IS NULL;
CREATE INDEX idx_mylab_cards_release_order
    ON mylab_cards(release_id, sort_order, post_key) WHERE deleted_at IS NULL;

CREATE TABLE mylab_card_tags (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    card_id    UUID        NOT NULL REFERENCES mylab_cards(id) ON DELETE CASCADE,
    tag_id     UUID        NOT NULL REFERENCES mylab_tags(id) ON DELETE RESTRICT,
    sort_order INTEGER     NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_mylab_card_tags_pair
    ON mylab_card_tags(card_id, tag_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_mylab_card_tags_order
    ON mylab_card_tags(card_id, sort_order) WHERE deleted_at IS NULL;
CREATE INDEX idx_mylab_card_tags_tag
    ON mylab_card_tags(tag_id, card_id) WHERE deleted_at IS NULL;

CREATE TABLE footprint_resources (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    footprint_id UUID        NOT NULL REFERENCES footprints(id) ON DELETE CASCADE,
    resource_id UUID         NOT NULL REFERENCES resources(id) ON DELETE RESTRICT,
    sort_order  INTEGER      NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_footprint_resources_resource
    ON footprint_resources(footprint_id, resource_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_footprint_resources_order
    ON footprint_resources(footprint_id, sort_order) WHERE deleted_at IS NULL;

CREATE TABLE hobby_resources (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    hobby_id    UUID        NOT NULL REFERENCES hobbies(id) ON DELETE CASCADE,
    resource_id UUID        NOT NULL REFERENCES resources(id) ON DELETE RESTRICT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_hobby_resources_hobby
    ON hobby_resources(hobby_id) WHERE deleted_at IS NULL;

CREATE TABLE mylab_resources (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    card_id             UUID        NOT NULL REFERENCES mylab_cards(id) ON DELETE CASCADE,
    image_resource_id   UUID REFERENCES resources(id) ON DELETE RESTRICT,
    content_resource_id UUID REFERENCES resources(id) ON DELETE RESTRICT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ,
    CONSTRAINT ck_mylab_resources_present CHECK (
        image_resource_id IS NOT NULL OR content_resource_id IS NOT NULL
    )
);
CREATE UNIQUE INDEX uq_mylab_resources_card
    ON mylab_resources(card_id) WHERE deleted_at IS NULL;

CREATE INDEX idx_resources_uploaded_by ON resources(uploaded_by);
CREATE INDEX idx_content_releases_published_by ON content_releases(published_by);
CREATE INDEX idx_content_releases_source ON content_releases(source_release_id);
CREATE INDEX idx_footprint_resources_resource ON footprint_resources(resource_id);
CREATE INDEX idx_hobby_resources_resource ON hobby_resources(resource_id);
CREATE INDEX idx_mylab_resources_image ON mylab_resources(image_resource_id);
CREATE INDEX idx_mylab_resources_content ON mylab_resources(content_resource_id);
