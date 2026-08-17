-- ============================================================================
-- V1 基线：合并原 V1~V6 迁移脚本的最终状态
-- 来源：2026-08-17 以 docker 生产库（myblog-db-1）实际 schema + 数据为准 pg_dump 生成
-- 说明：
--   1. 本文件是 Flyway 启用后的基线版本；存量库通过 baseline-on-migrate 标记为已应用，
--      不会重复执行；全新空库会完整执行本脚本完成初始化（含初始数据）。
--   2. 此后的 schema/数据变更一律新增 V2__xxx.sql 迁移文件，禁止修改本文件。
-- ============================================================================



SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: -

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: -

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;


-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner: -

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';


SET default_tablespace = '';

SET default_table_access_method = heap;

-- Name: about_bubbles; Type: TABLE; Schema: public; Owner: -

CREATE TABLE public.about_bubbles (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    about_content_id uuid NOT NULL,
    bubble_text character varying(60),
    bubble_size character varying(8),
    background_color character varying(7),
    text_color character varying(7),
    glow_color character varying(7),
    sort_order integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT about_bubbles_bubble_size_check CHECK (((bubble_size)::text = ANY ((ARRAY['big'::character varying, 'mid'::character varying])::text[]))),
    CONSTRAINT about_bubbles_sort_order_check CHECK ((sort_order >= 0))
);


-- Name: about_contents; Type: TABLE; Schema: public; Owner: -

CREATE TABLE public.about_contents (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    release_id uuid NOT NULL,
    profile_title character varying(80),
    avatar_resource_id uuid,
    avatar_alt character varying(160),
    intro text,
    outro text,
    ingredients_title character varying(80),
    ingredients_description text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone
);


-- Name: about_profile_bullets; Type: TABLE; Schema: public; Owner: -

CREATE TABLE public.about_profile_bullets (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    about_content_id uuid NOT NULL,
    contents text,
    sort_order integer NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT about_profile_bullets_sort_order_check CHECK (((sort_order >= 0) AND (sort_order <= 2)))
);


-- Name: content_releases; Type: TABLE; Schema: public; Owner: -

CREATE TABLE public.content_releases (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    module_key character varying(32) NOT NULL,
    version_no integer NOT NULL,
    state character varying(16) NOT NULL,
    published_by uuid,
    source_release_id uuid,
    published_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT ck_content_release_publication CHECK ((((state)::text = 'DRAFT'::text) OR ((published_by IS NOT NULL) AND (published_at IS NOT NULL)))),
    CONSTRAINT content_releases_module_key_check CHECK (((module_key)::text = ANY ((ARRAY['home'::character varying, 'about'::character varying, 'footprints'::character varying, 'hobbies'::character varying, 'mylab'::character varying, 'skills'::character varying, 'vibe'::character varying])::text[]))),
    CONSTRAINT content_releases_state_check CHECK (((state)::text = ANY ((ARRAY['DRAFT'::character varying, 'PUBLISHED'::character varying, 'ARCHIVED'::character varying, 'OFFLINE'::character varying])::text[]))),
    CONSTRAINT content_releases_version_no_check CHECK ((version_no > 0))
);


-- Name: footprint_resources; Type: TABLE; Schema: public; Owner: -

CREATE TABLE public.footprint_resources (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    footprint_id uuid NOT NULL,
    resource_id uuid NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT footprint_resources_sort_order_check CHECK ((sort_order >= 0))
);


-- Name: footprints; Type: TABLE; Schema: public; Owner: -

CREATE TABLE public.footprints (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    release_id uuid NOT NULL,
    city_key character varying(64) NOT NULL,
    title character varying(200),
    summary text,
    contents text,
    enabled boolean DEFAULT true NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT footprints_sort_order_check CHECK ((sort_order >= 0))
);


-- Name: hobbies; Type: TABLE; Schema: public; Owner: -

CREATE TABLE public.hobbies (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    release_id uuid NOT NULL,
    hobby_key character varying(64) NOT NULL,
    title character varying(160),
    description text,
    enabled boolean DEFAULT true NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT hobbies_sort_order_check CHECK ((sort_order >= 0))
);


-- Name: hobby_resources; Type: TABLE; Schema: public; Owner: -

CREATE TABLE public.hobby_resources (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    hobby_id uuid NOT NULL,
    resource_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone
);


-- Name: hobby_time_points; Type: TABLE; Schema: public; Owner: -

CREATE TABLE public.hobby_time_points (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    release_id uuid NOT NULL,
    age smallint NOT NULL,
    hobby1 numeric(4,1) NOT NULL,
    hobby2 numeric(4,1) NOT NULL,
    hobby3 numeric(4,1) NOT NULL,
    hobby4 numeric(4,1) NOT NULL,
    hobby5 numeric(4,1) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT ck_hobby_time_points_total CHECK ((((((hobby1 + hobby2) + hobby3) + hobby4) + hobby5) = 10.0)),
    CONSTRAINT hobby_time_points_age_check CHECK (((age >= '-1'::integer) AND (age <= 27))),
    CONSTRAINT hobby_time_points_coding_check CHECK (((hobby4 >= (0)::numeric) AND (hobby4 <= (10)::numeric))),
    CONSTRAINT hobby_time_points_game_check CHECK (((hobby3 >= (0)::numeric) AND (hobby3 <= (10)::numeric))),
    CONSTRAINT hobby_time_points_music_check CHECK (((hobby2 >= (0)::numeric) AND (hobby2 <= (10)::numeric))),
    CONSTRAINT hobby_time_points_social_check CHECK (((hobby5 >= (0)::numeric) AND (hobby5 <= (10)::numeric))),
    CONSTRAINT hobby_time_points_study_check CHECK (((hobby1 >= (0)::numeric) AND (hobby1 <= (10)::numeric)))
);


-- Name: hobby_time_tags; Type: TABLE; Schema: public; Owner: -

CREATE TABLE public.hobby_time_tags (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    release_id uuid NOT NULL,
    data_key character varying(16) NOT NULL,
    name character varying(64),
    color character varying(7),
    label_x smallint,
    label_y smallint,
    label_scale numeric(3,1),
    enabled boolean DEFAULT true NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT hobby_time_tags_data_key_check CHECK (((data_key)::text = ANY ((ARRAY['爱好1'::character varying, '爱好2'::character varying, '爱好3'::character varying, '爱好4'::character varying, '爱好5'::character varying])::text[]))),
    CONSTRAINT hobby_time_tags_label_scale_check CHECK (((label_scale >= 0.5) AND (label_scale <= 3.0))),
    CONSTRAINT hobby_time_tags_label_x_check CHECK (((label_x >= 0) AND (label_x <= 500))),
    CONSTRAINT hobby_time_tags_label_y_check CHECK (((label_y >= 0) AND (label_y <= 300))),
    CONSTRAINT hobby_time_tags_sort_order_check CHECK ((sort_order >= 0))
);


-- Name: home_images; Type: TABLE; Schema: public; Owner: -

CREATE TABLE public.home_images (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    release_id uuid NOT NULL,
    image_resource_id uuid,
    alt_text character varying(160),
    object_position character varying(32) DEFAULT '50% 50%'::character varying NOT NULL,
    sort_order integer NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT home_images_sort_order_check CHECK (((sort_order >= 0) AND (sort_order <= 5)))
);


-- Name: mylab_card_tags; Type: TABLE; Schema: public; Owner: -

CREATE TABLE public.mylab_card_tags (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    card_id uuid NOT NULL,
    tag_id uuid NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT mylab_card_tags_sort_order_check CHECK ((sort_order >= 0))
);


-- Name: mylab_cards; Type: TABLE; Schema: public; Owner: -

CREATE TABLE public.mylab_cards (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    release_id uuid NOT NULL,
    post_key character varying(96) NOT NULL,
    card_title character varying(240),
    card_summary text,
    post_date date,
    enabled boolean DEFAULT true NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    card_type character varying(16) NOT NULL,
    project_show_order integer,
    project_contents text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT ck_mylab_card_project_fields CHECK ((((card_type)::text = 'PROJECT'::text) OR (((card_type)::text = 'ARTICLE'::text) AND (project_show_order IS NULL) AND (project_contents IS NULL)))),
    CONSTRAINT mylab_cards_card_type_check CHECK (((card_type)::text = ANY ((ARRAY['PROJECT'::character varying, 'ARTICLE'::character varying])::text[]))),
    CONSTRAINT mylab_cards_project_show_order_check CHECK (((project_show_order IS NULL) OR (project_show_order >= 0))),
    CONSTRAINT mylab_cards_sort_order_check CHECK ((sort_order >= 0))
);


-- Name: mylab_engagement_stats; Type: TABLE; Schema: public; Owner: -

CREATE TABLE public.mylab_engagement_stats (
    post_key character varying(96) NOT NULL,
    view_count bigint DEFAULT 0 NOT NULL,
    like_count bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT mylab_engagement_stats_like_count_check CHECK ((like_count >= 0)),
    CONSTRAINT mylab_engagement_stats_view_count_check CHECK ((view_count >= 0))
);


-- Name: mylab_resources; Type: TABLE; Schema: public; Owner: -

CREATE TABLE public.mylab_resources (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    card_id uuid NOT NULL,
    image_resource_id uuid,
    content_resource_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT ck_mylab_resources_present CHECK (((image_resource_id IS NOT NULL) OR (content_resource_id IS NOT NULL)))
);


-- Name: mylab_tags; Type: TABLE; Schema: public; Owner: -

CREATE TABLE public.mylab_tags (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    tag_key character varying(64) NOT NULL,
    name character varying(96) NOT NULL,
    enabled boolean DEFAULT true NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT mylab_tags_sort_order_check CHECK ((sort_order >= 0))
);


-- Name: resources; Type: TABLE; Schema: public; Owner: -

CREATE TABLE public.resources (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    object_key character varying(512) NOT NULL,
    bucket character varying(128) NOT NULL,
    original_name character varying(255),
    mime_type character varying(128) NOT NULL,
    size bigint NOT NULL,
    uploaded_by uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT resources_size_check CHECK ((size >= 0))
);


-- Name: site_daily_stats; Type: TABLE; Schema: public; Owner: -

CREATE TABLE public.site_daily_stats (
    stat_date date NOT NULL,
    visit_count bigint DEFAULT 0 NOT NULL,
    view_count bigint DEFAULT 0 NOT NULL,
    like_count bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT site_daily_stats_like_count_check CHECK ((like_count >= 0)),
    CONSTRAINT site_daily_stats_view_count_check CHECK ((view_count >= 0)),
    CONSTRAINT site_daily_stats_visit_count_check CHECK ((visit_count >= 0))
);


-- Name: site_traffic_stats; Type: TABLE; Schema: public; Owner: -

CREATE TABLE public.site_traffic_stats (
    id smallint DEFAULT 1 NOT NULL,
    visit_count bigint DEFAULT 0 NOT NULL,
    total_view_count bigint DEFAULT 0 NOT NULL,
    total_like_count bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT site_traffic_stats_id_check CHECK ((id = 1)),
    CONSTRAINT site_traffic_stats_total_like_count_check CHECK ((total_like_count >= 0)),
    CONSTRAINT site_traffic_stats_total_view_count_check CHECK ((total_view_count >= 0)),
    CONSTRAINT site_traffic_stats_visit_count_check CHECK ((visit_count >= 0))
);


-- Name: skills; Type: TABLE; Schema: public; Owner: -

CREATE TABLE public.skills (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    release_id uuid NOT NULL,
    skill_key character varying(64) NOT NULL,
    name character varying(128),
    percentage smallint DEFAULT 0 NOT NULL,
    level_code character varying(32),
    level_text character varying(32),
    icon_resource_id uuid,
    bar_style character varying(32),
    is_new boolean DEFAULT false NOT NULL,
    enabled boolean DEFAULT true NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT skills_level_code_check CHECK (((level_code IS NULL) OR ((level_code)::text = ANY ((ARRAY['novice'::character varying, 'competent'::character varying, 'proficient'::character varying])::text[])))),
    CONSTRAINT skills_percentage_check CHECK (((percentage >= 0) AND (percentage <= 100))),
    CONSTRAINT skills_sort_order_check CHECK ((sort_order >= 0))
);


-- Name: users; Type: TABLE; Schema: public; Owner: -

CREATE TABLE public.users (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    username character varying(64) NOT NULL,
    password_hash character varying(255) NOT NULL,
    role character varying(32) DEFAULT 'viewer'::character varying NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    last_login_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['viewer'::character varying, 'editor'::character varying, 'admin'::character varying, 'superadmin'::character varying])::text[])))
);


-- Name: vibe_tools; Type: TABLE; Schema: public; Owner: -

CREATE TABLE public.vibe_tools (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    release_id uuid NOT NULL,
    tool_key character varying(64) NOT NULL,
    name character varying(128),
    percentage smallint DEFAULT 0 NOT NULL,
    description text,
    enabled boolean DEFAULT true NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT vibe_tools_percentage_check CHECK (((percentage >= 0) AND (percentage <= 100))),
    CONSTRAINT vibe_tools_sort_order_check CHECK ((sort_order >= 0))
);


-- Data for Name: about_bubbles; Type: TABLE DATA; Schema: public; Owner: -

INSERT INTO public.about_bubbles VALUES ('b8d5e0b0-fb43-51e0-a8ec-ce4319d5f486', '83447fbd-3617-5cd2-a3cb-ee7b5ccc5170', 'FPS牢玩家', 'big', '#FF6B6B', '#FF8A80', '#FF6B6B', 0, '2026-08-10 00:45:04.411347+08', '2026-08-10 00:45:04.411347+08', NULL);
INSERT INTO public.about_bubbles VALUES ('141f1f61-e47c-51d3-a108-86d0ae2112be', '83447fbd-3617-5cd2-a3cb-ee7b5ccc5170', '健身旅行者', 'big', '#2EC4B6', '#64FFDA', '#2EC4B6', 1, '2026-08-10 00:45:04.411347+08', '2026-08-10 00:45:04.411347+08', NULL);
INSERT INTO public.about_bubbles VALUES ('50036cdf-2280-584f-9dc2-1bc5ec1bfacb', '83447fbd-3617-5cd2-a3cb-ee7b5ccc5170', '动物保护旅行者', 'big', '#66BB6A', '#81C784', '#66BB6A', 2, '2026-08-10 00:45:04.411347+08', '2026-08-10 00:45:04.411347+08', NULL);
INSERT INTO public.about_bubbles VALUES ('41d9e43b-0462-59a2-877b-93f46aec8150', '83447fbd-3617-5cd2-a3cb-ee7b5ccc5170', '养老二次元', 'big', '#DB7093', '#F48FB1', '#DB7093', 3, '2026-08-10 00:45:04.411347+08', '2026-08-10 00:45:04.411347+08', NULL);
INSERT INTO public.about_bubbles VALUES ('d1bf6c18-1ad8-5461-a0fb-510c55c3d160', '83447fbd-3617-5cd2-a3cb-ee7b5ccc5170', '游戏旅行者', 'big', '#FF8A65', '#FFAB91', '#FF8A65', 4, '2026-08-10 00:45:04.411347+08', '2026-08-10 00:45:04.411347+08', NULL);
INSERT INTO public.about_bubbles VALUES ('c9e4bd76-ed0b-5e45-bdd8-f96f69224790', '83447fbd-3617-5cd2-a3cb-ee7b5ccc5170', '美食探索旅行者', 'mid', '#FF8A65', '#FFCCBC', '#FF8A65', 5, '2026-08-10 00:45:04.411347+08', '2026-08-10 00:45:04.411347+08', NULL);
INSERT INTO public.about_bubbles VALUES ('299d573e-b172-59e3-a968-9ebeafaef6d8', '83447fbd-3617-5cd2-a3cb-ee7b5ccc5170', '自然风光旅行者', 'mid', '#4CAF50', '#A5D6A7', '#4CAF50', 6, '2026-08-10 00:45:04.411347+08', '2026-08-10 00:45:04.411347+08', NULL);
INSERT INTO public.about_bubbles VALUES ('91681efb-875b-5f67-adcb-582eb5a81b3a', '83447fbd-3617-5cd2-a3cb-ee7b5ccc5170', '技术探索者', 'mid', '#5BA4E6', '#81D4FA', '#5BA4E6', 7, '2026-08-10 00:45:04.411347+08', '2026-08-10 00:45:04.411347+08', NULL);
INSERT INTO public.about_bubbles VALUES ('418ebe59-9f5f-5ccf-ba31-185cbddcff0f', '83447fbd-3617-5cd2-a3cb-ee7b5ccc5170', '摄影旅行者', 'mid', '#FFB347', '#FFE082', '#FFB347', 8, '2026-08-10 00:45:04.411347+08', '2026-08-10 00:45:04.411347+08', NULL);
INSERT INTO public.about_bubbles VALUES ('d17454bb-cab9-5741-91f2-4568741e09ac', '83447fbd-3617-5cd2-a3cb-ee7b5ccc5170', 'city walk', 'mid', '#64B5F6', '#90CAF9', '#64B5F6', 9, '2026-08-10 00:45:04.411347+08', '2026-08-10 00:45:04.411347+08', NULL);
INSERT INTO public.about_bubbles VALUES ('29a77ea8-5d7d-5731-84b5-57c453b2029e', '83447fbd-3617-5cd2-a3cb-ee7b5ccc5170', '电动版骑行爱好者', 'mid', '#66BB6A', '#A5D6A7', '#66BB6A', 10, '2026-08-10 00:45:04.411347+08', '2026-08-10 00:45:04.411347+08', NULL);
INSERT INTO public.about_bubbles VALUES ('e7e3bc98-1d7d-571d-82fe-f6d6bba78fc9', '83447fbd-3617-5cd2-a3cb-ee7b5ccc5170', '吃瓜旅行者', 'mid', '#AB47BC', '#CE93D8', '#AB47BC', 11, '2026-08-10 00:45:04.411347+08', '2026-08-10 00:45:04.411347+08', NULL);
INSERT INTO public.about_bubbles VALUES ('8aa23154-dc1d-5e7a-9878-11c2f430cfe9', '83447fbd-3617-5cd2-a3cb-ee7b5ccc5170', '代码强迫症', 'mid', '#26A69A', '#80CBC4', '#26A69A', 12, '2026-08-10 00:45:04.411347+08', '2026-08-10 00:45:04.411347+08', NULL);
INSERT INTO public.about_bubbles VALUES ('be8d621d-1c12-55a4-adec-de609ad6d8f8', '83447fbd-3617-5cd2-a3cb-ee7b5ccc5170', 'AI大人的爱徒', 'mid', '#00BCD4', '#4DD0E1', '#00BCD4', 13, '2026-08-10 00:45:04.411347+08', '2026-08-10 00:45:04.411347+08', NULL);
INSERT INTO public.about_bubbles VALUES ('0b69fd5d-2723-4fb6-98e7-ee8765ad0358', 'a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', 'FPS牢玩家', 'big', '#FF6B6B', '#FF8A80', '#FF6B6B', 0, '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);
INSERT INTO public.about_bubbles VALUES ('42179600-ea3d-4d52-bac0-84c32264539c', 'a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', '健身旅行者', 'big', '#2EC4B6', '#64FFDA', '#2EC4B6', 1, '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);
INSERT INTO public.about_bubbles VALUES ('c6af27ca-79a1-462c-95cd-fa6d1cf0e5ff', 'a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', '动物保护旅行者', 'big', '#66BB6A', '#81C784', '#66BB6A', 2, '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);
INSERT INTO public.about_bubbles VALUES ('d0d23c10-648d-42d3-ba2c-56740c0f23b2', 'a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', '养老二次元', 'big', '#DB7093', '#F48FB1', '#DB7093', 3, '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);
INSERT INTO public.about_bubbles VALUES ('64a6403b-3f5d-4961-a808-8eb9242413b9', 'a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', '游戏旅行者', 'big', '#FF8A65', '#FFAB91', '#FF8A65', 4, '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);
INSERT INTO public.about_bubbles VALUES ('a9b11c3f-5263-4ff4-95f7-7ef335fdb3f2', 'a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', '美食探索旅行者', 'mid', '#FF8A65', '#FFCCBC', '#FF8A65', 5, '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);
INSERT INTO public.about_bubbles VALUES ('2ffb26a3-a7e9-4150-ae5a-f5a6880f002d', 'a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', '自然风光旅行者', 'mid', '#4CAF50', '#A5D6A7', '#4CAF50', 6, '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);
INSERT INTO public.about_bubbles VALUES ('bdec58cb-8e0e-405d-8c13-ec0227e71cd3', 'a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', '技术探索者', 'mid', '#5BA4E6', '#81D4FA', '#5BA4E6', 7, '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);
INSERT INTO public.about_bubbles VALUES ('4861a186-13a4-4d99-88c9-dfae9b8c9aba', 'a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', '摄影旅行者', 'mid', '#FFB347', '#FFE082', '#FFB347', 8, '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);
INSERT INTO public.about_bubbles VALUES ('7dd9f587-dacb-4f80-a039-11a178dab81b', 'a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', 'city walk', 'mid', '#64B5F6', '#90CAF9', '#64B5F6', 9, '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);
INSERT INTO public.about_bubbles VALUES ('1546737e-c80b-4c18-a234-533cfff26b6b', 'a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', '电驴爱好者', 'mid', '#66BB6A', '#A5D6A7', '#66BB6A', 10, '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);
INSERT INTO public.about_bubbles VALUES ('f4a6b433-cc66-4108-9115-956d1340b7ca', 'a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', '吃瓜旅行者', 'mid', '#AB47BC', '#CE93D8', '#AB47BC', 11, '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);
INSERT INTO public.about_bubbles VALUES ('dd83d730-ed33-417c-bbdf-ad338cae8f5f', 'a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', 'AI大人的爱徒', 'mid', '#00BCD4', '#4DD0E1', '#00BCD4', 12, '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);


-- Data for Name: about_contents; Type: TABLE DATA; Schema: public; Owner: -

INSERT INTO public.about_contents VALUES ('83447fbd-3617-5cd2-a3cb-ee7b5ccc5170', 'c2cb4103-7797-5121-ac8a-6a45fa98ca2a', '关于我', '4889ba92-d366-55d2-82a4-a94833da1b8c', 'DNSamuel', '你好，我是 SHENNN，目前专注于全栈开发、AI agent学习实践中...', '努力成长，希望成为一名AI超级个人，通过AI让生活变得更美好。', '我的成分', '之前有人想查我的成分，我认真的思考了一下，我的成分应该是这样，不过随时有可能会变就是啦', '2026-08-10 00:45:04.40529+08', '2026-08-10 00:45:04.40529+08', NULL);
INSERT INTO public.about_contents VALUES ('a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', 'f0ae2633-cb04-4d74-ac81-6991c3771b1c', '关于我', '4889ba92-d366-55d2-82a4-a94833da1b8c', 'DNSamuel', '你好，我是 SHENNN，目前专注于全栈开发、AI agent学习实践中...', '努力成长，希望成为一名AI超级个人，通过AI让生活变得更美好。', '我的成分', '之前有人想查我的成分，我认真的思考了一下，我的成分应该是这样，不过随时有可能会变就是啦', '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);


-- Data for Name: about_profile_bullets; Type: TABLE DATA; Schema: public; Owner: -

INSERT INTO public.about_profile_bullets VALUES ('2e9d3af7-8f49-5d55-aee8-8ace1353ad34', '83447fbd-3617-5cd2-a3cb-ee7b5ccc5170', '上位机开发：C#/.NET，负责为实验室内若干智能装备进行上位机软件开发与维护', 0, '2026-08-10 00:45:04.407966+08', '2026-08-10 00:45:04.407966+08', NULL);
INSERT INTO public.about_profile_bullets VALUES ('ed241700-91fc-5caf-917b-7b1d302b70d6', '83447fbd-3617-5cd2-a3cb-ee7b5ccc5170', 'web开发：Java/SpringBoot服务端，TypeScript/React前端，做些个人兴趣项目', 1, '2026-08-10 00:45:04.407966+08', '2026-08-10 00:45:04.407966+08', NULL);
INSERT INTO public.about_profile_bullets VALUES ('aec31432-cde8-54a5-9b18-813c056f0d77', '83447fbd-3617-5cd2-a3cb-ee7b5ccc5170', '爱好自然观光、city walk，喜欢探索这个世界的美', 2, '2026-08-10 00:45:04.407966+08', '2026-08-10 00:45:04.407966+08', NULL);
INSERT INTO public.about_profile_bullets VALUES ('c4726872-aff1-4f93-ade7-ac1e2cf62ecd', 'a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', '上位机开发：C#/.NET，负责为实验室内若干智能装备进行上位机软件开发与维护', 0, '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);
INSERT INTO public.about_profile_bullets VALUES ('2764bd20-37e2-4242-94cd-8bd5bba56d75', 'a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', 'web开发：Java/SpringBoot服务端，TypeScript/React前端，做些个人兴趣项目', 1, '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);
INSERT INTO public.about_profile_bullets VALUES ('16adf43a-465b-405e-8087-4887047a6f41', 'a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', '爱好自然观光、city walk，喜欢探索这个世界的美', 2, '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);


-- Data for Name: content_releases; Type: TABLE DATA; Schema: public; Owner: -

INSERT INTO public.content_releases VALUES ('7db57c7a-60a7-5bf5-9432-f17a2c497537', 'skills', 1, 'ARCHIVED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', NULL, '2026-08-10 00:45:04.397081+08', '2026-08-10 00:45:04.397081+08', '2026-08-10 12:32:51.785934+08', NULL);
INSERT INTO public.content_releases VALUES ('60b4421f-cb30-591e-835a-82cc23272623', 'home', 1, 'ARCHIVED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', NULL, '2026-08-10 00:45:04.397081+08', '2026-08-10 00:45:04.397081+08', '2026-08-10 12:41:33.230854+08', NULL);
INSERT INTO public.content_releases VALUES ('aab5c136-4657-4b8f-91e6-155f4ec6ab82', 'hobbies', 6, 'ARCHIVED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', '2026-08-12 00:53:40.923675+08', '2026-08-12 00:53:40.831924+08', '2026-08-12 00:58:47.285514+08', NULL);
INSERT INTO public.content_releases VALUES ('bebf6767-0895-43cb-88fa-a4116d49f63f', 'home', 2, 'ARCHIVED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '60b4421f-cb30-591e-835a-82cc23272623', '2026-08-10 12:41:33.230854+08', '2026-08-10 12:39:25.685276+08', '2026-08-10 12:55:55.751916+08', NULL);
INSERT INTO public.content_releases VALUES ('53f769a6-4d8e-43b2-85f7-602dd87ad06f', 'skills', 2, 'ARCHIVED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '7db57c7a-60a7-5bf5-9432-f17a2c497537', '2026-08-10 12:32:51.785934+08', '2026-08-10 12:32:51.736389+08', '2026-08-11 14:45:20.210847+08', NULL);
INSERT INTO public.content_releases VALUES ('f6d1b717-651e-48d8-a0d3-8ceeb1666d36', 'skills', 3, 'PUBLISHED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '53f769a6-4d8e-43b2-85f7-602dd87ad06f', '2026-08-11 14:45:20.210847+08', '2026-08-11 14:45:20.163611+08', '2026-08-11 14:45:20.210847+08', NULL);
INSERT INTO public.content_releases VALUES ('d5aa99ad-9e7e-5022-9093-9fe8b80c92c9', 'vibe', 1, 'ARCHIVED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', NULL, '2026-08-10 00:45:04.397081+08', '2026-08-10 00:45:04.397081+08', '2026-08-11 15:17:27.616857+08', NULL);
INSERT INTO public.content_releases VALUES ('ce8fb576-a59a-55eb-a870-c929f086a306', 'footprints', 1, 'ARCHIVED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', NULL, '2026-08-10 00:45:04.397081+08', '2026-08-10 00:45:04.397081+08', '2026-08-11 16:52:39.331344+08', NULL);
INSERT INTO public.content_releases VALUES ('55263f48-b18e-4296-8a85-ba0bf8fc6611', 'footprints', 2, 'ARCHIVED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', 'ce8fb576-a59a-55eb-a870-c929f086a306', '2026-08-11 16:52:39.331344+08', '2026-08-11 16:52:39.267684+08', '2026-08-11 18:18:55.886087+08', NULL);
INSERT INTO public.content_releases VALUES ('2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 'hobbies', 7, 'ARCHIVED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', '2026-08-12 00:58:47.285514+08', '2026-08-12 00:58:47.192652+08', '2026-08-12 01:00:03.120463+08', NULL);
INSERT INTO public.content_releases VALUES ('dde9d421-d9b2-4abe-8639-db1c7b896f5d', 'hobbies', 8, 'ARCHIVED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', '2026-08-12 01:00:03.120463+08', '2026-08-12 01:00:03.022541+08', '2026-08-12 01:07:49.691089+08', NULL);
INSERT INTO public.content_releases VALUES ('9f44df14-bdbd-418d-b02f-5cbacc439b52', 'footprints', 3, 'ARCHIVED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '55263f48-b18e-4296-8a85-ba0bf8fc6611', '2026-08-11 18:18:55.886087+08', '2026-08-11 17:59:08.973317+08', '2026-08-11 22:03:46.219317+08', NULL);
INSERT INTO public.content_releases VALUES ('de5acb15-fe8b-4f3e-838d-d51c6e24c783', 'footprints', 4, 'ARCHIVED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '9f44df14-bdbd-418d-b02f-5cbacc439b52', '2026-08-11 22:03:46.219317+08', '2026-08-11 21:44:56.02574+08', '2026-08-11 22:06:19.550788+08', NULL);
INSERT INTO public.content_releases VALUES ('71445498-efe3-4dab-b8e6-655f9ce69c59', 'footprints', 5, 'ARCHIVED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', 'de5acb15-fe8b-4f3e-838d-d51c6e24c783', '2026-08-11 22:06:19.550788+08', '2026-08-11 22:06:19.489476+08', '2026-08-11 22:24:53.222218+08', NULL);
INSERT INTO public.content_releases VALUES ('de31d1f4-8021-4f6a-94d3-389121aff190', 'footprints', 6, 'PUBLISHED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '71445498-efe3-4dab-b8e6-655f9ce69c59', '2026-08-11 22:24:53.222218+08', '2026-08-11 22:24:53.164049+08', '2026-08-11 22:24:53.222218+08', NULL);
INSERT INTO public.content_releases VALUES ('4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 'hobbies', 2, 'ARCHIVED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '08a67fb1-a5fa-5edd-a203-4b684b828adc', '2026-08-12 00:31:59.290314+08', '2026-08-12 00:31:58.08254+08', '2026-08-12 00:40:58.264467+08', NULL);
INSERT INTO public.content_releases VALUES ('49261c09-f797-49c4-bf32-f451b28b91de', 'hobbies', 3, 'ARCHIVED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', '2026-08-12 00:40:58.264467+08', '2026-08-12 00:40:58.155624+08', '2026-08-12 00:49:38.545578+08', NULL);
INSERT INTO public.content_releases VALUES ('2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 'hobbies', 4, 'ARCHIVED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '49261c09-f797-49c4-bf32-f451b28b91de', '2026-08-12 00:49:38.545578+08', '2026-08-12 00:49:38.449588+08', '2026-08-12 00:51:31.96628+08', NULL);
INSERT INTO public.content_releases VALUES ('3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 'hobbies', 5, 'ARCHIVED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', '2026-08-12 00:51:31.96628+08', '2026-08-12 00:51:31.86839+08', '2026-08-12 00:53:40.923675+08', NULL);
INSERT INTO public.content_releases VALUES ('75f9fde8-300c-4616-ad83-aebd9b051891', 'hobbies', 9, 'ARCHIVED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', '2026-08-12 01:07:49.691089+08', '2026-08-12 01:07:49.593364+08', '2026-08-12 01:09:41.782572+08', NULL);
INSERT INTO public.content_releases VALUES ('b4f1b3ff-0017-4649-9260-277ab323f56f', 'hobbies', 10, 'ARCHIVED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '75f9fde8-300c-4616-ad83-aebd9b051891', '2026-08-12 01:09:41.782572+08', '2026-08-12 01:09:41.692512+08', '2026-08-12 01:12:59.374199+08', NULL);
INSERT INTO public.content_releases VALUES ('8eb7a954-14b1-5b57-a0f1-692b8d6a9e1b', 'mylab', 1, 'ARCHIVED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', NULL, '2026-08-10 00:45:04.397081+08', '2026-08-10 00:45:04.397081+08', '2026-08-12 19:42:15.547393+08', NULL);
INSERT INTO public.content_releases VALUES ('17fe8734-e813-475e-8cd4-9fd6de31ae5b', 'mylab', 2, 'PUBLISHED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '8eb7a954-14b1-5b57-a0f1-692b8d6a9e1b', '2026-08-12 19:42:15.547393+08', '2026-08-12 19:42:13.948183+08', '2026-08-12 19:42:15.547393+08', NULL);
INSERT INTO public.content_releases VALUES ('c2cb4103-7797-5121-ac8a-6a45fa98ca2a', 'about', 1, 'ARCHIVED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', NULL, '2026-08-10 00:45:04.397081+08', '2026-08-10 00:45:04.397081+08', '2026-08-13 13:39:00.95023+08', NULL);
INSERT INTO public.content_releases VALUES ('f0ae2633-cb04-4d74-ac81-6991c3771b1c', 'about', 2, 'PUBLISHED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', 'c2cb4103-7797-5121-ac8a-6a45fa98ca2a', '2026-08-13 13:39:00.95023+08', '2026-08-13 13:38:59.872204+08', '2026-08-13 13:39:00.95023+08', NULL);
INSERT INTO public.content_releases VALUES ('be28904d-fd8a-4422-9163-9c2014be29f6', 'hobbies', 11, 'ARCHIVED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', 'b4f1b3ff-0017-4649-9260-277ab323f56f', '2026-08-12 01:12:59.374199+08', '2026-08-12 01:12:59.275457+08', '2026-08-12 22:27:37.964543+08', NULL);
INSERT INTO public.content_releases VALUES ('a3eed050-75dd-46a4-b2b4-19f0b2d92b52', 'vibe', 2, 'ARCHIVED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', 'd5aa99ad-9e7e-5022-9093-9fe8b80c92c9', '2026-08-11 15:17:27.616857+08', '2026-08-11 15:17:05.704646+08', '2026-08-13 13:32:14.262812+08', NULL);
INSERT INTO public.content_releases VALUES ('e5d78b76-992b-4ed5-9b8c-9d187a628573', 'vibe', 3, 'PUBLISHED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', 'a3eed050-75dd-46a4-b2b4-19f0b2d92b52', '2026-08-13 13:32:14.262812+08', '2026-08-13 13:32:14.222716+08', '2026-08-13 13:32:14.262812+08', NULL);
INSERT INTO public.content_releases VALUES ('4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 'hobbies', 17, 'PUBLISHED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '46ca6189-6ad5-40a9-a327-aeac604f78c1', '2026-08-17 13:15:50.541737+08', '2026-08-17 13:15:50.431322+08', '2026-08-17 13:15:50.541737+08', NULL);
INSERT INTO public.content_releases VALUES ('08a67fb1-a5fa-5edd-a203-4b684b828adc', 'hobbies', 1, 'ARCHIVED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', NULL, '2026-08-10 00:45:04.397081+08', '2026-08-10 00:45:04.397081+08', '2026-08-13 20:56:28.41561+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.content_releases VALUES ('81210424-f77d-4f36-97d8-892ebef7b8ac', 'hobbies', 14, 'ARCHIVED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', '2026-08-16 22:56:12.31571+08', '2026-08-12 22:44:54.298186+08', '2026-08-17 19:02:25.625922+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.content_releases VALUES ('f6c20a67-76c8-4fdc-b591-df6bce39626e', 'hobbies', 12, 'ARCHIVED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', 'be28904d-fd8a-4422-9163-9c2014be29f6', '2026-08-12 22:27:37.964543+08', '2026-08-12 16:26:35.668494+08', '2026-08-17 19:02:57.363995+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.content_releases VALUES ('bf85d3b0-7d38-485b-a4f3-ca8980f08a99', 'home', 3, 'ARCHIVED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', 'bebf6767-0895-43cb-88fa-a4116d49f63f', '2026-08-10 12:55:55.751916+08', '2026-08-10 12:55:43.114947+08', '2026-08-13 22:10:38.763489+08', NULL);
INSERT INTO public.content_releases VALUES ('ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 'hobbies', 13, 'ARCHIVED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', '2026-08-12 22:42:08.60279+08', '2026-08-12 22:42:08.495911+08', '2026-08-17 19:02:58.750725+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.content_releases VALUES ('b2c66006-9e77-4c93-a033-1d310e775cbf', 'home', 4, 'ARCHIVED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', 'bf85d3b0-7d38-485b-a4f3-ca8980f08a99', '2026-08-13 22:10:38.763489+08', '2026-08-13 22:10:36.306127+08', '2026-08-13 22:11:04.847659+08', NULL);
INSERT INTO public.content_releases VALUES ('897c1c16-c070-461a-8d93-109d24c17979', 'home', 5, 'PUBLISHED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', 'bf85d3b0-7d38-485b-a4f3-ca8980f08a99', '2026-08-13 22:11:04.847659+08', '2026-08-13 22:11:01.022274+08', '2026-08-13 22:11:04.847659+08', NULL);
INSERT INTO public.content_releases VALUES ('e0710820-58b3-4ccd-8f9f-9b4c832b8552', 'hobbies', 15, 'ARCHIVED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '81210424-f77d-4f36-97d8-892ebef7b8ac', '2026-08-16 22:59:10.295774+08', '2026-08-16 22:59:10.183253+08', '2026-08-17 19:02:59.902078+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.content_releases VALUES ('46ca6189-6ad5-40a9-a327-aeac604f78c1', 'hobbies', 16, 'ARCHIVED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', '2026-08-17 13:12:08.336525+08', '2026-08-17 13:12:08.187114+08', '2026-08-17 19:03:01.981497+08', '2026-08-17 19:03:01.981497+08');


-- Data for Name: footprint_resources; Type: TABLE DATA; Schema: public; Owner: -

INSERT INTO public.footprint_resources VALUES ('ce6a6a7f-50cc-44c3-97a7-6b2d78fb1319', '4266435a-d4cf-4a80-b1bb-f47c9385eaf1', 'd3910fb4-a1ab-4153-931d-87d84893d62c', 0, '2026-08-11 16:52:39.261002+08', '2026-08-11 16:52:39.261002+08', NULL);
INSERT INTO public.footprint_resources VALUES ('05bd6355-78ab-424f-8fd7-d5770e956f22', '4266435a-d4cf-4a80-b1bb-f47c9385eaf1', '457fdd0f-2a53-4acf-a102-378d56b25f9b', 1, '2026-08-11 16:52:39.261002+08', '2026-08-11 16:52:39.261002+08', NULL);
INSERT INTO public.footprint_resources VALUES ('da387e32-ad42-4d11-bac6-87c2f400afcc', '4266435a-d4cf-4a80-b1bb-f47c9385eaf1', 'e2853460-a24b-438f-99d1-cb06c5123ffd', 2, '2026-08-11 16:52:39.261002+08', '2026-08-11 16:52:39.261002+08', NULL);
INSERT INTO public.footprint_resources VALUES ('0f3cb5b7-0788-4d96-b94a-4a880f4e49e3', '4266435a-d4cf-4a80-b1bb-f47c9385eaf1', '81bf69f5-23d2-484f-bb1d-35085fcc68b6', 3, '2026-08-11 16:52:39.261002+08', '2026-08-11 16:52:39.261002+08', NULL);
INSERT INTO public.footprint_resources VALUES ('686f7e26-1b21-4360-9794-b66ef42af2ce', '4266435a-d4cf-4a80-b1bb-f47c9385eaf1', 'bdd1af10-1578-45dd-bb8f-3fe91f141f29', 4, '2026-08-11 16:52:39.261002+08', '2026-08-11 16:52:39.261002+08', NULL);
INSERT INTO public.footprint_resources VALUES ('6b78155f-eb88-40dc-9aab-fb720af67253', '4266435a-d4cf-4a80-b1bb-f47c9385eaf1', '18478fc2-e6fd-4bc5-87cc-4a34646b373c', 5, '2026-08-11 16:52:39.261002+08', '2026-08-11 16:52:39.261002+08', NULL);
INSERT INTO public.footprint_resources VALUES ('67fe0356-b1b7-4fc8-b66e-457698730bfd', 'dd6994aa-47ef-4e32-811a-a5fd51c8e20c', '8dd096de-3db8-4941-ad75-eabee4e03633', 0, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('4070998c-1212-4289-bc1d-236d89dd66ea', 'dd6994aa-47ef-4e32-811a-a5fd51c8e20c', '8f48cfe1-978d-4636-b693-cdfd9d5aaf56', 1, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('58922016-5c97-4838-8d5f-b8cb5d555d3c', 'dd6994aa-47ef-4e32-811a-a5fd51c8e20c', '0afa7af5-725f-4e61-b0c8-04d37ab148b9', 2, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('9418f4fd-a80e-4396-8d72-bbafbd012fcc', 'dd6994aa-47ef-4e32-811a-a5fd51c8e20c', 'efefaae1-db49-473b-a22e-7ff05ecfbb62', 3, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('b542f80f-70c8-4cab-9805-8e54a121630c', 'dd6994aa-47ef-4e32-811a-a5fd51c8e20c', 'b0a6a34e-45d5-4b5b-be36-ab9084fb83df', 4, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('6802e6e8-42a7-4e0f-8e1f-bb692837fb47', 'dd6994aa-47ef-4e32-811a-a5fd51c8e20c', 'ceed1b03-ddf3-4ac7-97ec-01e6f6de42a8', 5, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('cee9a9e9-8ba1-49c8-a622-41dfc31e3033', 'cefa2398-f99c-422d-98f7-a514bd65b579', '8dd096de-3db8-4941-ad75-eabee4e03633', 0, '2026-08-11 18:18:55.822361+08', '2026-08-11 18:18:55.822361+08', NULL);
INSERT INTO public.footprint_resources VALUES ('5de4f29f-8ad8-4f0c-b2b4-ef693506a3d8', 'cefa2398-f99c-422d-98f7-a514bd65b579', '8f48cfe1-978d-4636-b693-cdfd9d5aaf56', 1, '2026-08-11 18:18:55.822361+08', '2026-08-11 18:18:55.822361+08', NULL);
INSERT INTO public.footprint_resources VALUES ('4ec5116d-b26a-42a7-9b03-c914e1b01b17', 'cefa2398-f99c-422d-98f7-a514bd65b579', '0afa7af5-725f-4e61-b0c8-04d37ab148b9', 2, '2026-08-11 18:18:55.822361+08', '2026-08-11 18:18:55.822361+08', NULL);
INSERT INTO public.footprint_resources VALUES ('e95f0ea4-dc70-4dd8-8b4a-e71d32fdd292', 'cefa2398-f99c-422d-98f7-a514bd65b579', 'efefaae1-db49-473b-a22e-7ff05ecfbb62', 3, '2026-08-11 18:18:55.822361+08', '2026-08-11 18:18:55.822361+08', NULL);
INSERT INTO public.footprint_resources VALUES ('c5c1343d-4083-408d-8393-786a02639f6c', 'cefa2398-f99c-422d-98f7-a514bd65b579', 'b0a6a34e-45d5-4b5b-be36-ab9084fb83df', 4, '2026-08-11 18:18:55.822361+08', '2026-08-11 18:18:55.822361+08', NULL);
INSERT INTO public.footprint_resources VALUES ('6bbb0fe1-7a73-48d4-9808-a566b0aa4996', 'cefa2398-f99c-422d-98f7-a514bd65b579', 'ceed1b03-ddf3-4ac7-97ec-01e6f6de42a8', 5, '2026-08-11 18:18:55.822361+08', '2026-08-11 18:18:55.822361+08', NULL);
INSERT INTO public.footprint_resources VALUES ('3aafc054-8a4b-499f-9953-90ecffa76acd', 'cefa2398-f99c-422d-98f7-a514bd65b579', '76e34059-adf8-4280-aa9c-545b6b9d27b8', 6, '2026-08-11 18:18:55.822361+08', '2026-08-11 18:18:55.822361+08', NULL);
INSERT INTO public.footprint_resources VALUES ('8423c399-5a90-4504-ad02-0a9965cb3d51', 'cefa2398-f99c-422d-98f7-a514bd65b579', '207dec49-3a28-4af1-8a29-b8196a7069d0', 7, '2026-08-11 18:18:55.822361+08', '2026-08-11 18:18:55.822361+08', NULL);
INSERT INTO public.footprint_resources VALUES ('0c0b3c24-c6de-458f-81c6-50ddf8cc382d', 'cefa2398-f99c-422d-98f7-a514bd65b579', '70e9bfbf-f11e-4c22-8994-42605e806621', 8, '2026-08-11 18:18:55.822361+08', '2026-08-11 18:18:55.822361+08', NULL);
INSERT INTO public.footprint_resources VALUES ('dd8ee8ea-76e1-4dc8-991c-3c4764245532', '2ce0ae9c-b776-46c8-b511-bde0478a25ab', 'd3910fb4-a1ab-4153-931d-87d84893d62c', 0, '2026-08-11 18:18:55.822361+08', '2026-08-11 18:18:55.822361+08', NULL);
INSERT INTO public.footprint_resources VALUES ('0e21976d-9520-49d5-89bb-b85d338fbe26', '2ce0ae9c-b776-46c8-b511-bde0478a25ab', '457fdd0f-2a53-4acf-a102-378d56b25f9b', 1, '2026-08-11 18:18:55.822361+08', '2026-08-11 18:18:55.822361+08', NULL);
INSERT INTO public.footprint_resources VALUES ('49dc7cbf-bc27-439a-8470-a273e0120893', '2ce0ae9c-b776-46c8-b511-bde0478a25ab', 'e2853460-a24b-438f-99d1-cb06c5123ffd', 2, '2026-08-11 18:18:55.822361+08', '2026-08-11 18:18:55.822361+08', NULL);
INSERT INTO public.footprint_resources VALUES ('5a29e30c-6d02-4f59-99e0-3f7f9b036709', '2ce0ae9c-b776-46c8-b511-bde0478a25ab', '81bf69f5-23d2-484f-bb1d-35085fcc68b6', 3, '2026-08-11 18:18:55.822361+08', '2026-08-11 18:18:55.822361+08', NULL);
INSERT INTO public.footprint_resources VALUES ('2a98ed18-45c8-4252-825e-c422398f5291', '2ce0ae9c-b776-46c8-b511-bde0478a25ab', 'bdd1af10-1578-45dd-bb8f-3fe91f141f29', 4, '2026-08-11 18:18:55.822361+08', '2026-08-11 18:18:55.822361+08', NULL);
INSERT INTO public.footprint_resources VALUES ('1c59f85e-fc4a-402e-8c98-87e92405eff2', '2ce0ae9c-b776-46c8-b511-bde0478a25ab', '18478fc2-e6fd-4bc5-87cc-4a34646b373c', 5, '2026-08-11 18:18:55.822361+08', '2026-08-11 18:18:55.822361+08', NULL);
INSERT INTO public.footprint_resources VALUES ('5637697c-9eb8-4939-9476-3222df7d67d7', 'dd6994aa-47ef-4e32-811a-a5fd51c8e20c', '76e34059-adf8-4280-aa9c-545b6b9d27b8', 6, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('91f97dd2-440b-4cdf-8b1d-0a0cfa4bc992', 'dd6994aa-47ef-4e32-811a-a5fd51c8e20c', '207dec49-3a28-4af1-8a29-b8196a7069d0', 7, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('013e7370-4126-40e1-bd4e-2e8a9c71184f', 'dd6994aa-47ef-4e32-811a-a5fd51c8e20c', '70e9bfbf-f11e-4c22-8994-42605e806621', 8, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('5a9fad88-a4f6-43b9-94cf-ca95b70be31d', '12e573a0-7a46-405d-9ccd-368c6e3aa426', 'd3910fb4-a1ab-4153-931d-87d84893d62c', 0, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('bbbfdd2e-a039-46c3-b630-f4c1e98a3fec', '12e573a0-7a46-405d-9ccd-368c6e3aa426', '457fdd0f-2a53-4acf-a102-378d56b25f9b', 1, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('7e53951d-9d78-4ba0-ac25-3e919f3d8082', '12e573a0-7a46-405d-9ccd-368c6e3aa426', 'e2853460-a24b-438f-99d1-cb06c5123ffd', 2, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('98319868-ca69-4e54-8cde-a5091964a2bc', '12e573a0-7a46-405d-9ccd-368c6e3aa426', '81bf69f5-23d2-484f-bb1d-35085fcc68b6', 3, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('2604a6d9-946a-471f-8b13-629770e80e72', '12e573a0-7a46-405d-9ccd-368c6e3aa426', 'bdd1af10-1578-45dd-bb8f-3fe91f141f29', 4, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('f44e99b1-9fe6-4061-9826-46427a31307a', '12e573a0-7a46-405d-9ccd-368c6e3aa426', '18478fc2-e6fd-4bc5-87cc-4a34646b373c', 5, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('ea17ab9e-d33c-43fe-80e3-a3505ef9fb00', 'ba5f3dd7-7112-4583-8f3c-cefffec9ff7d', 'cfdefefd-f50a-4c44-8195-0e59dedbc3be', 0, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('afb7ca0c-9f54-4cfa-95df-65f635a826ca', 'ba5f3dd7-7112-4583-8f3c-cefffec9ff7d', '73d5f633-1732-40ac-b5f0-797cdc308a05', 1, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('372dd4a0-1bbb-4e4e-af45-d6a56aa7b636', 'ba5f3dd7-7112-4583-8f3c-cefffec9ff7d', '06c1b717-05c4-4882-8601-7c35f6bed6b7', 2, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('4fa1023e-c9fa-41a4-b00b-9bd82a94dce3', 'ba5f3dd7-7112-4583-8f3c-cefffec9ff7d', '00591d27-ab26-4295-9e6f-3ce988b5a38d', 3, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('143c0bde-a04a-4e14-af3e-f03f0ada05ca', 'ba5f3dd7-7112-4583-8f3c-cefffec9ff7d', '2780f138-274a-43b2-97c5-939b5b01b8bc', 4, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('f03e45ed-9c03-4fcc-b212-e017fa47a3f2', 'ba5f3dd7-7112-4583-8f3c-cefffec9ff7d', 'd832949a-bf1f-4803-87f8-4410c3ecbf4e', 5, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('156fa568-f680-4bd6-b341-94d02065baab', 'cad4b8ac-ce14-4001-a572-12ee6057b1a0', '9643f1ff-cc52-4bbf-b9c6-aa657cd1078e', 0, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('58a8fed1-f244-466c-8f7d-5bc7e8d51876', 'cad4b8ac-ce14-4001-a572-12ee6057b1a0', 'ce487f10-8f9f-45be-bc23-1860f040854d', 1, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('ab05fe95-48e3-4962-a12b-7296a0994eda', 'cad4b8ac-ce14-4001-a572-12ee6057b1a0', 'f59535ef-234e-444e-a799-7af526962eba', 2, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('511efa86-b95a-4051-9ed1-1c16b47ea98d', 'cad4b8ac-ce14-4001-a572-12ee6057b1a0', 'cc48b9e0-f3d3-4980-b386-bfce181fd793', 3, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('7ffa0e03-04d9-41e3-8b99-698096a0259e', 'cad4b8ac-ce14-4001-a572-12ee6057b1a0', 'c64a2eee-a122-422d-a58a-25f629d0b80d', 4, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('94b9df49-119a-4b8e-b66d-4a8c366db448', 'cad4b8ac-ce14-4001-a572-12ee6057b1a0', '7c1fd1d0-3eac-478a-8713-6c65a85efdac', 5, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('f27b4eef-f9c4-400a-a289-9db398dfd6f9', 'db177458-573a-4b3f-8922-057c945db88d', '27815e56-3d57-4d20-86e3-78279115ba87', 0, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('6f34b93b-3bb7-4158-8150-97d81a62bc24', 'db177458-573a-4b3f-8922-057c945db88d', '2e6ee498-f690-4cff-9a1c-983417013ce4', 1, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('b131234f-31fe-4eff-a2ba-9a126f894915', 'db177458-573a-4b3f-8922-057c945db88d', '82915b3f-e053-4679-b57f-aa0c87f461fa', 2, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('0e7ad033-7f65-4aca-a3be-900b10c1923e', 'db177458-573a-4b3f-8922-057c945db88d', '4e10c48b-8447-4a2c-9db2-91f088df037b', 3, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('9512c6b9-3277-4fcf-826f-000e74afebb3', 'db177458-573a-4b3f-8922-057c945db88d', '47d6a325-b0c6-4ab3-b10f-bb87716acdbe', 4, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('35b30d5a-303e-49ba-961e-13e619cd1a95', 'db177458-573a-4b3f-8922-057c945db88d', '4b57bb78-37ba-49cf-82b3-04dfcd5a794b', 5, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('fb1284fc-e1bb-44e8-a0b6-76b511031734', 'db177458-573a-4b3f-8922-057c945db88d', '889405d9-e6b5-47ae-a4d7-f978b390d7a9', 6, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('42301a0a-905a-4fc4-9c64-f7c2124b5615', 'b124afe2-ba4f-4e16-8439-0a14f51eac05', '842d16f0-0ada-4bc0-b63c-2b92f3545f6f', 0, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('fa4f049b-60a9-4519-928f-a194a8c18034', 'b124afe2-ba4f-4e16-8439-0a14f51eac05', '2e0e9185-e407-4412-9601-820210a49f5c', 1, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('14de810a-e15a-4290-8c76-1bf25a65cc8a', 'b124afe2-ba4f-4e16-8439-0a14f51eac05', 'c34714fd-c984-4fb4-b301-305c4dffb8b2', 2, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('da65d3aa-4a28-4d0f-bfa6-5b2bdcb4f7aa', 'b124afe2-ba4f-4e16-8439-0a14f51eac05', '46accbe7-3d42-41c2-b81a-210776fd7b01', 3, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('50437e06-bb3b-49c6-bb1e-9ccf5581a908', 'b124afe2-ba4f-4e16-8439-0a14f51eac05', 'b1b8ffa1-1b70-41f5-9bc6-26c7f2a68090', 4, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprint_resources VALUES ('f7bf9012-4ab2-42a3-b5b7-35c52c7bd4fe', 'eafdf294-c9fc-48e5-becf-1b1e89a9a2ec', '8dd096de-3db8-4941-ad75-eabee4e03633', 0, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('a65e2fa0-16b4-4857-813d-d62f6ce5e559', 'eafdf294-c9fc-48e5-becf-1b1e89a9a2ec', '8f48cfe1-978d-4636-b693-cdfd9d5aaf56', 1, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('e16c149a-60d7-4c6a-a33b-c3936a3c0727', 'eafdf294-c9fc-48e5-becf-1b1e89a9a2ec', '0afa7af5-725f-4e61-b0c8-04d37ab148b9', 2, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('12ccc18a-7a88-44a3-8758-79d62abe302c', 'eafdf294-c9fc-48e5-becf-1b1e89a9a2ec', 'efefaae1-db49-473b-a22e-7ff05ecfbb62', 3, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('8a5aa2d7-5f5f-4dd2-8011-5327381f1ff5', 'eafdf294-c9fc-48e5-becf-1b1e89a9a2ec', 'b0a6a34e-45d5-4b5b-be36-ab9084fb83df', 4, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('cd9e3ac2-0994-4b4d-9f1a-9db4e865d626', 'eafdf294-c9fc-48e5-becf-1b1e89a9a2ec', 'ceed1b03-ddf3-4ac7-97ec-01e6f6de42a8', 5, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('21e74112-85f5-4a1b-9f37-50c234d46303', 'eafdf294-c9fc-48e5-becf-1b1e89a9a2ec', '76e34059-adf8-4280-aa9c-545b6b9d27b8', 6, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('bc997bad-8eed-4d63-98a6-84f7a8ab7068', 'eafdf294-c9fc-48e5-becf-1b1e89a9a2ec', '207dec49-3a28-4af1-8a29-b8196a7069d0', 7, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('f0c94039-713f-4881-b759-f4f39fc92735', 'eafdf294-c9fc-48e5-becf-1b1e89a9a2ec', '70e9bfbf-f11e-4c22-8994-42605e806621', 8, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('ff35e99f-840e-4548-a3c6-813e221e2efd', 'be473ea7-0662-4ca7-9849-6d2a0c4ad86b', 'd3910fb4-a1ab-4153-931d-87d84893d62c', 0, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('234277de-4425-4be8-8b50-19587982d36e', 'be473ea7-0662-4ca7-9849-6d2a0c4ad86b', '457fdd0f-2a53-4acf-a102-378d56b25f9b', 1, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('d90861e5-8cff-4282-8d04-8e7f9da1c17a', 'be473ea7-0662-4ca7-9849-6d2a0c4ad86b', 'e2853460-a24b-438f-99d1-cb06c5123ffd', 2, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('e73ffb7f-80b6-41f1-a841-b315cd614a3a', 'be473ea7-0662-4ca7-9849-6d2a0c4ad86b', '81bf69f5-23d2-484f-bb1d-35085fcc68b6', 3, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('5f1d54da-bc54-4cdc-a849-b070d4603b39', 'be473ea7-0662-4ca7-9849-6d2a0c4ad86b', 'bdd1af10-1578-45dd-bb8f-3fe91f141f29', 4, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('e43eaa6c-7513-42a6-8606-88c231e470cb', 'be473ea7-0662-4ca7-9849-6d2a0c4ad86b', '18478fc2-e6fd-4bc5-87cc-4a34646b373c', 5, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('71c80dec-af13-42f6-aabe-0a5eb992af9e', 'cb244a30-265e-4b63-b748-42231a2fd696', 'cfdefefd-f50a-4c44-8195-0e59dedbc3be', 0, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('ebe72567-f1c8-4860-a424-25b6642f4c02', 'cb244a30-265e-4b63-b748-42231a2fd696', '73d5f633-1732-40ac-b5f0-797cdc308a05', 1, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('2383e558-b248-4d81-a789-da11f1af2cfa', 'cb244a30-265e-4b63-b748-42231a2fd696', '06c1b717-05c4-4882-8601-7c35f6bed6b7', 2, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('409692dc-57d6-4c0d-b7b6-3f56b01de13f', 'cb244a30-265e-4b63-b748-42231a2fd696', '00591d27-ab26-4295-9e6f-3ce988b5a38d', 3, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('32590a6a-30a1-4b70-a8fd-f9c29f6377ea', 'cb244a30-265e-4b63-b748-42231a2fd696', '2780f138-274a-43b2-97c5-939b5b01b8bc', 4, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('653d2e03-d1f8-4c49-9795-e6b96f532fc4', 'cb244a30-265e-4b63-b748-42231a2fd696', 'd832949a-bf1f-4803-87f8-4410c3ecbf4e', 5, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('16e015b3-b101-4086-9dbf-b1703860ae3a', '748ad426-3a18-4ace-8fd2-cef5d96329a7', 'ce487f10-8f9f-45be-bc23-1860f040854d', 0, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('9435ad0c-5e78-439d-9153-0bd867466a22', '748ad426-3a18-4ace-8fd2-cef5d96329a7', '9643f1ff-cc52-4bbf-b9c6-aa657cd1078e', 1, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('e31bb9c4-f269-4ff5-b486-f8b5a700eea3', '748ad426-3a18-4ace-8fd2-cef5d96329a7', 'f59535ef-234e-444e-a799-7af526962eba', 2, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('8568d1dd-8619-43b3-8785-52cce3f29fe6', '748ad426-3a18-4ace-8fd2-cef5d96329a7', 'cc48b9e0-f3d3-4980-b386-bfce181fd793', 3, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('c815460c-d525-43d0-b517-22fb152e56c3', '748ad426-3a18-4ace-8fd2-cef5d96329a7', 'c64a2eee-a122-422d-a58a-25f629d0b80d', 4, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('a65fbe8c-9ff1-48f5-b2c2-9318998c0381', '748ad426-3a18-4ace-8fd2-cef5d96329a7', '7c1fd1d0-3eac-478a-8713-6c65a85efdac', 5, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('48715c85-10f4-42d5-b9a8-20c56a900648', '2e3e6a06-2a3c-44ea-80c6-a06ab8a8aac8', '27815e56-3d57-4d20-86e3-78279115ba87', 0, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('5ab919eb-074b-4c10-bc3b-a2a689edf065', '2e3e6a06-2a3c-44ea-80c6-a06ab8a8aac8', '82915b3f-e053-4679-b57f-aa0c87f461fa', 1, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('5aae88d9-03c4-4a6d-81b4-46ed8c2e6b7a', '2e3e6a06-2a3c-44ea-80c6-a06ab8a8aac8', '2e6ee498-f690-4cff-9a1c-983417013ce4', 2, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('81cdff7d-4585-4b6f-90ae-8f70684b4c1a', '2e3e6a06-2a3c-44ea-80c6-a06ab8a8aac8', '4e10c48b-8447-4a2c-9db2-91f088df037b', 3, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('26defd97-dc71-440c-a9f9-a0934b760405', '2e3e6a06-2a3c-44ea-80c6-a06ab8a8aac8', '47d6a325-b0c6-4ab3-b10f-bb87716acdbe', 4, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('71947366-2050-4218-af30-cc594740688d', '2e3e6a06-2a3c-44ea-80c6-a06ab8a8aac8', '4b57bb78-37ba-49cf-82b3-04dfcd5a794b', 5, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('cc68ede5-9521-4eeb-adbc-a147faa0b6a9', '2e3e6a06-2a3c-44ea-80c6-a06ab8a8aac8', '889405d9-e6b5-47ae-a4d7-f978b390d7a9', 6, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('22a6a733-7f27-435d-bbec-1a064e3d3c76', '5bd4bc39-545f-43e3-95b2-9ac587fe66f6', '842d16f0-0ada-4bc0-b63c-2b92f3545f6f', 0, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('3252338d-580a-4794-bb11-e51c7309a524', '5bd4bc39-545f-43e3-95b2-9ac587fe66f6', '2e0e9185-e407-4412-9601-820210a49f5c', 1, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('17c497e8-e104-422a-adde-b6b946e6062a', '5bd4bc39-545f-43e3-95b2-9ac587fe66f6', 'c34714fd-c984-4fb4-b301-305c4dffb8b2', 2, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('3fdfbc51-ed1b-4fd8-93be-fca3816ca2fe', '5bd4bc39-545f-43e3-95b2-9ac587fe66f6', '46accbe7-3d42-41c2-b81a-210776fd7b01', 3, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('3a5880e1-2194-4b2f-aa23-91cef8986480', '5bd4bc39-545f-43e3-95b2-9ac587fe66f6', 'b1b8ffa1-1b70-41f5-9bc6-26c7f2a68090', 4, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprint_resources VALUES ('5c120f2c-baaa-463a-9cd7-e4df7f731cfc', '2d23ba6a-2c82-4351-826f-fdc4095aae3d', '8dd096de-3db8-4941-ad75-eabee4e03633', 0, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('a3ec0175-dee5-4f7c-95be-659c3998735b', '2d23ba6a-2c82-4351-826f-fdc4095aae3d', '8f48cfe1-978d-4636-b693-cdfd9d5aaf56', 1, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('5fb2f27e-3c06-4c20-9afc-922066bf7256', '2d23ba6a-2c82-4351-826f-fdc4095aae3d', '0afa7af5-725f-4e61-b0c8-04d37ab148b9', 2, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('a8dd10ff-ec54-40f9-9d4c-f16801d4e9c0', '2d23ba6a-2c82-4351-826f-fdc4095aae3d', 'efefaae1-db49-473b-a22e-7ff05ecfbb62', 3, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('cea81006-dd8a-4ffb-b587-9c578b9a1881', '2d23ba6a-2c82-4351-826f-fdc4095aae3d', 'b0a6a34e-45d5-4b5b-be36-ab9084fb83df', 4, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('f1185033-49e5-4a2f-97e1-7372d622aed5', '2d23ba6a-2c82-4351-826f-fdc4095aae3d', 'ceed1b03-ddf3-4ac7-97ec-01e6f6de42a8', 5, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('85c962e5-6566-4287-9c4c-720114fb75ac', '2d23ba6a-2c82-4351-826f-fdc4095aae3d', '76e34059-adf8-4280-aa9c-545b6b9d27b8', 6, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('e8a424f7-f993-428d-934f-5777646f428d', '2d23ba6a-2c82-4351-826f-fdc4095aae3d', '207dec49-3a28-4af1-8a29-b8196a7069d0', 7, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('6f008c55-69e3-4413-aba4-15f01ab360d9', '2d23ba6a-2c82-4351-826f-fdc4095aae3d', '70e9bfbf-f11e-4c22-8994-42605e806621', 8, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('fd3ddeca-087f-4c26-aab6-365e1bdee6fd', 'aa6679ac-77d5-488e-99e0-feedc8829cf8', '9643f1ff-cc52-4bbf-b9c6-aa657cd1078e', 0, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('021dbb14-8c0c-408a-a514-abae6a39cc66', 'aa6679ac-77d5-488e-99e0-feedc8829cf8', 'ce487f10-8f9f-45be-bc23-1860f040854d', 1, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('415c46ed-870e-48e1-bde7-2c741fd40cc3', 'aa6679ac-77d5-488e-99e0-feedc8829cf8', 'f59535ef-234e-444e-a799-7af526962eba', 2, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('c778e9fa-f404-495b-a454-e5412db79e5c', 'aa6679ac-77d5-488e-99e0-feedc8829cf8', 'cc48b9e0-f3d3-4980-b386-bfce181fd793', 3, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('e86eff93-f12f-47e5-8bfa-9871b8d0d6a7', 'aa6679ac-77d5-488e-99e0-feedc8829cf8', 'c64a2eee-a122-422d-a58a-25f629d0b80d', 4, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('eafa1329-8a96-45e7-b28b-c07b9ae46dc3', 'aa6679ac-77d5-488e-99e0-feedc8829cf8', '7c1fd1d0-3eac-478a-8713-6c65a85efdac', 5, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('a098bd5c-a26c-4e93-8400-1a8f9df1ecb3', '22efc57b-263c-44b7-a058-b936bf3dd7ba', '27815e56-3d57-4d20-86e3-78279115ba87', 0, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('e7df4226-ba34-4f3b-9457-6898ec4f74cc', '22efc57b-263c-44b7-a058-b936bf3dd7ba', '2e6ee498-f690-4cff-9a1c-983417013ce4', 1, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('7bd6d3dd-92cd-4b91-be18-65b024aff88b', '22efc57b-263c-44b7-a058-b936bf3dd7ba', '82915b3f-e053-4679-b57f-aa0c87f461fa', 2, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('85b14cb7-941c-4f0c-a964-bf3a7d6c8659', '22efc57b-263c-44b7-a058-b936bf3dd7ba', '4e10c48b-8447-4a2c-9db2-91f088df037b', 3, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('20f7aefa-664f-41ab-b362-e149d4915950', '22efc57b-263c-44b7-a058-b936bf3dd7ba', '47d6a325-b0c6-4ab3-b10f-bb87716acdbe', 4, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('9e6cc7b3-89f0-4f1b-9afd-b5a12a585b4f', '22efc57b-263c-44b7-a058-b936bf3dd7ba', '4b57bb78-37ba-49cf-82b3-04dfcd5a794b', 5, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('be6c8c86-b22c-4e31-8904-298e6bcadbfa', '22efc57b-263c-44b7-a058-b936bf3dd7ba', '889405d9-e6b5-47ae-a4d7-f978b390d7a9', 6, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('07b57636-cbef-4592-9917-648b534c9e4f', 'eddbff5b-224d-4228-97ab-5e0376a641a1', 'd3910fb4-a1ab-4153-931d-87d84893d62c', 0, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('78d1e1b1-ee71-40d3-a732-6a46230f413f', 'eddbff5b-224d-4228-97ab-5e0376a641a1', '457fdd0f-2a53-4acf-a102-378d56b25f9b', 1, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('73538dda-dc04-4a2a-9a1c-604217fe779e', 'eddbff5b-224d-4228-97ab-5e0376a641a1', 'e2853460-a24b-438f-99d1-cb06c5123ffd', 2, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('0f8e2501-f7a9-40c8-a0ca-51e2568b83f8', 'eddbff5b-224d-4228-97ab-5e0376a641a1', '81bf69f5-23d2-484f-bb1d-35085fcc68b6', 3, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('6c010ed7-fadd-4758-a63d-53c9a0ef060c', 'eddbff5b-224d-4228-97ab-5e0376a641a1', 'bdd1af10-1578-45dd-bb8f-3fe91f141f29', 4, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('46f3b049-b8c6-405b-bccd-2173dac8d10f', 'eddbff5b-224d-4228-97ab-5e0376a641a1', '18478fc2-e6fd-4bc5-87cc-4a34646b373c', 5, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('a5448644-a8ca-43c9-911f-6cc9624bbd66', '282dfaf7-7594-4ced-b454-07cd9a2229cd', 'cfdefefd-f50a-4c44-8195-0e59dedbc3be', 0, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('58f0e178-aec0-4f9a-9d0c-c2f48c4387f6', '282dfaf7-7594-4ced-b454-07cd9a2229cd', '73d5f633-1732-40ac-b5f0-797cdc308a05', 1, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('a9476da0-1d81-410b-9f43-596c7ca13e0a', '282dfaf7-7594-4ced-b454-07cd9a2229cd', '06c1b717-05c4-4882-8601-7c35f6bed6b7', 2, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('5b9052c9-5250-4da2-b8af-e98fdbc8ca09', '282dfaf7-7594-4ced-b454-07cd9a2229cd', '00591d27-ab26-4295-9e6f-3ce988b5a38d', 3, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('58b8a258-5b9f-4050-b024-5d6f9cc4c2f0', '282dfaf7-7594-4ced-b454-07cd9a2229cd', '2780f138-274a-43b2-97c5-939b5b01b8bc', 4, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('76310ae2-34a4-41c2-8117-32bf6586e828', '282dfaf7-7594-4ced-b454-07cd9a2229cd', 'd832949a-bf1f-4803-87f8-4410c3ecbf4e', 5, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('156f176b-54c2-4e68-b9ef-3b5762997b25', '17257d1e-6459-493e-805f-a314d0df298f', '842d16f0-0ada-4bc0-b63c-2b92f3545f6f', 0, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('00acb1d3-7ed4-4714-8b89-84c387d86d19', '17257d1e-6459-493e-805f-a314d0df298f', '2e0e9185-e407-4412-9601-820210a49f5c', 1, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('61d53b12-988c-46c3-9b5a-1ddc424ee5e8', '17257d1e-6459-493e-805f-a314d0df298f', 'c34714fd-c984-4fb4-b301-305c4dffb8b2', 2, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('6b7d8cbd-2f96-4c0e-a98f-b000968af7ca', '17257d1e-6459-493e-805f-a314d0df298f', '46accbe7-3d42-41c2-b81a-210776fd7b01', 3, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources VALUES ('8c18dc40-4d98-430d-8eb8-de6e94856d09', '17257d1e-6459-493e-805f-a314d0df298f', 'b1b8ffa1-1b70-41f5-9bc6-26c7f2a68090', 4, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);


-- Data for Name: footprints; Type: TABLE DATA; Schema: public; Owner: -

INSERT INTO public.footprints VALUES ('f34d2f19-b676-5c67-80f5-b46015e12a18', 'ce8fb576-a59a-55eb-a870-c929f086a306', 'photo', '胶片摄影 · 西安城墙', '一台 Nikon FM2，几卷 Portra 400，和一段厚重的古城墙。', '西安是我拍胶片最密集的城市。古城墙是天然的引导线，傍晚时分，金色的光沿着砖缝流下来。

我喜欢在钟楼附近反复行走，让人流、车流和老建筑在取景框里形成自己的节奏。

胶片摄影对我来说不是怀旧，而是一种慢下来的观察方式。', true, 0, '2026-08-10 00:45:04.417524+08', '2026-08-10 00:45:04.417524+08', NULL);
INSERT INTO public.footprints VALUES ('d9e8038a-9a40-56a7-9d67-e8e5833e9690', 'ce8fb576-a59a-55eb-a870-c929f086a306', 'hike', '徒步 · 昆明 · 高海拔', '用脚步丈量高原，不是征服，是学会在稀薄空气里找到自己的节奏。', '昆明周边的山路让我重新理解了“距离”这件事：地图上的短线，走起来常常是完整的一天。

我喜欢徒步里那种简单的判断：补水、节奏、天气、脚下的路，每一件都真实具体。

最美的风景往往不在终点，而在“再坚持一下”之后的转角。', true, 1, '2026-08-10 00:45:04.417524+08', '2026-08-10 00:45:04.417524+08', NULL);
INSERT INTO public.footprints VALUES ('0659dbb2-c840-5949-bdf3-a579e51254c4', 'ce8fb576-a59a-55eb-a870-c929f086a306', 'coffee', '精品咖啡 · 上海武康路', '从豆子到杯子，一杯咖啡是一段小型的时间旅行。', '武康路是我在上海很喜欢的一段路。梧桐树影把阳光切成碎片，几家小店藏在老房子里。

咖啡对我来说是一种准时开始工作的仪式，不是醒神，而是给一天一个锚点。

我更在意一杯咖啡背后的风味描述、产地故事，以及它被认真对待的方式。', true, 2, '2026-08-10 00:45:04.417524+08', '2026-08-10 00:45:04.417524+08', NULL);
INSERT INTO public.footprints VALUES ('d085c38b-f2e6-5a68-8957-7b22b3b19ac4', 'ce8fb576-a59a-55eb-a870-c929f086a306', 'travel', '城市漫游 · 广州西关', '不急着去景点，只在陌生城市的街区里游荡几个小时。', '西关是广州老城里很迷人的一片：骑楼街、麻石巷、满洲窗，还有街坊聊天的声音。

我喜欢在这样的地方慢慢走，听街边的生活声，闻别人家的饭菜香。

城市漫游训练我对偶然的开放度：走错路，才更容易遇到没有被攻略写过的惊喜。', true, 3, '2026-08-10 00:45:04.417524+08', '2026-08-10 00:45:04.417524+08', NULL);
INSERT INTO public.footprints VALUES ('4148db94-1047-52d5-b38c-1e8f5414f0ae', 'ce8fb576-a59a-55eb-a870-c929f086a306', 'music', '黑胶与合成器 · 深圳 OCT', '一种回放时间，一种创造时间，它们都让我暂时离开屏幕。', '深圳的创意园区里有几家独立唱片店，是我固定会去的地方。

合成器是近几年新开的坑。把一个 pad 音色调出层次，本身就是一次小创作。

音乐对我而言是不被语言打扰的时间。项目做累了，切到 DAW 里乱按二十分钟，也是一种恢复。', true, 4, '2026-08-10 00:45:04.417524+08', '2026-08-10 00:45:04.417524+08', NULL);
INSERT INTO public.footprints VALUES ('158ced81-1ee4-5db6-bdd7-81c7ae89f265', 'ce8fb576-a59a-55eb-a870-c929f086a306', 'read', '独立书店 · 北京', '认识一座城市，最慢也最可靠的方式，是在它的书店里坐一个下午。', '北京有几条书店密度很高的街区，我喜欢把它们当作城市里的临时工作台。

我常常在独立书店里不急着买东西，只是翻完一本诗集，再翻完一本地理散文。

比起连锁书店，独立书店更像私人策展，选品本身就是一种表达。', true, 5, '2026-08-10 00:45:04.417524+08', '2026-08-10 00:45:04.417524+08', NULL);
INSERT INTO public.footprints VALUES ('4266435a-d4cf-4a80-b1bb-f47c9385eaf1', '55263f48-b18e-4296-8a85-ba0bf8fc6611', 'photo', '西安', '西安，一座让千年古韵与现代繁华温柔相拥的城市。', '西安的老城区以钟楼为中心，涵盖城墙周边的大片区域。这里历史沉淀深厚，市井气息浓郁，我喜欢穿梭在大街小巷之间感受平民生活的温度，在浏览园林景致中感受历史古韵。

而城市的另一面，则以行政中心为原点向周围铺展。高楼鳞次栉比，商场连绵不绝，与城墙内的古朴沉静遥相对望，构成一幅时空交错的独特画卷。', true, 0, '2026-08-11 16:52:39.261002+08', '2026-08-11 16:52:39.261002+08', NULL);
INSERT INTO public.footprints VALUES ('933bb486-79c3-4811-b4b4-cf08eb762cb0', '55263f48-b18e-4296-8a85-ba0bf8fc6611', 'hike', '徒步 · 昆明 · 高海拔', '用脚步丈量高原，不是征服，是学会在稀薄空气里找到自己的节奏。', '昆明周边的山路让我重新理解了“距离”这件事：地图上的短线，走起来常常是完整的一天。

我喜欢徒步里那种简单的判断：补水、节奏、天气、脚下的路，每一件都真实具体。

最美的风景往往不在终点，而在“再坚持一下”之后的转角。', true, 1, '2026-08-11 16:52:39.261002+08', '2026-08-11 16:52:39.261002+08', NULL);
INSERT INTO public.footprints VALUES ('eab0104d-1d58-480c-8e42-be0fca0ae4e6', '55263f48-b18e-4296-8a85-ba0bf8fc6611', 'coffee', '精品咖啡 · 上海武康路', '从豆子到杯子，一杯咖啡是一段小型的时间旅行。', '武康路是我在上海很喜欢的一段路。梧桐树影把阳光切成碎片，几家小店藏在老房子里。

咖啡对我来说是一种准时开始工作的仪式，不是醒神，而是给一天一个锚点。

我更在意一杯咖啡背后的风味描述、产地故事，以及它被认真对待的方式。', true, 2, '2026-08-11 16:52:39.261002+08', '2026-08-11 16:52:39.261002+08', NULL);
INSERT INTO public.footprints VALUES ('3e560784-9fad-4a31-a521-debe4c67af91', '55263f48-b18e-4296-8a85-ba0bf8fc6611', 'travel', '城市漫游 · 广州西关', '不急着去景点，只在陌生城市的街区里游荡几个小时。', '西关是广州老城里很迷人的一片：骑楼街、麻石巷、满洲窗，还有街坊聊天的声音。

我喜欢在这样的地方慢慢走，听街边的生活声，闻别人家的饭菜香。

城市漫游训练我对偶然的开放度：走错路，才更容易遇到没有被攻略写过的惊喜。', true, 3, '2026-08-11 16:52:39.261002+08', '2026-08-11 16:52:39.261002+08', NULL);
INSERT INTO public.footprints VALUES ('a29415ce-6433-41cc-9bfe-776fefc8f589', '55263f48-b18e-4296-8a85-ba0bf8fc6611', 'music', '黑胶与合成器 · 深圳 OCT', '一种回放时间，一种创造时间，它们都让我暂时离开屏幕。', '深圳的创意园区里有几家独立唱片店，是我固定会去的地方。

合成器是近几年新开的坑。把一个 pad 音色调出层次，本身就是一次小创作。

音乐对我而言是不被语言打扰的时间。项目做累了，切到 DAW 里乱按二十分钟，也是一种恢复。', true, 4, '2026-08-11 16:52:39.261002+08', '2026-08-11 16:52:39.261002+08', NULL);
INSERT INTO public.footprints VALUES ('041deeb1-0444-4c87-9b33-ac3999d6bd7a', '55263f48-b18e-4296-8a85-ba0bf8fc6611', 'read', '独立书店 · 北京', '认识一座城市，最慢也最可靠的方式，是在它的书店里坐一个下午。', '北京有几条书店密度很高的街区，我喜欢把它们当作城市里的临时工作台。

我常常在独立书店里不急着买东西，只是翻完一本诗集，再翻完一本地理散文。

比起连锁书店，独立书店更像私人策展，选品本身就是一种表达。', true, 5, '2026-08-11 16:52:39.261002+08', '2026-08-11 16:52:39.261002+08', NULL);
INSERT INTO public.footprints VALUES ('cefa2398-f99c-422d-98f7-a514bd65b579', '9f44df14-bdbd-418d-b02f-5cbacc439b52', 'hike', '昆明', '昆明，我的家，也是我存放了无数回忆的地方。', '在海埂公园吹过无数次滇池的风，爬过不知道多少次西山，吃过了各式各样刚出炉的鲜花饼，留下了无数的美好回忆。

风和日丽，四季如春，鲜花从不缺席，春天从未走远，这是被花草与暖阳偏爱的地方，也是我在任何其他城市都寻不到的奢侈。', true, 0, '2026-08-11 18:18:55.822361+08', '2026-08-11 18:18:55.822361+08', NULL);
INSERT INTO public.footprints VALUES ('2ce0ae9c-b776-46c8-b511-bde0478a25ab', '9f44df14-bdbd-418d-b02f-5cbacc439b52', 'photo', '西安', '西安，一座让千年古韵与现代繁华温柔相拥的城市。', '西安的老城区以钟楼为中心，涵盖城墙周边的大片区域。这里历史沉淀深厚，市井气息浓郁，我喜欢穿梭在大街小巷之间感受平民生活的温度，在浏览园林景致中感受历史古韵。

而城市的另一面，则以行政中心为原点向周围铺展。高楼鳞次栉比，商场连绵不绝，与城墙内的古朴沉静遥相对望，构成一幅时空交错的独特画卷。', true, 1, '2026-08-11 18:18:55.822361+08', '2026-08-11 18:18:55.822361+08', NULL);
INSERT INTO public.footprints VALUES ('583aa20c-70a2-4cc4-8414-26e762103aeb', '9f44df14-bdbd-418d-b02f-5cbacc439b52', 'coffee', '精品咖啡 · 上海武康路', '从豆子到杯子，一杯咖啡是一段小型的时间旅行。', '武康路是我在上海很喜欢的一段路。梧桐树影把阳光切成碎片，几家小店藏在老房子里。

咖啡对我来说是一种准时开始工作的仪式，不是醒神，而是给一天一个锚点。

我更在意一杯咖啡背后的风味描述、产地故事，以及它被认真对待的方式。', true, 2, '2026-08-11 18:18:55.822361+08', '2026-08-11 18:18:55.822361+08', NULL);
INSERT INTO public.footprints VALUES ('053bec27-c5a2-4efe-a3bc-8c96ae087a73', '9f44df14-bdbd-418d-b02f-5cbacc439b52', 'travel', '城市漫游 · 广州西关', '不急着去景点，只在陌生城市的街区里游荡几个小时。', '西关是广州老城里很迷人的一片：骑楼街、麻石巷、满洲窗，还有街坊聊天的声音。

我喜欢在这样的地方慢慢走，听街边的生活声，闻别人家的饭菜香。

城市漫游训练我对偶然的开放度：走错路，才更容易遇到没有被攻略写过的惊喜。', true, 3, '2026-08-11 18:18:55.822361+08', '2026-08-11 18:18:55.822361+08', NULL);
INSERT INTO public.footprints VALUES ('8e1ce788-a0c6-4d7e-a680-6424ce24b25d', '9f44df14-bdbd-418d-b02f-5cbacc439b52', 'music', '黑胶与合成器 · 深圳 OCT', '一种回放时间，一种创造时间，它们都让我暂时离开屏幕。', '深圳的创意园区里有几家独立唱片店，是我固定会去的地方。

合成器是近几年新开的坑。把一个 pad 音色调出层次，本身就是一次小创作。

音乐对我而言是不被语言打扰的时间。项目做累了，切到 DAW 里乱按二十分钟，也是一种恢复。', true, 4, '2026-08-11 18:18:55.822361+08', '2026-08-11 18:18:55.822361+08', NULL);
INSERT INTO public.footprints VALUES ('243984f2-f0bb-4cc8-beac-c0dbbbb849ee', '9f44df14-bdbd-418d-b02f-5cbacc439b52', 'read', '独立书店 · 北京', '认识一座城市，最慢也最可靠的方式，是在它的书店里坐一个下午。', '北京有几条书店密度很高的街区，我喜欢把它们当作城市里的临时工作台。

我常常在独立书店里不急着买东西，只是翻完一本诗集，再翻完一本地理散文。

比起连锁书店，独立书店更像私人策展，选品本身就是一种表达。', true, 5, '2026-08-11 18:18:55.822361+08', '2026-08-11 18:18:55.822361+08', NULL);
INSERT INTO public.footprints VALUES ('eafdf294-c9fc-48e5-becf-1b1e89a9a2ec', 'de5acb15-fe8b-4f3e-838d-d51c6e24c783', 'hike', '昆明', '昆明，我的家，也是我存放了无数回忆的地方。', '在海埂公园吹过无数次滇池的风，爬过不知道多少次西山，吃过了各式各样刚出炉的鲜花饼，留下了无数的美好回忆。

风和日丽，四季如春，鲜花从不缺席，春天从未走远，这是被花草与暖阳偏爱的地方，也是我在任何其他城市都寻不到的奢侈。', true, 0, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprints VALUES ('be473ea7-0662-4ca7-9849-6d2a0c4ad86b', 'de5acb15-fe8b-4f3e-838d-d51c6e24c783', 'photo', '西安', '西安，一座让千年古韵与现代繁华温柔相拥的城市。', '西安的老城区以钟楼为中心，涵盖城墙周边的大片区域。这里历史沉淀深厚，市井气息浓郁，我喜欢穿梭在大街小巷之间感受平民生活的温度，在浏览园林景致中感受历史古韵。

而城市的另一面，则以行政中心为原点向周围铺展。高楼鳞次栉比，商场连绵不绝，与城墙内的古朴沉静遥相对望，构成一幅时空交错的独特画卷。', true, 1, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprints VALUES ('cb244a30-265e-4b63-b748-42231a2fd696', 'de5acb15-fe8b-4f3e-838d-d51c6e24c783', 'coffee', '上海', '上海，一座在黄浦江两岸折叠时空的城市。', '外滩的百年建筑与陆家嘴的摩天楼群隔江相望，梧桐老洋房与石库门弄堂共存于同一片街区。

这里既有金融中心的快节奏，也有街角咖啡馆的慢时光。

中西合璧的海派文化，让传统与现代并肩而立，织就一幅独特的城市天际线。', true, 2, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprints VALUES ('748ad426-3a18-4ace-8fd2-cef5d96329a7', 'de5acb15-fe8b-4f3e-838d-d51c6e24c783', 'travel', '广州', '广州，大城市中生活气息最浓厚的城市。', '行走在广州老城里，骑楼街、麻石巷、满洲窗，聆听着街坊聊天的声音，闻着小店里炒菜的香气，这是广州随处可见的风景。

有时我愿意不急着去景点，只为陌生城市的街区里游荡几个小时。

这里既有小城的闲适，又有现代大都市的效率，传统商脉与现代商业在此共生，织就独特的广府气质。', true, 3, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprints VALUES ('2e3e6a06-2a3c-44ea-80c6-a06ab8a8aac8', 'de5acb15-fe8b-4f3e-838d-d51c6e24c783', 'music', '深圳', '深圳，一座被山海拥抱着奔跑的城市。', '从广州过来，新，就是我对深圳的第一印象，这里没有古老的城墙，只有各式各样的都市高楼。

深圳湾看海，梧桐山爬山，山海环绕，以及大大小小的公园，是深圳特有的城市景观。

改革开放的前沿，一座平均年龄极年轻的城市。山海连城，高楼与自然共生。没有历史包袱，只有创新基因。', true, 4, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprints VALUES ('5bd4bc39-545f-43e3-95b2-9ac587fe66f6', 'de5acb15-fe8b-4f3e-838d-d51c6e24c783', 'read', '北京', '北京，千年古都，国家心脏。', '红墙黄瓦与摩天楼群共存，胡同烟火与CBD繁华交织。

历史厚度与现代速度在此碰撞，政治文化中心与国际化都市并行。

四季分明，底蕴深厚，是梦想与现实持续交锋的北方重镇。', true, 5, '2026-08-11 22:03:46.148322+08', '2026-08-11 22:03:46.148322+08', NULL);
INSERT INTO public.footprints VALUES ('dd6994aa-47ef-4e32-811a-a5fd51c8e20c', '71445498-efe3-4dab-b8e6-655f9ce69c59', 'hike', '昆明', '昆明，我的家，也是我存放了无数回忆的地方。', '在海埂公园吹过无数次滇池的风，爬过不知道多少次西山，吃过了各式各样刚出炉的鲜花饼，留下了无数的美好回忆。

风和日丽，四季如春，鲜花从不缺席，春天从未走远，这是被花草与暖阳偏爱的地方，也是我在任何其他城市都寻不到的奢侈。', true, 0, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprints VALUES ('12e573a0-7a46-405d-9ccd-368c6e3aa426', '71445498-efe3-4dab-b8e6-655f9ce69c59', 'photo', '西安', '西安，一座让千年古韵与现代繁华温柔相拥的城市。', '西安的老城区以钟楼为中心，涵盖城墙周边的大片区域。这里历史沉淀深厚，市井气息浓郁，我喜欢穿梭在大街小巷之间感受平民生活的温度，在浏览园林景致中感受历史古韵。

而城市的另一面，则以行政中心为原点向周围铺展。高楼鳞次栉比，商场连绵不绝，与城墙内的古朴沉静遥相对望，构成一幅时空交错的独特画卷。', true, 1, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprints VALUES ('ba5f3dd7-7112-4583-8f3c-cefffec9ff7d', '71445498-efe3-4dab-b8e6-655f9ce69c59', 'coffee', '上海', '上海，一座在黄浦江两岸折叠时空的城市。', '外滩的百年建筑与陆家嘴的摩天楼群隔江相望，梧桐老洋房与石库门弄堂共存于同一片街区。

这里既有金融中心的快节奏，也有街角咖啡馆的慢时光。

中西合璧的海派文化，让传统与现代并肩而立，织就一幅独特的城市天际线。', true, 2, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprints VALUES ('cad4b8ac-ce14-4001-a572-12ee6057b1a0', '71445498-efe3-4dab-b8e6-655f9ce69c59', 'travel', '广州', '广州，大城市中生活气息最浓厚的城市。', '行走在广州老城里，骑楼街、麻石巷、满洲窗，聆听着街坊聊天的声音，闻着小店里炒菜的香气，这是广州随处可见的风景。

有时我愿意不急着去景点，只为陌生城市的街区里游荡几个小时。

这里既有小城的闲适，又有现代大都市的效率，传统商脉与现代商业在此共生，织就独特的广府气质。', true, 3, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprints VALUES ('db177458-573a-4b3f-8922-057c945db88d', '71445498-efe3-4dab-b8e6-655f9ce69c59', 'music', '深圳', '深圳，一座被山海拥抱着奔跑的城市。', '从广州过来，新，就是我对深圳的第一印象，这里没有古老的城墙，只有各式各样的都市高楼。

深圳湾看海，梧桐山爬山，山海环绕，以及大大小小的公园，是深圳特有的城市景观。

改革开放的前沿，一座平均年龄极年轻的城市。山海连城，高楼与自然共生。没有历史包袱，只有创新基因。', true, 4, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprints VALUES ('b124afe2-ba4f-4e16-8439-0a14f51eac05', '71445498-efe3-4dab-b8e6-655f9ce69c59', 'read', '北京', '北京，千年古都，国家心脏。', '红墙黄瓦与摩天楼群共存，胡同烟火与CBD繁华交织。

历史厚度与现代速度在此碰撞，政治文化中心与国际化都市并行。

四季分明，底蕴深厚，是梦想与现实持续交锋的北方重镇。', true, 5, '2026-08-11 22:06:19.474464+08', '2026-08-11 22:06:19.474464+08', NULL);
INSERT INTO public.footprints VALUES ('2d23ba6a-2c82-4351-826f-fdc4095aae3d', 'de31d1f4-8021-4f6a-94d3-389121aff190', 'hike', '昆明', '昆明，我的家，也是我存放了无数回忆的地方。', '在海埂公园吹过无数次滇池的风，爬过不知道多少次西山，吃过了各式各样刚出炉的鲜花饼，留下了无数的美好回忆。

风和日丽，四季如春，鲜花从不缺席，春天从未走远，这是被花草与暖阳偏爱的地方，也是我在任何其他城市都寻不到的奢侈。', true, 0, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprints VALUES ('aa6679ac-77d5-488e-99e0-feedc8829cf8', 'de31d1f4-8021-4f6a-94d3-389121aff190', 'travel', '广州', '广州，大城市中生活气息最浓厚的城市。', '行走在广州老城里，骑楼街、麻石巷、满洲窗，聆听着街坊聊天的声音，闻着小店里炒菜的香气，这是广州随处可见的风景。

有时我愿意不急着去景点，只为陌生城市的街区里游荡几个小时。

这里既有小城的闲适，又有现代大都市的效率，传统商脉与现代商业在此共生，织就独特的广府气质。', true, 1, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprints VALUES ('22efc57b-263c-44b7-a058-b936bf3dd7ba', 'de31d1f4-8021-4f6a-94d3-389121aff190', 'music', '深圳', '深圳，一座被山海拥抱着奔跑的城市。', '从广州过来，新，就是我对深圳的第一印象，这里没有古老的城墙，只有各式各样的都市高楼。

深圳湾看海，梧桐山爬山，山海环绕，以及大大小小的公园，是深圳特有的城市景观。

改革开放的前沿，一座平均年龄极年轻的城市。山海连城，高楼与自然共生。没有历史包袱，只有创新基因。', true, 2, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprints VALUES ('eddbff5b-224d-4228-97ab-5e0376a641a1', 'de31d1f4-8021-4f6a-94d3-389121aff190', 'photo', '西安', '西安，一座让千年古韵与现代繁华温柔相拥的城市。', '西安的老城区以钟楼为中心，涵盖城墙周边的大片区域。这里历史沉淀深厚，市井气息浓郁，我喜欢穿梭在大街小巷之间感受平民生活的温度，在浏览园林景致中感受历史古韵。

而城市的另一面，则以行政中心为原点向周围铺展。高楼鳞次栉比，商场连绵不绝，与城墙内的古朴沉静遥相对望，构成一幅时空交错的独特画卷。', true, 3, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprints VALUES ('282dfaf7-7594-4ced-b454-07cd9a2229cd', 'de31d1f4-8021-4f6a-94d3-389121aff190', 'coffee', '上海', '上海，一座在黄浦江两岸折叠时空的城市。', '外滩的百年建筑与陆家嘴的摩天楼群隔江相望，梧桐老洋房与石库门弄堂共存于同一片街区。

这里既有金融中心的快节奏，也有街角咖啡馆的慢时光。

中西合璧的海派文化，让传统与现代并肩而立，织就一幅独特的城市天际线。', true, 4, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprints VALUES ('17257d1e-6459-493e-805f-a314d0df298f', 'de31d1f4-8021-4f6a-94d3-389121aff190', 'read', '北京', '北京，千年古都，国家心脏。', '红墙黄瓦与摩天楼群共存，胡同烟火与CBD繁华交织。

历史厚度与现代速度在此碰撞，政治文化中心与国际化都市并行。

四季分明，底蕴深厚，是梦想与现实持续交锋的北方重镇。', true, 5, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);


-- Data for Name: hobbies; Type: TABLE DATA; Schema: public; Owner: -

INSERT INTO public.hobbies VALUES ('229596d8-38dc-4de5-827c-605cbfa7d72c', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 'counter-strike-2', 'Counter-Strike 2', '最喜欢它纯粹又残酷的博弈感，每一颗道具、每一次peek都要为团队节奏负责。', true, 0, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobbies VALUES ('d78fffe9-e8e0-44bf-a110-458647d09c5c', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 'apex', 'Apex 英雄', '机动性和临场决策很迷人，打赢一波混战时会有非常强的爽感和节奏感。', true, 1, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobbies VALUES ('91a4ee01-4e68-41ce-9360-e42b8d8556b0', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 'delta-force', '三角洲行动', '偏战术、偏压迫的枪线体验，适合认真研究路线、信息和团队配合。', true, 2, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobbies VALUES ('167fbd87-691a-4fc1-8302-bc946977d811', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 'valorant', '无畏契约', '技能和枪法互相牵制，回合制的紧张感很足，残局尤其容易让人上头。', true, 3, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobbies VALUES ('93e36d11-f99a-4d3c-8229-1c78e16c02fd', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 'overwatch-2', '守望先锋 2', '英雄机制和团战节奏变化很快，最吸引我的是团队位置和技能交换。', true, 4, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobbies VALUES ('635c811b-d382-4e3c-948d-3c247bc01716', '49261c09-f797-49c4-bf32-f451b28b91de', 'counter-strike-2', 'Counter-Strike 2', '最喜欢它纯粹又残酷的博弈感，每一颗道具、每一次peek都要为团队节奏负责。', true, 0, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobbies VALUES ('fd728644-1ac0-458a-8068-1be7ba052989', '49261c09-f797-49c4-bf32-f451b28b91de', 'apex', 'Apex 英雄', '机动性和临场决策很迷人，打赢一波混战时会有非常强的爽感和节奏感。', true, 1, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobbies VALUES ('779141fa-5ee2-4fa8-8a12-15eec28b5fb0', '49261c09-f797-49c4-bf32-f451b28b91de', 'delta-force', '三角洲行动', '偏战术、偏压迫的枪线体验，适合认真研究路线、信息和团队配合。', true, 2, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobbies VALUES ('3ac24a21-219b-43af-899d-c9a43a3eeff1', '49261c09-f797-49c4-bf32-f451b28b91de', 'valorant', '无畏契约', '技能和枪法互相牵制，回合制的紧张感很足，残局尤其容易让人上头。', true, 3, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobbies VALUES ('ca898ed8-a199-4239-b8c8-8e98d1dd89a2', '49261c09-f797-49c4-bf32-f451b28b91de', 'overwatch-2', '守望先锋 2', '英雄机制和团战节奏变化很快，最吸引我的是团队位置和技能交换。', true, 4, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobbies VALUES ('b2e54f14-10c4-4fe3-9cd2-39c1537dece8', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 'counter-strike-2', 'Counter-Strike 2', '最喜欢它纯粹又残酷的博弈感，每一颗道具、每一次peek都要为团队节奏负责。', true, 0, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobbies VALUES ('2c08d892-cf7f-407f-ac9f-2e4300666dd8', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 'apex', 'Apex 英雄', '机动性和临场决策很迷人，打赢一波混战时会有非常强的爽感和节奏感。', true, 1, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobbies VALUES ('b986961e-bcb3-424f-9ca6-ff9155a787fa', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 'delta-force', '三角洲行动', '偏战术、偏压迫的枪线体验，适合认真研究路线、信息和团队配合。', true, 2, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobbies VALUES ('6e54527d-f5ca-4c68-ba3c-c701d360b7f3', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 'valorant', '无畏契约', '技能和枪法互相牵制，回合制的紧张感很足，残局尤其容易让人上头。', true, 3, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobbies VALUES ('14029a88-8894-484d-af55-2d5b47cfdb6d', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 'overwatch-2', '守望先锋 2', '英雄机制和团战节奏变化很快，最吸引我的是团队位置和技能交换。', true, 4, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobbies VALUES ('66c04d7e-1b84-443f-a97c-2d8fdda14499', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 'counter-strike-2', 'Counter-Strike 2', '最喜欢它纯粹又残酷的博弈感，每一颗道具、每一次peek都要为团队节奏负责。', true, 0, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobbies VALUES ('77c581f9-9449-475f-b223-1b5d0e849dbc', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 'apex', 'Apex 英雄', '机动性和临场决策很迷人，打赢一波混战时会有非常强的爽感和节奏感。', true, 1, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobbies VALUES ('088842c0-b2e9-450f-81f4-e6d91b6c48a8', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 'delta-force', '三角洲行动', '偏战术、偏压迫的枪线体验，适合认真研究路线、信息和团队配合。', true, 2, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobbies VALUES ('ae0ac894-365e-475d-944d-4e546c1f1e71', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 'valorant', '无畏契约', '技能和枪法互相牵制，回合制的紧张感很足，残局尤其容易让人上头。', true, 3, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobbies VALUES ('be0f941a-ce46-42f9-adce-96e2b060baf8', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 'overwatch-2', '守望先锋 2', '英雄机制和团战节奏变化很快，最吸引我的是团队位置和技能交换。', true, 4, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobbies VALUES ('ae4d44c3-c201-4d5d-8a10-d5d42728d6ce', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 'counter-strike-2', 'Counter-Strike 2', '最喜欢它纯粹又残酷的博弈感，每一颗道具、每一次peek都要为团队节奏负责。', true, 0, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobbies VALUES ('c84b0dde-ff64-4b35-926a-3bb10cbda089', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 'apex', 'Apex 英雄', '机动性和临场决策很迷人，打赢一波混战时会有非常强的爽感和节奏感。', true, 1, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobbies VALUES ('38aabda2-be77-4e4b-9850-a3856f975420', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 'delta-force', '三角洲行动', '偏战术、偏压迫的枪线体验，适合认真研究路线、信息和团队配合。', true, 2, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobbies VALUES ('ab512ba8-1811-4b31-af77-c65920b9cdbe', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 'valorant', '无畏契约', '技能和枪法互相牵制，回合制的紧张感很足，残局尤其容易让人上头。', true, 3, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobbies VALUES ('e9b2e75c-4ac4-4a96-8e61-56cd5b39e6c5', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 'overwatch-2', '守望先锋 2', '英雄机制和团战节奏变化很快，最吸引我的是团队位置和技能交换。', true, 4, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobbies VALUES ('89ed76a0-0a70-49c7-9258-953ca051485a', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 'counter-strike-2', 'Counter-Strike 2', '最喜欢它纯粹又残酷的博弈感，每一颗道具、每一次peek都要为团队节奏负责。', true, 0, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobbies VALUES ('b30c6b38-b0c4-49be-8739-55400dd8cfdf', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 'apex', 'Apex 英雄', '机动性和临场决策很迷人，打赢一波混战时会有非常强的爽感和节奏感。', true, 1, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobbies VALUES ('09357493-e3d9-4e3b-9bed-04465c1e8831', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 'delta-force', '三角洲行动', '偏战术、偏压迫的枪线体验，适合认真研究路线、信息和团队配合。', true, 2, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobbies VALUES ('df9b07ca-a0d9-4dad-9d2b-46b172a72818', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 'valorant', '无畏契约', '技能和枪法互相牵制，回合制的紧张感很足，残局尤其容易让人上头。', true, 3, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobbies VALUES ('1a5fc5fb-1e10-4550-b3a3-a29962276f9f', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 'overwatch-2', '守望先锋 2', '英雄机制和团战节奏变化很快，最吸引我的是团队位置和技能交换。', true, 4, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobbies VALUES ('00c17013-7fa7-4906-aceb-9a7dee680a06', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 'counter-strike-2', 'Counter-Strike 2', '最喜欢它纯粹又残酷的博弈感，每一颗道具、每一次peek都要为团队节奏负责。', true, 0, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobbies VALUES ('26172ff2-f553-4cc1-aea2-0a31fe919fd1', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 'apex', 'Apex 英雄', '机动性和临场决策很迷人，打赢一波混战时会有非常强的爽感和节奏感。', true, 1, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobbies VALUES ('ad8badd8-073b-4ca0-a279-583e909de94b', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 'delta-force', '三角洲行动', '偏战术、偏压迫的枪线体验，适合认真研究路线、信息和团队配合。', true, 2, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobbies VALUES ('a75855c9-3f28-4e8e-97ff-9360140881a9', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 'valorant', '无畏契约', '技能和枪法互相牵制，回合制的紧张感很足，残局尤其容易让人上头。', true, 3, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobbies VALUES ('140a73b3-3246-4f4f-92f2-e5e8d587e2d4', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 'overwatch-2', '守望先锋 2', '英雄机制和团战节奏变化很快，最吸引我的是团队位置和技能交换。', true, 4, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobbies VALUES ('fd60fe25-48ad-4d53-b977-e75476e0ec96', '75f9fde8-300c-4616-ad83-aebd9b051891', 'counter-strike-2', 'Counter-Strike 2', '最喜欢它纯粹又残酷的博弈感，每一颗道具、每一次peek都要为团队节奏负责。', true, 0, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobbies VALUES ('17c62178-fa10-408d-aa2d-34243376e0a2', '75f9fde8-300c-4616-ad83-aebd9b051891', 'apex', 'Apex 英雄', '机动性和临场决策很迷人，打赢一波混战时会有非常强的爽感和节奏感。', true, 1, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobbies VALUES ('f545baa1-e514-4449-9722-4371a49dcd88', '75f9fde8-300c-4616-ad83-aebd9b051891', 'delta-force', '三角洲行动', '偏战术、偏压迫的枪线体验，适合认真研究路线、信息和团队配合。', true, 2, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobbies VALUES ('4f12cd1f-a9c5-47b2-8373-45bd5d30599a', '75f9fde8-300c-4616-ad83-aebd9b051891', 'valorant', '无畏契约', '技能和枪法互相牵制，回合制的紧张感很足，残局尤其容易让人上头。', true, 3, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobbies VALUES ('fe8207a2-fcdd-4d00-ae9f-e8f6873df881', '75f9fde8-300c-4616-ad83-aebd9b051891', 'overwatch-2', '守望先锋 2', '英雄机制和团战节奏变化很快，最吸引我的是团队位置和技能交换。', true, 4, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobbies VALUES ('218f4cd8-d11f-4ac1-b4a3-968df5d1f268', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 'counter-strike-2', 'Counter-Strike 2', '最喜欢它纯粹又残酷的博弈感，每一颗道具、每一次peek都要为团队节奏负责。', true, 0, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobbies VALUES ('f46e7bcf-554d-4827-9c2a-d31be3303907', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 'apex', 'Apex 英雄', '机动性和临场决策很迷人，打赢一波混战时会有非常强的爽感和节奏感。', true, 1, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobbies VALUES ('2abae103-ebb9-4cca-ae54-4cb442e43dc9', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 'delta-force', '三角洲行动', '偏战术、偏压迫的枪线体验，适合认真研究路线、信息和团队配合。', true, 2, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobbies VALUES ('f41f11b0-622c-49f8-9736-6c38352720cd', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 'valorant', '无畏契约', '技能和枪法互相牵制，回合制的紧张感很足，残局尤其容易让人上头。', true, 3, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobbies VALUES ('98b0589d-22f3-4ad7-b5da-23a360b8f0e3', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 'overwatch-2', '守望先锋 2', '英雄机制和团战节奏变化很快，最吸引我的是团队位置和技能交换。', true, 4, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobbies VALUES ('8d7dafbb-907f-487f-84a2-64c2c699b51c', 'be28904d-fd8a-4422-9163-9c2014be29f6', 'counter-strike-2', 'Counter-Strike 2', '最喜欢它纯粹又残酷的博弈感，每一颗道具、每一次peek都要为团队节奏负责。', true, 0, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobbies VALUES ('a550eaef-8ee5-49ca-a28e-bc539e77eb51', 'be28904d-fd8a-4422-9163-9c2014be29f6', 'apex', 'Apex 英雄', '机动性和临场决策很迷人，打赢一波混战时会有非常强的爽感和节奏感。', true, 1, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobbies VALUES ('e07883d0-4c7f-4178-9acb-76de3a5c53ce', 'be28904d-fd8a-4422-9163-9c2014be29f6', 'delta-force', '三角洲行动', '偏战术、偏压迫的枪线体验，适合认真研究路线、信息和团队配合。', true, 2, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobbies VALUES ('fc69ec3d-4771-4903-a901-66cd0d9b9abc', 'be28904d-fd8a-4422-9163-9c2014be29f6', 'valorant', '无畏契约', '技能和枪法互相牵制，回合制的紧张感很足，残局尤其容易让人上头。', true, 3, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobbies VALUES ('7e372c4b-a4a0-445d-945c-13430de063b4', 'be28904d-fd8a-4422-9163-9c2014be29f6', 'overwatch-2', '守望先锋 2', '英雄机制和团战节奏变化很快，最吸引我的是团队位置和技能交换。', true, 4, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobbies VALUES ('1703d30b-161c-4deb-9bcc-b96b50a0252c', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 'counter-strike-2', 'Travel', '与其说是旅行，不如说是生活。自然观光，爬山看海，city walk，美食探店等等，这些构成了我对美好生活的期待。', true, 0, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobbies VALUES ('3d61acc1-dcc0-4251-aba3-a9086824a6e7', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 'apex', 'Coding', '静下心专注的做某些事情能让我感到充实，想做些自己感兴趣的项目，想做些让生活更便利更美好的东西', true, 1, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobbies VALUES ('af3aa270-619e-4e90-9f04-d3bb75c32251', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 'delta-force', 'Sport', '爬山，骑车，在市区里走上一整天，有时偶尔健身', true, 2, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobbies VALUES ('5f06fd71-ef4a-440d-8270-c55f523bf658', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 'valorant', 'Game', '从小学三年级一直延续到现在，游戏是我曾经最大的爱好，现在希望能探索更多有趣的事情', true, 3, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobbies VALUES ('63d0a98f-2239-4190-b6d7-c16ad31b0759', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 'overwatch-2', 'Social or Family', '家人和朋友，是构成我以上所有爱好的基础，我会珍惜每一次相遇', true, 4, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobbies VALUES ('fbea37b7-5434-4b99-9609-84d63ed16527', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 'counter-strike-2', 'Travel', '与其说是旅行，不如说是生活。自然观光，爬山看海，city walk，美食探店等等，这些构成了我对美好生活的期待。', true, 0, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobbies VALUES ('d8ff7eee-dee3-4652-9935-5937c91fb7eb', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 'apex', 'Coding', '静下心专注的做某些事情能让我感到充实，想做些自己感兴趣的项目，想做些让生活更便利更美好', true, 1, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobbies VALUES ('fc4d90c1-e34e-46a1-934c-bd8c96c419dc', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 'delta-force', 'Sport', '爬山看风景，骑车兜下风，或者在市区里走上一整天', true, 2, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobbies VALUES ('50622acb-e1e5-42cb-805d-840da6894b5f', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 'valorant', 'Game', '从小学三年级一直延续到现在，游戏的是我曾经最大的爱好', true, 3, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobbies VALUES ('dd1ee6ed-67ba-422c-827d-b73ef533ed8d', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 'overwatch-2', 'Social or Family', '家人和朋友，是构成我以上所有爱好的基础，我会珍惜每一次相遇', true, 4, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobbies VALUES ('ed063d57-10a7-5a14-92a3-78d399de588d', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 'counter-strike-2', 'Counter-Strike 2', '最喜欢它纯粹又残酷的博弈感，每一颗道具、每一次peek都要为团队节奏负责。', true, 0, '2026-08-10 00:45:04.421277+08', '2026-08-10 00:45:04.421277+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobbies VALUES ('40e83876-0823-502e-b61c-efff4a881e31', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 'apex', 'Apex 英雄', '机动性和临场决策很迷人，打赢一波混战时会有非常强的爽感和节奏感。', true, 1, '2026-08-10 00:45:04.421277+08', '2026-08-10 00:45:04.421277+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobbies VALUES ('acacf50a-4210-5420-8479-1449f50d3be8', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 'delta-force', '三角洲行动', '偏战术、偏压迫的枪线体验，适合认真研究路线、信息和团队配合。', true, 2, '2026-08-10 00:45:04.421277+08', '2026-08-10 00:45:04.421277+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobbies VALUES ('0aaa8322-990a-5b1b-b3e9-9c16807956c8', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 'valorant', '无畏契约', '技能和枪法互相牵制，回合制的紧张感很足，残局尤其容易让人上头。', true, 3, '2026-08-10 00:45:04.421277+08', '2026-08-10 00:45:04.421277+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobbies VALUES ('1f22b966-2f3e-5207-b84a-1002d194e514', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 'overwatch-2', '守望先锋 2', '英雄机制和团战节奏变化很快，最吸引我的是团队位置和技能交换。', true, 4, '2026-08-10 00:45:04.421277+08', '2026-08-10 00:45:04.421277+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobbies VALUES ('3dc48f56-c0cb-48d3-9d73-229261a9bc82', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 'counter-strike-2', 'Travel', '与其说是旅行，不如说是生活。自然观光，爬山看海，city walk，美食探店等等，这些构成了我对美好生活的期待。', true, 0, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobbies VALUES ('7a649d78-4c66-4347-b93a-7e548601b5a0', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 'apex', 'Coding', '静下心专注的做某些事情能让我感到充实，想做些自己感兴趣的项目，想做些让生活更便利更美好', true, 1, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobbies VALUES ('58e363d4-f310-4b5b-b6df-43614bf247c9', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 'delta-force', 'Sport', '爬山看风景，骑车兜下风，或者在市区里走上一整天', true, 2, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobbies VALUES ('a508a848-078e-43cb-8bca-dd6517d55fd6', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 'valorant', 'Game', '从小学三年级一直延续到现在，游戏的是我曾经最大的爱好', true, 3, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobbies VALUES ('fa251f24-f459-4988-bde1-02bf57a7eee0', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 'overwatch-2', 'Social or Family', '家人和朋友，是构成我以上所有爱好的基础，我会珍惜每一次相遇', true, 4, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobbies VALUES ('71bb1344-2c54-45b0-a9a7-10b0eb48600e', '81210424-f77d-4f36-97d8-892ebef7b8ac', 'counter-strike-2', 'Travel', '与其说是旅行，不如说是生活。自然观光，爬山看海，city walk，美食探店等等，这些构成了我对美好生活的期待。', true, 0, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobbies VALUES ('5e6277aa-d50b-45f1-95dc-c3b20705bdd3', '81210424-f77d-4f36-97d8-892ebef7b8ac', 'apex', 'Coding', '静下心专注的做某些事情能让我感到充实，想做些自己感兴趣的项目，想做些让生活更便利更美好', true, 1, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobbies VALUES ('3e0d3149-2b36-41bc-b68d-5fa42c61335b', '81210424-f77d-4f36-97d8-892ebef7b8ac', 'delta-force', 'Sport', '爬山看风景，骑车兜下风，或者在市区里走上一整天', true, 2, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobbies VALUES ('e559be5f-7e07-4ce7-b948-3de0a87d647f', '81210424-f77d-4f36-97d8-892ebef7b8ac', 'valorant', 'Game', '从小学三年级一直延续到现在，游戏的是我曾经最大的爱好', true, 3, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobbies VALUES ('5dbcc462-bdfb-43a8-b8ab-cf671d773889', '81210424-f77d-4f36-97d8-892ebef7b8ac', 'overwatch-2', 'Social or Family', '家人和朋友，是构成我以上所有爱好的基础，我会珍惜每一次相遇', true, 4, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobbies VALUES ('c8119e79-e263-4dc8-893c-ed956fc0e4ec', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 'counter-strike-2', 'Travel', '与其说是旅行，不如说是生活。自然观光，爬山看海，city walk，美食探店等等，这些构成了我对美好生活的期待。', true, 0, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobbies VALUES ('ef67eb4a-f9dc-4313-b60a-2ebe519d6066', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 'apex', 'Coding', '静下心专注的做某些事情能让我感到充实，想做些自己感兴趣的项目，想做些让生活更便利更美好', true, 1, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobbies VALUES ('ef8c808f-a659-462b-a84b-75edaf20ccd0', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 'delta-force', 'Sport', '爬山看风景，骑车兜下风，或者在市区里走上一整天', true, 2, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobbies VALUES ('3675a33e-35b8-4cc4-85f7-1da0e11bb2a9', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 'valorant', 'Game', '从小学三年级一直延续到现在，游戏的是我曾经最大的爱好', true, 3, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobbies VALUES ('ccfbbbc4-5eea-433b-a228-7dd842a91679', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 'overwatch-2', 'Social or Family', '家人和朋友，是构成我以上所有爱好的基础，我会珍惜每一次相遇', true, 4, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobbies VALUES ('8fb4b74d-e7e9-41ac-8a1f-d996dd597365', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 'counter-strike-2', 'Travel', '与其说是旅行，不如说是生活。自然观光，爬山看海，city walk，美食探店等等，这些构成了我对美好生活的期待。', true, 0, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobbies VALUES ('86e9982a-f643-474c-878d-0f47e24a4d90', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 'apex', 'Coding', '静下心专注的做某些事情能让我感到充实，想做些自己感兴趣的项目，想做些让生活更便利更美好', true, 1, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobbies VALUES ('cf52b070-38cd-45fc-bc14-9d80b607205c', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 'delta-force', 'Sport', '爬山看风景，骑车兜下风，或者在市区里走上一整天', true, 2, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobbies VALUES ('fff77bf7-3bb8-4a89-a9f0-69111215155a', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 'valorant', 'Game', '从小学三年级一直延续到现在，游戏的是我曾经最大的爱好', true, 3, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobbies VALUES ('4fc31e57-0d40-46e0-add7-604a46a4ad8e', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 'overwatch-2', 'Social or Family', '家人和朋友，是构成我以上所有爱好的基础，我会珍惜每一次相遇', true, 4, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');


-- Data for Name: hobby_resources; Type: TABLE DATA; Schema: public; Owner: -

INSERT INTO public.hobby_resources VALUES ('e8945252-7e24-49eb-95e7-d2f303ad95c7', '229596d8-38dc-4de5-827c-605cbfa7d72c', '5cab4584-dad0-5c12-9d39-1b9352dfd7a7', '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_resources VALUES ('ec4d83b0-d832-444e-95d7-be0a9ef2cc73', 'd78fffe9-e8e0-44bf-a110-458647d09c5c', '1a26d874-bd0f-5808-acc3-4a6c963ce6e4', '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_resources VALUES ('425ea326-074c-4828-baeb-0cc216837f08', '91a4ee01-4e68-41ce-9360-e42b8d8556b0', '8fc83aca-e31e-5a65-9195-44584997c8df', '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_resources VALUES ('02686f55-54e9-4381-bda2-0739df42c8b1', '167fbd87-691a-4fc1-8302-bc946977d811', 'd898586e-5aa7-5a47-862a-698aedd0d287', '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_resources VALUES ('4ec62fdd-0c11-4a2f-88af-e4aaf4b8bf67', '93e36d11-f99a-4d3c-8229-1c78e16c02fd', '81eedeaa-b0df-5ead-a804-f8bea0560100', '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_resources VALUES ('d51ae829-e61e-4dcd-8799-9b9e5c9a9fef', '635c811b-d382-4e3c-948d-3c247bc01716', '5cab4584-dad0-5c12-9d39-1b9352dfd7a7', '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_resources VALUES ('7e39b3c1-009e-4a12-a6e9-4ea3c0c0e590', 'fd728644-1ac0-458a-8068-1be7ba052989', '1a26d874-bd0f-5808-acc3-4a6c963ce6e4', '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_resources VALUES ('31e2facb-bf95-41d8-92f6-dd3796dabd6b', '779141fa-5ee2-4fa8-8a12-15eec28b5fb0', '8fc83aca-e31e-5a65-9195-44584997c8df', '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_resources VALUES ('7c252ade-b15e-4e36-a12e-50bb33829817', '3ac24a21-219b-43af-899d-c9a43a3eeff1', 'd898586e-5aa7-5a47-862a-698aedd0d287', '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_resources VALUES ('a2121b86-7d3e-4c8d-ae87-02c78d75cddb', 'ca898ed8-a199-4239-b8c8-8e98d1dd89a2', '81eedeaa-b0df-5ead-a804-f8bea0560100', '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_resources VALUES ('808d0f96-6831-4d08-b228-dc955e1c024c', 'b2e54f14-10c4-4fe3-9cd2-39c1537dece8', '5cab4584-dad0-5c12-9d39-1b9352dfd7a7', '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_resources VALUES ('b3e8de4d-6247-4ac0-977c-c110d2b746dc', '2c08d892-cf7f-407f-ac9f-2e4300666dd8', '1a26d874-bd0f-5808-acc3-4a6c963ce6e4', '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_resources VALUES ('fca8b9ff-4da1-4089-b1c5-6c07c19a9f44', 'b986961e-bcb3-424f-9ca6-ff9155a787fa', '8fc83aca-e31e-5a65-9195-44584997c8df', '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_resources VALUES ('168b168e-d69b-4664-9e10-0ebb65e00c3e', '6e54527d-f5ca-4c68-ba3c-c701d360b7f3', 'd898586e-5aa7-5a47-862a-698aedd0d287', '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_resources VALUES ('fb340744-b697-46c3-a257-1313fa3e2bb0', '14029a88-8894-484d-af55-2d5b47cfdb6d', '81eedeaa-b0df-5ead-a804-f8bea0560100', '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_resources VALUES ('674c068e-aa57-4c40-a607-0be98795db9a', '66c04d7e-1b84-443f-a97c-2d8fdda14499', '5cab4584-dad0-5c12-9d39-1b9352dfd7a7', '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_resources VALUES ('f8f4343f-3758-4e63-82bb-9ed02b04737b', '77c581f9-9449-475f-b223-1b5d0e849dbc', '1a26d874-bd0f-5808-acc3-4a6c963ce6e4', '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_resources VALUES ('284c0716-4628-46fc-88a4-25197a11c46d', '088842c0-b2e9-450f-81f4-e6d91b6c48a8', '8fc83aca-e31e-5a65-9195-44584997c8df', '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_resources VALUES ('535440ed-0a53-439b-a27c-369ab4171707', 'ae0ac894-365e-475d-944d-4e546c1f1e71', 'd898586e-5aa7-5a47-862a-698aedd0d287', '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_resources VALUES ('6678a078-ba8a-42ea-aee9-51e243159ee1', 'be0f941a-ce46-42f9-adce-96e2b060baf8', '81eedeaa-b0df-5ead-a804-f8bea0560100', '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_resources VALUES ('b103a3c9-2746-44b4-b445-5e3f872f23be', 'ae4d44c3-c201-4d5d-8a10-d5d42728d6ce', '5cab4584-dad0-5c12-9d39-1b9352dfd7a7', '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_resources VALUES ('90d08721-9b5a-46c4-a2a2-983634445150', 'c84b0dde-ff64-4b35-926a-3bb10cbda089', '1a26d874-bd0f-5808-acc3-4a6c963ce6e4', '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_resources VALUES ('b3a54d41-8bc7-40a1-a298-fd47c5ea368f', '38aabda2-be77-4e4b-9850-a3856f975420', '8fc83aca-e31e-5a65-9195-44584997c8df', '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_resources VALUES ('b3914fa0-c957-4b6d-9b95-7035213fd4fb', 'ab512ba8-1811-4b31-af77-c65920b9cdbe', 'd898586e-5aa7-5a47-862a-698aedd0d287', '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_resources VALUES ('58e27f37-17fb-43c3-909c-a0c7a825636d', 'e9b2e75c-4ac4-4a96-8e61-56cd5b39e6c5', '81eedeaa-b0df-5ead-a804-f8bea0560100', '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_resources VALUES ('406cb7aa-5e0c-4f74-97fa-6f36dd396bdb', '89ed76a0-0a70-49c7-9258-953ca051485a', '5cab4584-dad0-5c12-9d39-1b9352dfd7a7', '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_resources VALUES ('89f197d2-5192-4715-860a-de4bc28d0b7b', 'b30c6b38-b0c4-49be-8739-55400dd8cfdf', '1a26d874-bd0f-5808-acc3-4a6c963ce6e4', '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_resources VALUES ('d85ac316-f91f-4b29-8b83-f44d6a2eb121', '09357493-e3d9-4e3b-9bed-04465c1e8831', '8fc83aca-e31e-5a65-9195-44584997c8df', '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_resources VALUES ('839501fb-67d0-448c-8c1d-362ebdd42800', 'df9b07ca-a0d9-4dad-9d2b-46b172a72818', 'd898586e-5aa7-5a47-862a-698aedd0d287', '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_resources VALUES ('572a2fab-70bd-45d9-a807-8da6b803e7ad', '1a5fc5fb-1e10-4550-b3a3-a29962276f9f', '81eedeaa-b0df-5ead-a804-f8bea0560100', '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_resources VALUES ('bf634be5-4dc0-4868-a63f-816b07245cc2', '00c17013-7fa7-4906-aceb-9a7dee680a06', '5cab4584-dad0-5c12-9d39-1b9352dfd7a7', '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_resources VALUES ('47d666f2-bd94-43d9-8454-c2dda2625d4d', '26172ff2-f553-4cc1-aea2-0a31fe919fd1', '1a26d874-bd0f-5808-acc3-4a6c963ce6e4', '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_resources VALUES ('596c44ac-cd95-4a58-8af5-eda1cd2c8c83', 'ad8badd8-073b-4ca0-a279-583e909de94b', '8fc83aca-e31e-5a65-9195-44584997c8df', '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_resources VALUES ('451c9d93-fea8-4c5c-b361-86987e70c7f3', 'a75855c9-3f28-4e8e-97ff-9360140881a9', 'd898586e-5aa7-5a47-862a-698aedd0d287', '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_resources VALUES ('00c915da-1e70-4b2b-8267-3743c3b064b5', '140a73b3-3246-4f4f-92f2-e5e8d587e2d4', '81eedeaa-b0df-5ead-a804-f8bea0560100', '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_resources VALUES ('c65587fb-5526-4cf0-88a7-844d3766e8e1', 'fd60fe25-48ad-4d53-b977-e75476e0ec96', '5cab4584-dad0-5c12-9d39-1b9352dfd7a7', '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_resources VALUES ('a2fb658a-0e0e-4153-8dc8-640136d553d8', '17c62178-fa10-408d-aa2d-34243376e0a2', '1a26d874-bd0f-5808-acc3-4a6c963ce6e4', '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_resources VALUES ('ac9fd26e-e9cd-46f8-94c6-4b3814eebd05', 'f545baa1-e514-4449-9722-4371a49dcd88', '8fc83aca-e31e-5a65-9195-44584997c8df', '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_resources VALUES ('76af4e11-042a-411e-958f-4307a9b10e23', '4f12cd1f-a9c5-47b2-8373-45bd5d30599a', 'd898586e-5aa7-5a47-862a-698aedd0d287', '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_resources VALUES ('81c06580-d466-445f-b717-d35eeb0b560a', 'fe8207a2-fcdd-4d00-ae9f-e8f6873df881', '81eedeaa-b0df-5ead-a804-f8bea0560100', '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_resources VALUES ('157e872a-d3d4-4f02-8be9-3ed66af16fef', '218f4cd8-d11f-4ac1-b4a3-968df5d1f268', '5cab4584-dad0-5c12-9d39-1b9352dfd7a7', '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_resources VALUES ('52514034-7b2c-4835-b59c-2bf4d527e83c', 'f46e7bcf-554d-4827-9c2a-d31be3303907', '1a26d874-bd0f-5808-acc3-4a6c963ce6e4', '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_resources VALUES ('eb127ae3-c66f-4055-963d-aa8e02047bc5', '2abae103-ebb9-4cca-ae54-4cb442e43dc9', '8fc83aca-e31e-5a65-9195-44584997c8df', '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_resources VALUES ('761fbb5f-b6e2-47d2-957c-5de87624243b', 'f41f11b0-622c-49f8-9736-6c38352720cd', 'd898586e-5aa7-5a47-862a-698aedd0d287', '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_resources VALUES ('58d005b2-c7ba-4920-bff5-227079995599', '98b0589d-22f3-4ad7-b5da-23a360b8f0e3', '81eedeaa-b0df-5ead-a804-f8bea0560100', '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_resources VALUES ('ca380535-b1ba-470a-af07-14685ae8df6c', '8d7dafbb-907f-487f-84a2-64c2c699b51c', '5cab4584-dad0-5c12-9d39-1b9352dfd7a7', '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_resources VALUES ('33aeeb1d-8d3d-472a-a2b5-9e83dbe46858', 'a550eaef-8ee5-49ca-a28e-bc539e77eb51', '1a26d874-bd0f-5808-acc3-4a6c963ce6e4', '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_resources VALUES ('d3008028-c832-4b01-b273-1a4bf72ace6d', 'e07883d0-4c7f-4178-9acb-76de3a5c53ce', '8fc83aca-e31e-5a65-9195-44584997c8df', '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_resources VALUES ('de8f7689-fe5e-4d3a-a4a8-fbede759c7cc', 'fc69ec3d-4771-4903-a901-66cd0d9b9abc', 'd898586e-5aa7-5a47-862a-698aedd0d287', '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_resources VALUES ('0aac7eb5-bdf5-4421-8a12-85cb4cb523a4', '7e372c4b-a4a0-445d-945c-13430de063b4', '81eedeaa-b0df-5ead-a804-f8bea0560100', '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_resources VALUES ('af511e13-91b7-5738-97ed-c74ba5aaa4bf', 'ed063d57-10a7-5a14-92a3-78d399de588d', '5cab4584-dad0-5c12-9d39-1b9352dfd7a7', '2026-08-10 00:45:04.425137+08', '2026-08-10 00:45:04.425137+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_resources VALUES ('96cfb0dc-08c5-5f1c-8c38-2eca9eda736c', '40e83876-0823-502e-b61c-efff4a881e31', '1a26d874-bd0f-5808-acc3-4a6c963ce6e4', '2026-08-10 00:45:04.425137+08', '2026-08-10 00:45:04.425137+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_resources VALUES ('dea70e2b-780f-5f00-8afb-3f58754d16e9', 'acacf50a-4210-5420-8479-1449f50d3be8', '8fc83aca-e31e-5a65-9195-44584997c8df', '2026-08-10 00:45:04.425137+08', '2026-08-10 00:45:04.425137+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_resources VALUES ('b3af7d63-8ac9-5d34-8b80-26634c470a79', '0aaa8322-990a-5b1b-b3e9-9c16807956c8', 'd898586e-5aa7-5a47-862a-698aedd0d287', '2026-08-10 00:45:04.425137+08', '2026-08-10 00:45:04.425137+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_resources VALUES ('3b26a18f-e227-5849-a600-4b47f19d943e', '1f22b966-2f3e-5207-b84a-1002d194e514', '81eedeaa-b0df-5ead-a804-f8bea0560100', '2026-08-10 00:45:04.425137+08', '2026-08-10 00:45:04.425137+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_resources VALUES ('22350534-ab97-4db8-aa4d-bde2fac34f03', '3dc48f56-c0cb-48d3-9d73-229261a9bc82', '75af93fa-1b78-43da-8e11-e1e4d39f2603', '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_resources VALUES ('3f4a4ee1-a45c-4866-9559-7487bf2b15af', '7a649d78-4c66-4347-b93a-7e548601b5a0', '61078cfe-24dc-4904-9c87-912d774bca62', '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_resources VALUES ('e5b095d9-dd84-4d1d-a9be-e636b473140a', '58e363d4-f310-4b5b-b6df-43614bf247c9', 'e94c3f43-ff43-4590-85a6-8e5b9a76ccca', '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_resources VALUES ('2b30214a-5157-40c5-9a16-ea3175fcdfec', 'a508a848-078e-43cb-8bca-dd6517d55fd6', '232ea6d4-b90b-48b0-8a6f-d1e7f1085933', '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_resources VALUES ('9dbf1dd1-01b0-46bf-b924-41a2ef4de924', 'fa251f24-f459-4988-bde1-02bf57a7eee0', '73ddaea2-c208-420a-a5a7-faca1d9e64c1', '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_resources VALUES ('449bc201-3507-45b1-966c-23111c805128', '1703d30b-161c-4deb-9bcc-b96b50a0252c', '3fbc5a0d-4b21-4b18-bcab-8e7772569203', '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_resources VALUES ('840cc2e9-5db7-4032-80db-ef552e53cdb1', '3d61acc1-dcc0-4251-aba3-a9086824a6e7', '61078cfe-24dc-4904-9c87-912d774bca62', '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_resources VALUES ('f5171972-187f-49c5-a079-e21ee5e3f307', 'af3aa270-619e-4e90-9f04-d3bb75c32251', '0064fca2-da7d-49f4-935e-5dbd66a9e45a', '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_resources VALUES ('51a09404-b899-4aa3-98cf-b206f4a0ccc1', '5f06fd71-ef4a-440d-8270-c55f523bf658', '5cab4584-dad0-5c12-9d39-1b9352dfd7a7', '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_resources VALUES ('ca4abb73-f539-4f90-8729-e186394b9e11', 'fbea37b7-5434-4b99-9609-84d63ed16527', '3fbc5a0d-4b21-4b18-bcab-8e7772569203', '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_resources VALUES ('fe953ce3-906d-49f2-848d-0dd14fff5bee', 'd8ff7eee-dee3-4652-9935-5937c91fb7eb', '61078cfe-24dc-4904-9c87-912d774bca62', '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_resources VALUES ('029706d4-9fef-4abe-8120-1a4fac09b989', 'fc4d90c1-e34e-46a1-934c-bd8c96c419dc', '0064fca2-da7d-49f4-935e-5dbd66a9e45a', '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_resources VALUES ('ac973d31-d57b-4e8b-ac1a-62f657597646', '50622acb-e1e5-42cb-805d-840da6894b5f', '5cab4584-dad0-5c12-9d39-1b9352dfd7a7', '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_resources VALUES ('4efe0d14-08ec-4ee6-ae99-7ccb1186b179', 'dd1ee6ed-67ba-422c-827d-b73ef533ed8d', '268a932a-8791-4337-8455-b0691231f76f', '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_resources VALUES ('aff1aa09-f954-4148-91e2-f4638475e7d0', 'c8119e79-e263-4dc8-893c-ed956fc0e4ec', '3fbc5a0d-4b21-4b18-bcab-8e7772569203', '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_resources VALUES ('3e851b74-621f-4635-9695-fe6db6e5829a', 'ef67eb4a-f9dc-4313-b60a-2ebe519d6066', '61078cfe-24dc-4904-9c87-912d774bca62', '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_resources VALUES ('53101012-eb20-4da5-bb73-7c1b25d433a8', 'ef8c808f-a659-462b-a84b-75edaf20ccd0', '0064fca2-da7d-49f4-935e-5dbd66a9e45a', '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_resources VALUES ('c948629a-0432-4e96-8304-e97a00255341', '3675a33e-35b8-4cc4-85f7-1da0e11bb2a9', '5cab4584-dad0-5c12-9d39-1b9352dfd7a7', '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_resources VALUES ('464398c4-6fe0-4d86-b824-475a1ce30b38', '8fb4b74d-e7e9-41ac-8a1f-d996dd597365', '3fbc5a0d-4b21-4b18-bcab-8e7772569203', '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_resources VALUES ('c8899455-1759-4f7f-bd60-07ea8022a539', '86e9982a-f643-474c-878d-0f47e24a4d90', '61078cfe-24dc-4904-9c87-912d774bca62', '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_resources VALUES ('22ea0f83-b13c-447a-8e7a-c3e5a7ad2b77', 'cf52b070-38cd-45fc-bc14-9d80b607205c', '0064fca2-da7d-49f4-935e-5dbd66a9e45a', '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_resources VALUES ('6c63a89c-359a-4405-afe5-6a48d9c03338', 'fff77bf7-3bb8-4a89-a9f0-69111215155a', '5cab4584-dad0-5c12-9d39-1b9352dfd7a7', '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_resources VALUES ('fcc7514c-83dd-4376-aa60-02ba54078431', '71bb1344-2c54-45b0-a9a7-10b0eb48600e', '3fbc5a0d-4b21-4b18-bcab-8e7772569203', '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_resources VALUES ('4187c0dd-8c33-4d38-b178-619808041e4c', '5e6277aa-d50b-45f1-95dc-c3b20705bdd3', '61078cfe-24dc-4904-9c87-912d774bca62', '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_resources VALUES ('eaf312d7-7c27-485e-9b94-4ae0b46072f1', '3e0d3149-2b36-41bc-b68d-5fa42c61335b', '0064fca2-da7d-49f4-935e-5dbd66a9e45a', '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_resources VALUES ('337b939c-b5d8-441d-ba43-3a71d34ed81d', 'e559be5f-7e07-4ce7-b948-3de0a87d647f', '5cab4584-dad0-5c12-9d39-1b9352dfd7a7', '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_resources VALUES ('854dedd9-28cf-4356-88c8-eb076df7c332', '5dbcc462-bdfb-43a8-b8ab-cf671d773889', '4c892724-9106-4acf-a5b1-01f4e6ec9fa7', '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_resources VALUES ('2bb6c048-caa8-4d48-84b2-894ca2d7e37a', '63d0a98f-2239-4190-b6d7-c16ad31b0759', '268a932a-8791-4337-8455-b0691231f76f', '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_resources VALUES ('914b6ee3-f86f-4f61-9800-cdc210aa6b2d', 'ccfbbbc4-5eea-433b-a228-7dd842a91679', '268a932a-8791-4337-8455-b0691231f76f', '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_resources VALUES ('89cd7637-fb8d-45bd-9aaf-5635e5903065', '4fc31e57-0d40-46e0-add7-604a46a4ad8e', '73ddaea2-c208-420a-a5a7-faca1d9e64c1', '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');


-- Data for Name: hobby_time_points; Type: TABLE DATA; Schema: public; Owner: -

INSERT INTO public.hobby_time_points VALUES ('ed8fe03c-6f7a-49e0-aba0-0a5e1a0f4fa6', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', -1, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('905386ee-0312-4dad-86d5-129487cfa8de', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 0, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('13bd08cc-dca0-488c-9981-999392ca1d4f', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 1, 1.0, 0.0, 0.0, 0.0, 9.0, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('5d94e3e8-ea2b-4dc0-98a9-f5c3eab1664e', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 2, 2.0, 0.0, 0.0, 0.0, 8.0, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('64820f31-e703-4c13-8963-25c6e930f50d', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 3, 2.4, 0.0, 0.0, 0.0, 7.6, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('b11d0988-18ba-42b9-9941-3b803d57b11e', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 4, 2.6, 0.0, 0.0, 0.0, 7.4, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('ca216cc2-58a4-4629-b817-818fc79b1da6', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 5, 2.8, 0.0, 0.0, 0.0, 7.2, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('eefcffb8-94e6-43ff-b374-0563e3a16d42', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 6, 3.2, 0.0, 0.0, 0.0, 6.8, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('28562c9d-4aed-4f9c-afe6-75a3e1ce0215', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 7, 3.6, 0.0, 0.0, 0.0, 6.4, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('13f87199-cee9-4064-a898-eb0dd56ea94b', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 8, 4.0, 0.0, 0.0, 0.0, 6.0, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('f33b0bfa-12c1-4d95-b19a-b4084a9dec86', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 9, 4.0, 0.0, 1.0, 0.0, 5.0, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('4557de4f-0c9c-4cc5-9135-b014ea2572a8', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 10, 3.8, 0.0, 1.5, 0.0, 4.7, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('e96be817-cd54-4d92-9a40-3f59a31db555', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 11, 3.6, 0.0, 2.0, 0.0, 4.4, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('a8bc9a5b-4134-4131-88b5-36e4c7e14aec', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', -1, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_points VALUES ('6cea87f3-37a6-4373-a102-bcbc3327baf3', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 0, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_points VALUES ('8b5a0d48-621d-427c-895e-85aee39a9ab0', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 1, 0.5, 0.0, 0.0, 0.0, 9.5, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_points VALUES ('f1b1cc26-3799-40bf-832d-9b6d2bbfbc23', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 2, 1.0, 0.0, 0.0, 0.0, 9.0, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_points VALUES ('ec080dfd-b70c-42e6-8948-a0896ac74e5f', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 3, 2.0, 0.0, 0.0, 0.0, 8.0, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_points VALUES ('1ced79fc-558a-444e-a6e1-e04887fd962d', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 4, 3.0, 0.0, 0.0, 0.0, 7.0, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_points VALUES ('faa6af5e-927b-4dce-85ae-f20cc0717ebb', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 5, 4.0, 0.0, 0.0, 0.0, 6.0, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_points VALUES ('cb7e73d7-2551-4dce-be1b-32e9f97899b6', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 6, 4.5, 0.0, 0.0, 0.0, 5.5, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_points VALUES ('fccd44df-03ae-433f-882e-7b14c6fbdeb7', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 7, 5.0, 0.0, 0.0, 0.0, 5.0, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_points VALUES ('766dd1c8-3005-4d11-96c3-ca7e531a4c96', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 8, 4.5, 0.0, 0.0, 0.0, 5.5, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_points VALUES ('378904d2-ecb1-4f4f-8d32-60a8a043a6fd', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 9, 4.0, 0.0, 1.0, 0.0, 5.0, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_points VALUES ('e0481f07-526c-4331-9b0f-206820f3c9cf', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 10, 3.7, 0.0, 1.5, 0.0, 4.8, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_points VALUES ('7890cb4e-56bc-4873-bb93-0fac9c15aded', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 11, 3.4, 0.0, 2.0, 0.0, 4.6, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_points VALUES ('dff96216-abfd-422c-a004-574799ac23fe', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 12, 3.1, 0.0, 2.5, 0.0, 4.4, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_points VALUES ('bcb247da-4b78-48ac-8127-9e25c2f22443', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 13, 2.9, 0.0, 3.0, 0.0, 4.1, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_points VALUES ('b8f258db-6c4f-4ca2-a27e-5aba3305fb50', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 14, 2.7, 0.0, 3.2, 0.0, 4.1, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_points VALUES ('2d2a39fa-9ade-4a6f-9789-4288c2384756', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 15, 2.5, 0.0, 3.4, 0.0, 4.1, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_points VALUES ('48bc5434-90fd-4bce-8bd2-57fa27f09ca7', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 16, 2.3, 0.0, 3.6, 0.0, 4.1, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_points VALUES ('2baba62f-b927-4f43-9403-46895dfa1104', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 17, 2.1, 0.0, 3.9, 0.0, 4.0, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_points VALUES ('2587d60d-577e-497c-8b11-f08fc9fef318', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 18, 2.0, 0.0, 4.1, 0.0, 3.9, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_points VALUES ('663e7613-240d-4beb-ae31-c8493907b236', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 19, 1.9, 0.5, 3.6, 0.5, 3.5, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_points VALUES ('a2f584d3-6d5f-40a1-a05a-ff3b2e0d4e2d', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 20, 1.8, 1.0, 3.2, 0.7, 3.3, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_points VALUES ('18e99ebe-656f-43cc-a6cd-a02530191a7b', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 21, 1.7, 1.5, 2.8, 1.0, 3.0, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_points VALUES ('4dd9f261-9f9c-4a03-b33e-d72a164f730a', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 22, 1.6, 1.5, 2.6, 2.0, 2.3, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_points VALUES ('f4bbfc3e-0b00-4eb7-933e-8ff1f7fc333f', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 23, 1.5, 2.0, 2.2, 3.0, 1.3, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_points VALUES ('b7df1209-7151-464d-a90b-3962cab4ebfb', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 24, 1.4, 2.2, 2.0, 3.0, 1.4, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_points VALUES ('f9ac8864-7b10-408c-b896-e44fea6bb62c', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 25, 1.3, 2.0, 2.0, 3.0, 1.7, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_points VALUES ('a128ba3c-e482-4a13-9502-70ea63cb8624', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 26, 1.2, 2.0, 1.5, 3.5, 1.8, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_points VALUES ('f66621bc-6f49-4e1a-9416-228e37ef8ba5', '46ca6189-6ad5-40a9-a327-aeac604f78c1', -1, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_points VALUES ('a492d6b1-805c-45b0-9a65-420911e83b3c', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 0, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_points VALUES ('0226bc05-d3b9-487a-b8ff-eb7d25d9a0b8', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 1, 0.5, 0.0, 0.0, 0.0, 9.5, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_points VALUES ('6b2bd7e3-cc63-43d8-8576-0af2ed3a97de', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 2, 1.0, 0.0, 0.0, 0.0, 9.0, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_points VALUES ('37df2252-51a9-463b-b867-2be20d98926d', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 3, 2.0, 0.0, 0.0, 0.0, 8.0, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_points VALUES ('a00b4075-be8c-4a24-bbbc-a904cd007240', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 4, 3.0, 0.0, 0.0, 0.0, 7.0, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_points VALUES ('a18ffc5b-2074-4637-bf28-aacf167ab609', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 5, 4.0, 0.0, 0.0, 0.0, 6.0, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_points VALUES ('6afcee7e-b977-40e7-975c-0721e0e4c4c6', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 6, 4.5, 0.0, 0.0, 0.0, 5.5, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_points VALUES ('88391d82-2470-41c0-9bfb-57ca853f6626', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 7, 5.0, 0.0, 0.0, 0.0, 5.0, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_points VALUES ('2c196ef7-0e61-4074-b89f-e294c7f50bd4', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 8, 4.5, 0.0, 0.0, 0.0, 5.5, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_points VALUES ('e642ddc0-bafc-4148-a887-8373263b8cd7', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 9, 4.0, 0.0, 1.0, 0.0, 5.0, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_points VALUES ('3033dbbb-ae73-4761-9cc8-80cdfad48622', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 10, 3.7, 0.0, 1.5, 0.0, 4.8, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_points VALUES ('32d18e9a-437f-4789-959d-3608338a1918', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 11, 3.4, 0.0, 2.0, 0.0, 4.6, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_points VALUES ('93be8c1a-f289-4bf2-99d2-6102a50e938c', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 12, 3.1, 0.0, 2.5, 0.0, 4.4, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_points VALUES ('b93fdcd0-045a-491a-bd50-1b5b3f75e916', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 13, 2.9, 0.0, 3.0, 0.0, 4.1, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_points VALUES ('f30de489-7fe3-40bd-94f2-d651ed9e0f22', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 14, 2.7, 0.0, 3.2, 0.0, 4.1, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_points VALUES ('059f184b-be93-4ff8-9184-dc9298e8ac66', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 15, 2.5, 0.0, 3.4, 0.0, 4.1, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_points VALUES ('1f066e04-2827-4c4c-acb4-22f54e6eada9', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 16, 2.3, 0.0, 3.6, 0.0, 4.1, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_points VALUES ('aaa0257a-45e3-472d-861e-309337eb2ce3', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 17, 2.1, 0.0, 3.9, 0.0, 4.0, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_points VALUES ('96a7c170-0131-47c4-92e4-9a180444711b', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 18, 2.0, 0.0, 4.1, 0.0, 3.9, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_points VALUES ('48ae8600-8b3d-4e62-996f-7dd68ec637fb', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 19, 1.9, 0.5, 3.6, 0.5, 3.5, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_points VALUES ('0d500b6c-2ae7-4daa-9e0c-a3152f88255a', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 20, 1.8, 1.0, 3.2, 0.7, 3.3, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_points VALUES ('0d586f9a-be9f-49ef-acd2-1a9c78abc968', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 21, 1.7, 1.5, 2.8, 1.0, 3.0, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_points VALUES ('31b5416c-f537-4c5d-b34e-555233c02b1b', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 22, 1.6, 1.5, 2.6, 2.0, 2.3, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_points VALUES ('80e86fe0-50fe-4bd7-936f-c0deea720fc6', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 23, 1.5, 2.0, 2.2, 3.0, 1.3, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_points VALUES ('5ff22f98-908f-4e18-9f3a-29243b02f6dd', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 24, 1.4, 2.2, 2.0, 3.0, 1.4, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_points VALUES ('d634e2b0-4371-4d9f-8fb4-1f074b3b4c9f', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 25, 1.3, 2.0, 2.0, 3.0, 1.7, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_points VALUES ('70ba0928-cc19-41fb-afbc-0549c8853524', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 26, 1.2, 2.0, 1.5, 3.5, 1.8, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_points VALUES ('9e89a3b5-c4e6-4e1c-a9a5-2bda61e7bd39', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 12, 3.4, 0.0, 2.5, 0.0, 4.1, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('d3a4816b-c089-4733-91ed-2d4d6f897d98', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 13, 3.2, 0.0, 3.0, 0.0, 3.8, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('99fd415e-2661-475a-bc3d-2d346ab3f03e', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 14, 3.1, 0.0, 3.2, 0.0, 3.7, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('8072ba1f-9014-4165-8ad8-a3b04b4acbf9', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 15, 3.1, 0.0, 3.2, 0.0, 3.7, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('be2ca2f9-a50e-4029-acf9-dcbd8488790f', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 16, 3.0, 0.0, 3.5, 0.0, 3.5, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('b77ce571-2e32-4516-a522-8272b7c6f4c4', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 17, 3.0, 0.0, 3.5, 0.0, 3.5, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('e18fafc0-aa81-499a-bcf1-54303ddcd207', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 18, 3.0, 0.0, 3.5, 0.0, 3.5, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('b7685265-8de5-47ad-8e97-d6757c85682f', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 19, 2.2, 0.5, 4.0, 0.5, 2.8, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('9052d277-ed83-4c7d-8b9c-2bce7f623090', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 20, 2.2, 1.0, 3.5, 0.7, 2.6, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('a52c4e09-b87a-401d-adf3-217aa1c15319', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 21, 2.1, 1.5, 3.0, 1.0, 2.4, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('66d8df8a-b3e3-4f05-9328-9cce99650095', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 22, 2.0, 1.5, 2.5, 2.0, 2.0, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('c3b74715-6956-4afa-95d5-dd9ac8ef4a9c', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 23, 1.0, 2.0, 2.0, 3.0, 2.0, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('f35411b8-120a-45e2-8374-4fadc510eb46', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 24, 1.0, 2.0, 2.0, 3.0, 2.0, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('c6b1cf8a-45ef-45e6-bb8a-af1254a5ea04', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 25, 1.0, 2.0, 2.0, 3.0, 2.0, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('15f5cb08-f890-4c7f-aa35-d28856647ea9', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 26, 1.0, 2.0, 1.5, 3.5, 2.0, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('b33d776a-1e82-45e4-a00c-3e499285ff9e', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', 27, 1.0, 2.0, 1.0, 4.0, 2.0, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('60c43a25-69eb-43bf-b1dc-e52baf899aea', '49261c09-f797-49c4-bf32-f451b28b91de', -1, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('23a2be16-1e50-4655-bdd4-c3a65cc5294e', '49261c09-f797-49c4-bf32-f451b28b91de', 0, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('fb576de0-64d8-4f5c-8cfc-4946abc62d8e', '49261c09-f797-49c4-bf32-f451b28b91de', 1, 0.5, 0.0, 0.0, 0.0, 9.5, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('8ac1e11f-de0a-42bd-a35b-20c492bd2c12', '49261c09-f797-49c4-bf32-f451b28b91de', 2, 1.0, 0.0, 0.0, 0.0, 9.0, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('421e1269-4d41-496f-9397-e064d691c25c', '49261c09-f797-49c4-bf32-f451b28b91de', 3, 2.0, 0.0, 0.0, 0.0, 8.0, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('043ea885-42c3-4bfb-ad83-d88712785565', '49261c09-f797-49c4-bf32-f451b28b91de', 4, 3.0, 0.0, 0.0, 0.0, 7.0, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('fc8e301a-62fb-4237-af6b-b38b50396506', '49261c09-f797-49c4-bf32-f451b28b91de', 5, 4.0, 0.0, 0.0, 0.0, 6.0, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('282b7c8e-32c1-4faf-a5f2-f1a895a84025', '49261c09-f797-49c4-bf32-f451b28b91de', 6, 4.5, 0.0, 0.0, 0.0, 5.5, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('cf9919a8-05e3-4f29-9fdf-34cdf332d483', '49261c09-f797-49c4-bf32-f451b28b91de', 7, 5.0, 0.0, 0.0, 0.0, 5.0, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('dd215ef5-0521-41e6-b146-bd4f43d8c737', '49261c09-f797-49c4-bf32-f451b28b91de', 8, 4.5, 0.0, 0.0, 0.0, 5.5, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('16f25763-fa43-4633-80bd-5cdc2734c636', '49261c09-f797-49c4-bf32-f451b28b91de', 9, 4.0, 0.0, 1.0, 0.0, 5.0, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('56ad91cd-b8de-41de-ad31-7feca3ce9dff', '49261c09-f797-49c4-bf32-f451b28b91de', 10, 3.7, 0.0, 1.5, 0.0, 4.8, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('8b7f603a-2d1d-443c-ae29-ac4c5ca34508', '49261c09-f797-49c4-bf32-f451b28b91de', 11, 3.4, 0.0, 2.0, 0.0, 4.6, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('98ee48d3-0051-4f6a-9715-e63df245b2e3', '49261c09-f797-49c4-bf32-f451b28b91de', 12, 3.1, 0.0, 2.5, 0.0, 4.4, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('47f19b8f-7d7e-442f-99ad-2b7527d9a641', '49261c09-f797-49c4-bf32-f451b28b91de', 13, 2.9, 0.0, 3.0, 0.0, 4.1, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('a44659d8-b3ac-4b81-af7a-481da9c04ade', '49261c09-f797-49c4-bf32-f451b28b91de', 14, 2.7, 0.0, 3.2, 0.0, 4.1, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('e1d472fb-245e-447f-88cc-43f7841d9ebb', '49261c09-f797-49c4-bf32-f451b28b91de', 15, 2.5, 0.0, 3.2, 0.0, 4.3, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('d23db295-bf97-409f-9a67-727b3131d0cb', '49261c09-f797-49c4-bf32-f451b28b91de', 16, 2.3, 0.0, 3.5, 0.0, 4.2, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('e08e5768-fa2d-4eaf-aed9-0c4741dc2598', '49261c09-f797-49c4-bf32-f451b28b91de', 17, 2.1, 0.0, 3.5, 0.0, 4.4, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('86bf7568-f342-4c3b-a222-b9fff668ffc1', '49261c09-f797-49c4-bf32-f451b28b91de', 18, 2.0, 0.0, 3.5, 0.0, 4.5, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('f36e1a80-82e4-4c35-9e76-2ad6542b4e74', '49261c09-f797-49c4-bf32-f451b28b91de', 19, 1.9, 0.5, 4.0, 0.5, 3.1, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('175d725e-6c95-49e7-afb6-f26177dd70e8', '49261c09-f797-49c4-bf32-f451b28b91de', 20, 1.8, 1.0, 3.5, 0.7, 3.0, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('2ff93c85-89cb-44e2-aa44-4dbaac173482', '49261c09-f797-49c4-bf32-f451b28b91de', 21, 1.7, 1.5, 3.0, 1.0, 2.8, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('0e6e102e-5d9f-4443-8b3e-ea41fa0e02c6', '49261c09-f797-49c4-bf32-f451b28b91de', 22, 1.6, 1.5, 2.5, 2.0, 2.4, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('419b1084-a59d-42b3-b9af-7b8b12ba8eea', '49261c09-f797-49c4-bf32-f451b28b91de', 23, 1.5, 2.0, 2.0, 3.0, 1.5, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('16c8caf4-24aa-4ed3-86e1-b12c90fbf77b', '49261c09-f797-49c4-bf32-f451b28b91de', 24, 1.4, 2.0, 2.0, 3.0, 1.6, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('6d5686cc-4a3e-42ef-a3f8-ffed61c2f15c', '49261c09-f797-49c4-bf32-f451b28b91de', 25, 1.3, 2.0, 2.0, 3.0, 1.7, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('d762be97-72bd-4e65-896c-fbaf3074b8a5', '49261c09-f797-49c4-bf32-f451b28b91de', 26, 1.2, 2.0, 1.5, 3.5, 1.8, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('0ea96fe0-6cf0-463a-a029-ac5bfd199139', '49261c09-f797-49c4-bf32-f451b28b91de', 27, 1.1, 2.0, 1.0, 4.0, 1.9, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('dca4c4ba-dd54-4281-8d60-93a0f065a421', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', -1, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('d7554b80-20ee-4802-aa0e-2bc5e389366e', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 0, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('541b3480-376f-4954-bbf1-dfd09cbf53a0', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 1, 0.5, 0.0, 0.0, 0.0, 9.5, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('6849d360-e75f-4d7b-ac04-d9a4bfa8d2f7', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 2, 1.0, 0.0, 0.0, 0.0, 9.0, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('50809155-884e-4ff7-8607-0393874e2dac', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 3, 2.0, 0.0, 0.0, 0.0, 8.0, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('d410903b-4be4-4fe0-835f-0240bc58f9fc', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 4, 3.0, 0.0, 0.0, 0.0, 7.0, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('09421870-96ae-4511-9547-1045a4bd6807', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 5, 4.0, 0.0, 0.0, 0.0, 6.0, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('55804be6-f62d-496f-985a-9d34f80e804a', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 6, 4.5, 0.0, 0.0, 0.0, 5.5, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('b604e10b-5f53-4709-8f19-5598bfa832bf', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 7, 5.0, 0.0, 0.0, 0.0, 5.0, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('f774330e-5c15-438e-a556-53f26e16a13d', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 8, 4.5, 0.0, 0.0, 0.0, 5.5, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('c90d5d52-bb8f-4220-9ed3-97da181af759', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 9, 4.0, 0.0, 1.0, 0.0, 5.0, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('4ce836dd-a309-4534-a644-14cd366f9d03', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 10, 3.7, 0.0, 1.5, 0.0, 4.8, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('841ad5ec-70c9-4679-96ce-865b576434e1', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 11, 3.4, 0.0, 2.0, 0.0, 4.6, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('32113758-328f-425b-9581-4d75774458a0', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 12, 3.1, 0.0, 2.5, 0.0, 4.4, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('d0d8b9da-5c05-4c3d-a3ea-c9705878d192', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 13, 2.9, 0.0, 3.0, 0.0, 4.1, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('db114b38-67d9-4dd0-8705-9aa0fb60eabf', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 14, 2.7, 0.0, 3.2, 0.0, 4.1, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('d1035650-fa40-4be2-aaf0-840528446fb5', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 15, 2.5, 0.0, 3.4, 0.0, 4.1, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('7d100f60-0029-4653-a6e5-546308248718', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 16, 2.3, 0.0, 3.6, 0.0, 4.1, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('9624ef47-198b-4921-b248-192aed6a759f', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 17, 2.1, 0.0, 3.8, 0.0, 4.1, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('da4be51c-5fd1-4e03-9951-1251f1310fb8', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 18, 2.0, 0.0, 3.9, 0.0, 4.1, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('50498298-a2f4-4462-a2d0-8321fa8487a3', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 19, 1.9, 0.5, 3.8, 0.5, 3.3, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('cea20d5c-5850-4046-8dda-5bac97c6a66e', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 20, 1.8, 1.0, 3.6, 0.7, 2.9, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('f0b4a9b8-54f7-48be-8812-e1c90e2ba42d', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 21, 1.7, 1.5, 3.4, 1.0, 2.4, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('0c6b2255-5b46-42e9-8405-d939ba5a50bc', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 22, 1.6, 1.5, 3.0, 2.0, 1.9, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('7e9b5440-aed6-4f31-a04a-681530acd065', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 23, 1.5, 2.0, 2.5, 3.0, 1.0, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('bbb0b034-dac3-49a5-a021-296454ed9429', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 24, 1.4, 2.2, 2.0, 3.0, 1.4, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('39a953a9-789c-4ea7-9b78-bccaf3c5b75c', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 25, 1.3, 2.0, 2.0, 3.0, 1.7, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('d682ca58-2784-463f-b1b6-01b94a95ff65', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 26, 1.2, 2.0, 1.5, 3.5, 1.8, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('df9ad1a3-f31d-49d7-9f90-a560b4e4a9c8', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', 27, 1.1, 2.0, 1.0, 4.0, 1.9, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('24555e30-e1e6-4550-b3e6-98edc7cf9920', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', -1, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('a6a43500-f357-45d6-8bd9-36364ca9c02f', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 0, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('12d1a74b-b706-4edb-9638-198ae8d103e8', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 1, 0.5, 0.0, 0.0, 0.0, 9.5, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('98e0ac73-9930-4838-abb5-c0d8941691e5', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 2, 1.0, 0.0, 0.0, 0.0, 9.0, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('b24668e2-b820-4425-ad8a-27a524ce60fa', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 3, 2.0, 0.0, 0.0, 0.0, 8.0, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('ae3d1404-4e5d-42a0-b4bc-6d3940fbf907', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 4, 3.0, 0.0, 0.0, 0.0, 7.0, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('1cce43ae-adf9-4fae-abaa-174d93615b3f', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 5, 4.0, 0.0, 0.0, 0.0, 6.0, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('ef9d5a14-e5e4-46e0-9f84-b1086a2919e6', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 6, 4.5, 0.0, 0.0, 0.0, 5.5, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('3fa7c3c0-1625-4e7e-b557-28e9c7c573b8', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 7, 5.0, 0.0, 0.0, 0.0, 5.0, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('f7ae13d6-ab8d-46b5-99b1-a0e25bd4466f', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 8, 4.5, 0.0, 0.0, 0.0, 5.5, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('df8ea2b6-3dc2-4aaa-846e-67f787599e0c', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 9, 4.0, 0.0, 1.0, 0.0, 5.0, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('64c70363-96fa-4808-bd58-9eae178e14dd', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 10, 3.7, 0.0, 1.5, 0.0, 4.8, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('bf1e2281-2211-42dd-a988-321e94655bde', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 11, 3.4, 0.0, 2.0, 0.0, 4.6, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('c2bc540a-edcb-432b-8452-813befb6961b', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 12, 3.1, 0.0, 2.5, 0.0, 4.4, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('aa8047f4-d671-4a43-b32e-7dc90c9573d8', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 13, 2.9, 0.0, 3.0, 0.0, 4.1, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('53aaf177-8bd4-4ecc-a814-cf0287952aed', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 14, 2.7, 0.0, 3.2, 0.0, 4.1, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('6c1dd7f1-2b14-421b-ae03-a0698087c8e9', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 15, 2.5, 0.0, 3.4, 0.0, 4.1, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('7f9011c1-9266-41f9-8eb2-0247ecc86b75', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 16, 2.3, 0.0, 3.6, 0.0, 4.1, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('3706c3a4-0b97-4bd1-91c2-bc0e81a19471', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 17, 2.1, 0.0, 3.9, 0.0, 4.0, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('bc00b2b3-6b93-4af8-9afe-74990724cd6a', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 18, 2.0, 0.0, 4.1, 0.0, 3.9, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('0bcaef0c-cfc0-4a3a-8797-78859bb4cbdf', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 19, 1.9, 0.5, 3.8, 0.5, 3.3, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('e162c4be-888b-4184-ab0a-4339d7eaad73', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 20, 1.8, 1.0, 3.6, 0.7, 2.9, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('016ae414-9919-48b5-9945-df65173bd9f3', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 21, 1.7, 1.5, 3.4, 1.0, 2.4, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('7475794b-e376-4eef-9542-abf10ddde618', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 22, 1.6, 1.5, 3.0, 2.0, 1.9, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('aabc0b2a-352e-490a-b2c2-a073016fd71e', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 23, 1.5, 2.0, 2.5, 3.0, 1.0, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('191d5a6d-fa70-4550-a9bc-de5fbc552057', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 24, 1.4, 2.2, 2.0, 3.0, 1.4, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('5daebef9-f52c-47d0-aa85-67410b9a8af1', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 25, 1.3, 2.0, 2.0, 3.0, 1.7, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('a74cf08a-cd24-4ee1-a643-21b31c7979a0', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 26, 1.2, 2.0, 1.5, 3.5, 1.8, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('2784bf43-fcf8-45f6-abc8-1fe1b008fd97', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', 27, 1.1, 2.0, 1.0, 4.0, 1.9, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('2746bbeb-c7ee-4ba5-9c40-e94d6b049a71', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', -1, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('f67c8b4d-6bb0-4723-9a08-692335541eda', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 0, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('9b3d95cd-1627-494a-bb46-1fc400572649', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 1, 0.5, 0.0, 0.0, 0.0, 9.5, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('4aa5927d-bf6e-4f13-a470-d5616289f76b', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 2, 1.0, 0.0, 0.0, 0.0, 9.0, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('a235c033-ca60-4b0c-b523-02efea9fe2fa', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 3, 2.0, 0.0, 0.0, 0.0, 8.0, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('2fb8891a-597b-47dc-95b2-10bdc180b508', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 4, 3.0, 0.0, 0.0, 0.0, 7.0, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('3181d7e9-6fb4-4eb3-bfb6-872974e66fa3', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 5, 4.0, 0.0, 0.0, 0.0, 6.0, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('2d6b0e9d-f451-447b-b0df-8dd39a52d4ea', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 6, 4.5, 0.0, 0.0, 0.0, 5.5, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('ee95d3cd-fd82-47c7-bd8e-42f8eb6c9d95', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 7, 5.0, 0.0, 0.0, 0.0, 5.0, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('80c5559c-0684-4e8d-91b0-e4a6d9a6372e', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 8, 4.5, 0.0, 0.0, 0.0, 5.5, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('ad0d0d47-bbf9-41cf-9d3a-b92029829522', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 9, 4.0, 0.0, 1.0, 0.0, 5.0, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('deadc70b-9ddb-40b0-bf71-c3e92d7bca2f', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 10, 3.7, 0.0, 1.5, 0.0, 4.8, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('c6290a34-0f5b-424e-a5f2-4f782307c258', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 11, 3.4, 0.0, 2.0, 0.0, 4.6, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('5455d6c8-48fe-46f8-a50a-bafe21bf1723', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 12, 3.1, 0.0, 2.5, 0.0, 4.4, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('75106428-41ed-4d4a-91b3-6689a2a90883', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 13, 2.9, 0.0, 3.0, 0.0, 4.1, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('72faa4e5-9d3b-491e-8cc5-e4df48c2e782', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 14, 2.7, 0.0, 3.2, 0.0, 4.1, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('9f6d51f2-d5d8-41cd-8fdb-ef1724974d38', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 15, 2.5, 0.0, 3.4, 0.0, 4.1, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('ab98aa4e-683e-4c71-a9a9-366678ed6363', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 16, 2.3, 0.0, 3.6, 0.0, 4.1, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('1e655f31-51cd-4a80-8c31-beb2e5a55696', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 17, 2.1, 0.0, 3.9, 0.0, 4.0, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('a65c9d10-a23f-4f6e-aa60-774813538739', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 18, 2.0, 0.0, 4.1, 0.0, 3.9, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('27430db7-69a7-442f-a1b5-25e4c54c69ab', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 19, 1.9, 0.5, 3.6, 0.5, 3.5, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('68dc433b-8aff-406d-91ce-bd342f3f04f6', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 20, 1.8, 1.0, 3.2, 0.7, 3.3, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('4cc50fe0-f781-47e4-b067-784c49a5acb7', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 21, 1.7, 1.5, 2.8, 1.0, 3.0, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('14ff3e51-af54-4b0f-9df3-d48bdba85ccb', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 22, 1.6, 1.5, 2.6, 2.0, 2.3, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('d6626137-98d6-47df-953a-a877b59e1cbe', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 23, 1.5, 2.0, 2.2, 3.0, 1.3, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('f68a7be0-bc07-4745-89c2-f724c3362a1e', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 24, 1.4, 2.2, 2.0, 3.0, 1.4, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('f1a077ce-6caa-4f92-8658-9470703370d7', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 25, 1.3, 2.0, 2.0, 3.0, 1.7, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('6f83a4f7-f92f-4d51-99d0-0b924333c931', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 26, 1.2, 2.0, 1.5, 3.5, 1.8, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('dd3568c9-9f9a-481a-8c1a-395bdb3197ce', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', 27, 1.1, 2.0, 1.0, 4.0, 1.9, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('9f2ca1d7-4e1e-48b8-869f-7f1783708f32', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', -1, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('52c794aa-959b-4611-8b80-3b53d592643e', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 0, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('c1eb0ce4-17fc-495b-bb6f-2c16a9f704c2', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 1, 0.5, 0.0, 0.0, 0.0, 9.5, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('9a94b39c-82bd-4c02-a1bd-5520508376f3', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 2, 1.0, 0.0, 0.0, 0.0, 9.0, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('55d5f51b-8a1b-43ef-8d33-344da3a7d7cd', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 3, 2.0, 0.0, 0.0, 0.0, 8.0, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('3b65b6a6-e3f6-4603-876e-ce3380c6dfdf', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 4, 3.0, 0.0, 0.0, 0.0, 7.0, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('aa7a17f8-6f36-41c6-ac0f-f61dff040d14', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 5, 4.0, 0.0, 0.0, 0.0, 6.0, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('203816a7-45b1-4f99-893c-80416322e287', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 6, 4.5, 0.0, 0.0, 0.0, 5.5, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('5abf8730-796a-4462-85b9-33051297d28a', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 7, 5.0, 0.0, 0.0, 0.0, 5.0, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('bdc929e5-2654-43cf-bac3-d6c0df8af0bf', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 8, 4.5, 0.0, 0.0, 0.0, 5.5, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('f0c11fb5-b43a-41cb-9dab-4d7ac65dff3e', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 9, 4.0, 0.0, 1.0, 0.0, 5.0, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('e31fd29f-70ee-413c-80b8-6190a90d40c9', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 10, 3.7, 0.0, 1.5, 0.0, 4.8, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('fa32db6e-5ddf-46a0-a54d-dbf37fb07c1c', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 11, 3.4, 0.0, 2.0, 0.0, 4.6, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('622671f9-20a9-4d3a-b443-4379a1a30083', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 12, 3.1, 0.0, 2.5, 0.0, 4.4, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('8e8385f0-163c-43ca-bb74-b0ddf1d406a7', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 13, 2.9, 0.0, 3.0, 0.0, 4.1, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('25f36df1-f2ff-4b7e-94a9-7552172cf216', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 14, 2.7, 0.0, 3.2, 0.0, 4.1, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('57a3d8d4-d4f4-4c1c-8170-a1cb3b24f103', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 15, 2.5, 0.0, 3.4, 0.0, 4.1, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('9193532b-fd7c-49b2-9329-5271f77f8840', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 16, 2.3, 0.0, 3.6, 0.0, 4.1, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('b18d372e-e41e-45e6-b7a3-14668f5ef636', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 17, 2.1, 0.0, 3.9, 0.0, 4.0, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('93b9ddfb-bbdc-4468-9f60-d7fc743bebde', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 18, 2.0, 0.0, 4.1, 0.0, 3.9, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('da71c17b-acc8-4e14-92b2-117fd454852e', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 19, 1.9, 0.5, 3.6, 0.5, 3.5, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('dfd30cb1-3523-4ba2-8bdd-671f049b63b4', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 20, 1.8, 1.0, 3.2, 0.7, 3.3, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('dfaab3d5-6283-4cf8-afab-27277dd13640', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 21, 1.7, 1.5, 2.8, 1.0, 3.0, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('b657d70a-889c-492f-966a-8e55c7b36d74', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 22, 1.6, 1.5, 2.6, 2.0, 2.3, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('aaf6a10b-f165-449a-809d-38aba389819b', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 23, 1.5, 2.0, 2.2, 3.0, 1.3, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('184d9577-5b0f-42bc-8e35-71bc33dd25d8', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 24, 1.4, 2.2, 2.0, 3.0, 1.4, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('0b39cbb4-7387-46f1-bf83-b2873b34e743', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 25, 1.3, 2.0, 2.0, 3.0, 1.7, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('3d5ebcf4-49e2-488d-b2df-3c7aa6731356', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 26, 1.2, 2.0, 1.5, 3.5, 1.8, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('cd5d7b34-a6cc-4236-863d-61de5d78f1b8', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', 27, 1.1, 2.0, 1.0, 4.0, 1.9, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('130d57ee-7782-437e-bb9a-81e6d5220fd3', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', -1, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('3596a892-9939-444e-8dfa-e83f49c35bec', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 0, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('89b7ed63-c19c-4e74-94d2-327087c0e8de', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 1, 0.5, 0.0, 0.0, 0.0, 9.5, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('8dd2304c-7153-49cf-b914-1a4e76b76486', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 2, 1.0, 0.0, 0.0, 0.0, 9.0, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('f4f9ad8f-4584-4c19-b8a6-02c30d22e0e0', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 3, 2.0, 0.0, 0.0, 0.0, 8.0, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('46a2b6d4-d1c5-4353-a34e-2f6e18cecaab', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 4, 3.0, 0.0, 0.0, 0.0, 7.0, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('40a88558-5f2f-4b93-910f-6abffbf7bbf0', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 5, 4.0, 0.0, 0.0, 0.0, 6.0, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('8710d9db-9690-4125-8785-7668c32749b1', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 6, 4.5, 0.0, 0.0, 0.0, 5.5, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('d95a526b-c81d-4095-a707-ba44a416c627', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 7, 5.0, 0.0, 0.0, 0.0, 5.0, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('57a3fa11-c781-4f2f-8fd6-2e19ee57fc1b', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 8, 4.5, 0.0, 0.0, 0.0, 5.5, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('c1137530-efbd-4de3-80b9-ffb1b2115c47', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 9, 4.0, 0.0, 1.0, 0.0, 5.0, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('fd4821c0-698c-4ad0-88bb-ec53460cff70', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 10, 3.7, 0.0, 1.5, 0.0, 4.8, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('1899ef90-39e2-4c5e-b2a2-4ca6035161f4', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 11, 3.4, 0.0, 2.0, 0.0, 4.6, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('18645178-496c-4fd2-b4d4-f61fcbe2bf89', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 12, 3.1, 0.0, 2.5, 0.0, 4.4, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('0538eef0-e65f-42f5-a735-cb90bfef8410', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 13, 2.9, 0.0, 3.0, 0.0, 4.1, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('d9492c72-882c-4831-be69-01b0a5e11261', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 14, 2.7, 0.0, 3.2, 0.0, 4.1, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('1a224bf4-2a1c-4c06-85e9-ed0e43673363', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 15, 2.5, 0.0, 3.4, 0.0, 4.1, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('f6339edc-fe02-443b-9b80-2c5805721e74', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 16, 2.3, 0.0, 3.6, 0.0, 4.1, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('e1fe5f20-02a6-484a-b88a-3f27d0f10da0', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 17, 2.1, 0.0, 3.9, 0.0, 4.0, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('174366d9-9da6-49c2-9077-a4431b9181fe', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 18, 2.0, 0.0, 4.1, 0.0, 3.9, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('d01262c3-3b3c-4a07-b451-4590d903c629', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 19, 1.9, 0.5, 3.6, 0.5, 3.5, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('795a705a-ae17-4dd4-af54-ff960c556d8f', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 20, 1.8, 1.0, 3.2, 0.7, 3.3, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('0c4e8315-065b-4384-bebd-582dccdbae83', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 21, 1.7, 1.5, 2.8, 1.0, 3.0, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('3c184ec5-f4a0-42ec-84b5-25b0530db549', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 22, 1.6, 1.5, 2.6, 2.0, 2.3, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('50422c76-317d-481e-948b-a9ed54f0bc70', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 23, 1.5, 2.0, 2.2, 3.0, 1.3, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('cbf9c760-2a37-4749-a12d-66bcb56aa085', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 24, 1.4, 2.2, 2.0, 3.0, 1.4, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('b5860d00-edb3-4aab-b6e3-69c7cb2d58d4', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 25, 1.3, 2.0, 2.0, 3.0, 1.7, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('e5c74283-fe12-4e5d-a820-5b4af9ec6b18', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 26, 1.2, 2.0, 1.5, 3.5, 1.8, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('752dabcb-dd9e-43a3-99c2-a5a64ca4c89d', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', 27, 1.1, 2.0, 1.0, 4.0, 1.9, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('07c6ac32-c4a6-4dbd-b4c5-2162160d5ed1', '75f9fde8-300c-4616-ad83-aebd9b051891', -1, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('b7cece40-97d6-4a19-9c50-8fe901ee12af', '75f9fde8-300c-4616-ad83-aebd9b051891', 0, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('707559f2-c8ee-4d49-97ba-b7ecef0284bf', '75f9fde8-300c-4616-ad83-aebd9b051891', 1, 0.5, 0.0, 0.0, 0.0, 9.5, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('6b29da51-9030-4b4d-90a7-9184ff5d078e', '75f9fde8-300c-4616-ad83-aebd9b051891', 2, 1.0, 0.0, 0.0, 0.0, 9.0, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('520a0e1a-671d-4f9a-80de-f91365861f56', '75f9fde8-300c-4616-ad83-aebd9b051891', 3, 2.0, 0.0, 0.0, 0.0, 8.0, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('b1ea86c7-3ad2-419b-98d3-793c6d0f9e05', '75f9fde8-300c-4616-ad83-aebd9b051891', 4, 3.0, 0.0, 0.0, 0.0, 7.0, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('dc0178d9-76b1-4226-928b-bdddc1c88556', '75f9fde8-300c-4616-ad83-aebd9b051891', 5, 4.0, 0.0, 0.0, 0.0, 6.0, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('b4ffef52-f856-48cc-bcd1-d4624856260f', '75f9fde8-300c-4616-ad83-aebd9b051891', 6, 4.5, 0.0, 0.0, 0.0, 5.5, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('78f2b26e-c85b-4818-92f4-98d0f84246f2', '75f9fde8-300c-4616-ad83-aebd9b051891', 7, 5.0, 0.0, 0.0, 0.0, 5.0, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('d1677b75-5bc7-4cbc-8a2e-09f5bbc0c8cc', '75f9fde8-300c-4616-ad83-aebd9b051891', 8, 4.5, 0.0, 0.0, 0.0, 5.5, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('9389572c-48dd-4467-8d02-ad8c9cd3732a', '75f9fde8-300c-4616-ad83-aebd9b051891', 9, 4.0, 0.0, 1.0, 0.0, 5.0, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('b7bb2742-7722-4d13-aade-338e656116d8', '75f9fde8-300c-4616-ad83-aebd9b051891', 10, 3.7, 0.0, 1.5, 0.0, 4.8, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('8e202af2-30b6-4d54-9995-72a1c8b5c8c8', '75f9fde8-300c-4616-ad83-aebd9b051891', 11, 3.4, 0.0, 2.0, 0.0, 4.6, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('e3ce4b32-1412-4204-981e-56fa76421cdd', '75f9fde8-300c-4616-ad83-aebd9b051891', 12, 3.1, 0.0, 2.5, 0.0, 4.4, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('942aaebf-1abb-40e0-a67f-98ba6a643545', '75f9fde8-300c-4616-ad83-aebd9b051891', 13, 2.9, 0.0, 3.0, 0.0, 4.1, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('d3b8021e-b868-462a-8afc-33df6372cbf5', '75f9fde8-300c-4616-ad83-aebd9b051891', 14, 2.7, 0.0, 3.2, 0.0, 4.1, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('8b26bd41-5253-478a-a96d-52b9190cd2d9', '75f9fde8-300c-4616-ad83-aebd9b051891', 15, 2.5, 0.0, 3.4, 0.0, 4.1, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('cacc62d6-9de6-4d80-ab3b-4fe6accef037', '75f9fde8-300c-4616-ad83-aebd9b051891', 16, 2.3, 0.0, 3.6, 0.0, 4.1, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('a0a46495-a178-4b47-8ef4-76eb23ddb041', '75f9fde8-300c-4616-ad83-aebd9b051891', 17, 2.1, 0.0, 3.9, 0.0, 4.0, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('3aa3646b-4cc9-4710-9918-ba344a80952b', '75f9fde8-300c-4616-ad83-aebd9b051891', 18, 2.0, 0.0, 4.1, 0.0, 3.9, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('e0996b52-2db3-4045-818e-0a6448b17b67', '75f9fde8-300c-4616-ad83-aebd9b051891', 19, 1.9, 0.5, 3.6, 0.5, 3.5, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('8a8461b3-0a8f-45a6-80c0-13092f7c5648', '75f9fde8-300c-4616-ad83-aebd9b051891', 20, 1.8, 1.0, 3.2, 0.7, 3.3, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('8b41ff47-0b2b-4f45-91e0-58a12504447e', '75f9fde8-300c-4616-ad83-aebd9b051891', 21, 1.7, 1.5, 2.8, 1.0, 3.0, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('514d544d-d08d-4919-9c48-495cee93f289', '75f9fde8-300c-4616-ad83-aebd9b051891', 22, 1.6, 1.5, 2.6, 2.0, 2.3, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('68417f72-e097-42ea-a5c8-3c5bae18a9d0', '75f9fde8-300c-4616-ad83-aebd9b051891', 23, 1.5, 2.0, 2.2, 3.0, 1.3, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('0f228eae-454b-4e1d-ac9f-3cf2be52d5fa', '75f9fde8-300c-4616-ad83-aebd9b051891', 24, 1.4, 2.2, 2.0, 3.0, 1.4, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('1f76ec27-7e91-42a3-a115-3c560b25cd68', '75f9fde8-300c-4616-ad83-aebd9b051891', 25, 1.3, 2.0, 2.0, 3.0, 1.7, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('66ed0e35-13e9-4b7c-8b7d-537a65f42260', '75f9fde8-300c-4616-ad83-aebd9b051891', 26, 1.2, 2.0, 1.5, 3.5, 1.8, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('cd3da92c-8301-4750-a8a5-40780cb5ad8c', '75f9fde8-300c-4616-ad83-aebd9b051891', 27, 1.1, 2.0, 1.0, 4.0, 1.9, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('75870b7a-ce7c-478e-8161-244ecb7423fa', 'b4f1b3ff-0017-4649-9260-277ab323f56f', -1, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('551dbb39-17d9-467c-8938-dcbb8af1d56f', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 0, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('20b21918-3478-4cc6-b2cd-423a8353202b', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 1, 0.5, 0.0, 0.0, 0.0, 9.5, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('f2c66e0b-d4ce-423c-838d-43b65a579bf8', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 2, 1.0, 0.0, 0.0, 0.0, 9.0, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('6777db86-7008-4ae3-b8eb-d28fb469b23d', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 3, 2.0, 0.0, 0.0, 0.0, 8.0, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('fcc72e92-4896-415a-bf25-1194780f8bb0', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 4, 3.0, 0.0, 0.0, 0.0, 7.0, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('24eb8aa0-2034-4ce0-987e-9d7f3cce86f8', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 5, 4.0, 0.0, 0.0, 0.0, 6.0, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('a2277037-b157-4a5a-a514-26f7d9bd35d8', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 6, 4.5, 0.0, 0.0, 0.0, 5.5, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('a394cfe8-0044-413b-8df9-437f168f1df4', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 7, 5.0, 0.0, 0.0, 0.0, 5.0, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('25c01c62-490d-4e57-9a29-5765ff2ce17f', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 8, 4.5, 0.0, 0.0, 0.0, 5.5, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('bcb0162f-9523-4454-b5df-186ba39ae785', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 9, 4.0, 0.0, 1.0, 0.0, 5.0, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('9e0f2d87-81ce-4306-94a4-5e01e9acc67f', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 10, 3.7, 0.0, 1.5, 0.0, 4.8, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('148a39be-66e3-4db3-b481-8da27e2c6310', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 11, 3.4, 0.0, 2.0, 0.0, 4.6, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('d004e3f0-1004-411f-9e3b-e931cbb02da5', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 12, 3.1, 0.0, 2.5, 0.0, 4.4, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('4e9cf103-5c31-4a8c-8984-ef930ad85cf7', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 13, 2.9, 0.0, 3.0, 0.0, 4.1, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('06e8ddc3-da73-47c6-91c5-ffba291d10ec', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 14, 2.7, 0.0, 3.2, 0.0, 4.1, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('66fe67f7-c8ac-4ee7-9ff9-7a0a8a842864', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 15, 2.5, 0.0, 3.4, 0.0, 4.1, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('14cce832-9c17-4656-b175-63973ec2a959', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 16, 2.3, 0.0, 3.6, 0.0, 4.1, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('52822c5a-2ca5-4c38-b296-ca535e370813', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 17, 2.1, 0.0, 3.9, 0.0, 4.0, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('a4a6ce35-a8a0-4de1-84d1-960de552888d', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 18, 2.0, 0.0, 4.1, 0.0, 3.9, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('3c5d533c-ee28-427c-889f-19f1537bec87', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 19, 1.9, 0.5, 3.6, 0.5, 3.5, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('d757a37e-d7b3-474f-8b4b-77a9854d6a65', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 20, 1.8, 1.0, 3.2, 0.7, 3.3, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('f662d54b-433a-4d40-8465-9d2c471f71e0', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 21, 1.7, 1.5, 2.8, 1.0, 3.0, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('393fd98e-86b9-43ca-92cb-7f273076f63d', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 22, 1.6, 1.5, 2.6, 2.0, 2.3, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('c79d0348-534c-48c1-82a1-40dad571f101', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 23, 1.5, 2.0, 2.2, 3.0, 1.3, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('71636116-eea6-4d2a-87ad-df675b237808', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 24, 1.4, 2.2, 2.0, 3.0, 1.4, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('4facad4f-28b1-4d81-8fa0-dc018a51df8f', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 25, 1.3, 2.0, 2.0, 3.0, 1.7, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('514dc0fb-c56e-47df-8dff-7523ccd64465', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 26, 1.2, 2.0, 1.5, 3.5, 1.8, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('b1acde9e-c7ea-4a19-bb9f-125bd688b36c', 'b4f1b3ff-0017-4649-9260-277ab323f56f', 27, 1.1, 2.0, 1.0, 4.0, 1.9, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('7f617724-0d5b-4baf-b46b-83e17e00e77c', 'be28904d-fd8a-4422-9163-9c2014be29f6', -1, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('0423ea6a-1c92-434e-941e-9202152b1857', 'be28904d-fd8a-4422-9163-9c2014be29f6', 0, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('18ad284b-3b53-4339-8c46-bbd0a8e891ab', 'be28904d-fd8a-4422-9163-9c2014be29f6', 1, 0.5, 0.0, 0.0, 0.0, 9.5, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('007b7f17-0fca-4030-839b-fab6e9400321', 'be28904d-fd8a-4422-9163-9c2014be29f6', 2, 1.0, 0.0, 0.0, 0.0, 9.0, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('20429e9c-9ce3-47a4-b2b3-8d0169b32d82', 'be28904d-fd8a-4422-9163-9c2014be29f6', 3, 2.0, 0.0, 0.0, 0.0, 8.0, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('1393ce0e-d52c-400f-8f8f-2c2edfe69fc3', 'be28904d-fd8a-4422-9163-9c2014be29f6', 4, 3.0, 0.0, 0.0, 0.0, 7.0, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('f98f4c7e-419c-4225-9962-c9a6d1ef0863', 'be28904d-fd8a-4422-9163-9c2014be29f6', 5, 4.0, 0.0, 0.0, 0.0, 6.0, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('ac4c2371-338c-4ab2-a8a1-635e996d6efb', 'be28904d-fd8a-4422-9163-9c2014be29f6', 6, 4.5, 0.0, 0.0, 0.0, 5.5, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('193c645c-8c83-4491-ad97-ac0411680e0f', 'be28904d-fd8a-4422-9163-9c2014be29f6', 7, 5.0, 0.0, 0.0, 0.0, 5.0, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('8571454e-8ebe-4cf9-9657-59b2955ada46', 'be28904d-fd8a-4422-9163-9c2014be29f6', 8, 4.5, 0.0, 0.0, 0.0, 5.5, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('3b3d7a29-2dc4-4f65-bba4-c0481b29ff51', 'be28904d-fd8a-4422-9163-9c2014be29f6', 9, 4.0, 0.0, 1.0, 0.0, 5.0, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('27891b02-6e92-4296-a9c0-70eedc32e956', 'be28904d-fd8a-4422-9163-9c2014be29f6', 10, 3.7, 0.0, 1.5, 0.0, 4.8, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('fe21121d-9431-4f12-9ad7-a514ee37dc9d', 'be28904d-fd8a-4422-9163-9c2014be29f6', 11, 3.4, 0.0, 2.0, 0.0, 4.6, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('4d6a724b-2b17-46d0-9aae-f787a9567efd', 'be28904d-fd8a-4422-9163-9c2014be29f6', 12, 3.1, 0.0, 2.5, 0.0, 4.4, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('57deffcc-46ea-4350-8726-ee5c005af7c8', 'be28904d-fd8a-4422-9163-9c2014be29f6', 13, 2.9, 0.0, 3.0, 0.0, 4.1, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('5e9cd39b-5312-4b7a-ae76-96d3a7b88196', 'be28904d-fd8a-4422-9163-9c2014be29f6', 14, 2.7, 0.0, 3.2, 0.0, 4.1, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('2b103a4d-5206-42cc-a657-ad0c7d0e7e7f', 'be28904d-fd8a-4422-9163-9c2014be29f6', 15, 2.5, 0.0, 3.4, 0.0, 4.1, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('18a918d4-9af7-42e7-9996-9b2e336ed59f', 'be28904d-fd8a-4422-9163-9c2014be29f6', 16, 2.3, 0.0, 3.6, 0.0, 4.1, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('30f5b7e2-1f57-4bbc-9b05-f618b57ae5a6', 'be28904d-fd8a-4422-9163-9c2014be29f6', 17, 2.1, 0.0, 3.9, 0.0, 4.0, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('39df8850-0970-4ed0-9af2-8e1a4db0676f', 'be28904d-fd8a-4422-9163-9c2014be29f6', 18, 2.0, 0.0, 4.1, 0.0, 3.9, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('7ca72d2c-f5f2-466f-bc45-3326b001ebb4', 'be28904d-fd8a-4422-9163-9c2014be29f6', 19, 1.9, 0.5, 3.6, 0.5, 3.5, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('dc54ffc2-b248-4c13-87ef-fa57b67fd0c7', 'be28904d-fd8a-4422-9163-9c2014be29f6', 20, 1.8, 1.0, 3.2, 0.7, 3.3, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('9b5fd0d1-24a8-4705-9ae9-c8e527aa4439', 'be28904d-fd8a-4422-9163-9c2014be29f6', 21, 1.7, 1.5, 2.8, 1.0, 3.0, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('820c4997-d459-45e2-8a54-2ae7e4f3fb2e', 'be28904d-fd8a-4422-9163-9c2014be29f6', 22, 1.6, 1.5, 2.6, 2.0, 2.3, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('05b43668-df2e-4e83-96eb-cf2299b4790e', 'be28904d-fd8a-4422-9163-9c2014be29f6', 23, 1.5, 2.0, 2.2, 3.0, 1.3, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('7c951a3b-cf04-47d1-85b9-ad2403a5353e', 'be28904d-fd8a-4422-9163-9c2014be29f6', 24, 1.4, 2.2, 2.0, 3.0, 1.4, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('937282b2-6c84-4850-8723-5fb7593911ce', 'be28904d-fd8a-4422-9163-9c2014be29f6', 25, 1.3, 2.0, 2.0, 3.0, 1.7, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('722220e7-36c4-4fb1-99de-23e5ba0643d1', 'be28904d-fd8a-4422-9163-9c2014be29f6', 26, 1.2, 2.0, 1.5, 3.5, 1.8, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('dea6381e-43e7-479e-af49-a9848d605171', 'be28904d-fd8a-4422-9163-9c2014be29f6', 27, 1.1, 2.0, 1.0, 4.0, 1.9, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('60514544-b6dc-4617-b8ac-3371a0c56b01', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', -1, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_points VALUES ('90ad0461-bad2-4982-ae22-c6eed27c4255', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 0, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_points VALUES ('cce612fc-021e-4795-8d5b-98615b17a05b', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 1, 0.5, 0.0, 0.0, 0.0, 9.5, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_points VALUES ('5849b8e7-dc25-4402-8749-f3d4e890e071', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 2, 1.0, 0.0, 0.0, 0.0, 9.0, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_points VALUES ('33ccc9a6-f68a-423e-b33b-e0ccb3caff99', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 3, 2.0, 0.0, 0.0, 0.0, 8.0, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_points VALUES ('89164cf2-8d2f-4e14-bdc1-1dd21c8fbadf', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 4, 3.0, 0.0, 0.0, 0.0, 7.0, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_points VALUES ('3f18462c-a5d2-412b-9cad-6f4a4e1e501d', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 5, 4.0, 0.0, 0.0, 0.0, 6.0, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_points VALUES ('6fc12241-74fa-431d-9ae7-d963fe0dd46a', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 6, 4.5, 0.0, 0.0, 0.0, 5.5, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_points VALUES ('20610f12-8169-4933-bfc1-b2c888563251', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 7, 5.0, 0.0, 0.0, 0.0, 5.0, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_points VALUES ('1e5662a0-bed6-4507-a3ab-66412c1d8cd4', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 8, 4.5, 0.0, 0.0, 0.0, 5.5, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_points VALUES ('e93be988-17b3-4424-9c69-b77258fe43e8', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 9, 4.0, 0.0, 1.0, 0.0, 5.0, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_points VALUES ('d5ebe146-f630-4a97-961b-a37d800cd55b', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 10, 3.7, 0.0, 1.5, 0.0, 4.8, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_points VALUES ('b6f73491-be44-4b7a-a651-fc99cde3996a', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 11, 3.4, 0.0, 2.0, 0.0, 4.6, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_points VALUES ('b01836cf-fc63-49a7-8567-e68cb65bf317', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 3, 2.0, 0.0, 0.0, 0.0, 8.0, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_points VALUES ('5c0681e4-8bfd-49cc-9d0e-c5469b8bb0ef', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 4, 3.0, 0.0, 0.0, 0.0, 7.0, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_points VALUES ('089b17c6-6f25-4a31-9de5-6310c8db106a', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 5, 4.0, 0.0, 0.0, 0.0, 6.0, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_points VALUES ('edf090c4-2e02-4536-affc-0849cfc284d7', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 6, 4.5, 0.0, 0.0, 0.0, 5.5, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_points VALUES ('4cbd77e9-845c-4108-8986-4166e24d38eb', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 7, 5.0, 0.0, 0.0, 0.0, 5.0, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_points VALUES ('f33043c3-6cd6-4bce-bfff-0418564cec61', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 8, 4.5, 0.0, 0.0, 0.0, 5.5, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_points VALUES ('1a2b870b-c81d-40ed-8257-8bec14d9aa2b', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 9, 4.0, 0.0, 1.0, 0.0, 5.0, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_points VALUES ('f829cfde-f502-43d2-89fd-3660fc4b498a', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 10, 3.7, 0.0, 1.5, 0.0, 4.8, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_points VALUES ('cf41143f-30c9-4641-a378-30faa501fe15', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 11, 3.4, 0.0, 2.0, 0.0, 4.6, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_points VALUES ('eb65334b-a919-4c5e-abe3-e27412a12f54', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 12, 3.1, 0.0, 2.5, 0.0, 4.4, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_points VALUES ('41d2671c-0dd1-4e33-aebe-8514ecef3a63', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 13, 2.9, 0.0, 3.0, 0.0, 4.1, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_points VALUES ('db111d40-43ef-482d-844f-55039b147786', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 14, 2.7, 0.0, 3.2, 0.0, 4.1, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_points VALUES ('42ffd1d3-c429-4a7f-8cdf-23c830497901', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 15, 2.5, 0.0, 3.4, 0.0, 4.1, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_points VALUES ('aa120014-9ebf-421d-9455-59b0ae4d4cc0', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 16, 2.3, 0.0, 3.6, 0.0, 4.1, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_points VALUES ('1b2c3b0f-bb37-4aad-ae18-7de8d6209054', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 17, 2.1, 0.0, 3.9, 0.0, 4.0, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_points VALUES ('dbdd62be-31ef-4111-8c5e-095b6f056544', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 18, 2.0, 0.0, 4.1, 0.0, 3.9, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_points VALUES ('93022dc0-ffd8-43e4-90ff-c5b8951951bf', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 19, 1.9, 0.5, 3.6, 0.5, 3.5, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_points VALUES ('854598bf-c016-4d5d-8a31-81e1967a09f3', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 20, 1.8, 1.0, 3.2, 0.7, 3.3, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_points VALUES ('bddae3fd-a68c-46e0-b10a-c7ab8a215973', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 21, 1.7, 1.5, 2.8, 1.0, 3.0, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_points VALUES ('94c45b57-2670-4c22-98ed-7929732d38e2', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 22, 1.6, 1.5, 2.6, 2.0, 2.3, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_points VALUES ('50c95956-189d-409c-8a7c-2526505bb108', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 23, 1.5, 2.0, 2.2, 3.0, 1.3, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_points VALUES ('2d20fbca-6bdb-48db-958d-83f1986ef9bb', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 24, 1.4, 2.2, 2.0, 3.0, 1.4, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_points VALUES ('071f1d2b-86ee-4574-8f04-4f503edc9d74', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 25, 1.3, 2.0, 2.0, 3.0, 1.7, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_points VALUES ('ded376d8-487a-469d-bedb-008cb9b6bbfd', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 26, 1.2, 2.0, 1.5, 3.5, 1.8, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_points VALUES ('6480d468-3ebf-4c59-8b4c-35982e92a6de', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 27, 1.1, 2.0, 1.0, 4.0, 1.9, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_points VALUES ('6e06d95e-0ffc-4541-bf24-5aa904f81950', '46ca6189-6ad5-40a9-a327-aeac604f78c1', 27, 1.1, 2.0, 1.0, 4.0, 1.9, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_points VALUES ('2600ac39-b8b9-44c3-9f23-7ec164f8ad54', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', -1, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('9a7f43df-94c4-492d-86a1-5f5879f5b2cf', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 0, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('116b030c-e075-416f-92f4-f0b418969def', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 1, 0.5, 0.0, 0.0, 0.0, 9.5, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('0272d180-5dc4-45ff-95e6-4bc5d458d6cf', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 2, 1.0, 0.0, 0.0, 0.0, 9.0, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('9bb61432-ea43-4b0d-ae31-2d3e223390d1', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 3, 2.0, 0.0, 0.0, 0.0, 8.0, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('559f7265-1ae6-4d46-a261-1b23171d1d57', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 4, 3.0, 0.0, 0.0, 0.0, 7.0, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('002ac216-762d-462c-b92f-805f726f4eb5', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 5, 4.0, 0.0, 0.0, 0.0, 6.0, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('fb4806fb-447f-43a3-aaf6-a62d7a4ba6d4', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 6, 4.5, 0.0, 0.0, 0.0, 5.5, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('6f59883f-8a65-42ac-8b73-8e6e6c8f4722', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 7, 5.0, 0.0, 0.0, 0.0, 5.0, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('22b7c838-0b8c-4f3f-92a2-8e078dd3ed1c', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 8, 4.5, 0.0, 0.0, 0.0, 5.5, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('5d6bb5cd-036b-4043-bdf0-83d63276d137', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 9, 4.0, 0.0, 1.0, 0.0, 5.0, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('1394f1e7-1d46-4ce4-b23c-b4153194f2d5', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 10, 3.7, 0.0, 1.5, 0.0, 4.8, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('afa62cce-ea82-4ad3-9092-74ee4ad18057', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 11, 3.4, 0.0, 2.0, 0.0, 4.6, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('aeed1c44-05f9-47c6-9cbb-51d3c5ef9329', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 12, 3.1, 0.0, 2.5, 0.0, 4.4, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('0325eca1-4e5c-4495-971d-a3774478da92', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 13, 2.9, 0.0, 3.0, 0.0, 4.1, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('6a6877c1-5e24-43c2-8640-088964eb69db', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 14, 2.7, 0.0, 3.2, 0.0, 4.1, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('abf38dbf-0e54-4a47-817b-229927ddba19', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 15, 2.5, 0.0, 3.4, 0.0, 4.1, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('fa53cc14-cebe-4806-b2bd-db3f160f17c9', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 16, 2.3, 0.0, 3.6, 0.0, 4.1, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('39045baf-bfce-41a8-bb17-e250328647aa', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 17, 2.1, 0.0, 3.9, 0.0, 4.0, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('56548200-11a1-461b-9beb-ad7adc5b2d70', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 18, 2.0, 0.0, 4.1, 0.0, 3.9, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('4116ba58-b8f1-44c6-8160-867ea0d2a76e', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 19, 1.9, 0.5, 3.6, 0.5, 3.5, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('bbd44731-b3c2-4d44-88a1-42dc6bdca640', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 20, 1.8, 1.0, 3.2, 0.7, 3.3, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('e85825e6-be15-4dcf-895a-b494c3dcefd3', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 21, 1.7, 1.5, 2.8, 1.0, 3.0, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('e292c17b-0875-4cbc-9ab0-e0f29986174e', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 22, 1.6, 1.5, 2.6, 2.0, 2.3, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('3029392e-331d-47b4-84ee-5824b0c84980', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 23, 1.5, 2.0, 2.2, 3.0, 1.3, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('8291142f-4128-458b-924f-22877d13baf9', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 24, 1.4, 2.2, 2.0, 3.0, 1.4, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('ccf16fa3-734e-4f4b-af01-07dcef6f2770', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 25, 1.3, 2.0, 2.0, 3.0, 1.7, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('0741c0b5-3a8f-4e7a-8df4-1b98be4e592c', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 26, 1.2, 2.0, 1.5, 3.5, 1.8, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('f0475a06-74ef-4884-aa77-11f045d8238f', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', 27, 1.1, 2.0, 1.0, 4.0, 1.9, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_points VALUES ('05c1000f-490a-5f90-951f-412a50c00381', '08a67fb1-a5fa-5edd-a203-4b684b828adc', -1, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-10 00:45:04.432447+08', '2026-08-10 00:45:04.432447+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_points VALUES ('863c3f79-53c5-5a47-b776-c215bac4b1d7', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 0, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-10 00:45:04.432447+08', '2026-08-10 00:45:04.432447+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_points VALUES ('2623bedb-d622-50c4-a5b0-56b1fe699ef1', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 1, 1.0, 0.0, 0.0, 0.0, 9.0, '2026-08-10 00:45:04.432447+08', '2026-08-10 00:45:04.432447+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_points VALUES ('54d4a9d0-46c7-57ce-bb1b-c635917a40d0', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 2, 2.0, 0.0, 0.0, 0.0, 8.0, '2026-08-10 00:45:04.432447+08', '2026-08-10 00:45:04.432447+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_points VALUES ('b6bfe20b-72b0-548b-99b0-edf5315e3971', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 3, 3.0, 0.0, 0.0, 0.0, 7.0, '2026-08-10 00:45:04.432447+08', '2026-08-10 00:45:04.432447+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_points VALUES ('3abde88e-9767-5485-8742-46ae5191a031', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 4, 4.0, 0.0, 0.0, 0.0, 6.0, '2026-08-10 00:45:04.432447+08', '2026-08-10 00:45:04.432447+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_points VALUES ('179077d5-fd15-51dc-a55a-6b0d45512763', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 5, 5.0, 0.0, 0.0, 0.0, 5.0, '2026-08-10 00:45:04.432447+08', '2026-08-10 00:45:04.432447+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_points VALUES ('f47e7b06-d2c7-5318-9863-8d09e2016c60', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 6, 6.0, 0.0, 0.0, 0.0, 4.0, '2026-08-10 00:45:04.432447+08', '2026-08-10 00:45:04.432447+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_points VALUES ('74d810c4-7197-57a6-8d67-29ce14a02fc0', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 7, 5.3, 0.0, 1.0, 0.0, 3.7, '2026-08-10 00:45:04.432447+08', '2026-08-10 00:45:04.432447+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_points VALUES ('a181bd7c-0b1b-54af-aab3-3e89eb6bcada', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 8, 4.7, 0.0, 2.0, 0.0, 3.3, '2026-08-10 00:45:04.432447+08', '2026-08-10 00:45:04.432447+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_points VALUES ('d6b9b820-102f-54ba-b091-2bc2c04024cf', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 9, 4.0, 0.0, 3.0, 0.0, 3.0, '2026-08-10 00:45:04.432447+08', '2026-08-10 00:45:04.432447+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_points VALUES ('8d3d53a9-b395-5788-a488-0cd733a4c5c3', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 10, 3.9, 0.0, 2.9, 0.3, 2.9, '2026-08-10 00:45:04.432447+08', '2026-08-10 00:45:04.432447+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_points VALUES ('2336bf3c-7ad7-57c2-aec0-1d1a3f3d8ff0', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 11, 3.8, 0.0, 2.8, 0.7, 2.7, '2026-08-10 00:45:04.432447+08', '2026-08-10 00:45:04.432447+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_points VALUES ('84b9cb44-2155-58eb-b76f-0c4bb640700f', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 12, 3.7, 0.0, 2.7, 1.0, 2.6, '2026-08-10 00:45:04.432447+08', '2026-08-10 00:45:04.432447+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_points VALUES ('5e7b0935-b283-55fd-b068-7d3b5eb3af72', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 13, 3.6, 0.0, 2.6, 1.3, 2.5, '2026-08-10 00:45:04.432447+08', '2026-08-10 00:45:04.432447+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_points VALUES ('b646772a-a9ea-5da8-8605-8a690e985f3b', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 14, 3.4, 0.0, 2.4, 1.7, 2.5, '2026-08-10 00:45:04.432447+08', '2026-08-10 00:45:04.432447+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_points VALUES ('175bb154-230a-51aa-bf97-7cf83186209a', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 15, 3.3, 0.0, 2.3, 2.0, 2.4, '2026-08-10 00:45:04.432447+08', '2026-08-10 00:45:04.432447+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_points VALUES ('f37b48f8-9f77-50c8-807c-194f69711778', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 16, 3.2, 0.0, 2.2, 2.3, 2.3, '2026-08-10 00:45:04.432447+08', '2026-08-10 00:45:04.432447+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_points VALUES ('ea470fd7-eb21-58fe-8e0b-24c8e2e882ce', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 17, 3.1, 0.0, 2.1, 2.7, 2.1, '2026-08-10 00:45:04.432447+08', '2026-08-10 00:45:04.432447+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_points VALUES ('e0ae8f75-7230-5d1f-85d3-a2bef4783691', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 18, 3.0, 0.0, 2.0, 3.0, 2.0, '2026-08-10 00:45:04.432447+08', '2026-08-10 00:45:04.432447+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_points VALUES ('0a52ef06-d7cb-5dfc-9cf8-e2a3dd2a112d', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 19, 2.8, 0.2, 2.0, 3.0, 2.0, '2026-08-10 00:45:04.432447+08', '2026-08-10 00:45:04.432447+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_points VALUES ('b63e11d2-3b0c-567d-8926-5b6b621218c8', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 20, 2.6, 0.4, 2.0, 3.0, 2.0, '2026-08-10 00:45:04.432447+08', '2026-08-10 00:45:04.432447+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_points VALUES ('3a0557dc-fc9e-541c-b824-4771efb7d387', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 21, 2.4, 0.6, 2.0, 3.0, 2.0, '2026-08-10 00:45:04.432447+08', '2026-08-10 00:45:04.432447+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_points VALUES ('8c20e311-53fe-51fe-9adb-bb64bb01ab6f', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 22, 2.2, 0.8, 2.0, 3.0, 2.0, '2026-08-10 00:45:04.432447+08', '2026-08-10 00:45:04.432447+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_points VALUES ('e3bde277-2f7e-5a2b-854d-cfde3d110a48', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 23, 2.0, 1.0, 2.0, 3.0, 2.0, '2026-08-10 00:45:04.432447+08', '2026-08-10 00:45:04.432447+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_points VALUES ('0ff4d0b2-0e21-5802-991d-8e54f4937295', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 24, 2.0, 1.0, 2.0, 3.0, 2.0, '2026-08-10 00:45:04.432447+08', '2026-08-10 00:45:04.432447+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_points VALUES ('5abe7522-2ff1-5a9b-bfe3-4bf9401bb07e', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 25, 2.0, 1.0, 2.0, 3.0, 2.0, '2026-08-10 00:45:04.432447+08', '2026-08-10 00:45:04.432447+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_points VALUES ('cf8cae2f-246d-52dc-b86f-18c73e296766', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 26, 2.5, 0.5, 1.5, 3.5, 2.0, '2026-08-10 00:45:04.432447+08', '2026-08-10 00:45:04.432447+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_points VALUES ('60fa70a4-edd5-5be4-b9e6-b6d8b3d79729', '08a67fb1-a5fa-5edd-a203-4b684b828adc', 27, 3.0, 0.0, 1.0, 4.0, 2.0, '2026-08-10 00:45:04.432447+08', '2026-08-10 00:45:04.432447+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_points VALUES ('14dd0bde-4be5-4dfa-820a-54fb1516232a', '81210424-f77d-4f36-97d8-892ebef7b8ac', -1, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_points VALUES ('fb90390f-08f4-49ad-b6e8-251240da45ad', '81210424-f77d-4f36-97d8-892ebef7b8ac', 0, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_points VALUES ('d56087b2-3636-4121-b686-7c0bcdcbfb48', '81210424-f77d-4f36-97d8-892ebef7b8ac', 1, 0.5, 0.0, 0.0, 0.0, 9.5, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_points VALUES ('e8765caa-9133-4157-a910-46a0f07ace7d', '81210424-f77d-4f36-97d8-892ebef7b8ac', 2, 1.0, 0.0, 0.0, 0.0, 9.0, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_points VALUES ('5689db24-31a5-4810-9ac4-47511c49a694', '81210424-f77d-4f36-97d8-892ebef7b8ac', 3, 2.0, 0.0, 0.0, 0.0, 8.0, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_points VALUES ('96163900-1f9f-4dd1-a997-5a9f2c048187', '81210424-f77d-4f36-97d8-892ebef7b8ac', 4, 3.0, 0.0, 0.0, 0.0, 7.0, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_points VALUES ('dafcb1c0-f68b-4183-b901-ae18e682ad77', '81210424-f77d-4f36-97d8-892ebef7b8ac', 5, 4.0, 0.0, 0.0, 0.0, 6.0, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_points VALUES ('49c9e13b-85da-4da3-ba53-f519d90dcb73', '81210424-f77d-4f36-97d8-892ebef7b8ac', 6, 4.5, 0.0, 0.0, 0.0, 5.5, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_points VALUES ('b096a4e3-28df-42d8-ae35-0a694f0ac9d7', '81210424-f77d-4f36-97d8-892ebef7b8ac', 7, 5.0, 0.0, 0.0, 0.0, 5.0, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_points VALUES ('fab2f85e-e8fb-4898-984f-1eeb412b302e', '81210424-f77d-4f36-97d8-892ebef7b8ac', 8, 4.5, 0.0, 0.0, 0.0, 5.5, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_points VALUES ('43b7c5b4-33d3-448d-b13e-6fd3dfa9e001', '81210424-f77d-4f36-97d8-892ebef7b8ac', 9, 4.0, 0.0, 1.0, 0.0, 5.0, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_points VALUES ('ef0b0b3b-8063-4ee1-953e-2d49e6a70bf1', '81210424-f77d-4f36-97d8-892ebef7b8ac', 10, 3.7, 0.0, 1.5, 0.0, 4.8, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_points VALUES ('5b748b5e-7afb-42d8-8104-36805df77789', '81210424-f77d-4f36-97d8-892ebef7b8ac', 11, 3.4, 0.0, 2.0, 0.0, 4.6, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_points VALUES ('916bf671-7206-4611-9a33-5732ad652e9a', '81210424-f77d-4f36-97d8-892ebef7b8ac', 12, 3.1, 0.0, 2.5, 0.0, 4.4, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_points VALUES ('d14165a1-323d-4efe-bfe7-4b55592ced97', '81210424-f77d-4f36-97d8-892ebef7b8ac', 13, 2.9, 0.0, 3.0, 0.0, 4.1, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_points VALUES ('c3ef8c6a-210f-442d-9d40-eaabdbf81eba', '81210424-f77d-4f36-97d8-892ebef7b8ac', 14, 2.7, 0.0, 3.2, 0.0, 4.1, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_points VALUES ('ba7c9de4-a2c8-4a25-b01a-44a4c7f0157e', '81210424-f77d-4f36-97d8-892ebef7b8ac', 15, 2.5, 0.0, 3.4, 0.0, 4.1, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_points VALUES ('84250aec-85a0-422d-b398-82493f40961d', '81210424-f77d-4f36-97d8-892ebef7b8ac', 16, 2.3, 0.0, 3.6, 0.0, 4.1, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_points VALUES ('5626865f-498b-4274-875c-d569eef5a617', '81210424-f77d-4f36-97d8-892ebef7b8ac', 17, 2.1, 0.0, 3.9, 0.0, 4.0, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_points VALUES ('24f13cc9-a71c-4f11-ab15-3541bc267228', '81210424-f77d-4f36-97d8-892ebef7b8ac', 18, 2.0, 0.0, 4.1, 0.0, 3.9, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_points VALUES ('1c438caf-0596-4d30-af88-3275acadcdca', '81210424-f77d-4f36-97d8-892ebef7b8ac', 19, 1.9, 0.5, 3.6, 0.5, 3.5, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_points VALUES ('13c79367-8826-476b-8cf4-5a350d134656', '81210424-f77d-4f36-97d8-892ebef7b8ac', 20, 1.8, 1.0, 3.2, 0.7, 3.3, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_points VALUES ('5b15f1d8-e30d-4c22-bf28-3c8e32f8f52a', '81210424-f77d-4f36-97d8-892ebef7b8ac', 21, 1.7, 1.5, 2.8, 1.0, 3.0, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_points VALUES ('c0149bd5-036b-4c64-98bf-0203c061b3af', '81210424-f77d-4f36-97d8-892ebef7b8ac', 22, 1.6, 1.5, 2.6, 2.0, 2.3, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_points VALUES ('b7eedaf1-896c-4cf8-ab0b-8714ad5874db', '81210424-f77d-4f36-97d8-892ebef7b8ac', 23, 1.5, 2.0, 2.2, 3.0, 1.3, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_points VALUES ('fe3b602b-5ded-478c-8984-f69ec2428328', '81210424-f77d-4f36-97d8-892ebef7b8ac', 24, 1.4, 2.2, 2.0, 3.0, 1.4, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_points VALUES ('c7de3916-231d-4369-bc35-8b49e257fcd9', '81210424-f77d-4f36-97d8-892ebef7b8ac', 25, 1.3, 2.0, 2.0, 3.0, 1.7, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_points VALUES ('12c80116-95bf-46da-8a48-1b6b66f33997', '81210424-f77d-4f36-97d8-892ebef7b8ac', 26, 1.2, 2.0, 1.5, 3.5, 1.8, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_points VALUES ('f4152fc9-cb09-4c27-a5f0-39151df6b2ef', '81210424-f77d-4f36-97d8-892ebef7b8ac', 27, 1.1, 2.0, 1.0, 4.0, 1.9, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_points VALUES ('ae06fb48-b30d-4817-98ed-620929396912', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 12, 3.1, 0.0, 2.5, 0.0, 4.4, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_points VALUES ('c4d0415d-9db8-421b-bcda-1ca051e60d38', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 13, 2.9, 0.0, 3.0, 0.0, 4.1, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_points VALUES ('daf99a07-717f-4658-9f10-e0556ba0fb01', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 14, 2.7, 0.0, 3.2, 0.0, 4.1, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_points VALUES ('7f670f1f-427d-4757-b92f-a7f075364450', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 15, 2.5, 0.0, 3.4, 0.0, 4.1, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_points VALUES ('b5875409-9ab4-45bc-8404-f11edb2d5f4d', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 16, 2.3, 0.0, 3.6, 0.0, 4.1, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_points VALUES ('e5c9321b-b54f-49bf-88f3-fc4806f263de', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 17, 2.1, 0.0, 3.9, 0.0, 4.0, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_points VALUES ('2afb9074-c493-442b-a86c-831d844d9cca', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 18, 2.0, 0.0, 4.1, 0.0, 3.9, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_points VALUES ('3d50e2e5-8901-4aaf-9111-0065dfcbc929', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 19, 1.9, 0.5, 3.6, 0.5, 3.5, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_points VALUES ('8fe7184c-82ac-42cb-af96-5aa9aae897c7', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 20, 1.8, 1.0, 3.2, 0.7, 3.3, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_points VALUES ('ac64f10a-7c45-488e-9215-a791de1ea49f', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 21, 1.7, 1.5, 2.8, 1.0, 3.0, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_points VALUES ('0f7be558-a654-4a16-9db6-4e4d0b6708de', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 22, 1.6, 1.5, 2.6, 2.0, 2.3, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_points VALUES ('a4287650-aab7-458c-a287-f5d9612ecdb6', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 23, 1.5, 2.0, 2.2, 3.0, 1.3, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_points VALUES ('e4cf053b-1b28-4b43-bafa-853a765dffa6', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 24, 1.4, 2.2, 2.0, 3.0, 1.4, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_points VALUES ('972a7f02-66aa-4c24-96e3-bdb357b60d01', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 25, 1.3, 2.0, 2.0, 3.0, 1.7, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_points VALUES ('4816d8b0-dc67-407d-8c4b-94c0c09bcffa', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 26, 1.2, 2.0, 1.5, 3.5, 1.8, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_points VALUES ('cd2614af-6237-4074-bc5d-4fd682076f01', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', 27, 1.1, 2.0, 1.0, 4.0, 1.9, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_points VALUES ('86eb9fa3-d014-4253-b8cc-eb0bb25ec5ef', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', 27, 1.1, 2.0, 1.0, 4.0, 1.9, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_points VALUES ('0e4d8f29-c114-4207-8eda-4f78df33b1de', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', -1, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_points VALUES ('8e21dcb7-0dfd-4f99-82d2-d6eedf0d7137', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 0, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_points VALUES ('fdda4d81-201c-4c11-8c2c-537170de9ec5', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 1, 0.5, 0.0, 0.0, 0.0, 9.5, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_points VALUES ('4b563503-aa4b-49f7-9a66-2b306b026db2', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', 2, 1.0, 0.0, 0.0, 0.0, 9.0, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');


-- Data for Name: hobby_time_tags; Type: TABLE DATA; Schema: public; Owner: -

INSERT INTO public.hobby_time_tags VALUES ('543e8d73-8eb6-52bb-8540-a0c8b4361dc6', '08a67fb1-a5fa-5edd-a203-4b684b828adc', '爱好1', 'Study', '#93C5FD', 110, 240, 1.5, true, 0, '2026-08-10 00:45:04.427987+08', '2026-08-10 00:45:04.427987+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_tags VALUES ('b7a1d400-7352-5695-911c-9b9f287c3642', '08a67fb1-a5fa-5edd-a203-4b684b828adc', '爱好2', 'Music', '#7DD3FC', 410, 232, 1.3, true, 1, '2026-08-10 00:45:04.427987+08', '2026-08-10 00:45:04.427987+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_tags VALUES ('86203e3b-f178-5991-9c27-bc57a8817fba', '08a67fb1-a5fa-5edd-a203-4b684b828adc', '爱好3', 'Game', '#67E8F9', 195, 150, 1.5, true, 2, '2026-08-10 00:45:04.427987+08', '2026-08-10 00:45:04.427987+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_tags VALUES ('03c6f5c5-ef9b-5f29-9984-e1b52ba09bab', '08a67fb1-a5fa-5edd-a203-4b684b828adc', '爱好4', 'Coding', '#5EEAD4', 340, 110, 1.5, true, 3, '2026-08-10 00:45:04.427987+08', '2026-08-10 00:45:04.427987+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_tags VALUES ('f2f58695-6e7f-515a-a39c-b10d222c314e', '08a67fb1-a5fa-5edd-a203-4b684b828adc', '爱好5', 'Social or Family', '#6EE7B7', 63, 65, 1.5, true, 4, '2026-08-10 00:45:04.427987+08', '2026-08-10 00:45:04.427987+08', '2026-08-13 20:56:28.41561+08');
INSERT INTO public.hobby_time_tags VALUES ('0ff7e323-dc6d-421a-a9aa-d62e73e3988b', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', '爱好1', 'Sport', '#f8b659', 110, 240, 1.5, true, 0, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('8d517955-f0dd-4205-810e-833c4b172312', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', '爱好2', 'Travel', '#57caff', 410, 240, 1.3, true, 1, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('ec6bcad5-03f4-4ca8-ba6b-4bf353ea7a20', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', '爱好3', 'Game', '#656afb', 300, 170, 1.5, true, 2, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('1c0e885f-1d20-4042-90e7-147c1a0a9387', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', '爱好4', 'Coding', '#33ff77', 410, 110, 1.5, true, 3, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('13263e94-334f-4332-9ed1-34774ac4abcb', '4de032ee-97c9-48e3-97d5-6bc5d5ebdf1c', '爱好5', 'Social or Family', '#70fff5', 63, 100, 1.5, true, 4, '2026-08-17 13:15:50.428564+08', '2026-08-17 13:15:50.428564+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('0db7c094-0839-450d-aa6a-5ffb151c3e99', '81210424-f77d-4f36-97d8-892ebef7b8ac', '爱好1', 'Sport', '#f8b659', 110, 240, 1.5, true, 0, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_tags VALUES ('cdbc3ee3-cafa-4893-b540-5f4dc059dd95', '81210424-f77d-4f36-97d8-892ebef7b8ac', '爱好2', 'Travel', '#57caff', 410, 240, 1.3, true, 1, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_tags VALUES ('065cecfb-9829-46d9-af08-462886fda3bd', '81210424-f77d-4f36-97d8-892ebef7b8ac', '爱好3', 'Game', '#656afb', 300, 170, 1.5, true, 2, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_tags VALUES ('58e0a53e-1519-4bcc-89b1-858cd9a96d0f', '81210424-f77d-4f36-97d8-892ebef7b8ac', '爱好4', 'Coding', '#33ff77', 410, 110, 1.5, true, 3, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_tags VALUES ('73a9d0f8-3c22-4714-8f02-bce66544698a', '81210424-f77d-4f36-97d8-892ebef7b8ac', '爱好5', 'Social or Family', '#70fff5', 63, 100, 1.5, true, 4, '2026-08-16 22:56:12.197772+08', '2026-08-16 22:56:12.197772+08', '2026-08-17 19:02:25.625922+08');
INSERT INTO public.hobby_time_tags VALUES ('bd500746-b33d-4880-8340-bc52573fc345', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', '爱好1', 'Sport', '#f8b659', 110, 240, 1.5, true, 0, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_tags VALUES ('901891b0-e038-43a3-a7c8-96b7ee9998a6', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', '爱好2', 'Travel', '#57caff', 410, 240, 1.3, true, 1, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_tags VALUES ('7ab99eed-5b5e-47fc-916f-6e5958fd557a', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', '爱好3', 'Game', '#656afb', 300, 170, 1.5, true, 2, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_tags VALUES ('d8beba6d-1143-4d25-92d4-fb080905172f', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', '爱好4', 'Coding', '#33ff77', 410, 110, 1.5, true, 3, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_tags VALUES ('22edc4ac-c0f3-425e-b7ab-cbfbfb791db5', 'ecf8eac5-dc4d-40ac-b6e5-98e04401e540', '爱好5', 'Social or Family', '#70fff5', 63, 100, 1.5, true, 4, '2026-08-12 22:42:08.490999+08', '2026-08-12 22:42:08.490999+08', '2026-08-17 19:02:58.750725+08');
INSERT INTO public.hobby_time_tags VALUES ('28d8c72b-e4ab-4345-889c-483cc3e24f6e', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', '爱好1', 'Sport', '#f8b659', 110, 240, 1.5, true, 0, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_tags VALUES ('55b2b875-0f2e-45cf-a51a-5824f6dfe4cc', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', '爱好2', 'Travel', '#57caff', 410, 240, 1.3, true, 1, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_tags VALUES ('47113fc1-aba9-4fc2-85d5-f40d9425d8f6', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', '爱好3', 'Game', '#656afb', 300, 170, 1.5, true, 2, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_tags VALUES ('559a5e67-d1a2-40c1-9c55-028f06fe1cca', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', '爱好4', 'Coding', '#33ff77', 410, 110, 1.5, true, 3, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_tags VALUES ('f243f537-91e2-4435-a415-0125d668a59b', 'e0710820-58b3-4ccd-8f9f-9b4c832b8552', '爱好5', 'Social or Family', '#70fff5', 63, 100, 1.5, true, 4, '2026-08-16 22:59:10.181008+08', '2026-08-16 22:59:10.181008+08', '2026-08-17 19:02:59.902078+08');
INSERT INTO public.hobby_time_tags VALUES ('b2ab70f9-d29d-408b-909b-c2993100d091', '46ca6189-6ad5-40a9-a327-aeac604f78c1', '爱好1', 'Sport', '#f8b659', 110, 240, 1.5, true, 0, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_tags VALUES ('fbc1cf12-0649-4064-b8cd-23699b5b2e8e', '46ca6189-6ad5-40a9-a327-aeac604f78c1', '爱好2', 'Travel', '#57caff', 410, 240, 1.3, true, 1, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_tags VALUES ('ade6d1aa-4c14-4b74-84aa-431e0b2dc237', '46ca6189-6ad5-40a9-a327-aeac604f78c1', '爱好3', 'Game', '#656afb', 300, 170, 1.5, true, 2, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_tags VALUES ('c78b4deb-ab83-4781-ad0a-394beb9dc6bc', '46ca6189-6ad5-40a9-a327-aeac604f78c1', '爱好4', 'Coding', '#33ff77', 410, 110, 1.5, true, 3, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_tags VALUES ('38c95aad-0991-4cb9-bbe9-147dfc5549c8', '46ca6189-6ad5-40a9-a327-aeac604f78c1', '爱好5', 'Social or Family', '#70fff5', 63, 100, 1.5, true, 4, '2026-08-17 13:12:08.18099+08', '2026-08-17 13:12:08.18099+08', '2026-08-17 19:03:01.981497+08');
INSERT INTO public.hobby_time_tags VALUES ('3225529b-294b-477a-9fca-9735be19ddb7', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', '爱好1', 'Sport', '#93C5FD', 110, 240, 1.5, true, 0, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('d515afdf-3872-4942-b85e-2aae688405f8', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', '爱好2', 'Travel', '#7DD3FC', 410, 232, 1.3, true, 1, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('6455ddd9-1995-4cf2-a7b2-05d7021de53a', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', '爱好3', 'Game', '#67E8F9', 195, 150, 1.5, true, 2, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('55fe7504-fd59-44f8-ab20-6b66f49b96bf', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', '爱好4', 'Coding', '#5EEAD4', 340, 110, 1.5, true, 3, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('f87e4600-e8be-49e6-bc8e-dcd953944e25', '4fa1e93d-11f5-425b-b576-3cff8a9a93c5', '爱好5', 'Social or Family', '#6EE7B7', 63, 65, 1.5, true, 4, '2026-08-12 00:31:59.194206+08', '2026-08-12 00:31:59.194206+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('3c11e838-621b-459a-b03f-06dfed5ab457', '49261c09-f797-49c4-bf32-f451b28b91de', '爱好1', 'Sport', '#93C5FD', 110, 240, 1.5, true, 0, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('aa25b937-839a-4b59-ad35-4126981f74be', '49261c09-f797-49c4-bf32-f451b28b91de', '爱好2', 'Travel', '#7DD3FC', 410, 232, 1.3, true, 1, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('31cfae50-e762-4856-a8b2-055d9f01bb3f', '49261c09-f797-49c4-bf32-f451b28b91de', '爱好3', 'Game', '#67E8F9', 195, 150, 1.5, true, 2, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('daa37da3-f25e-44ae-acaf-6ed30c16b173', '49261c09-f797-49c4-bf32-f451b28b91de', '爱好4', 'Coding', '#5EEAD4', 340, 110, 1.5, true, 3, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('83196f99-ff8a-4c6e-8e1b-919911524746', '49261c09-f797-49c4-bf32-f451b28b91de', '爱好5', 'Social or Family', '#6EE7B7', 63, 65, 1.5, true, 4, '2026-08-12 00:40:58.147068+08', '2026-08-12 00:40:58.147068+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('a39f0181-bc9b-490c-b703-0d37796e9d8b', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', '爱好1', 'Sport', '#93C5FD', 110, 240, 1.5, true, 0, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('0785ddc0-a4a1-41ce-bf73-0d6cf3c87205', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', '爱好2', 'Travel', '#7DD3FC', 410, 232, 1.3, true, 1, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('48b391e4-e3e9-4831-b16f-0d10ff5ea951', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', '爱好3', 'Game', '#67E8F9', 195, 150, 1.5, true, 2, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('ef1182aa-c0c2-4f78-88f3-b76f2cf045ce', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', '爱好4', 'Coding', '#5EEAD4', 340, 110, 1.5, true, 3, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('a98e98e9-36be-4e2a-91ca-92b5f36c6370', '2f7fc477-62b8-4a4d-b7d4-8005c7ac6de5', '爱好5', 'Social or Family', '#6EE7B7', 63, 65, 1.5, true, 4, '2026-08-12 00:49:38.445116+08', '2026-08-12 00:49:38.445116+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('11ea28da-22e8-4fb3-8680-c6ddd5a27d09', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', '爱好1', 'Sport', '#93C5FD', 110, 240, 1.5, true, 0, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('e1f6b67e-d3dc-478a-8440-4350e700413a', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', '爱好2', 'Travel', '#7DD3FC', 410, 232, 1.3, true, 1, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('5146e80d-d5ac-44e3-8269-d6c68e6dd6aa', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', '爱好3', 'Game', '#67E8F9', 195, 150, 1.5, true, 2, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('86354b28-eb1a-4a32-a8e4-03015782b793', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', '爱好4', 'Coding', '#5EEAD4', 340, 110, 1.5, true, 3, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('8ea30618-bb54-4e00-8d22-532025539081', '3ed6feb0-fcef-4b34-ac11-7b6be365dd5f', '爱好5', 'Social or Family', '#6EE7B7', 63, 65, 1.5, true, 4, '2026-08-12 00:51:31.86534+08', '2026-08-12 00:51:31.86534+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('f6c3ce1b-7b03-4507-a2c8-2d752e7d2513', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', '爱好1', 'Sport', '#93C5FD', 110, 240, 1.5, true, 0, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('a24d555a-f31a-45a3-a80f-797e80498951', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', '爱好2', 'Travel', '#7DD3FC', 410, 232, 1.3, true, 1, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('f044561b-6e5d-440e-bb83-9de771b27ddf', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', '爱好3', 'Game', '#67E8F9', 195, 150, 1.5, true, 2, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('7da83d9d-ca2d-4734-89f3-1aecab855fa8', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', '爱好4', 'Coding', '#5EEAD4', 340, 110, 1.5, true, 3, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('21ac8a78-1674-43f5-9c9e-97887469b554', 'aab5c136-4657-4b8f-91e6-155f4ec6ab82', '爱好5', 'Social or Family', '#6EE7B7', 63, 65, 1.5, true, 4, '2026-08-12 00:53:40.82899+08', '2026-08-12 00:53:40.82899+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('a85e9dc1-33b1-4acf-bfc9-b372a5ff9500', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', '爱好1', 'Sport', '#93C5FD', 110, 240, 1.5, true, 0, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('36631e15-0d17-4bf8-9848-203bb4eaeaf1', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', '爱好2', 'Travel', '#7DD3FC', 410, 232, 1.3, true, 1, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('93dbc06d-37ac-4917-909c-995d748068a3', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', '爱好3', 'Game', '#67E8F9', 300, 170, 1.5, true, 2, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('7749333f-95e3-4cb2-bbf9-bc3ea2e34106', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', '爱好4', 'Coding', '#5EEAD4', 410, 110, 1.5, true, 3, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('f2d15f88-74be-4a51-9456-44125ed0c67f', '2d96d0fa-3598-4e3e-a56a-f843f9edd82f', '爱好5', 'Social or Family', '#6EE7B7', 63, 100, 1.5, true, 4, '2026-08-12 00:58:47.189956+08', '2026-08-12 00:58:47.189956+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('0d1c4864-26d6-4cac-a0cc-ce7fc8f0ebea', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', '爱好1', 'Sport', '#93C5FD', 110, 240, 1.5, true, 0, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('cb0edcdd-304b-4000-80df-a168c14ff2cc', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', '爱好2', 'Travel', '#7DD3FC', 410, 240, 1.3, true, 1, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('79e1ba8a-f3d9-4897-9f86-8095eccd57d9', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', '爱好3', 'Game', '#67E8F9', 300, 170, 1.5, true, 2, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('fa4b228b-ca07-4e6e-b5c9-3bb37dc67696', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', '爱好4', 'Coding', '#5EEAD4', 410, 110, 1.5, true, 3, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('73a4c788-f7ec-4989-a00a-69c1521fe760', 'dde9d421-d9b2-4abe-8639-db1c7b896f5d', '爱好5', 'Social or Family', '#6EE7B7', 63, 100, 1.5, true, 4, '2026-08-12 01:00:03.017629+08', '2026-08-12 01:00:03.017629+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('f0c3edb4-1948-4a3c-be29-be9181d4d34d', '75f9fde8-300c-4616-ad83-aebd9b051891', '爱好1', 'Sport', '#f85959', 110, 240, 1.5, true, 0, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('d94c675c-29a7-4c4e-b799-525a7673fca0', '75f9fde8-300c-4616-ad83-aebd9b051891', '爱好2', 'Travel', '#57caff', 410, 240, 1.3, true, 1, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('0507f6b6-66de-4213-a04c-d1f1f017b77b', '75f9fde8-300c-4616-ad83-aebd9b051891', '爱好3', 'Game', '#656afb', 300, 170, 1.5, true, 2, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('6f0d0324-bf6d-4651-8675-82e837e9de10', '75f9fde8-300c-4616-ad83-aebd9b051891', '爱好4', 'Coding', '#33ff77', 410, 110, 1.5, true, 3, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('4fd2349e-91bc-443a-bcb4-85465e0641c4', '75f9fde8-300c-4616-ad83-aebd9b051891', '爱好5', 'Social or Family', '#ee70ff', 63, 100, 1.5, true, 4, '2026-08-12 01:07:49.59009+08', '2026-08-12 01:07:49.59009+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('8e314ded-ec5e-4e5c-a87b-1365ee40728a', 'b4f1b3ff-0017-4649-9260-277ab323f56f', '爱好1', 'Sport', '#f85959', 110, 240, 1.5, true, 0, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('62e667c9-317e-469b-8422-565dd1aca6fc', 'b4f1b3ff-0017-4649-9260-277ab323f56f', '爱好2', 'Travel', '#57caff', 410, 240, 1.3, true, 1, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('24d7281b-7e92-4f9b-8fef-fd03c7dc9a3b', 'b4f1b3ff-0017-4649-9260-277ab323f56f', '爱好3', 'Game', '#656afb', 300, 170, 1.5, true, 2, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('80edc09b-b0f5-40f6-a49d-6f237ce39169', 'b4f1b3ff-0017-4649-9260-277ab323f56f', '爱好4', 'Coding', '#33ff77', 410, 110, 1.5, true, 3, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('3aaa0ca1-658b-4e92-b547-4671a9cce514', 'b4f1b3ff-0017-4649-9260-277ab323f56f', '爱好5', 'Social or Family', '#70fff5', 63, 100, 1.5, true, 4, '2026-08-12 01:09:41.689444+08', '2026-08-12 01:09:41.689444+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('6c503115-a77a-4dbc-be68-f896f7c2727a', 'be28904d-fd8a-4422-9163-9c2014be29f6', '爱好1', 'Sport', '#e5f859', 110, 240, 1.5, true, 0, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('f621532d-9d2d-4a64-9720-8a5b80c2696f', 'be28904d-fd8a-4422-9163-9c2014be29f6', '爱好2', 'Travel', '#57caff', 410, 240, 1.3, true, 1, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('4b021f7f-60cb-4ac3-bd2f-41475bf40992', 'be28904d-fd8a-4422-9163-9c2014be29f6', '爱好3', 'Game', '#656afb', 300, 170, 1.5, true, 2, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('c5a9e6ed-f5e8-4d6b-b9f7-65ea4c49db0b', 'be28904d-fd8a-4422-9163-9c2014be29f6', '爱好4', 'Coding', '#33ff77', 410, 110, 1.5, true, 3, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('0dbcf8a2-8e30-4011-b014-12bfd26ce3fa', 'be28904d-fd8a-4422-9163-9c2014be29f6', '爱好5', 'Social or Family', '#70fff5', 63, 100, 1.5, true, 4, '2026-08-12 01:12:59.272049+08', '2026-08-12 01:12:59.272049+08', NULL);
INSERT INTO public.hobby_time_tags VALUES ('374c6b4c-db3a-4f82-8b2e-9183e3ac2baf', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', '爱好1', 'Sport', '#f8b659', 110, 240, 1.5, true, 0, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_tags VALUES ('add6591f-e2ed-4bf9-87f9-36cbb0e92852', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', '爱好2', 'Travel', '#57caff', 410, 240, 1.3, true, 1, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_tags VALUES ('f4bf174a-fa75-4e1e-a1e1-3658907584bf', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', '爱好3', 'Game', '#656afb', 300, 170, 1.5, true, 2, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_tags VALUES ('99b41945-954c-4cb9-b254-ff48983529eb', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', '爱好4', 'Coding', '#33ff77', 410, 110, 1.5, true, 3, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');
INSERT INTO public.hobby_time_tags VALUES ('5c16d499-9f30-4b55-a982-1b3425aa88d5', 'f6c20a67-76c8-4fdc-b591-df6bce39626e', '爱好5', 'Social or Family', '#70fff5', 63, 100, 1.5, true, 4, '2026-08-12 22:27:37.870732+08', '2026-08-12 22:27:37.870732+08', '2026-08-17 19:02:57.363995+08');


-- Data for Name: home_images; Type: TABLE DATA; Schema: public; Owner: -

INSERT INTO public.home_images VALUES ('3e34396f-8091-58c5-ae19-58a2813dd2a7', '60b4421f-cb30-591e-835a-82cc23272623', '17a0aca0-a554-5d7c-811f-87950438bd0b', '香港太平山城市远景', '50% 35%', 0, '2026-08-10 00:45:04.401191+08', '2026-08-10 00:45:04.401191+08', NULL);
INSERT INTO public.home_images VALUES ('7a214e9c-0a03-5432-8758-790d6db9eeaa', '60b4421f-cb30-591e-835a-82cc23272623', '39c26cd2-109b-5c9a-99b1-0dcb7d19865a', '蓝天下飞翔的海鸥', '50% 42%', 1, '2026-08-10 00:45:04.401191+08', '2026-08-10 00:45:04.401191+08', NULL);
INSERT INTO public.home_images VALUES ('672bed67-e9d8-5ad5-a10e-fcd96b8ca71c', '60b4421f-cb30-591e-835a-82cc23272623', '266331b3-1978-5495-ad36-362647692ada', '海面与云层', '50% 50%', 2, '2026-08-10 00:45:04.401191+08', '2026-08-10 00:45:04.401191+08', NULL);
INSERT INTO public.home_images VALUES ('80968374-40b5-5362-880a-306ed6534bee', '60b4421f-cb30-591e-835a-82cc23272623', '16760407-ecf2-5587-8d75-000a4c91686d', '夜色城市灯光', '50% 45%', 3, '2026-08-10 00:45:04.401191+08', '2026-08-10 00:45:04.401191+08', NULL);
INSERT INTO public.home_images VALUES ('975d4a14-041d-58eb-b35f-0f66a522f711', '60b4421f-cb30-591e-835a-82cc23272623', 'cd1316ac-c206-584c-982d-e024a084ec2d', '落日晚霞山景', '50% 50%', 4, '2026-08-10 00:45:04.401191+08', '2026-08-10 00:45:04.401191+08', NULL);
INSERT INTO public.home_images VALUES ('ec4b8e0f-09b0-5d42-b428-125ad0c54be0', '60b4421f-cb30-591e-835a-82cc23272623', '931c1f55-66e5-536a-ad42-2ceec8cf5e5d', '海边公路与云', '50% 50%', 5, '2026-08-10 00:45:04.401191+08', '2026-08-10 00:45:04.401191+08', NULL);
INSERT INTO public.home_images VALUES ('0cacc7ff-1340-4855-8fc6-6a17b5e2576f', 'bebf6767-0895-43cb-88fa-a4116d49f63f', '17a0aca0-a554-5d7c-811f-87950438bd0b', '香港太平山城市远景', '50% 35%', 0, '2026-08-10 12:41:33.199247+08', '2026-08-10 12:41:33.199247+08', NULL);
INSERT INTO public.home_images VALUES ('068aeb58-a9ce-43bf-acf2-a064ba249cef', 'bebf6767-0895-43cb-88fa-a4116d49f63f', '39c26cd2-109b-5c9a-99b1-0dcb7d19865a', '蓝天下飞翔的海鸥', '50% 42%', 1, '2026-08-10 12:41:33.199247+08', '2026-08-10 12:41:33.199247+08', NULL);
INSERT INTO public.home_images VALUES ('138b7412-bed0-4883-9acd-14e1ed27af98', 'bebf6767-0895-43cb-88fa-a4116d49f63f', '266331b3-1978-5495-ad36-362647692ada', '海面与云层', '50% 50%', 2, '2026-08-10 12:41:33.199247+08', '2026-08-10 12:41:33.199247+08', NULL);
INSERT INTO public.home_images VALUES ('bb8e0b1a-561f-43d4-83f4-4866ae320766', 'bebf6767-0895-43cb-88fa-a4116d49f63f', '16760407-ecf2-5587-8d75-000a4c91686d', '夜色城市灯光', '50% 45%', 3, '2026-08-10 12:41:33.199247+08', '2026-08-10 12:41:33.199247+08', NULL);
INSERT INTO public.home_images VALUES ('15014f1b-3885-41a8-820b-5590c5102df8', 'bebf6767-0895-43cb-88fa-a4116d49f63f', 'cd1316ac-c206-584c-982d-e024a084ec2d', '落日晚霞山景', '50% 50%', 4, '2026-08-10 12:41:33.199247+08', '2026-08-10 12:41:33.199247+08', NULL);
INSERT INTO public.home_images VALUES ('b7278c8f-ee4f-43e1-b5d8-51882bf70bba', 'bebf6767-0895-43cb-88fa-a4116d49f63f', '931c1f55-66e5-536a-ad42-2ceec8cf5e5d', '海边公路与云', '50% 50%', 5, '2026-08-10 12:41:33.199247+08', '2026-08-10 12:41:33.199247+08', NULL);
INSERT INTO public.home_images VALUES ('2721fd2c-c4fe-479f-bd54-84953ead177f', 'bf85d3b0-7d38-485b-a4f3-ca8980f08a99', '17a0aca0-a554-5d7c-811f-87950438bd0b', '香港太平山城市远景', '50% 35%', 0, '2026-08-10 12:55:55.722717+08', '2026-08-10 12:55:55.722717+08', NULL);
INSERT INTO public.home_images VALUES ('b07102e0-f6c0-4755-9035-2c2d798f9b36', 'bf85d3b0-7d38-485b-a4f3-ca8980f08a99', '39c26cd2-109b-5c9a-99b1-0dcb7d19865a', '蓝天下飞翔的海鸥', '50% 42%', 1, '2026-08-10 12:55:55.722717+08', '2026-08-10 12:55:55.722717+08', NULL);
INSERT INTO public.home_images VALUES ('863526f1-5d18-4c5f-92a5-aa016f53eb0a', 'bf85d3b0-7d38-485b-a4f3-ca8980f08a99', 'cd1316ac-c206-584c-982d-e024a084ec2d', '落日晚霞山景', '50% 50%', 2, '2026-08-10 12:55:55.722717+08', '2026-08-10 12:55:55.722717+08', NULL);
INSERT INTO public.home_images VALUES ('f7d75007-bec1-4e07-84e4-df412928cfff', 'bf85d3b0-7d38-485b-a4f3-ca8980f08a99', '266331b3-1978-5495-ad36-362647692ada', '海面与云层', '50% 50%', 3, '2026-08-10 12:55:55.722717+08', '2026-08-10 12:55:55.722717+08', NULL);
INSERT INTO public.home_images VALUES ('841c3803-93f5-42d8-a838-01e87ac38dbd', 'bf85d3b0-7d38-485b-a4f3-ca8980f08a99', '16760407-ecf2-5587-8d75-000a4c91686d', '夜色城市灯光', '50% 45%', 4, '2026-08-10 12:55:55.722717+08', '2026-08-10 12:55:55.722717+08', NULL);
INSERT INTO public.home_images VALUES ('1abcd06b-3b28-42da-b788-131f58142aa3', 'bf85d3b0-7d38-485b-a4f3-ca8980f08a99', '931c1f55-66e5-536a-ad42-2ceec8cf5e5d', '海边公路与云', '50% 50%', 5, '2026-08-10 12:55:55.722717+08', '2026-08-10 12:55:55.722717+08', NULL);
INSERT INTO public.home_images VALUES ('d1646010-d30f-4e08-a0c1-d9709f34fa2d', 'b2c66006-9e77-4c93-a033-1d310e775cbf', '17a0aca0-a554-5d7c-811f-87950438bd0b', '香港太平山城市远景', '50% 35%', 0, '2026-08-13 22:10:38.724226+08', '2026-08-13 22:10:38.724226+08', NULL);
INSERT INTO public.home_images VALUES ('bcbf7a7e-92e3-451c-948b-3fa0c94d2d19', 'b2c66006-9e77-4c93-a033-1d310e775cbf', '39c26cd2-109b-5c9a-99b1-0dcb7d19865a', '蓝天下飞翔的海鸥', '50% 42%', 1, '2026-08-13 22:10:38.724226+08', '2026-08-13 22:10:38.724226+08', NULL);
INSERT INTO public.home_images VALUES ('25659923-4bbf-43a8-9303-2da0c3656528', 'b2c66006-9e77-4c93-a033-1d310e775cbf', 'cd1316ac-c206-584c-982d-e024a084ec2d', '落日晚霞山景', '50% 50%', 2, '2026-08-13 22:10:38.724226+08', '2026-08-13 22:10:38.724226+08', NULL);
INSERT INTO public.home_images VALUES ('b3815668-9c0a-4b25-a53c-75498a9b51a8', 'b2c66006-9e77-4c93-a033-1d310e775cbf', '266331b3-1978-5495-ad36-362647692ada', '海面与云层', '50% 50%', 3, '2026-08-13 22:10:38.724226+08', '2026-08-13 22:10:38.724226+08', NULL);
INSERT INTO public.home_images VALUES ('d16c1120-a6b2-45d3-9e8e-6500618ab1ac', 'b2c66006-9e77-4c93-a033-1d310e775cbf', '931c1f55-66e5-536a-ad42-2ceec8cf5e5d', '海边公路与云', '50% 50%', 4, '2026-08-13 22:10:38.724226+08', '2026-08-13 22:10:38.724226+08', NULL);
INSERT INTO public.home_images VALUES ('073202bb-4709-4115-97f8-e979e14742df', 'b2c66006-9e77-4c93-a033-1d310e775cbf', '16760407-ecf2-5587-8d75-000a4c91686d', '夜色城市灯光', '50% 45%', 5, '2026-08-13 22:10:38.724226+08', '2026-08-13 22:10:38.724226+08', NULL);
INSERT INTO public.home_images VALUES ('318e186b-9263-40f1-8549-ebde6c437dc7', '897c1c16-c070-461a-8d93-109d24c17979', '17a0aca0-a554-5d7c-811f-87950438bd0b', '香港太平山城市远景', '50% 35%', 0, '2026-08-13 22:11:04.814812+08', '2026-08-13 22:11:04.814812+08', NULL);
INSERT INTO public.home_images VALUES ('a9438491-8e88-4b3d-b2ee-112d1eccbb15', '897c1c16-c070-461a-8d93-109d24c17979', '39c26cd2-109b-5c9a-99b1-0dcb7d19865a', '蓝天下飞翔的海鸥', '50% 42%', 1, '2026-08-13 22:11:04.814812+08', '2026-08-13 22:11:04.814812+08', NULL);
INSERT INTO public.home_images VALUES ('6f3366e1-24f7-4785-8be3-774c415eb982', '897c1c16-c070-461a-8d93-109d24c17979', 'cd1316ac-c206-584c-982d-e024a084ec2d', '落日晚霞山景', '50% 50%', 2, '2026-08-13 22:11:04.814812+08', '2026-08-13 22:11:04.814812+08', NULL);
INSERT INTO public.home_images VALUES ('a69311c3-9300-40e6-8a80-95f3e55ce21d', '897c1c16-c070-461a-8d93-109d24c17979', '266331b3-1978-5495-ad36-362647692ada', '海面与云层', '50% 50%', 3, '2026-08-13 22:11:04.814812+08', '2026-08-13 22:11:04.814812+08', NULL);
INSERT INTO public.home_images VALUES ('4976fdf7-bd47-4767-b97a-a0c9ded64876', '897c1c16-c070-461a-8d93-109d24c17979', '16760407-ecf2-5587-8d75-000a4c91686d', '夜色城市灯光', '50% 45%', 4, '2026-08-13 22:11:04.814812+08', '2026-08-13 22:11:04.814812+08', NULL);
INSERT INTO public.home_images VALUES ('efaf5e98-02c9-44d0-afd3-128a8875cdb4', '897c1c16-c070-461a-8d93-109d24c17979', '931c1f55-66e5-536a-ad42-2ceec8cf5e5d', '海边公路与云', '50% 50%', 5, '2026-08-13 22:11:04.814812+08', '2026-08-13 22:11:04.814812+08', NULL);


-- Data for Name: mylab_card_tags; Type: TABLE DATA; Schema: public; Owner: -

INSERT INTO public.mylab_card_tags VALUES ('c8365100-6fb0-5084-a0b9-67974f4e1533', '56c60853-3cca-59e6-831a-f141df2d0497', 'cd3776a7-8d5f-5c3f-b7d2-b81fc8974d7a', 0, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('82321c64-1764-5584-8b83-b9bd78a90bc3', '56c60853-3cca-59e6-831a-f141df2d0497', '9b7ab32a-5c1f-566b-a8dc-48bdf9a6fc16', 1, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('5412c582-ac7b-5c20-b984-752a65184b6e', '56c60853-3cca-59e6-831a-f141df2d0497', '100b7029-067a-59db-8c2b-f9c524fb520c', 2, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('55f5473f-dc2f-5d1f-aaaf-a5dd1b0e642f', '56c60853-3cca-59e6-831a-f141df2d0497', 'f153754c-e92a-5d82-9642-250ef1972aa8', 3, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('700c6ef7-f720-5acb-a356-b666785fd8fb', '3b30b9c5-72db-584c-ab32-bca5fca782d9', 'cd3776a7-8d5f-5c3f-b7d2-b81fc8974d7a', 0, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('bce8dcd0-6c27-50fd-ab0f-4a86b4873d57', '3b30b9c5-72db-584c-ab32-bca5fca782d9', '2d9db0ad-a7fb-5bcf-99ff-040ba892267c', 1, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('1ea3f4f9-c04e-54c5-9bcd-86bd6483465c', '3b30b9c5-72db-584c-ab32-bca5fca782d9', '0fc6f305-8fae-53fd-ac1a-85d060a4f2bf', 2, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('8f77f363-cfbd-5a66-836d-28fd5d2c1a5c', '1eb33cdd-2066-597c-9367-4b148ec80bf4', 'cd3776a7-8d5f-5c3f-b7d2-b81fc8974d7a', 0, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('fdb7ebfd-440d-5b0b-85e4-e1076971350e', '1eb33cdd-2066-597c-9367-4b148ec80bf4', '6310e463-4ad4-59f3-8cf3-e735e3fd3691', 1, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('199143ca-0b22-5f88-9264-72d9c4766053', '1eb33cdd-2066-597c-9367-4b148ec80bf4', 'faba2f26-a83f-575a-9c0e-590251dc6909', 2, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('d02b7b3e-12fb-563c-8604-551993d37a75', '1eb5e084-af77-5474-b2d4-e5ffe38c0ce0', '9919a272-2ca5-5a32-9796-d6d90f4c9b3b', 0, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('a8b8448c-fa06-5700-80ed-b61f95b66eb8', '1eb5e084-af77-5474-b2d4-e5ffe38c0ce0', '16649271-892d-57bf-8555-a43662e53956', 1, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('fc07bdff-b57d-5de2-ba92-a2cab95313a6', '1eb5e084-af77-5474-b2d4-e5ffe38c0ce0', '85dd4c28-cbe0-52b4-88bf-82cd376f20f3', 2, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('1be88b13-6535-57ec-a3e4-42ba620ec280', '1eb5e084-af77-5474-b2d4-e5ffe38c0ce0', '902ac023-ea59-51e6-b308-75aae2fefa14', 3, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('8750bfef-b610-51cd-8c25-84ae4b0cbcbe', '68501165-e263-5c9a-86fb-e4c16f3945f7', '37cd8fe1-52cc-525d-a286-5c8930205148', 0, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('aa47cbe3-fadd-57e9-9ac0-b0734eeaa2cf', '68501165-e263-5c9a-86fb-e4c16f3945f7', '40685b33-1982-589e-8c09-2df8b75b5655', 1, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('cb7e7f3c-63c1-5542-9a8f-f775c65c627a', '68501165-e263-5c9a-86fb-e4c16f3945f7', '11894919-dbfa-5d3d-8a2f-3bfa1580c4b6', 2, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('7419e9c8-3823-5ccf-8c20-60076e2869f5', '68501165-e263-5c9a-86fb-e4c16f3945f7', '1bba22a4-d4c5-5f5e-8dcf-bce71d13388a', 3, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('0c49b746-d787-55cd-898d-ad1cd0e3bc94', 'd8180653-8f46-5cec-a634-cd69f336a295', '7b9563ee-ecd1-5910-a4d0-d508b51223f5', 0, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('24a1c984-2f0a-5b31-80c6-9db8f9f684a0', 'd8180653-8f46-5cec-a634-cd69f336a295', 'e64d08a8-eedf-5d6c-be92-91a51a9c294f', 1, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('4079329a-28da-5542-ae1d-98d7a124016a', 'd8180653-8f46-5cec-a634-cd69f336a295', 'c144f9fa-8ffb-5225-a51c-2b68aaeb276c', 2, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('78005f43-ccf1-5ad9-9f5b-10f5e0a86185', 'd8180653-8f46-5cec-a634-cd69f336a295', 'b08090e1-19eb-598d-bbee-1ff39bcf31a0', 3, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('4f47dd82-26fb-508a-a9aa-2b4c82b7c04d', '7209d613-0ee6-5b8d-a14d-71d2a7228fe1', 'b820842b-8c3e-5dba-a955-393633ab888e', 0, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('712ab87d-98fd-59a5-a731-f0734ae7f1ba', '7209d613-0ee6-5b8d-a14d-71d2a7228fe1', '85538585-342a-58bb-b4d5-37fe10d2f05e', 1, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('d6f58f50-e3e1-54ec-a095-1c3b670c2773', '7209d613-0ee6-5b8d-a14d-71d2a7228fe1', '61dc6897-31dd-5b1f-bc22-b32e1ad4c51c', 2, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('f4c4921b-7c2d-5bb4-85b3-bb4c19be6055', 'e674dc05-c563-57d5-a5ba-df51bb942c12', 'e64d08a8-eedf-5d6c-be92-91a51a9c294f', 0, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('38dfa4d7-d2f1-56bb-8a09-653cc040fb1a', 'e674dc05-c563-57d5-a5ba-df51bb942c12', 'bed13764-9b69-54c0-b976-a9a0f6a594ae', 1, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('e8fdfd1f-bc34-516e-bddb-f641df6ee9b7', 'e674dc05-c563-57d5-a5ba-df51bb942c12', 'be50c0a5-d841-5287-80cf-b3272fa0277b', 2, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('8df83434-39ae-5a3c-8c6f-71f5643a8b41', 'b4fcced3-778a-5158-894e-6e7d11f3f477', '0ad86620-6da9-5bee-890c-b8a10f8a845b', 0, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('6b586f50-0a40-54ac-a28e-90b855576a46', 'b4fcced3-778a-5158-894e-6e7d11f3f477', '5ca12400-1681-5462-afe2-9f08f9e629d1', 1, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('dc9deed7-b809-5630-a278-6e4e9452c300', '12ae81d0-cd5f-533b-bc4d-e5b758f7503e', 'd4f70c09-16a3-54a1-b715-dc608b90859e', 0, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('d8e0215a-da17-507b-b6bf-80cb88a2c899', '12ae81d0-cd5f-533b-bc4d-e5b758f7503e', 'be50c0a5-d841-5287-80cf-b3272fa0277b', 1, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('276f563c-5277-5c5b-8c62-ba0740e9adc7', '12ae81d0-cd5f-533b-bc4d-e5b758f7503e', '4ac8673b-86ef-5af1-a70a-e446cc2cf799', 2, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('0baf9707-da07-55ba-851a-68ad9fe5c5f3', 'd9585765-d9fd-5ad6-bebc-77f0d055e1eb', 'f82960eb-2fb1-57c2-86d2-ce5b7a1650e0', 0, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('a4877572-ba8d-57d7-b57a-f5846b27c267', 'd9585765-d9fd-5ad6-bebc-77f0d055e1eb', '61dc6897-31dd-5b1f-bc22-b32e1ad4c51c', 1, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('db3ef358-d189-537c-b8a3-9455ce441e28', 'd9585765-d9fd-5ad6-bebc-77f0d055e1eb', 'a0594a0e-8118-5315-9180-7a8f390342da', 2, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('b4698070-5a66-59be-82b2-95d376d81bac', 'ff079c66-547a-57e8-9c14-47b65010fafe', 'e64d08a8-eedf-5d6c-be92-91a51a9c294f', 0, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('aae2f60e-1508-52ac-a4b4-24fe276bfc11', 'ff079c66-547a-57e8-9c14-47b65010fafe', 'be50c0a5-d841-5287-80cf-b3272fa0277b', 1, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('aaced9e0-e2df-5808-a721-05cb65c2aeba', 'f35f251f-64e0-5622-8504-b20bfea008a9', '0ad86620-6da9-5bee-890c-b8a10f8a845b', 0, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('e9bac66c-9549-55b2-a64e-7e95eb3f5f71', 'f35f251f-64e0-5622-8504-b20bfea008a9', '5ca12400-1681-5462-afe2-9f08f9e629d1', 1, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('3edd0135-294e-58b0-b6ea-1a5d9e44a29f', 'f35f251f-64e0-5622-8504-b20bfea008a9', 'f064a292-232f-5c61-8ca2-8389e50bbf1b', 2, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('86696a8c-7352-5144-bbf6-87ec3b451842', 'c028966d-908a-51dd-a06d-d4af028b4709', 'ce23d581-aa67-5c91-a808-0fba88586935', 0, '2026-08-10 00:45:04.451497+08', '2026-08-10 00:45:04.451497+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('8362d242-e21d-4ad6-8f57-8cc54c3f0b1c', '4e0b681e-995f-4d19-a480-ee9a75ff7417', 'cd3776a7-8d5f-5c3f-b7d2-b81fc8974d7a', 0, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('5c0446e8-b241-47c8-8047-b0a173aab827', '4e0b681e-995f-4d19-a480-ee9a75ff7417', '9b7ab32a-5c1f-566b-a8dc-48bdf9a6fc16', 1, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('a8827861-bf50-474d-aa9d-d4f227f19543', '4e0b681e-995f-4d19-a480-ee9a75ff7417', '100b7029-067a-59db-8c2b-f9c524fb520c', 2, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('a22cc425-f4b8-45ad-ac39-514559024bbc', '4e0b681e-995f-4d19-a480-ee9a75ff7417', 'f153754c-e92a-5d82-9642-250ef1972aa8', 3, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('c257b963-3c26-45d5-894b-741f8ee5f1ad', '4243fb7e-be3e-4217-8d88-228fe222dcbd', 'cd3776a7-8d5f-5c3f-b7d2-b81fc8974d7a', 0, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('1c81d905-ebd6-4626-8865-b46542e85e1a', '4243fb7e-be3e-4217-8d88-228fe222dcbd', '2d9db0ad-a7fb-5bcf-99ff-040ba892267c', 1, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('2067e2fc-4267-4106-9559-f63610cc040e', '4243fb7e-be3e-4217-8d88-228fe222dcbd', '0fc6f305-8fae-53fd-ac1a-85d060a4f2bf', 2, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('97dbfd88-8153-442a-b734-3ee7aaafc3fc', '128248c5-e8b4-4ac7-a97d-75295a7bf209', 'cd3776a7-8d5f-5c3f-b7d2-b81fc8974d7a', 0, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('7dcbe02b-1cc1-4f3f-a7ea-ca4b3cf97938', '128248c5-e8b4-4ac7-a97d-75295a7bf209', '6310e463-4ad4-59f3-8cf3-e735e3fd3691', 1, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('7327ef73-bd9b-43fc-9f37-6f38ca2e4085', '128248c5-e8b4-4ac7-a97d-75295a7bf209', 'faba2f26-a83f-575a-9c0e-590251dc6909', 2, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('e475b381-984b-451f-a4fe-97d4fbdaea5d', '89e0e724-ba1c-4d97-a749-db35b582c442', '16649271-892d-57bf-8555-a43662e53956', 0, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('c50c1c1c-5b5b-4a61-a512-66d7eb98baaf', '89e0e724-ba1c-4d97-a749-db35b582c442', '85dd4c28-cbe0-52b4-88bf-82cd376f20f3', 1, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('1414fd78-0821-4a9e-83b9-251ae3cfd11e', '89e0e724-ba1c-4d97-a749-db35b582c442', '902ac023-ea59-51e6-b308-75aae2fefa14', 2, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('49cc98c4-21c6-4c42-89f5-9d6fa71ca067', 'dd97bc37-d174-41f6-8feb-37c04dfe4f6e', '40685b33-1982-589e-8c09-2df8b75b5655', 0, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('5dae750a-6e30-4ad1-b794-8b85ca756c44', 'dd97bc37-d174-41f6-8feb-37c04dfe4f6e', '11894919-dbfa-5d3d-8a2f-3bfa1580c4b6', 1, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('07ec8900-1de8-4b77-926a-41e60828034e', 'dd97bc37-d174-41f6-8feb-37c04dfe4f6e', '1bba22a4-d4c5-5f5e-8dcf-bce71d13388a', 2, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('43c9b83e-08a7-4074-8f23-6d1fd206555f', '819e7e7c-68d6-49c1-8e71-440056f58176', 'e64d08a8-eedf-5d6c-be92-91a51a9c294f', 0, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('0b7d4cd0-7a60-42ee-8502-0303949fdc76', '819e7e7c-68d6-49c1-8e71-440056f58176', 'c144f9fa-8ffb-5225-a51c-2b68aaeb276c', 1, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('eda6c8c5-4c16-4431-af5f-e5a50cadfc32', '819e7e7c-68d6-49c1-8e71-440056f58176', 'b08090e1-19eb-598d-bbee-1ff39bcf31a0', 2, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('7b71117f-975d-4ea3-ad34-1d0404ee360e', 'cb9612f3-8a7b-422b-ab76-46f6dffdb1f0', 'b820842b-8c3e-5dba-a955-393633ab888e', 0, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('85f6a18c-b814-4688-902e-231498876c41', 'cb9612f3-8a7b-422b-ab76-46f6dffdb1f0', '85538585-342a-58bb-b4d5-37fe10d2f05e', 1, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('1315b195-6138-412f-abd9-6b24dc9bb8b2', 'cb9612f3-8a7b-422b-ab76-46f6dffdb1f0', '61dc6897-31dd-5b1f-bc22-b32e1ad4c51c', 2, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('a6740b48-082b-417b-9341-8e8a9c06b2f4', '1887dc02-b9d7-489c-8e58-8bc9335df1ba', 'e64d08a8-eedf-5d6c-be92-91a51a9c294f', 0, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('5ff6861b-ff6e-4d98-a52b-73844eef1dba', '1887dc02-b9d7-489c-8e58-8bc9335df1ba', 'bed13764-9b69-54c0-b976-a9a0f6a594ae', 1, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('79e9f940-73a6-43fb-9abb-bcf516b30853', '1887dc02-b9d7-489c-8e58-8bc9335df1ba', 'be50c0a5-d841-5287-80cf-b3272fa0277b', 2, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('2d4ba2ea-df4d-4f0f-b96e-23f74ea3729e', '1d5504f7-07c1-4e38-87f5-9a03678d12dc', '0ad86620-6da9-5bee-890c-b8a10f8a845b', 0, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('2deec248-fad1-4627-aa99-b6a3b4c516c7', '1d5504f7-07c1-4e38-87f5-9a03678d12dc', '5ca12400-1681-5462-afe2-9f08f9e629d1', 1, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('0504d732-52fb-49f6-8734-4fb278897081', '500e9f16-65c3-4161-bb4c-a9c8c74fa0ea', 'd4f70c09-16a3-54a1-b715-dc608b90859e', 0, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('09aab806-58d1-40f0-b16e-f0236150e677', '500e9f16-65c3-4161-bb4c-a9c8c74fa0ea', 'be50c0a5-d841-5287-80cf-b3272fa0277b', 1, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('cbda3670-dcf8-42ba-b005-771c075f26d7', '500e9f16-65c3-4161-bb4c-a9c8c74fa0ea', '4ac8673b-86ef-5af1-a70a-e446cc2cf799', 2, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('e73ab472-dbf9-4f39-907c-7f1212bdf9e9', '884d3d97-0381-4560-8516-e7d9269fc1fd', 'f82960eb-2fb1-57c2-86d2-ce5b7a1650e0', 0, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('697711dc-2a3f-4d0c-b3ba-a85b91087c18', '884d3d97-0381-4560-8516-e7d9269fc1fd', '61dc6897-31dd-5b1f-bc22-b32e1ad4c51c', 1, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('be8ed39d-09c4-4394-9464-fa989b6cb2ed', '884d3d97-0381-4560-8516-e7d9269fc1fd', 'a0594a0e-8118-5315-9180-7a8f390342da', 2, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('a0452b6b-2e68-47ba-9fac-c6d2dd3a8b82', 'f0aff8be-0687-4858-83d9-612406950c74', 'e64d08a8-eedf-5d6c-be92-91a51a9c294f', 0, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('b2632933-eaf7-4ae0-a57d-2ae934d446b7', 'f0aff8be-0687-4858-83d9-612406950c74', 'be50c0a5-d841-5287-80cf-b3272fa0277b', 1, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('359506d3-4ac8-41eb-bb9f-4a408f87a4f5', '4329819c-ac77-4546-b513-3716877985bc', '0ad86620-6da9-5bee-890c-b8a10f8a845b', 0, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('37089e7e-0c8a-4f8a-9183-a05e7c8d0dce', '4329819c-ac77-4546-b513-3716877985bc', '5ca12400-1681-5462-afe2-9f08f9e629d1', 1, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('1da27c8e-debc-462f-b5c5-497bc1cec8f6', '4329819c-ac77-4546-b513-3716877985bc', 'f064a292-232f-5c61-8ca2-8389e50bbf1b', 2, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_card_tags VALUES ('790ff6de-fc57-4e28-8cc0-f5e2c25e4ff1', '363a5420-a825-4418-9760-a142607b5bbb', 'ce23d581-aa67-5c91-a808-0fba88586935', 0, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);


-- Data for Name: mylab_cards; Type: TABLE DATA; Schema: public; Owner: -

INSERT INTO public.mylab_cards VALUES ('56c60853-3cca-59e6-831a-f141df2d0497', '8eb7a954-14b1-5b57-a0f1-692b8d6a9e1b', 'project-gm1', 'Moth and Bat：项目研究记录', '48 小时 GameJam 作品，关于夜色中两种生物的相会。', '2024-01-01', true, 0, 'PROJECT', 0, '这是一款关于夜晚相遇的解谜游戏。

Unity、C#、Aseprite', '2026-08-10 00:45:04.446688+08', '2026-08-10 00:45:04.446688+08', NULL);
INSERT INTO public.mylab_cards VALUES ('3b30b9c5-72db-584c-ab32-bca5fca782d9', '8eb7a954-14b1-5b57-a0f1-692b8d6a9e1b', 'project-gm2', 'Naughty Cat：项目研究记录', '一只总想搞破坏的猫与一个不肯关机的扫地机器人。', '2023-01-01', true, 1, 'PROJECT', 1, '一款轻松幽默的平台跳跃游戏。

Godot、GDScript', '2026-08-10 00:45:04.446688+08', '2026-08-10 00:45:04.446688+08', NULL);
INSERT INTO public.mylab_cards VALUES ('1eb33cdd-2066-597c-9367-4b148ec80bf4', '8eb7a954-14b1-5b57-a0f1-692b8d6a9e1b', 'project-gm3', 'Naughty Boy：项目研究记录', '规则与违抗之间的游戏化实验，关于儿童行为心理学的隐喻。', '2023-01-01', true, 2, 'PROJECT', 2, '探索规则边界的叙事游戏。

Phaser、JavaScript', '2026-08-10 00:45:04.446688+08', '2026-08-10 00:45:04.446688+08', NULL);
INSERT INTO public.mylab_cards VALUES ('1eb5e084-af77-5474-b2d4-e5ffe38c0ce0', '8eb7a954-14b1-5b57-a0f1-692b8d6a9e1b', 'project-gm4', 'Ring of Elysium：项目研究记录', '参与腾讯北极光工作室《无限法则》的玩法与系统设计。', '2022-01-01', true, 3, 'PROJECT', 3, '作为玩法设计师参与开发的大逃杀游戏。

Unreal Engine、C++、Lua', '2026-08-10 00:45:04.446688+08', '2026-08-10 00:45:04.446688+08', NULL);
INSERT INTO public.mylab_cards VALUES ('68501165-e263-5c9a-86fb-e4c16f3945f7', '8eb7a954-14b1-5b57-a0f1-692b8d6a9e1b', 'project-gm5', 'Moodlog：项目研究记录', '一个极简的情绪记录工具，专注输入体验与一年后的回看。', '2024-01-01', true, 4, 'PROJECT', 4, '帮助你记录情绪变化的日常工具。

React、TypeScript、Supabase', '2026-08-10 00:45:04.446688+08', '2026-08-10 00:45:04.446688+08', NULL);
INSERT INTO public.mylab_cards VALUES ('d8180653-8f46-5cec-a634-cd69f336a295', '8eb7a954-14b1-5b57-a0f1-692b8d6a9e1b', 'project-gm6', 'Beat Lab：项目研究记录', '浏览器内的鼓机与音序器，使用 Web Audio API 实时合成。', '2023-01-01', true, 5, 'PROJECT', 5, '在线音乐创作工具。

Vue、Web Audio API、Tone.js', '2026-08-10 00:45:04.446688+08', '2026-08-10 00:45:04.446688+08', NULL);
INSERT INTO public.mylab_cards VALUES ('7209d613-0ee6-5b8d-a14d-71d2a7228fe1', '8eb7a954-14b1-5b57-a0f1-692b8d6a9e1b', 'blog-docker-deploy', '个人博客 Docker + Nginx 部署全流程记录', '从 Dockerfile 多阶段构建到 nginx SPA 回退与 gzip 配置，把博客塞进容器的完整折腾过程。', '2026-07-28', true, 6, 'ARTICLE', NULL, NULL, '2026-08-10 00:45:04.446688+08', '2026-08-10 00:45:04.446688+08', NULL);
INSERT INTO public.mylab_cards VALUES ('e674dc05-c563-57d5-a5ba-df51bb942c12', '8eb7a954-14b1-5b57-a0f1-692b8d6a9e1b', 'vue-gsap-hero', '用 GSAP 给首页 Hero 做电影感动效', 'ScrollTrigger 驱动的滚动叙事：分镜、视差与滚动提示文字的入场编排。', '2026-07-15', true, 7, 'ARTICLE', NULL, NULL, '2026-08-10 00:45:04.446688+08', '2026-08-10 00:45:04.446688+08', NULL);
INSERT INTO public.mylab_cards VALUES ('b4fcced3-778a-5158-894e-6e7d11f3f477', '8eb7a954-14b1-5b57-a0f1-692b8d6a9e1b', 'leetcode-binary-search', '二分查找的几种边界写法整理', '闭区间 / 左闭右开两种模板的循环不变量对比，附几道经典题的应用。', '2026-06-30', true, 8, 'ARTICLE', NULL, NULL, '2026-08-10 00:45:04.446688+08', '2026-08-10 00:45:04.446688+08', NULL);
INSERT INTO public.mylab_cards VALUES ('12ae81d0-cd5f-533b-bc4d-e5b758f7503e', '8eb7a954-14b1-5b57-a0f1-692b8d6a9e1b', 'tailwind-migration', '项目迁移 Tailwind CSS v4 的坑', 'v4 改为 CSS-first 配置后，postcss 插件与 @theme 写法的迁移笔记。', '2026-06-12', true, 9, 'ARTICLE', NULL, NULL, '2026-08-10 00:45:04.446688+08', '2026-08-10 00:45:04.446688+08', NULL);
INSERT INTO public.mylab_cards VALUES ('d9585765-d9fd-5ad6-bebc-77f0d055e1eb', '8eb7a954-14b1-5b57-a0f1-692b8d6a9e1b', 'raspberry-pi-nas', '树莓派搭家用 NAS：Samba 与硬盘休眠', 'Samba 共享配置、挂载点权限，以及 hdparm 让闲置硬盘自动休眠省电。', '2026-05-20', true, 10, 'ARTICLE', NULL, NULL, '2026-08-10 00:45:04.446688+08', '2026-08-10 00:45:04.446688+08', NULL);
INSERT INTO public.mylab_cards VALUES ('ff079c66-547a-57e8-9c14-47b65010fafe', '8eb7a954-14b1-5b57-a0f1-692b8d6a9e1b', 'vue-composable-mouse-tilt', '封装一个 useMouseTilt 组合式函数', '用 requestAnimationFrame 节流鼠标事件，给卡片做跟随视角的 3D 倾斜。', '2026-05-06', true, 11, 'ARTICLE', NULL, NULL, '2026-08-10 00:45:04.446688+08', '2026-08-10 00:45:04.446688+08', NULL);
INSERT INTO public.mylab_cards VALUES ('f35f251f-64e0-5622-8504-b20bfea008a9', '8eb7a954-14b1-5b57-a0f1-692b8d6a9e1b', 'leetcode-dp-notes', '动态规划刷题小结：从背包到区间 DP', '状态定义优先还是转移优先？整理了自己刷 DP 题时的思考 checklist。', '2026-04-18', true, 12, 'ARTICLE', NULL, NULL, '2026-08-10 00:45:04.446688+08', '2026-08-10 00:45:04.446688+08', NULL);
INSERT INTO public.mylab_cards VALUES ('c028966d-908a-51dd-a06d-d4af028b4709', '8eb7a954-14b1-5b57-a0f1-692b8d6a9e1b', 'first-post', 'MyLab 开张：为什么单独开一个实验记录页', '项目展示放在首页，零散的学习与折腾记录集中收在这里，方便检索与回顾。', '2026-04-01', true, 13, 'ARTICLE', NULL, NULL, '2026-08-10 00:45:04.446688+08', '2026-08-10 00:45:04.446688+08', NULL);
INSERT INTO public.mylab_cards VALUES ('4e0b681e-995f-4d19-a480-ee9a75ff7417', '17fe8734-e813-475e-8cd4-9fd6de31ae5b', 'project-gm1', 'Moth and Bat：项目研究记录', '48 小时 GameJam 作品，关于夜色中两种生物的相会。', '2024-01-01', true, 0, 'PROJECT', 0, '这是一款关于夜晚相遇的解谜游戏。

Unity、C#、Aseprite', '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_cards VALUES ('4243fb7e-be3e-4217-8d88-228fe222dcbd', '17fe8734-e813-475e-8cd4-9fd6de31ae5b', 'project-gm2', 'Naughty Cat：项目研究记录', '一只总想搞破坏的猫与一个不肯关机的扫地机器人。', '2023-01-01', true, 1, 'PROJECT', 1, '一款轻松幽默的平台跳跃游戏。

Godot、GDScript', '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_cards VALUES ('128248c5-e8b4-4ac7-a97d-75295a7bf209', '17fe8734-e813-475e-8cd4-9fd6de31ae5b', 'project-gm3', 'Naughty Boy：项目研究记录', '规则与违抗之间的游戏化实验，关于儿童行为心理学的隐喻。', '2023-01-01', true, 2, 'PROJECT', 2, '探索规则边界的叙事游戏。

Phaser、JavaScript', '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_cards VALUES ('89e0e724-ba1c-4d97-a749-db35b582c442', '17fe8734-e813-475e-8cd4-9fd6de31ae5b', 'project-gm4', 'Ring of Elysium：项目研究记录', '参与腾讯北极光工作室《无限法则》的玩法与系统设计。', '2022-01-01', true, 3, 'PROJECT', 3, '作为玩法设计师参与开发的大逃杀游戏。

Unreal Engine、C++、Lua', '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_cards VALUES ('dd97bc37-d174-41f6-8feb-37c04dfe4f6e', '17fe8734-e813-475e-8cd4-9fd6de31ae5b', 'project-gm5', 'Moodlog：项目研究记录', '一个极简的情绪记录工具，专注输入体验与一年后的回看。', '2024-01-01', true, 4, 'PROJECT', 4, '帮助你记录情绪变化的日常工具。

React、TypeScript、Supabase', '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_cards VALUES ('819e7e7c-68d6-49c1-8e71-440056f58176', '17fe8734-e813-475e-8cd4-9fd6de31ae5b', 'project-gm6', 'Beat Lab：项目研究记录', '浏览器内的鼓机与音序器，使用 Web Audio API 实时合成。', '2023-01-01', true, 5, 'PROJECT', 5, '在线音乐创作工具。

Vue、Web Audio API、Tone.js', '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_cards VALUES ('cb9612f3-8a7b-422b-ab76-46f6dffdb1f0', '17fe8734-e813-475e-8cd4-9fd6de31ae5b', 'blog-docker-deploy', '个人博客 Docker + Nginx 部署全流程记录', '从 Dockerfile 多阶段构建到 nginx SPA 回退与 gzip 配置，把博客塞进容器的完整折腾过程。', '2026-07-28', true, 6, 'ARTICLE', NULL, NULL, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_cards VALUES ('1887dc02-b9d7-489c-8e58-8bc9335df1ba', '17fe8734-e813-475e-8cd4-9fd6de31ae5b', 'vue-gsap-hero', '用 GSAP 给首页 Hero 做电影感动效', 'ScrollTrigger 驱动的滚动叙事：分镜、视差与滚动提示文字的入场编排。', '2026-07-15', true, 7, 'ARTICLE', NULL, NULL, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_cards VALUES ('1d5504f7-07c1-4e38-87f5-9a03678d12dc', '17fe8734-e813-475e-8cd4-9fd6de31ae5b', 'leetcode-binary-search', '二分查找的几种边界写法整理', '闭区间 / 左闭右开两种模板的循环不变量对比，附几道经典题的应用。', '2026-06-30', true, 8, 'ARTICLE', NULL, NULL, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_cards VALUES ('500e9f16-65c3-4161-bb4c-a9c8c74fa0ea', '17fe8734-e813-475e-8cd4-9fd6de31ae5b', 'tailwind-migration', '项目迁移 Tailwind CSS v4 的坑', 'v4 改为 CSS-first 配置后，postcss 插件与 @theme 写法的迁移笔记。', '2026-06-12', true, 9, 'ARTICLE', NULL, NULL, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_cards VALUES ('884d3d97-0381-4560-8516-e7d9269fc1fd', '17fe8734-e813-475e-8cd4-9fd6de31ae5b', 'raspberry-pi-nas', '树莓派搭家用 NAS：Samba 与硬盘休眠', 'Samba 共享配置、挂载点权限，以及 hdparm 让闲置硬盘自动休眠省电。', '2026-05-20', true, 10, 'ARTICLE', NULL, NULL, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_cards VALUES ('f0aff8be-0687-4858-83d9-612406950c74', '17fe8734-e813-475e-8cd4-9fd6de31ae5b', 'vue-composable-mouse-tilt', '封装一个 useMouseTilt 组合式函数', '用 requestAnimationFrame 节流鼠标事件，给卡片做跟随视角的 3D 倾斜。', '2026-05-06', true, 11, 'ARTICLE', NULL, NULL, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_cards VALUES ('4329819c-ac77-4546-b513-3716877985bc', '17fe8734-e813-475e-8cd4-9fd6de31ae5b', 'leetcode-dp-notes', '动态规划刷题小结：从背包到区间 DP', '状态定义优先还是转移优先？整理了自己刷 DP 题时的思考 checklist。', '2026-04-18', true, 12, 'ARTICLE', NULL, NULL, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_cards VALUES ('363a5420-a825-4418-9760-a142607b5bbb', '17fe8734-e813-475e-8cd4-9fd6de31ae5b', 'first-post', 'MyLab 开张：为什么单独开一个实验记录页', '项目展示放在首页，零散的学习与折腾记录集中收在这里，方便检索与回顾。', '2026-04-01', true, 13, 'ARTICLE', NULL, NULL, '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);


-- Data for Name: mylab_engagement_stats; Type: TABLE DATA; Schema: public; Owner: -

INSERT INTO public.mylab_engagement_stats VALUES ('project-gm1', 8, 1, '2026-08-10 21:24:49.05799+08', '2026-08-13 20:39:14.881587+08');


-- Data for Name: mylab_resources; Type: TABLE DATA; Schema: public; Owner: -

INSERT INTO public.mylab_resources VALUES ('bf2be036-830e-5711-9457-ea560766c607', '56c60853-3cca-59e6-831a-f141df2d0497', 'edc8f7b4-861b-5df8-8fe2-10fb34579671', 'a7ee90e0-9b3d-5dca-a5c5-0b8e119fc594', '2026-08-10 00:45:04.456942+08', '2026-08-10 18:10:31.12814+08', NULL);
INSERT INTO public.mylab_resources VALUES ('d0dc1496-11e9-5fcf-a3f0-ff424b0d6ba4', '3b30b9c5-72db-584c-ab32-bca5fca782d9', '66c98ded-1585-53e3-81bb-d3d81cd07551', '513d2877-761e-51f0-ba14-648dd9d9b4e1', '2026-08-10 00:45:04.456942+08', '2026-08-10 18:10:31.12814+08', NULL);
INSERT INTO public.mylab_resources VALUES ('f0b62a01-8049-5cae-8429-c60f29af1863', '1eb33cdd-2066-597c-9367-4b148ec80bf4', 'fa390e9e-11ae-578f-9cda-c59ce887e5b3', '247a975b-8bfc-5065-9887-dae2df61df79', '2026-08-10 00:45:04.456942+08', '2026-08-10 18:10:31.12814+08', NULL);
INSERT INTO public.mylab_resources VALUES ('4ebb0ac8-a171-5fd3-b1bd-ff82b17c250e', '1eb5e084-af77-5474-b2d4-e5ffe38c0ce0', '9d1b7101-100b-5f3f-9185-11a59d08da77', 'c4901a3e-ec1e-5a78-8703-3c006996fb3e', '2026-08-10 00:45:04.456942+08', '2026-08-10 18:10:31.12814+08', NULL);
INSERT INTO public.mylab_resources VALUES ('9f267d68-3e39-5db8-af4f-dd2290d417f4', '68501165-e263-5c9a-86fb-e4c16f3945f7', '51decfbf-b194-5795-a31d-4b7dd377d0bb', '454666a1-482b-5687-abbf-769c3c676d6e', '2026-08-10 00:45:04.456942+08', '2026-08-10 18:10:31.12814+08', NULL);
INSERT INTO public.mylab_resources VALUES ('8f36793d-a285-5304-b7a4-acfe5931acbe', 'd8180653-8f46-5cec-a634-cd69f336a295', '5e60c20f-9e5c-5d58-b7a9-121bf18696c8', '47993a1b-d360-54b6-b869-42438a3f05d7', '2026-08-10 00:45:04.456942+08', '2026-08-10 18:10:31.12814+08', NULL);
INSERT INTO public.mylab_resources VALUES ('1f3dd102-8dca-539e-83c4-5c13c9b4fce9', '7209d613-0ee6-5b8d-a14d-71d2a7228fe1', 'edc8f7b4-861b-5df8-8fe2-10fb34579671', '0efb8ec1-d08c-5296-a226-ff241e9a1c3e', '2026-08-10 00:45:04.456942+08', '2026-08-10 18:10:31.12814+08', NULL);
INSERT INTO public.mylab_resources VALUES ('a7eec4a3-3a0c-505f-8c56-5e47a0b7ecf5', 'e674dc05-c563-57d5-a5ba-df51bb942c12', '66c98ded-1585-53e3-81bb-d3d81cd07551', '5eaf8bb5-1a8a-5791-a9fd-66a8dda4ce66', '2026-08-10 00:45:04.456942+08', '2026-08-10 18:10:31.12814+08', NULL);
INSERT INTO public.mylab_resources VALUES ('d2db9d04-3962-58c7-8778-0e4d41d4e093', 'b4fcced3-778a-5158-894e-6e7d11f3f477', 'fa390e9e-11ae-578f-9cda-c59ce887e5b3', '59dcbac3-1f4d-5ee1-aa4e-2a2b47d2562e', '2026-08-10 00:45:04.456942+08', '2026-08-10 18:10:31.12814+08', NULL);
INSERT INTO public.mylab_resources VALUES ('4cefd8c3-5a7b-5446-addf-6b390bfa697f', '12ae81d0-cd5f-533b-bc4d-e5b758f7503e', '9d1b7101-100b-5f3f-9185-11a59d08da77', '37ec099c-2dca-593f-a9db-3eb8f0de6f0b', '2026-08-10 00:45:04.456942+08', '2026-08-10 18:10:31.12814+08', NULL);
INSERT INTO public.mylab_resources VALUES ('11766b46-f1d2-5ca3-8dde-112fdbeff9ac', 'd9585765-d9fd-5ad6-bebc-77f0d055e1eb', '51decfbf-b194-5795-a31d-4b7dd377d0bb', 'cec23d40-5027-5987-bf2b-cdf4791973e4', '2026-08-10 00:45:04.456942+08', '2026-08-10 18:10:31.12814+08', NULL);
INSERT INTO public.mylab_resources VALUES ('212e8c82-c3aa-545d-aa0a-83cca043db18', 'ff079c66-547a-57e8-9c14-47b65010fafe', '5e60c20f-9e5c-5d58-b7a9-121bf18696c8', 'ee612e27-b895-5946-beaa-6a9b272855f8', '2026-08-10 00:45:04.456942+08', '2026-08-10 18:10:31.12814+08', NULL);
INSERT INTO public.mylab_resources VALUES ('e5fcf14f-1a09-5e12-a57d-471375e24e94', 'f35f251f-64e0-5622-8504-b20bfea008a9', 'edc8f7b4-861b-5df8-8fe2-10fb34579671', 'a86dc95b-e300-5422-aaf7-fa87c94375bc', '2026-08-10 00:45:04.456942+08', '2026-08-10 18:10:31.12814+08', NULL);
INSERT INTO public.mylab_resources VALUES ('26609027-b053-581d-a320-f9c844e9a680', 'c028966d-908a-51dd-a06d-d4af028b4709', '66c98ded-1585-53e3-81bb-d3d81cd07551', '2d0214f8-6e4f-5c3e-ac39-b54e4fd9de57', '2026-08-10 00:45:04.456942+08', '2026-08-10 18:10:31.12814+08', NULL);
INSERT INTO public.mylab_resources VALUES ('a240d0c3-53b6-4c1d-b81a-4a2e2ef921c7', '4e0b681e-995f-4d19-a480-ee9a75ff7417', 'edc8f7b4-861b-5df8-8fe2-10fb34579671', 'a7ee90e0-9b3d-5dca-a5c5-0b8e119fc594', '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_resources VALUES ('34b36758-2134-47fa-952a-933560364e2e', '4243fb7e-be3e-4217-8d88-228fe222dcbd', '66c98ded-1585-53e3-81bb-d3d81cd07551', '513d2877-761e-51f0-ba14-648dd9d9b4e1', '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_resources VALUES ('7ad0d346-f224-490c-b6e4-417a48919b46', '128248c5-e8b4-4ac7-a97d-75295a7bf209', 'fa390e9e-11ae-578f-9cda-c59ce887e5b3', '247a975b-8bfc-5065-9887-dae2df61df79', '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_resources VALUES ('90e6c925-03be-48ff-ace8-2339245e49c2', '89e0e724-ba1c-4d97-a749-db35b582c442', '9d1b7101-100b-5f3f-9185-11a59d08da77', 'c4901a3e-ec1e-5a78-8703-3c006996fb3e', '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_resources VALUES ('66a846d5-c7ae-4cb1-b454-3f6f65ee0871', 'dd97bc37-d174-41f6-8feb-37c04dfe4f6e', '51decfbf-b194-5795-a31d-4b7dd377d0bb', '454666a1-482b-5687-abbf-769c3c676d6e', '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_resources VALUES ('dc292b72-a92c-4926-83b3-9a2e59cb6844', '819e7e7c-68d6-49c1-8e71-440056f58176', '5e60c20f-9e5c-5d58-b7a9-121bf18696c8', '47993a1b-d360-54b6-b869-42438a3f05d7', '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_resources VALUES ('250f67a5-f0a9-4629-9b9c-5f88c9ba77f6', 'cb9612f3-8a7b-422b-ab76-46f6dffdb1f0', 'edc8f7b4-861b-5df8-8fe2-10fb34579671', '0efb8ec1-d08c-5296-a226-ff241e9a1c3e', '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_resources VALUES ('2771f2d0-9606-41b2-a54c-cf381423cf0d', '1887dc02-b9d7-489c-8e58-8bc9335df1ba', '66c98ded-1585-53e3-81bb-d3d81cd07551', '5eaf8bb5-1a8a-5791-a9fd-66a8dda4ce66', '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_resources VALUES ('c94364b5-5e3e-4419-babd-0161fe5d16ce', '1d5504f7-07c1-4e38-87f5-9a03678d12dc', 'fa390e9e-11ae-578f-9cda-c59ce887e5b3', '59dcbac3-1f4d-5ee1-aa4e-2a2b47d2562e', '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_resources VALUES ('83a2a588-7d7f-4f72-923a-16e64ce4b173', '500e9f16-65c3-4161-bb4c-a9c8c74fa0ea', '9d1b7101-100b-5f3f-9185-11a59d08da77', '37ec099c-2dca-593f-a9db-3eb8f0de6f0b', '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_resources VALUES ('391f91f4-ceae-487a-82ea-42e3ccc979ec', '884d3d97-0381-4560-8516-e7d9269fc1fd', '51decfbf-b194-5795-a31d-4b7dd377d0bb', 'cec23d40-5027-5987-bf2b-cdf4791973e4', '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_resources VALUES ('e4846e6b-e284-4153-815a-8557629aa248', 'f0aff8be-0687-4858-83d9-612406950c74', '5e60c20f-9e5c-5d58-b7a9-121bf18696c8', 'ee612e27-b895-5946-beaa-6a9b272855f8', '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_resources VALUES ('32cf4a35-0ce7-4bfa-abbd-6d6102158a92', '4329819c-ac77-4546-b513-3716877985bc', 'edc8f7b4-861b-5df8-8fe2-10fb34579671', 'a86dc95b-e300-5422-aaf7-fa87c94375bc', '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);
INSERT INTO public.mylab_resources VALUES ('77bda078-5974-4bef-9e19-ec551bb6fbbd', '363a5420-a825-4418-9760-a142607b5bbb', '66c98ded-1585-53e3-81bb-d3d81cd07551', '2d0214f8-6e4f-5c3e-ac39-b54e4fd9de57', '2026-08-12 19:42:15.448661+08', '2026-08-12 19:42:15.448661+08', NULL);


-- Data for Name: mylab_tags; Type: TABLE DATA; Schema: public; Owner: -

INSERT INTO public.mylab_tags VALUES ('100b7029-067a-59db-8c2b-f9c524fb520c', 'csharp', 'C#', true, 2, '2026-08-10 00:45:04.442137+08', '2026-08-10 00:45:04.442137+08', NULL);
INSERT INTO public.mylab_tags VALUES ('2d9db0ad-a7fb-5bcf-99ff-040ba892267c', 'godot', 'Godot', true, 4, '2026-08-10 00:45:04.442137+08', '2026-08-10 00:45:04.442137+08', NULL);
INSERT INTO public.mylab_tags VALUES ('faba2f26-a83f-575a-9c0e-590251dc6909', 'javascript', 'JavaScript', true, 7, '2026-08-10 00:45:04.442137+08', '2026-08-10 00:45:04.442137+08', NULL);
INSERT INTO public.mylab_tags VALUES ('f064a292-232f-5c61-8ca2-8389e50bbf1b', 'dp', 'DP', true, 28, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:42:13.825644+08', NULL);
INSERT INTO public.mylab_tags VALUES ('ce23d581-aa67-5c91-a808-0fba88586935', 'essay', '随笔', true, 29, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:42:13.836713+08', NULL);
INSERT INTO public.mylab_tags VALUES ('7b9563ee-ecd1-5910-a4d0-d508b51223f5', 'web-lab', 'Web 实验', false, 15, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:42:13.845809+08', '2026-08-12 19:42:13.845809+08');
INSERT INTO public.mylab_tags VALUES ('37cd8fe1-52cc-525d-a286-5c8930205148', 'indie-tool', '独立工具', true, 11, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:42:13.855268+08', '2026-08-12 19:42:13.855268+08');
INSERT INTO public.mylab_tags VALUES ('cd3776a7-8d5f-5c3f-b7d2-b81fc8974d7a', 'gamejam', 'GameJam', true, 0, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:41:18.390638+08', NULL);
INSERT INTO public.mylab_tags VALUES ('9b7ab32a-5c1f-566b-a8dc-48bdf9a6fc16', 'unity', 'Unity', true, 1, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:41:18.402332+08', NULL);
INSERT INTO public.mylab_tags VALUES ('f153754c-e92a-5d82-9642-250ef1972aa8', 'aseprite', 'Aseprite', true, 3, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:41:18.413451+08', NULL);
INSERT INTO public.mylab_tags VALUES ('0fc6f305-8fae-53fd-ac1a-85d060a4f2bf', 'gdscript', 'GDScript', true, 5, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:41:18.424097+08', NULL);
INSERT INTO public.mylab_tags VALUES ('6310e463-4ad4-59f3-8cf3-e735e3fd3691', 'phaser', 'Phaser', true, 6, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:41:18.434157+08', NULL);
INSERT INTO public.mylab_tags VALUES ('16649271-892d-57bf-8555-a43662e53956', 'unreal-engine', 'Unreal Engine', true, 8, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:41:18.444738+08', NULL);
INSERT INTO public.mylab_tags VALUES ('85dd4c28-cbe0-52b4-88bf-82cd376f20f3', 'cpp', 'C++', true, 9, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:41:18.458571+08', NULL);
INSERT INTO public.mylab_tags VALUES ('902ac023-ea59-51e6-b308-75aae2fefa14', 'lua', 'Lua', true, 10, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:41:18.469409+08', NULL);
INSERT INTO public.mylab_tags VALUES ('9919a272-2ca5-5a32-9796-d6d90f4c9b3b', 'commercial-project', '商业项目', false, 8, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:41:18.681964+08', '2026-08-12 19:41:18.681964+08');
INSERT INTO public.mylab_tags VALUES ('40685b33-1982-589e-8c09-2df8b75b5655', 'react', 'React', true, 11, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:42:13.646007+08', NULL);
INSERT INTO public.mylab_tags VALUES ('11894919-dbfa-5d3d-8a2f-3bfa1580c4b6', 'typescript', 'TypeScript', true, 12, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:42:13.659233+08', NULL);
INSERT INTO public.mylab_tags VALUES ('1bba22a4-d4c5-5f5e-8dcf-bce71d13388a', 'supabase', 'Supabase', true, 13, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:42:13.670395+08', NULL);
INSERT INTO public.mylab_tags VALUES ('e64d08a8-eedf-5d6c-be92-91a51a9c294f', 'vue', 'Vue', true, 14, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:42:13.680025+08', NULL);
INSERT INTO public.mylab_tags VALUES ('c144f9fa-8ffb-5225-a51c-2b68aaeb276c', 'web-audio-api', 'Web Audio API', true, 15, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:42:13.689248+08', NULL);
INSERT INTO public.mylab_tags VALUES ('b08090e1-19eb-598d-bbee-1ff39bcf31a0', 'tone-js', 'Tone.js', true, 16, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:42:13.699414+08', NULL);
INSERT INTO public.mylab_tags VALUES ('b820842b-8c3e-5dba-a955-393633ab888e', 'docker', 'Docker', true, 17, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:42:13.709934+08', NULL);
INSERT INTO public.mylab_tags VALUES ('85538585-342a-58bb-b4d5-37fe10d2f05e', 'nginx', 'Nginx', true, 18, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:42:13.720226+08', NULL);
INSERT INTO public.mylab_tags VALUES ('61dc6897-31dd-5b1f-bc22-b32e1ad4c51c', 'ops', '运维', true, 19, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:42:13.730937+08', NULL);
INSERT INTO public.mylab_tags VALUES ('bed13764-9b69-54c0-b976-a9a0f6a594ae', 'gsap', 'GSAP', true, 20, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:42:13.742979+08', NULL);
INSERT INTO public.mylab_tags VALUES ('be50c0a5-d841-5287-80cf-b3272fa0277b', 'frontend', '前端', true, 21, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:42:13.753979+08', NULL);
INSERT INTO public.mylab_tags VALUES ('0ad86620-6da9-5bee-890c-b8a10f8a845b', 'leetcode', 'Leetcode', true, 22, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:42:13.764747+08', NULL);
INSERT INTO public.mylab_tags VALUES ('5ca12400-1681-5462-afe2-9f08f9e629d1', 'algorithm', '算法', true, 23, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:42:13.773872+08', NULL);
INSERT INTO public.mylab_tags VALUES ('d4f70c09-16a3-54a1-b715-dc608b90859e', 'tailwind', 'Tailwind', true, 24, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:42:13.783043+08', NULL);
INSERT INTO public.mylab_tags VALUES ('4ac8673b-86ef-5af1-a70a-e446cc2cf799', 'engineering', '工程化', true, 25, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:42:13.793834+08', NULL);
INSERT INTO public.mylab_tags VALUES ('f82960eb-2fb1-57c2-86d2-ce5b7a1650e0', 'raspberry-pi', '树莓派', true, 26, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:42:13.803104+08', NULL);
INSERT INTO public.mylab_tags VALUES ('a0594a0e-8118-5315-9180-7a8f390342da', 'hardware', '硬件', true, 27, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:42:13.813713+08', NULL);


-- Data for Name: resources; Type: TABLE DATA; Schema: public; Owner: -

INSERT INTO public.resources VALUES ('17a0aca0-a554-5d7c-811f-87950438bd0b', 'hero/hero-1.webp', 'ysy-myblog', 'hero-1.webp', 'image/webp', 600970, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('39c26cd2-109b-5c9a-99b1-0dcb7d19865a', 'hero/hero-2.webp', 'ysy-myblog', 'hero-2.webp', 'image/webp', 123308, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('266331b3-1978-5495-ad36-362647692ada', 'hero/hero-3.webp', 'ysy-myblog', 'hero-3.webp', 'image/webp', 678026, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('16760407-ecf2-5587-8d75-000a4c91686d', 'hero/hero-4.webp', 'ysy-myblog', 'hero-4.webp', 'image/webp', 435332, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('cd1316ac-c206-584c-982d-e024a084ec2d', 'hero/hero-5.webp', 'ysy-myblog', 'hero-5.webp', 'image/webp', 232300, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('931c1f55-66e5-536a-ad42-2ceec8cf5e5d', 'hero/hero-6.webp', 'ysy-myblog', 'hero-6.webp', 'image/webp', 703994, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('4889ba92-d366-55d2-82a4-a94833da1b8c', 'icon/avatar.png', 'ysy-myblog', 'avatar.png', 'image/png', 31793, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('106e0cb3-c95b-5d3f-96dd-997169ac6510', 'icon/csharp-dotnet.png', 'ysy-myblog', 'csharp-dotnet.png', 'image/png', 536302, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('ecae2d77-5b15-5918-93d7-12ef0e0544db', 'icon/java-spring-boot.png', 'ysy-myblog', 'java-spring-boot.png', 'image/png', 317103, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('63df3f0a-bda9-5de0-a068-d77d75916815', 'icon/docker.png', 'ysy-myblog', 'docker.png', 'image/png', 307425, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('9b936246-207d-5841-a65b-a9f4b8d1aacb', 'icon/sql.png', 'ysy-myblog', 'sql.png', 'image/png', 425596, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('c31fb74c-cd2a-52b2-bb35-e817627271f3', 'icon/javascript-typescript.png', 'ysy-myblog', 'javascript-typescript.png', 'image/png', 401596, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('2bb66285-e985-5cf8-bdc8-0d77264a195f', 'icon/react-vue.png', 'ysy-myblog', 'react-vue.png', 'image/png', 478440, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('53587a9f-f3a2-584d-ae4e-a8c7a2fa62a3', 'icon/python.png', 'ysy-myblog', 'python.png', 'image/png', 390178, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('5cab4584-dad0-5c12-9d39-1b9352dfd7a7', 'hobbies/cs2.jpg', 'ysy-myblog', 'cs2.jpg', 'image/jpeg', 727214, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('1a26d874-bd0f-5808-acc3-4a6c963ce6e4', 'hobbies/apex.jpg', 'ysy-myblog', 'apex.jpg', 'image/jpeg', 986834, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('8fc83aca-e31e-5a65-9195-44584997c8df', 'hobbies/delta-force.jpg', 'ysy-myblog', 'delta-force.jpg', 'image/jpeg', 576688, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('d898586e-5aa7-5a47-862a-698aedd0d287', 'hobbies/the-finals.jpg', 'ysy-myblog', 'the-finals.jpg', 'image/jpeg', 308002, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('81eedeaa-b0df-5ead-a804-f8bea0560100', 'hobbies/overwatch2.jpeg', 'ysy-myblog', 'overwatch2.jpeg', 'image/jpeg', 75442, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('a7ee90e0-9b3d-5dca-a5c5-0b8e119fc594', 'mylab/project-gm1.md', 'ysy-myblog', 'project-gm1.md', 'text/markdown', 197, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('513d2877-761e-51f0-ba14-648dd9d9b4e1', 'mylab/project-gm2.md', 'ysy-myblog', 'project-gm2.md', 'text/markdown', 215, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('247a975b-8bfc-5065-9887-dae2df61df79', 'mylab/project-gm3.md', 'ysy-myblog', 'project-gm3.md', 'text/markdown', 206, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('c4901a3e-ec1e-5a78-8703-3c006996fb3e', 'mylab/project-gm4.md', 'ysy-myblog', 'project-gm4.md', 'text/markdown', 219, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('454666a1-482b-5687-abbf-769c3c676d6e', 'mylab/project-gm5.md', 'ysy-myblog', 'project-gm5.md', 'text/markdown', 196, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('47993a1b-d360-54b6-b869-42438a3f05d7', 'mylab/project-gm6.md', 'ysy-myblog', 'project-gm6.md', 'text/markdown', 201, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('0efb8ec1-d08c-5296-a226-ff241e9a1c3e', 'mylab/blog-docker-deploy.md', 'ysy-myblog', 'blog-docker-deploy.md', 'text/markdown', 698, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('5eaf8bb5-1a8a-5791-a9fd-66a8dda4ce66', 'mylab/vue-gsap-hero.md', 'ysy-myblog', 'vue-gsap-hero.md', 'text/markdown', 531, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('59dcbac3-1f4d-5ee1-aa4e-2a2b47d2562e', 'mylab/leetcode-binary-search.md', 'ysy-myblog', 'leetcode-binary-search.md', 'text/markdown', 557, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('37ec099c-2dca-593f-a9db-3eb8f0de6f0b', 'mylab/tailwind-migration.md', 'ysy-myblog', 'tailwind-migration.md', 'text/markdown', 360, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('cec23d40-5027-5987-bf2b-cdf4791973e4', 'mylab/raspberry-pi-nas.md', 'ysy-myblog', 'raspberry-pi-nas.md', 'text/markdown', 411, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('ee612e27-b895-5946-beaa-6a9b272855f8', 'mylab/vue-composable-mouse-tilt.md', 'ysy-myblog', 'vue-composable-mouse-tilt.md', 'text/markdown', 398, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('a86dc95b-e300-5422-aaf7-fa87c94375bc', 'mylab/leetcode-dp-notes.md', 'ysy-myblog', 'leetcode-dp-notes.md', 'text/markdown', 562, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('2d0214f8-6e4f-5c3e-ac39-b54e4fd9de57', 'mylab/first-post.md', 'ysy-myblog', 'first-post.md', 'text/markdown', 434, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources VALUES ('edc8f7b4-861b-5df8-8fe2-10fb34579671', 'mylab-post/project-cover-1.webp', 'ysy-myblog', 'project-cover-1.webp', 'image/webp', 600970, NULL, '2026-08-10 18:10:31.12814+08', '2026-08-10 18:10:31.12814+08', NULL);
INSERT INTO public.resources VALUES ('66c98ded-1585-53e3-81bb-d3d81cd07551', 'mylab-post/project-cover-2.webp', 'ysy-myblog', 'project-cover-2.webp', 'image/webp', 123308, NULL, '2026-08-10 18:10:31.12814+08', '2026-08-10 18:10:31.12814+08', NULL);
INSERT INTO public.resources VALUES ('fa390e9e-11ae-578f-9cda-c59ce887e5b3', 'mylab-post/project-cover-3.webp', 'ysy-myblog', 'project-cover-3.webp', 'image/webp', 678026, NULL, '2026-08-10 18:10:31.12814+08', '2026-08-10 18:10:31.12814+08', NULL);
INSERT INTO public.resources VALUES ('9d1b7101-100b-5f3f-9185-11a59d08da77', 'mylab-post/project-cover-4.webp', 'ysy-myblog', 'project-cover-4.webp', 'image/webp', 435332, NULL, '2026-08-10 18:10:31.12814+08', '2026-08-10 18:10:31.12814+08', NULL);
INSERT INTO public.resources VALUES ('51decfbf-b194-5795-a31d-4b7dd377d0bb', 'mylab-post/project-cover-5.webp', 'ysy-myblog', 'project-cover-5.webp', 'image/webp', 232300, NULL, '2026-08-10 18:10:31.12814+08', '2026-08-10 18:10:31.12814+08', NULL);
INSERT INTO public.resources VALUES ('5e60c20f-9e5c-5d58-b7a9-121bf18696c8', 'mylab-post/project-cover-6.webp', 'ysy-myblog', 'project-cover-6.webp', 'image/webp', 703994, NULL, '2026-08-10 18:10:31.12814+08', '2026-08-10 18:10:31.12814+08', NULL);
INSERT INTO public.resources VALUES ('18478fc2-e6fd-4bc5-87cc-4a34646b373c', 'footstep/2026/08/83a8c122d1dd435186ab281fbf5c814c.webp', 'ysy-myblog', 'IMG_20240928_185814.webp', 'image/webp', 726158, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 16:13:01.167569+08', '2026-08-11 16:13:01.167569+08', NULL);
INSERT INTO public.resources VALUES ('fb729e9d-6084-5dc4-9e1d-460d4811ba4d', 'footstep/footstep-2.webp', 'ysy-myblog', 'footstep-2.webp', 'image/webp', 123308, NULL, '2026-08-10 18:10:31.12814+08', '2026-08-11 16:16:01.928863+08', '2026-08-11 16:16:01.928831+08');
INSERT INTO public.resources VALUES ('5ed7cab1-69d1-5e6e-be24-3aaa47a745c3', 'footstep/footstep-5.webp', 'ysy-myblog', 'footstep-5.webp', 'image/webp', 232300, NULL, '2026-08-10 18:10:31.12814+08', '2026-08-11 16:16:04.394425+08', '2026-08-11 16:16:04.394393+08');
INSERT INTO public.resources VALUES ('f47a2b17-beed-5a41-87de-d268cde08a90', 'footstep/footstep-6.webp', 'ysy-myblog', 'footstep-6.webp', 'image/webp', 703994, NULL, '2026-08-10 18:10:31.12814+08', '2026-08-11 16:16:06.304174+08', '2026-08-11 16:16:06.304158+08');
INSERT INTO public.resources VALUES ('1b5c1646-481c-5239-9f05-181a7d70158d', 'footstep/footstep-4.webp', 'ysy-myblog', 'footstep-4.webp', 'image/webp', 435332, NULL, '2026-08-10 18:10:31.12814+08', '2026-08-11 16:16:08.849871+08', '2026-08-11 16:16:08.849863+08');
INSERT INTO public.resources VALUES ('174b77b8-cb70-5394-9105-def41d9117ee', 'footstep/footstep-1.webp', 'ysy-myblog', 'footstep-1.webp', 'image/webp', 600970, NULL, '2026-08-10 18:10:31.12814+08', '2026-08-11 16:16:11.036738+08', '2026-08-11 16:16:11.036731+08');
INSERT INTO public.resources VALUES ('af7b3773-bbbe-50c2-a7f5-5cc426813480', 'footstep/footstep-3.webp', 'ysy-myblog', 'footstep-3.webp', 'image/webp', 678026, NULL, '2026-08-10 18:10:31.12814+08', '2026-08-11 16:16:13.237978+08', '2026-08-11 16:16:13.237974+08');
INSERT INTO public.resources VALUES ('81bf69f5-23d2-484f-bb1d-35085fcc68b6', 'footstep/2026/08/4d39e19186bf476f809da4d424bea2d2.webp', 'ysy-myblog', 'IMG_20240921_165341.webp', 'image/webp', 509856, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 16:30:48.591593+08', '2026-08-11 16:30:48.591593+08', NULL);
INSERT INTO public.resources VALUES ('e2853460-a24b-438f-99d1-cb06c5123ffd', 'footstep/2026/08/5f9225a96ed3412aa519008b996a2dcf.webp', 'ysy-myblog', 'IMG_20240921_164255.webp', 'image/webp', 593988, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 16:30:52.465825+08', '2026-08-11 16:30:52.465825+08', NULL);
INSERT INTO public.resources VALUES ('c3385923-95e9-4c99-8905-8dbc1c492fb8', 'footstep/2026/08/36a1acdb72174838b74d6782dad0ff3f.webp', 'ysy-myblog', 'IMG_20240916_153024.webp', 'image/webp', 1194988, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 16:32:28.638108+08', '2026-08-11 16:32:28.638108+08', NULL);
INSERT INTO public.resources VALUES ('457fdd0f-2a53-4acf-a102-378d56b25f9b', 'footstep/2026/08/40ef0e9b860541f089fe2f9035e60e44.webp', 'ysy-myblog', 'IMG_20240916_145226.webp', 'image/webp', 378398, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 16:32:31.765689+08', '2026-08-11 16:32:31.765689+08', NULL);
INSERT INTO public.resources VALUES ('bdd1af10-1578-45dd-bb8f-3fe91f141f29', 'footstep/2026/08/2c5f80d703374311b63f705c978d2a47.webp', 'ysy-myblog', 'IMG_20240904_194636.webp', 'image/webp', 705836, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 16:32:34.192264+08', '2026-08-11 16:32:34.192264+08', NULL);
INSERT INTO public.resources VALUES ('d3910fb4-a1ab-4153-931d-87d84893d62c', 'footstep/2026/08/f5ebd8da74104c84b445df0e18739799.webp', 'ysy-myblog', 'IMG_20240328_140339.webp', 'image/webp', 1257950, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 16:32:37.129722+08', '2026-08-11 16:32:37.129722+08', NULL);
INSERT INTO public.resources VALUES ('76e34059-adf8-4280-aa9c-545b6b9d27b8', 'footstep/2026/08/5c47a7ed7d074b1d9f189576b639cce6.webp', 'ysy-myblog', 'IMG_20210101_170755.webp', 'image/webp', 894238, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 18:02:42.58006+08', '2026-08-11 18:02:42.58006+08', NULL);
INSERT INTO public.resources VALUES ('ceed1b03-ddf3-4ac7-97ec-01e6f6de42a8', 'footstep/2026/08/39e18ea49cd046e2aaf9ab87c69d513e.webp', 'ysy-myblog', 'IMG_20211004_112701.webp', 'image/webp', 2205048, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 18:02:45.763191+08', '2026-08-11 18:02:45.763191+08', NULL);
INSERT INTO public.resources VALUES ('207dec49-3a28-4af1-8a29-b8196a7069d0', 'footstep/2026/08/37b46dffc50e4b79aa41d36cfdfa23bc.webp', 'ysy-myblog', 'IMG_20230731_154213.webp', 'image/webp', 420066, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 18:02:53.945346+08', '2026-08-11 18:02:53.945346+08', NULL);
INSERT INTO public.resources VALUES ('efefaae1-db49-473b-a22e-7ff05ecfbb62', 'footstep/2026/08/835cee412fe444e686e16e8e101d9d47.webp', 'ysy-myblog', 'IMG_20240105_165019.webp', 'image/webp', 1557604, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 18:02:56.676392+08', '2026-08-11 18:02:56.676392+08', NULL);
INSERT INTO public.resources VALUES ('70e9bfbf-f11e-4c22-8994-42605e806621', 'footstep/2026/08/0fe9675d16bf40e49f79da07118fc403.webp', 'ysy-myblog', 'IMG_20240405_165340.webp', 'image/webp', 734434, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 18:02:59.082686+08', '2026-08-11 18:02:59.082686+08', NULL);
INSERT INTO public.resources VALUES ('0afa7af5-725f-4e61-b0c8-04d37ab148b9', 'footstep/2026/08/8a8100a61a544bbda94250e0e3b735c2.jpg', 'ysy-myblog', 'IMG_20240407_161125_1786439901351.jpg', 'image/jpeg', 595825, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 18:03:02.373669+08', '2026-08-11 18:03:02.373669+08', NULL);
INSERT INTO public.resources VALUES ('8f48cfe1-978d-4636-b693-cdfd9d5aaf56', 'footstep/2026/08/ca257556249044d3a9bfc058e6723349.webp', 'ysy-myblog', 'IMG_20240407_163704.webp', 'image/webp', 1095640, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 18:03:08.715023+08', '2026-08-11 18:03:08.715023+08', NULL);
INSERT INTO public.resources VALUES ('8dd096de-3db8-4941-ad75-eabee4e03633', 'footstep/2026/08/1a53602b32d244e5acc70822b0ec0c10.webp', 'ysy-myblog', 'IMG_20240407_173713.webp', 'image/webp', 1007330, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 18:03:11.768882+08', '2026-08-11 18:03:11.768882+08', NULL);
INSERT INTO public.resources VALUES ('b0a6a34e-45d5-4b5b-be36-ab9084fb83df', 'footstep/2026/08/3e454d4fbf534a72975fb13aa28b988e.webp', 'ysy-myblog', 'IMG_20240408_034110.webp', 'image/webp', 1380520, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 18:03:14.433946+08', '2026-08-11 18:03:14.433946+08', NULL);
INSERT INTO public.resources VALUES ('00591d27-ab26-4295-9e6f-3ce988b5a38d', 'footstep/2026/08/34d886746e7146ab85ffd3c233e90581.webp', 'ysy-myblog', 'IMG_20250117_143106.webp', 'image/webp', 733976, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 19:20:26.316108+08', '2026-08-11 19:20:26.316108+08', NULL);
INSERT INTO public.resources VALUES ('d832949a-bf1f-4803-87f8-4410c3ecbf4e', 'footstep/2026/08/5475a19ca5af47b9b6b6c5237b03d191.webp', 'ysy-myblog', 'IMG_20250117_211553.webp', 'image/webp', 889016, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 19:20:28.859122+08', '2026-08-11 19:20:28.859122+08', NULL);
INSERT INTO public.resources VALUES ('73d5f633-1732-40ac-b5f0-797cdc308a05', 'footstep/2026/08/e11187bc24404699b3d7f033ce657254.webp', 'ysy-myblog', 'IMG_20250118_160947.webp', 'image/webp', 1231444, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 19:20:32.212568+08', '2026-08-11 19:20:32.212568+08', NULL);
INSERT INTO public.resources VALUES ('06c1b717-05c4-4882-8601-7c35f6bed6b7', 'footstep/2026/08/b09050eeeec54dfd8ccf01705c2b5646.webp', 'ysy-myblog', 'IMG_20250118_164207.webp', 'image/webp', 784522, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 19:20:34.958518+08', '2026-08-11 19:20:34.958518+08', NULL);
INSERT INTO public.resources VALUES ('2780f138-274a-43b2-97c5-939b5b01b8bc', 'footstep/2026/08/75a7383abe01432992a74fb978e94a85.webp', 'ysy-myblog', 'IMG_20250118_170117.webp', 'image/webp', 568638, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 19:20:38.225553+08', '2026-08-11 19:20:38.225553+08', NULL);
INSERT INTO public.resources VALUES ('cfdefefd-f50a-4c44-8195-0e59dedbc3be', 'footstep/2026/08/f7a2784a364a40e1ba938a58fb65b20e.webp', 'ysy-myblog', 'IMG_20260811_185631.webp', 'image/webp', 855186, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 19:20:40.671934+08', '2026-08-11 19:20:40.671934+08', NULL);
INSERT INTO public.resources VALUES ('ce487f10-8f9f-45be-bc23-1860f040854d', 'footstep/2026/08/ab79dcbec3d442139643f13a35558dd3.webp', 'ysy-myblog', 'IMG_20250301_180722.webp', 'image/webp', 393064, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 19:58:43.864581+08', '2026-08-11 19:58:43.864581+08', NULL);
INSERT INTO public.resources VALUES ('9643f1ff-cc52-4bbf-b9c6-aa657cd1078e', 'footstep/2026/08/af3f897f8c0b44c59152dc75ee71830b.webp', 'ysy-myblog', 'IMG_20250329_204626.webp', 'image/webp', 602716, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 19:58:47.115923+08', '2026-08-11 19:58:47.115923+08', NULL);
INSERT INTO public.resources VALUES ('f59535ef-234e-444e-a799-7af526962eba', 'footstep/2026/08/7703e8c70bc544e7831335d74e06be1f.webp', 'ysy-myblog', 'IMG_20250329_204827.webp', 'image/webp', 602570, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 19:58:49.814686+08', '2026-08-11 19:58:49.814686+08', NULL);
INSERT INTO public.resources VALUES ('cc48b9e0-f3d3-4980-b386-bfce181fd793', 'footstep/2026/08/ed220ebca6c64c9d86462cae6009625f.webp', 'ysy-myblog', 'IMG_20250330_151549.webp', 'image/webp', 857848, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 19:58:52.367548+08', '2026-08-11 19:58:52.367548+08', NULL);
INSERT INTO public.resources VALUES ('7c1fd1d0-3eac-478a-8713-6c65a85efdac', 'footstep/2026/08/6eb7dcc807c24c9fb765a9cbd93e058f.webp', 'ysy-myblog', 'IMG_20250330_160514.webp', 'image/webp', 917746, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 19:58:54.96726+08', '2026-08-11 19:58:54.96726+08', NULL);
INSERT INTO public.resources VALUES ('6e113de0-e08e-4603-9e3d-0cf509d40a3d', 'footstep/2026/08/96ba8669bec4487d899b305eb4954f6a.webp', 'ysy-myblog', 'IMG_20250525_132316.webp', 'image/webp', 1899416, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 19:58:57.375971+08', '2026-08-11 20:00:32.326348+08', '2026-08-11 20:00:32.326339+08');
INSERT INTO public.resources VALUES ('3e49373d-7b25-424d-b8b8-94434a242bd1', 'footstep/2026/08/184895197e74461bbf89045f665b12be.webp', 'ysy-myblog', 'IMG_20260811_191431.webp', 'image/webp', 369318, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 19:58:59.913538+08', '2026-08-11 20:02:25.451336+08', '2026-08-11 20:02:25.451304+08');
INSERT INTO public.resources VALUES ('c64a2eee-a122-422d-a58a-25f629d0b80d', 'footstep/2026/08/25c6bb333b9f4a918786fa1cd143c6e6.webp', 'ysy-myblog', 'IMG_20260811_195946.webp', 'image/webp', 1885802, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 20:46:27.334013+08', '2026-08-11 20:46:27.334013+08', NULL);
INSERT INTO public.resources VALUES ('889405d9-e6b5-47ae-a4d7-f978b390d7a9', 'footstep/2026/08/54ea648367ea460d82c8de9e3a369cba.webp', 'ysy-myblog', 'IMG_20260811_194159.webp', 'image/webp', 817860, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 20:46:30.283628+08', '2026-08-11 20:46:30.283628+08', NULL);
INSERT INTO public.resources VALUES ('4b57bb78-37ba-49cf-82b3-04dfcd5a794b', 'footstep/2026/08/d3658d71ef0f490db5d8f652225cd488.webp', 'ysy-myblog', 'IMG_20260811_193947.webp', 'image/webp', 1500006, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 20:46:33.696704+08', '2026-08-11 20:46:33.696704+08', NULL);
INSERT INTO public.resources VALUES ('4e10c48b-8447-4a2c-9db2-91f088df037b', 'footstep/2026/08/5f707d0e0f204eadb2e1487e105ed727.webp', 'ysy-myblog', 'IMG_20260811_193913.webp', 'image/webp', 1166804, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 20:46:36.250447+08', '2026-08-11 20:46:36.250447+08', NULL);
INSERT INTO public.resources VALUES ('27815e56-3d57-4d20-86e3-78279115ba87', 'footstep/2026/08/8f3521e069c64d4f99a50d59b13572b8.webp', 'ysy-myblog', 'IMG_20260811_193729.webp', 'image/webp', 804304, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 20:46:39.209775+08', '2026-08-11 20:46:39.209775+08', NULL);
INSERT INTO public.resources VALUES ('47d6a325-b0c6-4ab3-b10f-bb87716acdbe', 'footstep/2026/08/5640d0e1cabe4ccbbad27c86a0dcb5c8.webp', 'ysy-myblog', 'IMG_20260811_193502.webp', 'image/webp', 1025808, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 20:46:42.532545+08', '2026-08-11 20:46:42.532545+08', NULL);
INSERT INTO public.resources VALUES ('2e6ee498-f690-4cff-9a1c-983417013ce4', 'footstep/2026/08/55247788526a4595811ccd8c9889fb48.webp', 'ysy-myblog', 'IMG_20260811_193140.webp', 'image/webp', 988376, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 20:46:45.212397+08', '2026-08-11 20:46:45.212397+08', NULL);
INSERT INTO public.resources VALUES ('82915b3f-e053-4679-b57f-aa0c87f461fa', 'footstep/2026/08/357ec1b34caa4e318ecba750ca2ace25.webp', 'ysy-myblog', 'IMG_20260811_193123.webp', 'image/webp', 814458, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 20:46:47.748723+08', '2026-08-11 20:46:47.748723+08', NULL);
INSERT INTO public.resources VALUES ('46accbe7-3d42-41c2-b81a-210776fd7b01', 'footstep/2026/08/e7b11b5321044ac9bb285aa8bc1e2d25.webp', 'ysy-myblog', 'IMG_20181002_095448.webp', 'image/webp', 959160, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 20:58:43.040972+08', '2026-08-11 20:58:43.040972+08', NULL);
INSERT INTO public.resources VALUES ('c34714fd-c984-4fb4-b301-305c4dffb8b2', 'footstep/2026/08/254ae42b86ea4069ba3a28d636563509.webp', 'ysy-myblog', 'IMG_20181002_102942.webp', 'image/webp', 522000, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 20:58:45.724145+08', '2026-08-11 20:58:45.724145+08', NULL);
INSERT INTO public.resources VALUES ('b1b8ffa1-1b70-41f5-9bc6-26c7f2a68090', 'footstep/2026/08/3800f6ebc57244fa87deed6a9cf516e1.webp', 'ysy-myblog', 'IMG_20181002_103227.webp', 'image/webp', 1158404, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 20:58:48.165029+08', '2026-08-11 20:58:48.165029+08', NULL);
INSERT INTO public.resources VALUES ('2e0e9185-e407-4412-9601-820210a49f5c', 'footstep/2026/08/7113360b1aab4bcd81d449dcb2c623c9.webp', 'ysy-myblog', 'IMG_20260811_200812.webp', 'image/webp', 253720, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 20:58:50.924877+08', '2026-08-11 20:58:50.924877+08', NULL);
INSERT INTO public.resources VALUES ('842d16f0-0ada-4bc0-b63c-2b92f3545f6f', 'footstep/2026/08/764cddf4509b473780ca43658157b625.webp', 'ysy-myblog', 'IMG_20260811_201957.webp', 'image/webp', 703946, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 20:58:53.666547+08', '2026-08-11 20:58:53.666547+08', NULL);
INSERT INTO public.resources VALUES ('61078cfe-24dc-4904-9c87-912d774bca62', 'hobbies/2026/08/c23b7427342a40e5954fbf2778d66bff.jpg', 'ysy-myblog', '1.jpeg', 'image/jpeg', 214421, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-12 21:54:12.22205+08', '2026-08-12 21:54:12.22205+08', NULL);
INSERT INTO public.resources VALUES ('4ac6e837-df3f-48e3-bd9d-dcbcc09a97cf', 'hero/2026/08/8ec0a182242f4524857353f665fdd311.png', 'ysy-myblog', 'sport.png', 'image/png', 576586, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-12 22:24:36.156443+08', '2026-08-12 22:25:37.762196+08', '2026-08-12 22:25:37.762182+08');
INSERT INTO public.resources VALUES ('88e96b5c-bbe1-4675-bb24-aee5edb71bfb', 'hero/2026/08/9cd94fd60818434b932c67daadd44231.png', 'ysy-myblog', 'family and friends.png', 'image/png', 621217, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-12 22:13:06.797432+08', '2026-08-12 22:25:39.439188+08', '2026-08-12 22:25:39.439179+08');
INSERT INTO public.resources VALUES ('73ddaea2-c208-420a-a5a7-faca1d9e64c1', 'hobbies/2026/08/771e613d1a0d4d09b77a41dc403cf41b.jpg', 'ysy-myblog', 'hobby-crop-1786943520644.jpg', 'image/jpeg', 257059, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-17 13:12:00.951951+08', '2026-08-17 13:12:00.951951+08', NULL);
INSERT INTO public.resources VALUES ('75af93fa-1b78-43da-8e11-e1e4d39f2603', 'hobbies/2026/08/b45984f5991d47aca4c8ed9bc414046b.jpg', 'ysy-myblog', 'hobby-crop-1786943701821.jpg', 'image/jpeg', 256689, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-17 13:15:02.057458+08', '2026-08-17 13:15:02.057458+08', NULL);
INSERT INTO public.resources VALUES ('232ea6d4-b90b-48b0-8a6f-d1e7f1085933', 'hobbies/2026/08/89b58f72dae04874a141654b0ad94e37.jpg', 'ysy-myblog', 'hobby-crop-1786943726022.jpg', 'image/jpeg', 236050, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-17 13:15:26.1017+08', '2026-08-17 13:15:26.1017+08', NULL);
INSERT INTO public.resources VALUES ('e94c3f43-ff43-4590-85a6-8e5b9a76ccca', 'hobbies/2026/08/32120b0437464bce862450f3a0c09e1a.jpg', 'ysy-myblog', 'hobby-crop-1786943743254.jpg', 'image/jpeg', 515076, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-17 13:15:43.381232+08', '2026-08-17 13:15:43.381232+08', NULL);
INSERT INTO public.resources VALUES ('4c892724-9106-4acf-a5b1-01f4e6ec9fa7', 'hobbies/2026/08/e5702f6e897d4438823d860509ed4b6a.jpg', 'ysy-myblog', 'family and friends.jpg', 'image/jpeg', 847337, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-16 22:55:48.960609+08', '2026-08-17 19:02:31.746557+08', '2026-08-17 19:02:31.74655+08');
INSERT INTO public.resources VALUES ('268a932a-8791-4337-8455-b0691231f76f', 'hobbies/2026/08/6a8f4fc4301246f28ce84aaa684b2639.png', 'ysy-myblog', 'family and friends.png', 'image/png', 621217, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-12 22:25:58.899892+08', '2026-08-17 19:03:12.238053+08', '2026-08-17 19:03:12.238047+08');
INSERT INTO public.resources VALUES ('0064fca2-da7d-49f4-935e-5dbd66a9e45a', 'hobbies/2026/08/22c73eb69c22459887c8fb8b776b3ac3.png', 'ysy-myblog', 'sport.png', 'image/png', 576586, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-12 22:25:46.011414+08', '2026-08-17 19:03:14.508692+08', '2026-08-17 19:03:14.508687+08');
INSERT INTO public.resources VALUES ('3fbc5a0d-4b21-4b18-bcab-8e7772569203', 'hobbies/2026/08/b46bbc25cca943c785179c6a291d22e4.png', 'ysy-myblog', '屏幕截图 2026-08-12 204206.png', 'image/png', 1580345, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-12 21:53:23.577919+08', '2026-08-17 19:03:17.830349+08', '2026-08-17 19:03:17.830343+08');


-- Data for Name: site_daily_stats; Type: TABLE DATA; Schema: public; Owner: -

INSERT INTO public.site_daily_stats VALUES ('2026-08-10', 4, 3, 2, '2026-08-10 21:24:49.05799+08', '2026-08-10 21:57:09.469897+08');
INSERT INTO public.site_daily_stats VALUES ('2026-08-11', 8, 1, 0, '2026-08-11 14:29:31.228112+08', '2026-08-11 23:47:00.134519+08');
INSERT INTO public.site_daily_stats VALUES ('2026-08-12', 4, 2, 0, '2026-08-12 00:33:00.132072+08', '2026-08-12 22:18:18.630517+08');
INSERT INTO public.site_daily_stats VALUES ('2026-08-13', 4, 2, 0, '2026-08-13 13:11:43.973572+08', '2026-08-13 22:11:06.034628+08');
INSERT INTO public.site_daily_stats VALUES ('2026-08-14', 1, 0, 0, '2026-08-14 01:55:32.061146+08', '2026-08-14 01:55:32.061146+08');
INSERT INTO public.site_daily_stats VALUES ('2026-08-16', 4, 0, 0, '2026-08-16 20:12:30.596956+08', '2026-08-16 22:53:27.776964+08');
INSERT INTO public.site_daily_stats VALUES ('2026-08-17', 1, 0, 0, '2026-08-17 13:08:32.004659+08', '2026-08-17 13:08:32.004659+08');


-- Data for Name: site_traffic_stats; Type: TABLE DATA; Schema: public; Owner: -

INSERT INTO public.site_traffic_stats VALUES (1, 26, 8, 1, '2026-08-10 21:22:45.82724+08', '2026-08-17 13:08:32.004659+08');


-- Data for Name: skills; Type: TABLE DATA; Schema: public; Owner: -

INSERT INTO public.skills VALUES ('b12ac7a2-5db1-5e5c-9be8-84bf1be05b2c', '7db57c7a-60a7-5bf5-9432-f17a2c497537', 'csharp-dotnet', 'C# / .NET', 80, 'proficient', '熟练', '106e0cb3-c95b-5d3f-96dd-997169ac6510', 'coral', false, true, 0, '2026-08-10 00:45:04.413491+08', '2026-08-10 00:45:04.413491+08', NULL);
INSERT INTO public.skills VALUES ('5b831018-dd3e-5a71-a35b-cd8096fab6ca', '7db57c7a-60a7-5bf5-9432-f17a2c497537', 'java-spring-boot', 'Java / Spring Boot', 80, 'proficient', '熟练', 'ecae2d77-5b15-5918-93d7-12ef0e0544db', 'coral', false, true, 1, '2026-08-10 00:45:04.413491+08', '2026-08-10 00:45:04.413491+08', NULL);
INSERT INTO public.skills VALUES ('445140a2-2472-5c15-8cce-159a0dfdf04e', '7db57c7a-60a7-5bf5-9432-f17a2c497537', 'docker', 'Docker', 70, 'competent', '熟练', '63df3f0a-bda9-5de0-a068-d77d75916815', 'teal', false, true, 2, '2026-08-10 00:45:04.413491+08', '2026-08-10 00:45:04.413491+08', NULL);
INSERT INTO public.skills VALUES ('5e6b8d4a-e5bc-549e-8914-90c4832057bc', '7db57c7a-60a7-5bf5-9432-f17a2c497537', 'sql', 'SQL', 70, 'competent', '熟练', '9b936246-207d-5841-a65b-a9f4b8d1aacb', 'teal', false, true, 3, '2026-08-10 00:45:04.413491+08', '2026-08-10 00:45:04.413491+08', NULL);
INSERT INTO public.skills VALUES ('93b2051b-fad2-52f6-90b6-3e0179619603', '7db57c7a-60a7-5bf5-9432-f17a2c497537', 'javascript-typescript', 'JavaScript / TypeScript', 30, 'novice', '入门', 'c31fb74c-cd2a-52b2-bb35-e817627271f3', 'coral', true, true, 4, '2026-08-10 00:45:04.413491+08', '2026-08-10 00:45:04.413491+08', NULL);
INSERT INTO public.skills VALUES ('16270450-df29-559e-9d2f-75170e89c6f4', '7db57c7a-60a7-5bf5-9432-f17a2c497537', 'react-vue', 'React / Vue', 30, 'novice', '入门', '2bb66285-e985-5cf8-bdc8-0d77264a195f', 'coral', true, true, 5, '2026-08-10 00:45:04.413491+08', '2026-08-10 00:45:04.413491+08', NULL);
INSERT INTO public.skills VALUES ('8f22a833-7142-5c70-8041-b02a5ccf0bdc', '7db57c7a-60a7-5bf5-9432-f17a2c497537', 'python', 'Python', 30, 'novice', '入门', '53587a9f-f3a2-584d-ae4e-a8c7a2fa62a3', 'coral', true, true, 6, '2026-08-10 00:45:04.413491+08', '2026-08-10 00:45:04.413491+08', NULL);
INSERT INTO public.skills VALUES ('cb09a7f1-763e-4a6e-91ee-2dd87903f75c', '53f769a6-4d8e-43b2-85f7-602dd87ad06f', 'csharp-dotnet', 'C# / .NET', 80, 'proficient', '熟练', '106e0cb3-c95b-5d3f-96dd-997169ac6510', 'coral', false, true, 0, '2026-08-10 12:32:51.729688+08', '2026-08-10 12:32:51.729688+08', NULL);
INSERT INTO public.skills VALUES ('91f43dd0-43ba-4cf6-a430-3fcb97f67535', '53f769a6-4d8e-43b2-85f7-602dd87ad06f', 'java-spring-boot', 'Java / Spring Boot', 80, 'proficient', '熟练', 'ecae2d77-5b15-5918-93d7-12ef0e0544db', 'coral', false, true, 1, '2026-08-10 12:32:51.729688+08', '2026-08-10 12:32:51.729688+08', NULL);
INSERT INTO public.skills VALUES ('608838e2-4daa-450f-b1dc-706e229d5c34', '53f769a6-4d8e-43b2-85f7-602dd87ad06f', 'docker', 'Docker', 70, 'competent', '熟练', '63df3f0a-bda9-5de0-a068-d77d75916815', 'teal', false, true, 2, '2026-08-10 12:32:51.729688+08', '2026-08-10 12:32:51.729688+08', NULL);
INSERT INTO public.skills VALUES ('96ed9d9a-cbab-4681-990f-1ce63b956a4a', '53f769a6-4d8e-43b2-85f7-602dd87ad06f', 'sql', 'SQL', 70, 'competent', '熟练', '9b936246-207d-5841-a65b-a9f4b8d1aacb', 'teal', false, true, 3, '2026-08-10 12:32:51.729688+08', '2026-08-10 12:32:51.729688+08', NULL);
INSERT INTO public.skills VALUES ('56615bc7-9993-4ae3-91c6-ed25ff4a9dfb', '53f769a6-4d8e-43b2-85f7-602dd87ad06f', 'javascript-typescript', 'JavaScript / TypeScript', 30, 'novice', '入门', 'c31fb74c-cd2a-52b2-bb35-e817627271f3', 'coral', false, true, 4, '2026-08-10 12:32:51.729688+08', '2026-08-10 12:32:51.729688+08', NULL);
INSERT INTO public.skills VALUES ('e541e9ed-2df7-4429-b665-45c638659546', '53f769a6-4d8e-43b2-85f7-602dd87ad06f', 'react-vue', 'React / Vue', 30, 'novice', '入门', '2bb66285-e985-5cf8-bdc8-0d77264a195f', 'coral', false, true, 5, '2026-08-10 12:32:51.729688+08', '2026-08-10 12:32:51.729688+08', NULL);
INSERT INTO public.skills VALUES ('4d554160-6059-4fc4-8a8a-874a8557240a', '53f769a6-4d8e-43b2-85f7-602dd87ad06f', 'python', 'Python', 30, 'novice', '入门', '53587a9f-f3a2-584d-ae4e-a8c7a2fa62a3', 'coral', false, true, 6, '2026-08-10 12:32:51.729688+08', '2026-08-10 12:32:51.729688+08', NULL);
INSERT INTO public.skills VALUES ('51457a05-3062-4ee5-afac-8792a218c3d3', 'f6d1b717-651e-48d8-a0d3-8ceeb1666d36', 'java-spring-boot', 'Java / Spring Boot', 80, 'proficient', '熟练', 'ecae2d77-5b15-5918-93d7-12ef0e0544db', 'coral', false, true, 0, '2026-08-11 14:45:20.156552+08', '2026-08-11 14:45:20.156552+08', NULL);
INSERT INTO public.skills VALUES ('22789985-c0e7-4422-ac2d-c56c26fcbd82', 'f6d1b717-651e-48d8-a0d3-8ceeb1666d36', 'csharp-dotnet', 'C# / .NET', 80, 'proficient', '熟练', '106e0cb3-c95b-5d3f-96dd-997169ac6510', 'coral', false, true, 1, '2026-08-11 14:45:20.156552+08', '2026-08-11 14:45:20.156552+08', NULL);
INSERT INTO public.skills VALUES ('0e0df5db-614e-4b5e-abdb-425348a5f901', 'f6d1b717-651e-48d8-a0d3-8ceeb1666d36', 'sql', 'MySQL', 70, 'competent', '掌握', '9b936246-207d-5841-a65b-a9f4b8d1aacb', 'teal', false, true, 2, '2026-08-11 14:45:20.156552+08', '2026-08-11 14:45:20.156552+08', NULL);
INSERT INTO public.skills VALUES ('6ae47e56-6727-4361-bed4-e767a957f2b0', 'f6d1b717-651e-48d8-a0d3-8ceeb1666d36', 'docker', 'Docker', 70, 'competent', '掌握', '63df3f0a-bda9-5de0-a068-d77d75916815', 'teal', false, true, 3, '2026-08-11 14:45:20.156552+08', '2026-08-11 14:45:20.156552+08', NULL);
INSERT INTO public.skills VALUES ('9b7a9531-b2df-4ca7-9545-7ed41ca17b7c', 'f6d1b717-651e-48d8-a0d3-8ceeb1666d36', 'javascript-typescript', 'JavaScript / TypeScript', 30, 'novice', '入门', 'c31fb74c-cd2a-52b2-bb35-e817627271f3', 'gray-white', false, true, 4, '2026-08-11 14:45:20.156552+08', '2026-08-11 14:45:20.156552+08', NULL);
INSERT INTO public.skills VALUES ('33fbcd1e-7f29-4ff6-8456-6a50c07d94b9', 'f6d1b717-651e-48d8-a0d3-8ceeb1666d36', 'react-vue', 'React / Vue', 30, 'novice', '入门', '2bb66285-e985-5cf8-bdc8-0d77264a195f', 'gray-white', false, true, 5, '2026-08-11 14:45:20.156552+08', '2026-08-11 14:45:20.156552+08', NULL);
INSERT INTO public.skills VALUES ('97fe561b-a156-409d-8398-7a32a3266908', 'f6d1b717-651e-48d8-a0d3-8ceeb1666d36', 'python', 'Python', 30, 'novice', '入门', '53587a9f-f3a2-584d-ae4e-a8c7a2fa62a3', 'gray-white', false, true, 6, '2026-08-11 14:45:20.156552+08', '2026-08-11 14:45:20.156552+08', NULL);


-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: -

INSERT INTO public.users VALUES ('b7b1a013-fc83-579a-a1e3-bb1cc0483bac', 'admin', '$2a$12$6feNM80PGgXs0en.BWDbzeUZzp71yNmPNGakhiHmuzf5TKUxdPOPG', 'superadmin', true, '2026-08-16 22:45:57.641684+08', '2026-08-10 00:45:04.351009+08', '2026-08-10 00:45:04.351009+08', NULL);


-- Data for Name: vibe_tools; Type: TABLE DATA; Schema: public; Owner: -

INSERT INTO public.vibe_tools VALUES ('61f16c9b-f42b-53f2-b1d9-e60f2a1fd69a', 'd5aa99ad-9e7e-5022-9093-9fe8b80c92c9', 'cursor', 'Cursor', 80, '代码编写主力，执行明确任务，性价比高', true, 0, '2026-08-10 00:45:04.438105+08', '2026-08-10 00:45:04.438105+08', NULL);
INSERT INTO public.vibe_tools VALUES ('1311966d-7927-5666-9afb-3095f23b8e5b', 'd5aa99ad-9e7e-5022-9093-9fe8b80c92c9', 'codex', 'Codex', 80, '代码编写主力，用户意图理解力强，执行需求模糊的任务', true, 1, '2026-08-10 00:45:04.438105+08', '2026-08-10 00:45:04.438105+08', NULL);
INSERT INTO public.vibe_tools VALUES ('d817e699-0ba9-5322-8494-529f541748ad', 'd5aa99ad-9e7e-5022-9093-9fe8b80c92c9', 'claude-code', 'Claude Code', 60, '代码编写辅助，生成代码质量高，执行复杂任务', true, 2, '2026-08-10 00:45:04.438105+08', '2026-08-10 00:45:04.438105+08', NULL);
INSERT INTO public.vibe_tools VALUES ('7f29b85c-ada2-527a-9416-ab672bc37c61', 'd5aa99ad-9e7e-5022-9093-9fe8b80c92c9', 'kimi', 'Kimi', 60, '我最初使用的AI工具，目前作为日常辅助问答以及API调用', true, 3, '2026-08-10 00:45:04.438105+08', '2026-08-10 00:45:04.438105+08', NULL);
INSERT INTO public.vibe_tools VALUES ('4a3f36ef-40b5-530b-9c29-be833a5ead70', 'd5aa99ad-9e7e-5022-9093-9fe8b80c92c9', 'deepseek', 'DeepSeek', 40, '有时疑似被Kimi拉黑，作为国产模型探索以及kimi的替代', true, 4, '2026-08-10 00:45:04.438105+08', '2026-08-10 00:45:04.438105+08', NULL);
INSERT INTO public.vibe_tools VALUES ('739dfdfb-2258-58d1-a070-3f7b8ec729c0', 'd5aa99ad-9e7e-5022-9093-9fe8b80c92c9', 'chatgpt', 'ChatGPT', 20, '图片素材生成，以及日常辅助问答(暗黑版)', true, 5, '2026-08-10 00:45:04.438105+08', '2026-08-10 00:45:04.438105+08', NULL);
INSERT INTO public.vibe_tools VALUES ('e3092d99-70f2-4a69-8122-07ccf4fec5bf', 'a3eed050-75dd-46a4-b2b4-19f0b2d92b52', 'codex', 'Codex', 80, '代码编写主力，用户意图理解力强，用于执行关键任务', true, 0, '2026-08-11 15:17:27.588254+08', '2026-08-11 15:17:27.588254+08', NULL);
INSERT INTO public.vibe_tools VALUES ('5d4d7c12-ceb1-4e60-81a6-661e90ecebc6', 'a3eed050-75dd-46a4-b2b4-19f0b2d92b52', 'kimi', 'Kimi', 70, '代码编写辅助，作为国产模型探索以及codex替补，执行需求模糊的任务', true, 1, '2026-08-11 15:17:27.588254+08', '2026-08-11 15:17:27.588254+08', NULL);
INSERT INTO public.vibe_tools VALUES ('43eff8ce-c126-4a3c-a222-a1e032f108d5', 'a3eed050-75dd-46a4-b2b4-19f0b2d92b52', 'cursor', 'Cursor', 70, '前代码编写主力，目前用作项目分析，方案编写以及知识问答等', true, 2, '2026-08-11 15:17:27.588254+08', '2026-08-11 15:17:27.588254+08', NULL);
INSERT INTO public.vibe_tools VALUES ('e6ed2fed-d6e6-4d52-b587-f29d2438cf88', 'a3eed050-75dd-46a4-b2b4-19f0b2d92b52', 'claude-code', 'Claude Code', 60, '前代码编写主力，生成代码质量高，执行复杂任务', true, 3, '2026-08-11 15:17:27.588254+08', '2026-08-11 15:17:27.588254+08', NULL);
INSERT INTO public.vibe_tools VALUES ('cfbcb18f-b3d1-4688-a73e-b7608ca550d4', 'a3eed050-75dd-46a4-b2b4-19f0b2d92b52', 'deepseek', 'DeepSeek', 40, '同作为国产模型探索，并作为主要的API调用，以及一些日常辅助问答', true, 4, '2026-08-11 15:17:27.588254+08', '2026-08-11 15:17:27.588254+08', NULL);
INSERT INTO public.vibe_tools VALUES ('8610fba2-b793-447e-b0e8-218228b04305', 'a3eed050-75dd-46a4-b2b4-19f0b2d92b52', 'chatgpt', 'ChatGPT', 20, '图片素材生成，以及日常辅助问答(暗黑版)', true, 5, '2026-08-11 15:17:27.588254+08', '2026-08-11 15:17:27.588254+08', NULL);
INSERT INTO public.vibe_tools VALUES ('9c7561ae-e130-4b2a-98a6-3c2446c83866', 'e5d78b76-992b-4ed5-9b8c-9d187a628573', 'codex', 'Codex', 80, '代码编写主力，用户意图理解力强，用于执行关键任务', true, 0, '2026-08-13 13:32:14.22007+08', '2026-08-13 13:32:14.22007+08', NULL);
INSERT INTO public.vibe_tools VALUES ('6a5c82ce-72d1-4b12-9343-fda44d6efb47', 'e5d78b76-992b-4ed5-9b8c-9d187a628573', 'kimi', 'Kimi', 70, '代码编写辅助，codex的替补，执行相对边缘的任务', true, 1, '2026-08-13 13:32:14.22007+08', '2026-08-13 13:32:14.22007+08', NULL);
INSERT INTO public.vibe_tools VALUES ('ccef2b0a-cf35-4e66-ab37-219fa59b7654', 'e5d78b76-992b-4ed5-9b8c-9d187a628573', 'cursor', 'Cursor', 60, '前代码编写主力，目前用作项目分析，方案编写以及知识问答等', true, 2, '2026-08-13 13:32:14.22007+08', '2026-08-13 13:32:14.22007+08', NULL);
INSERT INTO public.vibe_tools VALUES ('94f0a311-53dd-40f9-822f-4123cdb19d42', 'e5d78b76-992b-4ed5-9b8c-9d187a628573', 'claude-code', 'Claude Code', 60, '前代码编写主力，生成代码质量高，执行复杂任务', true, 3, '2026-08-13 13:32:14.22007+08', '2026-08-13 13:32:14.22007+08', NULL);
INSERT INTO public.vibe_tools VALUES ('f3ee5ce8-3a21-4328-a833-135343e39faf', 'e5d78b76-992b-4ed5-9b8c-9d187a628573', 'deepseek', 'DeepSeek', 40, '作为国产模型探索，主要的API调用，以及一些日常辅助问答', true, 4, '2026-08-13 13:32:14.22007+08', '2026-08-13 13:32:14.22007+08', NULL);
INSERT INTO public.vibe_tools VALUES ('b38a7717-ce69-4484-8c46-3a3376bd0070', 'e5d78b76-992b-4ed5-9b8c-9d187a628573', 'chatgpt', 'ChatGPT', 20, '图片素材生成，以及日常辅助问答(暗黑版)', true, 5, '2026-08-13 13:32:14.22007+08', '2026-08-13 13:32:14.22007+08', NULL);


-- Name: about_bubbles about_bubbles_pkey; Type: CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.about_bubbles
    ADD CONSTRAINT about_bubbles_pkey PRIMARY KEY (id);


-- Name: about_contents about_contents_pkey; Type: CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.about_contents
    ADD CONSTRAINT about_contents_pkey PRIMARY KEY (id);


-- Name: about_profile_bullets about_profile_bullets_pkey; Type: CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.about_profile_bullets
    ADD CONSTRAINT about_profile_bullets_pkey PRIMARY KEY (id);


-- Name: content_releases content_releases_pkey; Type: CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.content_releases
    ADD CONSTRAINT content_releases_pkey PRIMARY KEY (id);


-- Name: footprint_resources footprint_resources_pkey; Type: CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.footprint_resources
    ADD CONSTRAINT footprint_resources_pkey PRIMARY KEY (id);


-- Name: footprints footprints_pkey; Type: CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.footprints
    ADD CONSTRAINT footprints_pkey PRIMARY KEY (id);


-- Name: hobbies hobbies_pkey; Type: CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.hobbies
    ADD CONSTRAINT hobbies_pkey PRIMARY KEY (id);


-- Name: hobby_resources hobby_resources_pkey; Type: CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.hobby_resources
    ADD CONSTRAINT hobby_resources_pkey PRIMARY KEY (id);


-- Name: hobby_time_points hobby_time_points_pkey; Type: CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.hobby_time_points
    ADD CONSTRAINT hobby_time_points_pkey PRIMARY KEY (id);


-- Name: hobby_time_tags hobby_time_tags_pkey; Type: CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.hobby_time_tags
    ADD CONSTRAINT hobby_time_tags_pkey PRIMARY KEY (id);


-- Name: home_images home_images_pkey; Type: CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.home_images
    ADD CONSTRAINT home_images_pkey PRIMARY KEY (id);


-- Name: mylab_card_tags mylab_card_tags_pkey; Type: CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.mylab_card_tags
    ADD CONSTRAINT mylab_card_tags_pkey PRIMARY KEY (id);


-- Name: mylab_cards mylab_cards_pkey; Type: CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.mylab_cards
    ADD CONSTRAINT mylab_cards_pkey PRIMARY KEY (id);


-- Name: mylab_engagement_stats mylab_engagement_stats_pkey; Type: CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.mylab_engagement_stats
    ADD CONSTRAINT mylab_engagement_stats_pkey PRIMARY KEY (post_key);


-- Name: mylab_resources mylab_resources_pkey; Type: CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.mylab_resources
    ADD CONSTRAINT mylab_resources_pkey PRIMARY KEY (id);


-- Name: mylab_tags mylab_tags_pkey; Type: CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.mylab_tags
    ADD CONSTRAINT mylab_tags_pkey PRIMARY KEY (id);


-- Name: resources resources_object_key_key; Type: CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.resources
    ADD CONSTRAINT resources_object_key_key UNIQUE (object_key);


-- Name: resources resources_pkey; Type: CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.resources
    ADD CONSTRAINT resources_pkey PRIMARY KEY (id);


-- Name: site_daily_stats site_daily_stats_pkey; Type: CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.site_daily_stats
    ADD CONSTRAINT site_daily_stats_pkey PRIMARY KEY (stat_date);


-- Name: site_traffic_stats site_traffic_stats_pkey; Type: CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.site_traffic_stats
    ADD CONSTRAINT site_traffic_stats_pkey PRIMARY KEY (id);


-- Name: skills skills_pkey; Type: CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.skills
    ADD CONSTRAINT skills_pkey PRIMARY KEY (id);


-- Name: content_releases uq_content_release_version; Type: CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.content_releases
    ADD CONSTRAINT uq_content_release_version UNIQUE (module_key, version_no);


-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


-- Name: users users_username_key; Type: CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_username_key UNIQUE (username);


-- Name: vibe_tools vibe_tools_pkey; Type: CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.vibe_tools
    ADD CONSTRAINT vibe_tools_pkey PRIMARY KEY (id);


-- Name: idx_about_contents_avatar; Type: INDEX; Schema: public; Owner: -

CREATE INDEX idx_about_contents_avatar ON public.about_contents USING btree (avatar_resource_id);


-- Name: idx_content_release_history; Type: INDEX; Schema: public; Owner: -

CREATE INDEX idx_content_release_history ON public.content_releases USING btree (module_key, version_no DESC) WHERE (deleted_at IS NULL);


-- Name: idx_content_releases_published_by; Type: INDEX; Schema: public; Owner: -

CREATE INDEX idx_content_releases_published_by ON public.content_releases USING btree (published_by);


-- Name: idx_content_releases_source; Type: INDEX; Schema: public; Owner: -

CREATE INDEX idx_content_releases_source ON public.content_releases USING btree (source_release_id);


-- Name: idx_footprint_resources_resource; Type: INDEX; Schema: public; Owner: -

CREATE INDEX idx_footprint_resources_resource ON public.footprint_resources USING btree (resource_id);


-- Name: idx_footprints_release_order; Type: INDEX; Schema: public; Owner: -

CREATE INDEX idx_footprints_release_order ON public.footprints USING btree (release_id, sort_order, city_key) WHERE (deleted_at IS NULL);


-- Name: idx_hobbies_release_order; Type: INDEX; Schema: public; Owner: -

CREATE INDEX idx_hobbies_release_order ON public.hobbies USING btree (release_id, sort_order, hobby_key) WHERE (deleted_at IS NULL);


-- Name: idx_hobby_resources_resource; Type: INDEX; Schema: public; Owner: -

CREATE INDEX idx_hobby_resources_resource ON public.hobby_resources USING btree (resource_id);


-- Name: idx_home_images_resource; Type: INDEX; Schema: public; Owner: -

CREATE INDEX idx_home_images_resource ON public.home_images USING btree (image_resource_id);


-- Name: idx_mylab_card_tags_tag; Type: INDEX; Schema: public; Owner: -

CREATE INDEX idx_mylab_card_tags_tag ON public.mylab_card_tags USING btree (tag_id, card_id) WHERE (deleted_at IS NULL);


-- Name: idx_mylab_cards_release_order; Type: INDEX; Schema: public; Owner: -

CREATE INDEX idx_mylab_cards_release_order ON public.mylab_cards USING btree (release_id, sort_order, post_key) WHERE (deleted_at IS NULL);


-- Name: idx_mylab_resources_content; Type: INDEX; Schema: public; Owner: -

CREATE INDEX idx_mylab_resources_content ON public.mylab_resources USING btree (content_resource_id);


-- Name: idx_mylab_resources_image; Type: INDEX; Schema: public; Owner: -

CREATE INDEX idx_mylab_resources_image ON public.mylab_resources USING btree (image_resource_id);


-- Name: idx_mylab_tags_order; Type: INDEX; Schema: public; Owner: -

CREATE INDEX idx_mylab_tags_order ON public.mylab_tags USING btree (sort_order, tag_key) WHERE (deleted_at IS NULL);


-- Name: idx_resources_uploaded_by; Type: INDEX; Schema: public; Owner: -

CREATE INDEX idx_resources_uploaded_by ON public.resources USING btree (uploaded_by);


-- Name: idx_skills_icon_resource; Type: INDEX; Schema: public; Owner: -

CREATE INDEX idx_skills_icon_resource ON public.skills USING btree (icon_resource_id);


-- Name: idx_skills_release_order; Type: INDEX; Schema: public; Owner: -

CREATE INDEX idx_skills_release_order ON public.skills USING btree (release_id, sort_order, skill_key) WHERE (deleted_at IS NULL);


-- Name: idx_vibe_tools_release_order; Type: INDEX; Schema: public; Owner: -

CREATE INDEX idx_vibe_tools_release_order ON public.vibe_tools USING btree (release_id, sort_order, tool_key) WHERE (deleted_at IS NULL);


-- Name: uq_about_bubbles_order; Type: INDEX; Schema: public; Owner: -

CREATE UNIQUE INDEX uq_about_bubbles_order ON public.about_bubbles USING btree (about_content_id, sort_order) WHERE (deleted_at IS NULL);


-- Name: uq_about_contents_release; Type: INDEX; Schema: public; Owner: -

CREATE UNIQUE INDEX uq_about_contents_release ON public.about_contents USING btree (release_id) WHERE (deleted_at IS NULL);


-- Name: uq_about_profile_bullets_order; Type: INDEX; Schema: public; Owner: -

CREATE UNIQUE INDEX uq_about_profile_bullets_order ON public.about_profile_bullets USING btree (about_content_id, sort_order) WHERE (deleted_at IS NULL);


-- Name: uq_content_release_current; Type: INDEX; Schema: public; Owner: -

CREATE UNIQUE INDEX uq_content_release_current ON public.content_releases USING btree (module_key) WHERE (((state)::text = ANY ((ARRAY['PUBLISHED'::character varying, 'OFFLINE'::character varying])::text[])) AND (deleted_at IS NULL));


-- Name: uq_content_release_draft; Type: INDEX; Schema: public; Owner: -

CREATE UNIQUE INDEX uq_content_release_draft ON public.content_releases USING btree (module_key) WHERE (((state)::text = 'DRAFT'::text) AND (deleted_at IS NULL));


-- Name: uq_footprint_resources_order; Type: INDEX; Schema: public; Owner: -

CREATE UNIQUE INDEX uq_footprint_resources_order ON public.footprint_resources USING btree (footprint_id, sort_order) WHERE (deleted_at IS NULL);


-- Name: uq_footprint_resources_resource; Type: INDEX; Schema: public; Owner: -

CREATE UNIQUE INDEX uq_footprint_resources_resource ON public.footprint_resources USING btree (footprint_id, resource_id) WHERE (deleted_at IS NULL);


-- Name: uq_footprints_release_key; Type: INDEX; Schema: public; Owner: -

CREATE UNIQUE INDEX uq_footprints_release_key ON public.footprints USING btree (release_id, city_key) WHERE (deleted_at IS NULL);


-- Name: uq_hobbies_release_key; Type: INDEX; Schema: public; Owner: -

CREATE UNIQUE INDEX uq_hobbies_release_key ON public.hobbies USING btree (release_id, hobby_key) WHERE (deleted_at IS NULL);


-- Name: uq_hobby_resources_hobby; Type: INDEX; Schema: public; Owner: -

CREATE UNIQUE INDEX uq_hobby_resources_hobby ON public.hobby_resources USING btree (hobby_id) WHERE (deleted_at IS NULL);


-- Name: uq_hobby_time_points_release_age; Type: INDEX; Schema: public; Owner: -

CREATE UNIQUE INDEX uq_hobby_time_points_release_age ON public.hobby_time_points USING btree (release_id, age) WHERE (deleted_at IS NULL);


-- Name: uq_hobby_time_tags_release_key; Type: INDEX; Schema: public; Owner: -

CREATE UNIQUE INDEX uq_hobby_time_tags_release_key ON public.hobby_time_tags USING btree (release_id, data_key) WHERE (deleted_at IS NULL);


-- Name: uq_hobby_time_tags_release_order; Type: INDEX; Schema: public; Owner: -

CREATE UNIQUE INDEX uq_hobby_time_tags_release_order ON public.hobby_time_tags USING btree (release_id, sort_order) WHERE (deleted_at IS NULL);


-- Name: uq_home_images_release_order; Type: INDEX; Schema: public; Owner: -

CREATE UNIQUE INDEX uq_home_images_release_order ON public.home_images USING btree (release_id, sort_order) WHERE (deleted_at IS NULL);


-- Name: uq_mylab_card_tags_order; Type: INDEX; Schema: public; Owner: -

CREATE UNIQUE INDEX uq_mylab_card_tags_order ON public.mylab_card_tags USING btree (card_id, sort_order) WHERE (deleted_at IS NULL);


-- Name: uq_mylab_card_tags_pair; Type: INDEX; Schema: public; Owner: -

CREATE UNIQUE INDEX uq_mylab_card_tags_pair ON public.mylab_card_tags USING btree (card_id, tag_id) WHERE (deleted_at IS NULL);


-- Name: uq_mylab_cards_project_order; Type: INDEX; Schema: public; Owner: -

CREATE UNIQUE INDEX uq_mylab_cards_project_order ON public.mylab_cards USING btree (release_id, project_show_order) WHERE (((card_type)::text = 'PROJECT'::text) AND (deleted_at IS NULL));


-- Name: uq_mylab_cards_release_key; Type: INDEX; Schema: public; Owner: -

CREATE UNIQUE INDEX uq_mylab_cards_release_key ON public.mylab_cards USING btree (release_id, post_key) WHERE (deleted_at IS NULL);


-- Name: uq_mylab_resources_card; Type: INDEX; Schema: public; Owner: -

CREATE UNIQUE INDEX uq_mylab_resources_card ON public.mylab_resources USING btree (card_id) WHERE (deleted_at IS NULL);


-- Name: uq_mylab_tags_key; Type: INDEX; Schema: public; Owner: -

CREATE UNIQUE INDEX uq_mylab_tags_key ON public.mylab_tags USING btree (tag_key) WHERE (deleted_at IS NULL);


-- Name: uq_mylab_tags_name; Type: INDEX; Schema: public; Owner: -

CREATE UNIQUE INDEX uq_mylab_tags_name ON public.mylab_tags USING btree (name) WHERE (deleted_at IS NULL);


-- Name: uq_skills_release_key; Type: INDEX; Schema: public; Owner: -

CREATE UNIQUE INDEX uq_skills_release_key ON public.skills USING btree (release_id, skill_key) WHERE (deleted_at IS NULL);


-- Name: uq_vibe_tools_release_key; Type: INDEX; Schema: public; Owner: -

CREATE UNIQUE INDEX uq_vibe_tools_release_key ON public.vibe_tools USING btree (release_id, tool_key) WHERE (deleted_at IS NULL);


-- Name: about_bubbles about_bubbles_about_content_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.about_bubbles
    ADD CONSTRAINT about_bubbles_about_content_id_fkey FOREIGN KEY (about_content_id) REFERENCES public.about_contents(id) ON DELETE CASCADE;


-- Name: about_contents about_contents_avatar_resource_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.about_contents
    ADD CONSTRAINT about_contents_avatar_resource_id_fkey FOREIGN KEY (avatar_resource_id) REFERENCES public.resources(id) ON DELETE RESTRICT;


-- Name: about_contents about_contents_release_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.about_contents
    ADD CONSTRAINT about_contents_release_id_fkey FOREIGN KEY (release_id) REFERENCES public.content_releases(id) ON DELETE CASCADE;


-- Name: about_profile_bullets about_profile_bullets_about_content_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.about_profile_bullets
    ADD CONSTRAINT about_profile_bullets_about_content_id_fkey FOREIGN KEY (about_content_id) REFERENCES public.about_contents(id) ON DELETE CASCADE;


-- Name: content_releases content_releases_published_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.content_releases
    ADD CONSTRAINT content_releases_published_by_fkey FOREIGN KEY (published_by) REFERENCES public.users(id) ON DELETE SET NULL;


-- Name: content_releases content_releases_source_release_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.content_releases
    ADD CONSTRAINT content_releases_source_release_id_fkey FOREIGN KEY (source_release_id) REFERENCES public.content_releases(id) ON DELETE RESTRICT;


-- Name: footprint_resources footprint_resources_footprint_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.footprint_resources
    ADD CONSTRAINT footprint_resources_footprint_id_fkey FOREIGN KEY (footprint_id) REFERENCES public.footprints(id) ON DELETE CASCADE;


-- Name: footprint_resources footprint_resources_resource_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.footprint_resources
    ADD CONSTRAINT footprint_resources_resource_id_fkey FOREIGN KEY (resource_id) REFERENCES public.resources(id) ON DELETE RESTRICT;


-- Name: footprints footprints_release_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.footprints
    ADD CONSTRAINT footprints_release_id_fkey FOREIGN KEY (release_id) REFERENCES public.content_releases(id) ON DELETE CASCADE;


-- Name: hobbies hobbies_release_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.hobbies
    ADD CONSTRAINT hobbies_release_id_fkey FOREIGN KEY (release_id) REFERENCES public.content_releases(id) ON DELETE CASCADE;


-- Name: hobby_resources hobby_resources_hobby_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.hobby_resources
    ADD CONSTRAINT hobby_resources_hobby_id_fkey FOREIGN KEY (hobby_id) REFERENCES public.hobbies(id) ON DELETE CASCADE;


-- Name: hobby_resources hobby_resources_resource_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.hobby_resources
    ADD CONSTRAINT hobby_resources_resource_id_fkey FOREIGN KEY (resource_id) REFERENCES public.resources(id) ON DELETE RESTRICT;


-- Name: hobby_time_points hobby_time_points_release_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.hobby_time_points
    ADD CONSTRAINT hobby_time_points_release_id_fkey FOREIGN KEY (release_id) REFERENCES public.content_releases(id) ON DELETE CASCADE;


-- Name: hobby_time_tags hobby_time_tags_release_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.hobby_time_tags
    ADD CONSTRAINT hobby_time_tags_release_id_fkey FOREIGN KEY (release_id) REFERENCES public.content_releases(id) ON DELETE CASCADE;


-- Name: home_images home_images_image_resource_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.home_images
    ADD CONSTRAINT home_images_image_resource_id_fkey FOREIGN KEY (image_resource_id) REFERENCES public.resources(id) ON DELETE RESTRICT;


-- Name: home_images home_images_release_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.home_images
    ADD CONSTRAINT home_images_release_id_fkey FOREIGN KEY (release_id) REFERENCES public.content_releases(id) ON DELETE CASCADE;


-- Name: mylab_card_tags mylab_card_tags_card_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.mylab_card_tags
    ADD CONSTRAINT mylab_card_tags_card_id_fkey FOREIGN KEY (card_id) REFERENCES public.mylab_cards(id) ON DELETE CASCADE;


-- Name: mylab_card_tags mylab_card_tags_tag_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.mylab_card_tags
    ADD CONSTRAINT mylab_card_tags_tag_id_fkey FOREIGN KEY (tag_id) REFERENCES public.mylab_tags(id) ON DELETE RESTRICT;


-- Name: mylab_cards mylab_cards_release_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.mylab_cards
    ADD CONSTRAINT mylab_cards_release_id_fkey FOREIGN KEY (release_id) REFERENCES public.content_releases(id) ON DELETE CASCADE;


-- Name: mylab_resources mylab_resources_card_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.mylab_resources
    ADD CONSTRAINT mylab_resources_card_id_fkey FOREIGN KEY (card_id) REFERENCES public.mylab_cards(id) ON DELETE CASCADE;


-- Name: mylab_resources mylab_resources_content_resource_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.mylab_resources
    ADD CONSTRAINT mylab_resources_content_resource_id_fkey FOREIGN KEY (content_resource_id) REFERENCES public.resources(id) ON DELETE RESTRICT;


-- Name: mylab_resources mylab_resources_image_resource_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.mylab_resources
    ADD CONSTRAINT mylab_resources_image_resource_id_fkey FOREIGN KEY (image_resource_id) REFERENCES public.resources(id) ON DELETE RESTRICT;


-- Name: resources resources_uploaded_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.resources
    ADD CONSTRAINT resources_uploaded_by_fkey FOREIGN KEY (uploaded_by) REFERENCES public.users(id) ON DELETE SET NULL;


-- Name: skills skills_icon_resource_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.skills
    ADD CONSTRAINT skills_icon_resource_id_fkey FOREIGN KEY (icon_resource_id) REFERENCES public.resources(id) ON DELETE RESTRICT;


-- Name: skills skills_release_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.skills
    ADD CONSTRAINT skills_release_id_fkey FOREIGN KEY (release_id) REFERENCES public.content_releases(id) ON DELETE CASCADE;


-- Name: vibe_tools vibe_tools_release_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -

ALTER TABLE ONLY public.vibe_tools
    ADD CONSTRAINT vibe_tools_release_id_fkey FOREIGN KEY (release_id) REFERENCES public.content_releases(id) ON DELETE CASCADE;




