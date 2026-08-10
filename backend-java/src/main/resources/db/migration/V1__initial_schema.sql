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
                      CHECK (module_key IN ('home', 'about', 'footprints', 'hobbies', 'mylab', 'skills', 'vibe')),
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

CREATE TABLE home_images (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    release_id        UUID         NOT NULL REFERENCES content_releases(id) ON DELETE CASCADE,
    image_resource_id UUID REFERENCES resources(id) ON DELETE RESTRICT,
    alt_text          VARCHAR(160),
    object_position   VARCHAR(32)  NOT NULL DEFAULT '50% 50%',
    sort_order        INTEGER      NOT NULL CHECK (sort_order BETWEEN 0 AND 5),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at        TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_home_images_release_order
    ON home_images(release_id, sort_order) WHERE deleted_at IS NULL;

CREATE TABLE about_contents (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    release_id              UUID         NOT NULL REFERENCES content_releases(id) ON DELETE CASCADE,
    profile_title           VARCHAR(80),
    avatar_resource_id      UUID REFERENCES resources(id) ON DELETE RESTRICT,
    avatar_alt              VARCHAR(160),
    intro                   TEXT,
    outro                   TEXT,
    ingredients_title       VARCHAR(80),
    ingredients_description TEXT,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at              TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_about_contents_release
    ON about_contents(release_id) WHERE deleted_at IS NULL;

CREATE TABLE about_profile_bullets (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    about_content_id UUID        NOT NULL REFERENCES about_contents(id) ON DELETE CASCADE,
    contents         TEXT,
    sort_order       INTEGER     NOT NULL CHECK (sort_order BETWEEN 0 AND 2),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_about_profile_bullets_order
    ON about_profile_bullets(about_content_id, sort_order) WHERE deleted_at IS NULL;

CREATE TABLE about_bubbles (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    about_content_id UUID        NOT NULL REFERENCES about_contents(id) ON DELETE CASCADE,
    bubble_text      VARCHAR(60),
    bubble_size      VARCHAR(8)  CHECK (bubble_size IN ('big', 'mid')),
    background_color VARCHAR(7),
    text_color       VARCHAR(7),
    glow_color       VARCHAR(7),
    sort_order       INTEGER     NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_about_bubbles_order
    ON about_bubbles(about_content_id, sort_order) WHERE deleted_at IS NULL;

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

CREATE TABLE hobby_time_tags (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    release_id  UUID         NOT NULL REFERENCES content_releases(id) ON DELETE CASCADE,
    data_key    VARCHAR(16)  NOT NULL
                CHECK (data_key IN ('Study', 'Music', 'Game', 'Coding', 'Social')),
    name        VARCHAR(64),
    color       VARCHAR(7),
    label_x     SMALLINT     CHECK (label_x BETWEEN 0 AND 500),
    label_y     SMALLINT     CHECK (label_y BETWEEN 0 AND 300),
    label_scale NUMERIC(3,1) CHECK (label_scale BETWEEN 0.5 AND 3.0),
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order  INTEGER      NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_hobby_time_tags_release_key
    ON hobby_time_tags(release_id, data_key) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_hobby_time_tags_release_order
    ON hobby_time_tags(release_id, sort_order) WHERE deleted_at IS NULL;

CREATE TABLE hobby_time_points (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    release_id UUID         NOT NULL REFERENCES content_releases(id) ON DELETE CASCADE,
    age        SMALLINT     NOT NULL CHECK (age BETWEEN -1 AND 27),
    study      NUMERIC(4,1) NOT NULL CHECK (study BETWEEN 0 AND 10),
    music      NUMERIC(4,1) NOT NULL CHECK (music BETWEEN 0 AND 10),
    game       NUMERIC(4,1) NOT NULL CHECK (game BETWEEN 0 AND 10),
    coding     NUMERIC(4,1) NOT NULL CHECK (coding BETWEEN 0 AND 10),
    social     NUMERIC(4,1) NOT NULL CHECK (social BETWEEN 0 AND 10),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT ck_hobby_time_points_total CHECK (study + music + game + coding + social = 10.0)
);
CREATE UNIQUE INDEX uq_hobby_time_points_release_age
    ON hobby_time_points(release_id, age) WHERE deleted_at IS NULL;

CREATE TABLE skills (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    release_id  UUID         NOT NULL REFERENCES content_releases(id) ON DELETE CASCADE,
    skill_key   VARCHAR(64)  NOT NULL,
    name        VARCHAR(128),
    percentage  SMALLINT     NOT NULL DEFAULT 0 CHECK (percentage BETWEEN 0 AND 100),
    level_code  VARCHAR(32)  CHECK (level_code IS NULL OR level_code IN ('novice', 'competent', 'proficient')),
    level_text  VARCHAR(32),
    icon_resource_id UUID REFERENCES resources(id) ON DELETE RESTRICT,
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
CREATE INDEX idx_home_images_resource ON home_images(image_resource_id);
CREATE INDEX idx_about_contents_avatar ON about_contents(avatar_resource_id);
CREATE INDEX idx_skills_icon_resource ON skills(icon_resource_id);
CREATE INDEX idx_footprint_resources_resource ON footprint_resources(resource_id);
CREATE INDEX idx_hobby_resources_resource ON hobby_resources(resource_id);
CREATE INDEX idx_mylab_resources_image ON mylab_resources(image_resource_id);
CREATE INDEX idx_mylab_resources_content ON mylab_resources(content_resource_id);
