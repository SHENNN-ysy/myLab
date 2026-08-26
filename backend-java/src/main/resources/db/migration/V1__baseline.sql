-- ============================================================================
-- V1 基线：以 2026-08-27 清理软删除数据后的本地 PostgreSQL 为准
-- 每张表按“建表、约束与索引、初始数据、外键”分组，便于独立核对。
-- 全新数据库由 Flyway 执行本脚本；后续结构变更应新增更高版本迁移。
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

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;

SET default_tablespace = '';
SET default_table_access_method = heap;

-- ============================================================================
-- 01. users（用户）
-- ============================================================================

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
    CONSTRAINT users_role_check CHECK (((role)::text = ANY (ARRAY[('viewer'::character varying)::text, ('editor'::character varying)::text, ('admin'::character varying)::text, ('superadmin'::character varying)::text])))
);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_username_key UNIQUE (username);



-- users 初始数据
-- 兼容用户仅用于承接历史发布外键，应用首次启动时立即按 INIT_ADMIN_* 接管账号信息。
INSERT INTO public.users (id, username, password_hash, role, is_active, last_login_at, created_at, updated_at, deleted_at) VALUES ('b7b1a013-fc83-579a-a1e3-bb1cc0483bac', 'admin', '$2a$12$6feNM80PGgXs0en.BWDbzeUZzp71yNmPNGakhiHmuzf5TKUxdPOPG', 'superadmin', true, NULL, '2026-08-10 00:45:04.351009+08', '2026-08-10 00:45:04.351009+08', NULL);

-- ============================================================================
-- 02. resources（资源）
-- ============================================================================

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

ALTER TABLE ONLY public.resources
    ADD CONSTRAINT resources_object_key_key UNIQUE (object_key);

ALTER TABLE ONLY public.resources
    ADD CONSTRAINT resources_pkey PRIMARY KEY (id);

CREATE INDEX idx_resources_uploaded_by ON public.resources USING btree (uploaded_by);

-- resources 初始数据
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('17a0aca0-a554-5d7c-811f-87950438bd0b', 'hero/hero-1.webp', 'ysy-myblog', 'hero-1.webp', 'image/webp', 600970, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('39c26cd2-109b-5c9a-99b1-0dcb7d19865a', 'hero/hero-2.webp', 'ysy-myblog', 'hero-2.webp', 'image/webp', 123308, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('266331b3-1978-5495-ad36-362647692ada', 'hero/hero-3.webp', 'ysy-myblog', 'hero-3.webp', 'image/webp', 678026, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('16760407-ecf2-5587-8d75-000a4c91686d', 'hero/hero-4.webp', 'ysy-myblog', 'hero-4.webp', 'image/webp', 435332, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('cd1316ac-c206-584c-982d-e024a084ec2d', 'hero/hero-5.webp', 'ysy-myblog', 'hero-5.webp', 'image/webp', 232300, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('931c1f55-66e5-536a-ad42-2ceec8cf5e5d', 'hero/hero-6.webp', 'ysy-myblog', 'hero-6.webp', 'image/webp', 703994, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('4889ba92-d366-55d2-82a4-a94833da1b8c', 'icon/avatar.png', 'ysy-myblog', 'avatar.png', 'image/png', 31793, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('106e0cb3-c95b-5d3f-96dd-997169ac6510', 'icon/csharp-dotnet.png', 'ysy-myblog', 'csharp-dotnet.png', 'image/png', 536302, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('ecae2d77-5b15-5918-93d7-12ef0e0544db', 'icon/java-spring-boot.png', 'ysy-myblog', 'java-spring-boot.png', 'image/png', 317103, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('63df3f0a-bda9-5de0-a068-d77d75916815', 'icon/docker.png', 'ysy-myblog', 'docker.png', 'image/png', 307425, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('9b936246-207d-5841-a65b-a9f4b8d1aacb', 'icon/sql.png', 'ysy-myblog', 'sql.png', 'image/png', 425596, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('c31fb74c-cd2a-52b2-bb35-e817627271f3', 'icon/javascript-typescript.png', 'ysy-myblog', 'javascript-typescript.png', 'image/png', 401596, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('2bb66285-e985-5cf8-bdc8-0d77264a195f', 'icon/react-vue.png', 'ysy-myblog', 'react-vue.png', 'image/png', 478440, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('53587a9f-f3a2-584d-ae4e-a8c7a2fa62a3', 'icon/python.png', 'ysy-myblog', 'python.png', 'image/png', 390178, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('5cab4584-dad0-5c12-9d39-1b9352dfd7a7', 'hobbies/cs2.jpg', 'ysy-myblog', 'cs2.jpg', 'image/jpeg', 727214, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('1a26d874-bd0f-5808-acc3-4a6c963ce6e4', 'hobbies/apex.jpg', 'ysy-myblog', 'apex.jpg', 'image/jpeg', 986834, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('8fc83aca-e31e-5a65-9195-44584997c8df', 'hobbies/delta-force.jpg', 'ysy-myblog', 'delta-force.jpg', 'image/jpeg', 576688, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('d898586e-5aa7-5a47-862a-698aedd0d287', 'hobbies/the-finals.jpg', 'ysy-myblog', 'the-finals.jpg', 'image/jpeg', 308002, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('81eedeaa-b0df-5ead-a804-f8bea0560100', 'hobbies/overwatch2.jpeg', 'ysy-myblog', 'overwatch2.jpeg', 'image/jpeg', 75442, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('a7ee90e0-9b3d-5dca-a5c5-0b8e119fc594', 'mylab/project-gm1.md', 'ysy-myblog', 'project-gm1.md', 'text/markdown', 197, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('513d2877-761e-51f0-ba14-648dd9d9b4e1', 'mylab/project-gm2.md', 'ysy-myblog', 'project-gm2.md', 'text/markdown', 215, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('247a975b-8bfc-5065-9887-dae2df61df79', 'mylab/project-gm3.md', 'ysy-myblog', 'project-gm3.md', 'text/markdown', 206, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('c4901a3e-ec1e-5a78-8703-3c006996fb3e', 'mylab/project-gm4.md', 'ysy-myblog', 'project-gm4.md', 'text/markdown', 219, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('454666a1-482b-5687-abbf-769c3c676d6e', 'mylab/project-gm5.md', 'ysy-myblog', 'project-gm5.md', 'text/markdown', 196, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('47993a1b-d360-54b6-b869-42438a3f05d7', 'mylab/project-gm6.md', 'ysy-myblog', 'project-gm6.md', 'text/markdown', 201, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('0efb8ec1-d08c-5296-a226-ff241e9a1c3e', 'mylab/blog-docker-deploy.md', 'ysy-myblog', 'blog-docker-deploy.md', 'text/markdown', 698, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('5eaf8bb5-1a8a-5791-a9fd-66a8dda4ce66', 'mylab/vue-gsap-hero.md', 'ysy-myblog', 'vue-gsap-hero.md', 'text/markdown', 531, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('37ec099c-2dca-593f-a9db-3eb8f0de6f0b', 'mylab/tailwind-migration.md', 'ysy-myblog', 'tailwind-migration.md', 'text/markdown', 360, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('ee612e27-b895-5946-beaa-6a9b272855f8', 'mylab/vue-composable-mouse-tilt.md', 'ysy-myblog', 'vue-composable-mouse-tilt.md', 'text/markdown', 398, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('2d0214f8-6e4f-5c3e-ac39-b54e4fd9de57', 'mylab/first-post.md', 'ysy-myblog', 'first-post.md', 'text/markdown', 434, NULL, '2026-08-10 00:45:04.393785+08', '2026-08-10 16:23:06.133799+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('edc8f7b4-861b-5df8-8fe2-10fb34579671', 'mylab-post/project-cover-1.webp', 'ysy-myblog', 'project-cover-1.webp', 'image/webp', 600970, NULL, '2026-08-10 18:10:31.12814+08', '2026-08-10 18:10:31.12814+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('66c98ded-1585-53e3-81bb-d3d81cd07551', 'mylab-post/project-cover-2.webp', 'ysy-myblog', 'project-cover-2.webp', 'image/webp', 123308, NULL, '2026-08-10 18:10:31.12814+08', '2026-08-10 18:10:31.12814+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('fa390e9e-11ae-578f-9cda-c59ce887e5b3', 'mylab-post/project-cover-3.webp', 'ysy-myblog', 'project-cover-3.webp', 'image/webp', 678026, NULL, '2026-08-10 18:10:31.12814+08', '2026-08-10 18:10:31.12814+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('9d1b7101-100b-5f3f-9185-11a59d08da77', 'mylab-post/project-cover-4.webp', 'ysy-myblog', 'project-cover-4.webp', 'image/webp', 435332, NULL, '2026-08-10 18:10:31.12814+08', '2026-08-10 18:10:31.12814+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('51decfbf-b194-5795-a31d-4b7dd377d0bb', 'mylab-post/project-cover-5.webp', 'ysy-myblog', 'project-cover-5.webp', 'image/webp', 232300, NULL, '2026-08-10 18:10:31.12814+08', '2026-08-10 18:10:31.12814+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('5e60c20f-9e5c-5d58-b7a9-121bf18696c8', 'mylab-post/project-cover-6.webp', 'ysy-myblog', 'project-cover-6.webp', 'image/webp', 703994, NULL, '2026-08-10 18:10:31.12814+08', '2026-08-10 18:10:31.12814+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('18478fc2-e6fd-4bc5-87cc-4a34646b373c', 'footstep/2026/08/83a8c122d1dd435186ab281fbf5c814c.webp', 'ysy-myblog', 'IMG_20240928_185814.webp', 'image/webp', 726158, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 16:13:01.167569+08', '2026-08-11 16:13:01.167569+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('81bf69f5-23d2-484f-bb1d-35085fcc68b6', 'footstep/2026/08/4d39e19186bf476f809da4d424bea2d2.webp', 'ysy-myblog', 'IMG_20240921_165341.webp', 'image/webp', 509856, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 16:30:48.591593+08', '2026-08-11 16:30:48.591593+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('e2853460-a24b-438f-99d1-cb06c5123ffd', 'footstep/2026/08/5f9225a96ed3412aa519008b996a2dcf.webp', 'ysy-myblog', 'IMG_20240921_164255.webp', 'image/webp', 593988, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 16:30:52.465825+08', '2026-08-11 16:30:52.465825+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('c3385923-95e9-4c99-8905-8dbc1c492fb8', 'footstep/2026/08/36a1acdb72174838b74d6782dad0ff3f.webp', 'ysy-myblog', 'IMG_20240916_153024.webp', 'image/webp', 1194988, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 16:32:28.638108+08', '2026-08-11 16:32:28.638108+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('457fdd0f-2a53-4acf-a102-378d56b25f9b', 'footstep/2026/08/40ef0e9b860541f089fe2f9035e60e44.webp', 'ysy-myblog', 'IMG_20240916_145226.webp', 'image/webp', 378398, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 16:32:31.765689+08', '2026-08-11 16:32:31.765689+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('bdd1af10-1578-45dd-bb8f-3fe91f141f29', 'footstep/2026/08/2c5f80d703374311b63f705c978d2a47.webp', 'ysy-myblog', 'IMG_20240904_194636.webp', 'image/webp', 705836, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 16:32:34.192264+08', '2026-08-11 16:32:34.192264+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('d3910fb4-a1ab-4153-931d-87d84893d62c', 'footstep/2026/08/f5ebd8da74104c84b445df0e18739799.webp', 'ysy-myblog', 'IMG_20240328_140339.webp', 'image/webp', 1257950, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 16:32:37.129722+08', '2026-08-11 16:32:37.129722+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('76e34059-adf8-4280-aa9c-545b6b9d27b8', 'footstep/2026/08/5c47a7ed7d074b1d9f189576b639cce6.webp', 'ysy-myblog', 'IMG_20210101_170755.webp', 'image/webp', 894238, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 18:02:42.58006+08', '2026-08-11 18:02:42.58006+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('ceed1b03-ddf3-4ac7-97ec-01e6f6de42a8', 'footstep/2026/08/39e18ea49cd046e2aaf9ab87c69d513e.webp', 'ysy-myblog', 'IMG_20211004_112701.webp', 'image/webp', 2205048, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 18:02:45.763191+08', '2026-08-11 18:02:45.763191+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('207dec49-3a28-4af1-8a29-b8196a7069d0', 'footstep/2026/08/37b46dffc50e4b79aa41d36cfdfa23bc.webp', 'ysy-myblog', 'IMG_20230731_154213.webp', 'image/webp', 420066, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 18:02:53.945346+08', '2026-08-11 18:02:53.945346+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('efefaae1-db49-473b-a22e-7ff05ecfbb62', 'footstep/2026/08/835cee412fe444e686e16e8e101d9d47.webp', 'ysy-myblog', 'IMG_20240105_165019.webp', 'image/webp', 1557604, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 18:02:56.676392+08', '2026-08-11 18:02:56.676392+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('70e9bfbf-f11e-4c22-8994-42605e806621', 'footstep/2026/08/0fe9675d16bf40e49f79da07118fc403.webp', 'ysy-myblog', 'IMG_20240405_165340.webp', 'image/webp', 734434, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 18:02:59.082686+08', '2026-08-11 18:02:59.082686+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('0afa7af5-725f-4e61-b0c8-04d37ab148b9', 'footstep/2026/08/8a8100a61a544bbda94250e0e3b735c2.jpg', 'ysy-myblog', 'IMG_20240407_161125_1786439901351.jpg', 'image/jpeg', 595825, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 18:03:02.373669+08', '2026-08-11 18:03:02.373669+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('8f48cfe1-978d-4636-b693-cdfd9d5aaf56', 'footstep/2026/08/ca257556249044d3a9bfc058e6723349.webp', 'ysy-myblog', 'IMG_20240407_163704.webp', 'image/webp', 1095640, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 18:03:08.715023+08', '2026-08-11 18:03:08.715023+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('8dd096de-3db8-4941-ad75-eabee4e03633', 'footstep/2026/08/1a53602b32d244e5acc70822b0ec0c10.webp', 'ysy-myblog', 'IMG_20240407_173713.webp', 'image/webp', 1007330, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 18:03:11.768882+08', '2026-08-11 18:03:11.768882+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('b0a6a34e-45d5-4b5b-be36-ab9084fb83df', 'footstep/2026/08/3e454d4fbf534a72975fb13aa28b988e.webp', 'ysy-myblog', 'IMG_20240408_034110.webp', 'image/webp', 1380520, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 18:03:14.433946+08', '2026-08-11 18:03:14.433946+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('00591d27-ab26-4295-9e6f-3ce988b5a38d', 'footstep/2026/08/34d886746e7146ab85ffd3c233e90581.webp', 'ysy-myblog', 'IMG_20250117_143106.webp', 'image/webp', 733976, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 19:20:26.316108+08', '2026-08-11 19:20:26.316108+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('d832949a-bf1f-4803-87f8-4410c3ecbf4e', 'footstep/2026/08/5475a19ca5af47b9b6b6c5237b03d191.webp', 'ysy-myblog', 'IMG_20250117_211553.webp', 'image/webp', 889016, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 19:20:28.859122+08', '2026-08-11 19:20:28.859122+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('73d5f633-1732-40ac-b5f0-797cdc308a05', 'footstep/2026/08/e11187bc24404699b3d7f033ce657254.webp', 'ysy-myblog', 'IMG_20250118_160947.webp', 'image/webp', 1231444, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 19:20:32.212568+08', '2026-08-11 19:20:32.212568+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('06c1b717-05c4-4882-8601-7c35f6bed6b7', 'footstep/2026/08/b09050eeeec54dfd8ccf01705c2b5646.webp', 'ysy-myblog', 'IMG_20250118_164207.webp', 'image/webp', 784522, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 19:20:34.958518+08', '2026-08-11 19:20:34.958518+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('2780f138-274a-43b2-97c5-939b5b01b8bc', 'footstep/2026/08/75a7383abe01432992a74fb978e94a85.webp', 'ysy-myblog', 'IMG_20250118_170117.webp', 'image/webp', 568638, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 19:20:38.225553+08', '2026-08-11 19:20:38.225553+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('cfdefefd-f50a-4c44-8195-0e59dedbc3be', 'footstep/2026/08/f7a2784a364a40e1ba938a58fb65b20e.webp', 'ysy-myblog', 'IMG_20260811_185631.webp', 'image/webp', 855186, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 19:20:40.671934+08', '2026-08-11 19:20:40.671934+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('ce487f10-8f9f-45be-bc23-1860f040854d', 'footstep/2026/08/ab79dcbec3d442139643f13a35558dd3.webp', 'ysy-myblog', 'IMG_20250301_180722.webp', 'image/webp', 393064, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 19:58:43.864581+08', '2026-08-11 19:58:43.864581+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('9643f1ff-cc52-4bbf-b9c6-aa657cd1078e', 'footstep/2026/08/af3f897f8c0b44c59152dc75ee71830b.webp', 'ysy-myblog', 'IMG_20250329_204626.webp', 'image/webp', 602716, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 19:58:47.115923+08', '2026-08-11 19:58:47.115923+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('f59535ef-234e-444e-a799-7af526962eba', 'footstep/2026/08/7703e8c70bc544e7831335d74e06be1f.webp', 'ysy-myblog', 'IMG_20250329_204827.webp', 'image/webp', 602570, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 19:58:49.814686+08', '2026-08-11 19:58:49.814686+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('cc48b9e0-f3d3-4980-b386-bfce181fd793', 'footstep/2026/08/ed220ebca6c64c9d86462cae6009625f.webp', 'ysy-myblog', 'IMG_20250330_151549.webp', 'image/webp', 857848, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 19:58:52.367548+08', '2026-08-11 19:58:52.367548+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('7c1fd1d0-3eac-478a-8713-6c65a85efdac', 'footstep/2026/08/6eb7dcc807c24c9fb765a9cbd93e058f.webp', 'ysy-myblog', 'IMG_20250330_160514.webp', 'image/webp', 917746, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 19:58:54.96726+08', '2026-08-11 19:58:54.96726+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('c64a2eee-a122-422d-a58a-25f629d0b80d', 'footstep/2026/08/25c6bb333b9f4a918786fa1cd143c6e6.webp', 'ysy-myblog', 'IMG_20260811_195946.webp', 'image/webp', 1885802, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 20:46:27.334013+08', '2026-08-11 20:46:27.334013+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('889405d9-e6b5-47ae-a4d7-f978b390d7a9', 'footstep/2026/08/54ea648367ea460d82c8de9e3a369cba.webp', 'ysy-myblog', 'IMG_20260811_194159.webp', 'image/webp', 817860, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 20:46:30.283628+08', '2026-08-11 20:46:30.283628+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('4b57bb78-37ba-49cf-82b3-04dfcd5a794b', 'footstep/2026/08/d3658d71ef0f490db5d8f652225cd488.webp', 'ysy-myblog', 'IMG_20260811_193947.webp', 'image/webp', 1500006, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 20:46:33.696704+08', '2026-08-11 20:46:33.696704+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('4e10c48b-8447-4a2c-9db2-91f088df037b', 'footstep/2026/08/5f707d0e0f204eadb2e1487e105ed727.webp', 'ysy-myblog', 'IMG_20260811_193913.webp', 'image/webp', 1166804, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 20:46:36.250447+08', '2026-08-11 20:46:36.250447+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('27815e56-3d57-4d20-86e3-78279115ba87', 'footstep/2026/08/8f3521e069c64d4f99a50d59b13572b8.webp', 'ysy-myblog', 'IMG_20260811_193729.webp', 'image/webp', 804304, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 20:46:39.209775+08', '2026-08-11 20:46:39.209775+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('47d6a325-b0c6-4ab3-b10f-bb87716acdbe', 'footstep/2026/08/5640d0e1cabe4ccbbad27c86a0dcb5c8.webp', 'ysy-myblog', 'IMG_20260811_193502.webp', 'image/webp', 1025808, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 20:46:42.532545+08', '2026-08-11 20:46:42.532545+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('2e6ee498-f690-4cff-9a1c-983417013ce4', 'footstep/2026/08/55247788526a4595811ccd8c9889fb48.webp', 'ysy-myblog', 'IMG_20260811_193140.webp', 'image/webp', 988376, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 20:46:45.212397+08', '2026-08-11 20:46:45.212397+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('82915b3f-e053-4679-b57f-aa0c87f461fa', 'footstep/2026/08/357ec1b34caa4e318ecba750ca2ace25.webp', 'ysy-myblog', 'IMG_20260811_193123.webp', 'image/webp', 814458, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 20:46:47.748723+08', '2026-08-11 20:46:47.748723+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('46accbe7-3d42-41c2-b81a-210776fd7b01', 'footstep/2026/08/e7b11b5321044ac9bb285aa8bc1e2d25.webp', 'ysy-myblog', 'IMG_20181002_095448.webp', 'image/webp', 959160, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 20:58:43.040972+08', '2026-08-11 20:58:43.040972+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('c34714fd-c984-4fb4-b301-305c4dffb8b2', 'footstep/2026/08/254ae42b86ea4069ba3a28d636563509.webp', 'ysy-myblog', 'IMG_20181002_102942.webp', 'image/webp', 522000, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 20:58:45.724145+08', '2026-08-11 20:58:45.724145+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('b1b8ffa1-1b70-41f5-9bc6-26c7f2a68090', 'footstep/2026/08/3800f6ebc57244fa87deed6a9cf516e1.webp', 'ysy-myblog', 'IMG_20181002_103227.webp', 'image/webp', 1158404, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 20:58:48.165029+08', '2026-08-11 20:58:48.165029+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('2e0e9185-e407-4412-9601-820210a49f5c', 'footstep/2026/08/7113360b1aab4bcd81d449dcb2c623c9.webp', 'ysy-myblog', 'IMG_20260811_200812.webp', 'image/webp', 253720, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 20:58:50.924877+08', '2026-08-11 20:58:50.924877+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('842d16f0-0ada-4bc0-b63c-2b92f3545f6f', 'footstep/2026/08/764cddf4509b473780ca43658157b625.webp', 'ysy-myblog', 'IMG_20260811_201957.webp', 'image/webp', 703946, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-11 20:58:53.666547+08', '2026-08-11 20:58:53.666547+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('61078cfe-24dc-4904-9c87-912d774bca62', 'hobbies/2026/08/c23b7427342a40e5954fbf2778d66bff.jpg', 'ysy-myblog', '1.jpeg', 'image/jpeg', 214421, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-12 21:54:12.22205+08', '2026-08-12 21:54:12.22205+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('73ddaea2-c208-420a-a5a7-faca1d9e64c1', 'hobbies/2026/08/771e613d1a0d4d09b77a41dc403cf41b.jpg', 'ysy-myblog', 'hobby-crop-1786943520644.jpg', 'image/jpeg', 257059, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-17 13:12:00.951951+08', '2026-08-17 13:12:00.951951+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('75af93fa-1b78-43da-8e11-e1e4d39f2603', 'hobbies/2026/08/b45984f5991d47aca4c8ed9bc414046b.jpg', 'ysy-myblog', 'hobby-crop-1786943701821.jpg', 'image/jpeg', 256689, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-17 13:15:02.057458+08', '2026-08-17 13:15:02.057458+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('232ea6d4-b90b-48b0-8a6f-d1e7f1085933', 'hobbies/2026/08/89b58f72dae04874a141654b0ad94e37.jpg', 'ysy-myblog', 'hobby-crop-1786943726022.jpg', 'image/jpeg', 236050, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-17 13:15:26.1017+08', '2026-08-17 13:15:26.1017+08', NULL);
INSERT INTO public.resources (id, object_key, bucket, original_name, mime_type, size, uploaded_by, created_at, updated_at, deleted_at) VALUES ('e94c3f43-ff43-4590-85a6-8e5b9a76ccca', 'hobbies/2026/08/32120b0437464bce862450f3a0c09e1a.jpg', 'ysy-myblog', 'hobby-crop-1786943743254.jpg', 'image/jpeg', 515076, 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', '2026-08-17 13:15:43.381232+08', '2026-08-17 13:15:43.381232+08', NULL);

ALTER TABLE ONLY public.resources
    ADD CONSTRAINT resources_uploaded_by_fkey FOREIGN KEY (uploaded_by) REFERENCES public.users(id) ON DELETE SET NULL;

-- ============================================================================
-- 03. content_releases（内容发布版本）
-- ============================================================================

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
    CONSTRAINT content_releases_module_key_check CHECK (((module_key)::text = ANY (ARRAY[('home'::character varying)::text, ('about'::character varying)::text, ('footprints'::character varying)::text, ('hobbies'::character varying)::text, ('mylab'::character varying)::text, ('skills'::character varying)::text, ('vibe'::character varying)::text]))),
    CONSTRAINT content_releases_state_check CHECK (((state)::text = ANY (ARRAY[('DRAFT'::character varying)::text, ('PUBLISHED'::character varying)::text, ('ARCHIVED'::character varying)::text, ('OFFLINE'::character varying)::text]))),
    CONSTRAINT content_releases_version_no_check CHECK ((version_no > 0))
);

ALTER TABLE ONLY public.content_releases
    ADD CONSTRAINT content_releases_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.content_releases
    ADD CONSTRAINT uq_content_release_version UNIQUE (module_key, version_no);

CREATE INDEX idx_content_release_history ON public.content_releases USING btree (module_key, version_no DESC) WHERE (deleted_at IS NULL);

CREATE INDEX idx_content_releases_published_by ON public.content_releases USING btree (published_by);

CREATE INDEX idx_content_releases_source ON public.content_releases USING btree (source_release_id);

CREATE UNIQUE INDEX uq_content_release_current ON public.content_releases USING btree (module_key) WHERE (((state)::text = ANY (ARRAY[('PUBLISHED'::character varying)::text, ('OFFLINE'::character varying)::text])) AND (deleted_at IS NULL));

CREATE UNIQUE INDEX uq_content_release_draft ON public.content_releases USING btree (module_key) WHERE (((state)::text = 'DRAFT'::text) AND (deleted_at IS NULL));

-- content_releases 初始数据
INSERT INTO public.content_releases (id, module_key, version_no, state, published_by, source_release_id, published_at, created_at, updated_at, deleted_at) VALUES ('6e5ce8e2-4351-4763-9ddb-8b5dbe528226', 'home', 6, 'DRAFT', NULL, '897c1c16-c070-461a-8d93-109d24c17979', NULL, '2026-08-27 00:01:08.753891+08', '2026-08-27 00:01:08.753891+08', NULL);
INSERT INTO public.content_releases (id, module_key, version_no, state, published_by, source_release_id, published_at, created_at, updated_at, deleted_at) VALUES ('897c1c16-c070-461a-8d93-109d24c17979', 'home', 5, 'PUBLISHED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', NULL, '2026-08-13 22:11:04.847659+08', '2026-08-13 22:11:01.022274+08', '2026-08-27 00:38:49.777728+08', NULL);
INSERT INTO public.content_releases (id, module_key, version_no, state, published_by, source_release_id, published_at, created_at, updated_at, deleted_at) VALUES ('f0ae2633-cb04-4d74-ac81-6991c3771b1c', 'about', 2, 'PUBLISHED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', NULL, '2026-08-13 13:39:00.95023+08', '2026-08-13 13:38:59.872204+08', '2026-08-27 00:38:49.777728+08', NULL);
INSERT INTO public.content_releases (id, module_key, version_no, state, published_by, source_release_id, published_at, created_at, updated_at, deleted_at) VALUES ('f6d1b717-651e-48d8-a0d3-8ceeb1666d36', 'skills', 3, 'PUBLISHED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', NULL, '2026-08-11 14:45:20.210847+08', '2026-08-11 14:45:20.163611+08', '2026-08-27 00:38:49.777728+08', NULL);
INSERT INTO public.content_releases (id, module_key, version_no, state, published_by, source_release_id, published_at, created_at, updated_at, deleted_at) VALUES ('de31d1f4-8021-4f6a-94d3-389121aff190', 'footprints', 6, 'PUBLISHED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', NULL, '2026-08-11 22:24:53.222218+08', '2026-08-11 22:24:53.164049+08', '2026-08-27 00:38:49.777728+08', NULL);
INSERT INTO public.content_releases (id, module_key, version_no, state, published_by, source_release_id, published_at, created_at, updated_at, deleted_at) VALUES ('a2f87024-1aaf-4687-afa1-325a1c7b9f57', 'hobbies', 18, 'PUBLISHED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', NULL, '2026-08-26 23:52:43.942288+08', '2026-08-26 23:52:27.992993+08', '2026-08-27 00:38:49.777728+08', NULL);
INSERT INTO public.content_releases (id, module_key, version_no, state, published_by, source_release_id, published_at, created_at, updated_at, deleted_at) VALUES ('e5d78b76-992b-4ed5-9b8c-9d187a628573', 'vibe', 3, 'PUBLISHED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', NULL, '2026-08-13 13:32:14.262812+08', '2026-08-13 13:32:14.222716+08', '2026-08-27 00:38:49.777728+08', NULL);
INSERT INTO public.content_releases (id, module_key, version_no, state, published_by, source_release_id, published_at, created_at, updated_at, deleted_at) VALUES ('b061ab16-f7ee-4806-a4af-7628124f5082', 'mylab', 4, 'PUBLISHED', 'b7b1a013-fc83-579a-a1e3-bb1cc0483bac', NULL, '2026-08-26 23:54:13.001026+08', '2026-08-26 23:53:00.989758+08', '2026-08-27 00:38:49.777728+08', NULL);

ALTER TABLE ONLY public.content_releases
    ADD CONSTRAINT content_releases_published_by_fkey FOREIGN KEY (published_by) REFERENCES public.users(id) ON DELETE SET NULL;

ALTER TABLE ONLY public.content_releases
    ADD CONSTRAINT content_releases_source_release_id_fkey FOREIGN KEY (source_release_id) REFERENCES public.content_releases(id) ON DELETE RESTRICT;

-- ============================================================================
-- 04. mylab_tags（实验室标签）
-- ============================================================================

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

ALTER TABLE ONLY public.mylab_tags
    ADD CONSTRAINT mylab_tags_pkey PRIMARY KEY (id);

CREATE INDEX idx_mylab_tags_order ON public.mylab_tags USING btree (sort_order, tag_key) WHERE (deleted_at IS NULL);

CREATE UNIQUE INDEX uq_mylab_tags_key ON public.mylab_tags USING btree (tag_key) WHERE (deleted_at IS NULL);

CREATE UNIQUE INDEX uq_mylab_tags_name ON public.mylab_tags USING btree (name) WHERE (deleted_at IS NULL);

-- mylab_tags 初始数据
INSERT INTO public.mylab_tags (id, tag_key, name, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('100b7029-067a-59db-8c2b-f9c524fb520c', 'csharp', 'C#', true, 2, '2026-08-10 00:45:04.442137+08', '2026-08-10 00:45:04.442137+08', NULL);
INSERT INTO public.mylab_tags (id, tag_key, name, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('2d9db0ad-a7fb-5bcf-99ff-040ba892267c', 'godot', 'Godot', true, 4, '2026-08-10 00:45:04.442137+08', '2026-08-10 00:45:04.442137+08', NULL);
INSERT INTO public.mylab_tags (id, tag_key, name, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('faba2f26-a83f-575a-9c0e-590251dc6909', 'javascript', 'JavaScript', true, 7, '2026-08-10 00:45:04.442137+08', '2026-08-10 00:45:04.442137+08', NULL);
INSERT INTO public.mylab_tags (id, tag_key, name, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('cd3776a7-8d5f-5c3f-b7d2-b81fc8974d7a', 'gamejam', 'GameJam', true, 0, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:41:18.390638+08', NULL);
INSERT INTO public.mylab_tags (id, tag_key, name, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('9b7ab32a-5c1f-566b-a8dc-48bdf9a6fc16', 'unity', 'Unity', true, 1, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:41:18.402332+08', NULL);
INSERT INTO public.mylab_tags (id, tag_key, name, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('f153754c-e92a-5d82-9642-250ef1972aa8', 'aseprite', 'Aseprite', true, 3, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:41:18.413451+08', NULL);
INSERT INTO public.mylab_tags (id, tag_key, name, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('0fc6f305-8fae-53fd-ac1a-85d060a4f2bf', 'gdscript', 'GDScript', true, 5, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:41:18.424097+08', NULL);
INSERT INTO public.mylab_tags (id, tag_key, name, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('6310e463-4ad4-59f3-8cf3-e735e3fd3691', 'phaser', 'Phaser', true, 6, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:41:18.434157+08', NULL);
INSERT INTO public.mylab_tags (id, tag_key, name, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('16649271-892d-57bf-8555-a43662e53956', 'unreal-engine', 'Unreal Engine', true, 8, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:41:18.444738+08', NULL);
INSERT INTO public.mylab_tags (id, tag_key, name, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('85dd4c28-cbe0-52b4-88bf-82cd376f20f3', 'cpp', 'C++', true, 9, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:41:18.458571+08', NULL);
INSERT INTO public.mylab_tags (id, tag_key, name, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('902ac023-ea59-51e6-b308-75aae2fefa14', 'lua', 'Lua', true, 10, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:41:18.469409+08', NULL);
INSERT INTO public.mylab_tags (id, tag_key, name, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('40685b33-1982-589e-8c09-2df8b75b5655', 'react', 'React', true, 11, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:42:13.646007+08', NULL);
INSERT INTO public.mylab_tags (id, tag_key, name, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('11894919-dbfa-5d3d-8a2f-3bfa1580c4b6', 'typescript', 'TypeScript', true, 12, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:42:13.659233+08', NULL);
INSERT INTO public.mylab_tags (id, tag_key, name, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('1bba22a4-d4c5-5f5e-8dcf-bce71d13388a', 'supabase', 'Supabase', true, 13, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:42:13.670395+08', NULL);
INSERT INTO public.mylab_tags (id, tag_key, name, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('e64d08a8-eedf-5d6c-be92-91a51a9c294f', 'vue', 'Vue', true, 14, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:42:13.680025+08', NULL);
INSERT INTO public.mylab_tags (id, tag_key, name, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('c144f9fa-8ffb-5225-a51c-2b68aaeb276c', 'web-audio-api', 'Web Audio API', true, 15, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:42:13.689248+08', NULL);
INSERT INTO public.mylab_tags (id, tag_key, name, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('b08090e1-19eb-598d-bbee-1ff39bcf31a0', 'tone-js', 'Tone.js', true, 16, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:42:13.699414+08', NULL);
INSERT INTO public.mylab_tags (id, tag_key, name, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('b820842b-8c3e-5dba-a955-393633ab888e', 'docker', 'Docker', true, 17, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:42:13.709934+08', NULL);
INSERT INTO public.mylab_tags (id, tag_key, name, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('85538585-342a-58bb-b4d5-37fe10d2f05e', 'nginx', 'Nginx', true, 18, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:42:13.720226+08', NULL);
INSERT INTO public.mylab_tags (id, tag_key, name, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('61dc6897-31dd-5b1f-bc22-b32e1ad4c51c', 'ops', '运维', true, 19, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:42:13.730937+08', NULL);
INSERT INTO public.mylab_tags (id, tag_key, name, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('bed13764-9b69-54c0-b976-a9a0f6a594ae', 'gsap', 'GSAP', true, 20, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:42:13.742979+08', NULL);
INSERT INTO public.mylab_tags (id, tag_key, name, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('be50c0a5-d841-5287-80cf-b3272fa0277b', 'frontend', '前端', true, 21, '2026-08-10 00:45:04.442137+08', '2026-08-12 19:42:13.753979+08', NULL);
INSERT INTO public.mylab_tags (id, tag_key, name, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('d4f70c09-16a3-54a1-b715-dc608b90859e', 'tailwind', 'Tailwind', true, 22, '2026-08-10 00:45:04.442137+08', '2026-08-26 23:47:55.98258+08', NULL);
INSERT INTO public.mylab_tags (id, tag_key, name, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('4ac8673b-86ef-5af1-a70a-e446cc2cf799', 'engineering', '工程化', true, 23, '2026-08-10 00:45:04.442137+08', '2026-08-26 23:47:55.995296+08', NULL);
INSERT INTO public.mylab_tags (id, tag_key, name, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('f82960eb-2fb1-57c2-86d2-ce5b7a1650e0', 'raspberry-pi', '树莓派', true, 24, '2026-08-10 00:45:04.442137+08', '2026-08-26 23:47:56.004761+08', NULL);
INSERT INTO public.mylab_tags (id, tag_key, name, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('a0594a0e-8118-5315-9180-7a8f390342da', 'hardware', '硬件', true, 25, '2026-08-10 00:45:04.442137+08', '2026-08-26 23:47:56.014133+08', NULL);
INSERT INTO public.mylab_tags (id, tag_key, name, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('f064a292-232f-5c61-8ca2-8389e50bbf1b', 'dp', 'DP', true, 26, '2026-08-10 00:45:04.442137+08', '2026-08-26 23:47:56.023691+08', NULL);
INSERT INTO public.mylab_tags (id, tag_key, name, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('ce23d581-aa67-5c91-a808-0fba88586935', 'essay', '随笔', true, 27, '2026-08-10 00:45:04.442137+08', '2026-08-26 23:47:56.031694+08', NULL);

-- ============================================================================
-- 05. about_contents（关于页内容）
-- ============================================================================

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

ALTER TABLE ONLY public.about_contents
    ADD CONSTRAINT about_contents_pkey PRIMARY KEY (id);

CREATE INDEX idx_about_contents_avatar ON public.about_contents USING btree (avatar_resource_id);

CREATE UNIQUE INDEX uq_about_contents_release ON public.about_contents USING btree (release_id) WHERE (deleted_at IS NULL);

-- about_contents 初始数据
INSERT INTO public.about_contents (id, release_id, profile_title, avatar_resource_id, avatar_alt, intro, outro, ingredients_title, ingredients_description, created_at, updated_at, deleted_at) VALUES ('a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', 'f0ae2633-cb04-4d74-ac81-6991c3771b1c', '关于我', '4889ba92-d366-55d2-82a4-a94833da1b8c', 'DNSamuel', '你好，我是 SHENNN，目前专注于全栈开发、AI agent学习实践中...', '努力成长，希望成为一名AI超级个人，通过AI让生活变得更美好。', '我的成分', '之前有人想查我的成分，我认真的思考了一下，我的成分应该是这样，不过随时有可能会变就是啦', '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);

ALTER TABLE ONLY public.about_contents
    ADD CONSTRAINT about_contents_avatar_resource_id_fkey FOREIGN KEY (avatar_resource_id) REFERENCES public.resources(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.about_contents
    ADD CONSTRAINT about_contents_release_id_fkey FOREIGN KEY (release_id) REFERENCES public.content_releases(id) ON DELETE CASCADE;

-- ============================================================================
-- 06. about_bubbles（关于页气泡）
-- ============================================================================

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
    CONSTRAINT about_bubbles_bubble_size_check CHECK (((bubble_size)::text = ANY (ARRAY[('big'::character varying)::text, ('mid'::character varying)::text]))),
    CONSTRAINT about_bubbles_sort_order_check CHECK ((sort_order >= 0))
);

ALTER TABLE ONLY public.about_bubbles
    ADD CONSTRAINT about_bubbles_pkey PRIMARY KEY (id);

CREATE UNIQUE INDEX uq_about_bubbles_order ON public.about_bubbles USING btree (about_content_id, sort_order) WHERE (deleted_at IS NULL);

-- about_bubbles 初始数据
INSERT INTO public.about_bubbles (id, about_content_id, bubble_text, bubble_size, background_color, text_color, glow_color, sort_order, created_at, updated_at, deleted_at) VALUES ('0b69fd5d-2723-4fb6-98e7-ee8765ad0358', 'a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', 'FPS牢玩家', 'big', '#FF6B6B', '#FF8A80', '#FF6B6B', 0, '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);
INSERT INTO public.about_bubbles (id, about_content_id, bubble_text, bubble_size, background_color, text_color, glow_color, sort_order, created_at, updated_at, deleted_at) VALUES ('42179600-ea3d-4d52-bac0-84c32264539c', 'a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', '健身旅行者', 'big', '#2EC4B6', '#64FFDA', '#2EC4B6', 1, '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);
INSERT INTO public.about_bubbles (id, about_content_id, bubble_text, bubble_size, background_color, text_color, glow_color, sort_order, created_at, updated_at, deleted_at) VALUES ('c6af27ca-79a1-462c-95cd-fa6d1cf0e5ff', 'a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', '动物保护旅行者', 'big', '#66BB6A', '#81C784', '#66BB6A', 2, '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);
INSERT INTO public.about_bubbles (id, about_content_id, bubble_text, bubble_size, background_color, text_color, glow_color, sort_order, created_at, updated_at, deleted_at) VALUES ('d0d23c10-648d-42d3-ba2c-56740c0f23b2', 'a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', '养老二次元', 'big', '#DB7093', '#F48FB1', '#DB7093', 3, '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);
INSERT INTO public.about_bubbles (id, about_content_id, bubble_text, bubble_size, background_color, text_color, glow_color, sort_order, created_at, updated_at, deleted_at) VALUES ('64a6403b-3f5d-4961-a808-8eb9242413b9', 'a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', '游戏旅行者', 'big', '#FF8A65', '#FFAB91', '#FF8A65', 4, '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);
INSERT INTO public.about_bubbles (id, about_content_id, bubble_text, bubble_size, background_color, text_color, glow_color, sort_order, created_at, updated_at, deleted_at) VALUES ('a9b11c3f-5263-4ff4-95f7-7ef335fdb3f2', 'a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', '美食探索旅行者', 'mid', '#FF8A65', '#FFCCBC', '#FF8A65', 5, '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);
INSERT INTO public.about_bubbles (id, about_content_id, bubble_text, bubble_size, background_color, text_color, glow_color, sort_order, created_at, updated_at, deleted_at) VALUES ('2ffb26a3-a7e9-4150-ae5a-f5a6880f002d', 'a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', '自然风光旅行者', 'mid', '#4CAF50', '#A5D6A7', '#4CAF50', 6, '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);
INSERT INTO public.about_bubbles (id, about_content_id, bubble_text, bubble_size, background_color, text_color, glow_color, sort_order, created_at, updated_at, deleted_at) VALUES ('bdec58cb-8e0e-405d-8c13-ec0227e71cd3', 'a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', '技术探索者', 'mid', '#5BA4E6', '#81D4FA', '#5BA4E6', 7, '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);
INSERT INTO public.about_bubbles (id, about_content_id, bubble_text, bubble_size, background_color, text_color, glow_color, sort_order, created_at, updated_at, deleted_at) VALUES ('4861a186-13a4-4d99-88c9-dfae9b8c9aba', 'a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', '摄影旅行者', 'mid', '#FFB347', '#FFE082', '#FFB347', 8, '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);
INSERT INTO public.about_bubbles (id, about_content_id, bubble_text, bubble_size, background_color, text_color, glow_color, sort_order, created_at, updated_at, deleted_at) VALUES ('7dd9f587-dacb-4f80-a039-11a178dab81b', 'a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', 'city walk', 'mid', '#64B5F6', '#90CAF9', '#64B5F6', 9, '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);
INSERT INTO public.about_bubbles (id, about_content_id, bubble_text, bubble_size, background_color, text_color, glow_color, sort_order, created_at, updated_at, deleted_at) VALUES ('1546737e-c80b-4c18-a234-533cfff26b6b', 'a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', '电驴爱好者', 'mid', '#66BB6A', '#A5D6A7', '#66BB6A', 10, '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);
INSERT INTO public.about_bubbles (id, about_content_id, bubble_text, bubble_size, background_color, text_color, glow_color, sort_order, created_at, updated_at, deleted_at) VALUES ('f4a6b433-cc66-4108-9115-956d1340b7ca', 'a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', '吃瓜旅行者', 'mid', '#AB47BC', '#CE93D8', '#AB47BC', 11, '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);
INSERT INTO public.about_bubbles (id, about_content_id, bubble_text, bubble_size, background_color, text_color, glow_color, sort_order, created_at, updated_at, deleted_at) VALUES ('dd83d730-ed33-417c-bbdf-ad338cae8f5f', 'a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', 'AI大人的爱徒', 'mid', '#00BCD4', '#4DD0E1', '#00BCD4', 12, '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);

ALTER TABLE ONLY public.about_bubbles
    ADD CONSTRAINT about_bubbles_about_content_id_fkey FOREIGN KEY (about_content_id) REFERENCES public.about_contents(id) ON DELETE CASCADE;

-- ============================================================================
-- 07. about_profile_bullets（关于页简介条目）
-- ============================================================================

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

ALTER TABLE ONLY public.about_profile_bullets
    ADD CONSTRAINT about_profile_bullets_pkey PRIMARY KEY (id);

CREATE UNIQUE INDEX uq_about_profile_bullets_order ON public.about_profile_bullets USING btree (about_content_id, sort_order) WHERE (deleted_at IS NULL);

-- about_profile_bullets 初始数据
INSERT INTO public.about_profile_bullets (id, about_content_id, contents, sort_order, created_at, updated_at, deleted_at) VALUES ('c4726872-aff1-4f93-ade7-ac1e2cf62ecd', 'a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', '上位机开发：C#/.NET，负责为实验室内若干智能装备进行上位机软件开发与维护', 0, '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);
INSERT INTO public.about_profile_bullets (id, about_content_id, contents, sort_order, created_at, updated_at, deleted_at) VALUES ('2764bd20-37e2-4242-94cd-8bd5bba56d75', 'a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', 'web开发：Java/SpringBoot服务端，TypeScript/React前端，做些个人兴趣项目', 1, '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);
INSERT INTO public.about_profile_bullets (id, about_content_id, contents, sort_order, created_at, updated_at, deleted_at) VALUES ('16adf43a-465b-405e-8087-4887047a6f41', 'a94f8ccb-cb6c-4c6a-9aa0-936f20dec01a', '爱好自然观光、city walk，喜欢探索这个世界的美', 2, '2026-08-13 13:39:00.914173+08', '2026-08-13 13:39:00.914173+08', NULL);

ALTER TABLE ONLY public.about_profile_bullets
    ADD CONSTRAINT about_profile_bullets_about_content_id_fkey FOREIGN KEY (about_content_id) REFERENCES public.about_contents(id) ON DELETE CASCADE;

-- ============================================================================
-- 08. footprints（足迹）
-- ============================================================================

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

ALTER TABLE ONLY public.footprints
    ADD CONSTRAINT footprints_pkey PRIMARY KEY (id);

CREATE INDEX idx_footprints_release_order ON public.footprints USING btree (release_id, sort_order, city_key) WHERE (deleted_at IS NULL);

CREATE UNIQUE INDEX uq_footprints_release_key ON public.footprints USING btree (release_id, city_key) WHERE (deleted_at IS NULL);

-- footprints 初始数据
INSERT INTO public.footprints (id, release_id, city_key, title, summary, contents, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('2d23ba6a-2c82-4351-826f-fdc4095aae3d', 'de31d1f4-8021-4f6a-94d3-389121aff190', 'hike', '昆明', '昆明，我的家，也是我存放了无数回忆的地方。', '在海埂公园吹过无数次滇池的风，爬过不知道多少次西山，吃过了各式各样刚出炉的鲜花饼，留下了无数的美好回忆。

风和日丽，四季如春，鲜花从不缺席，春天从未走远，这是被花草与暖阳偏爱的地方，也是我在任何其他城市都寻不到的奢侈。', true, 0, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprints (id, release_id, city_key, title, summary, contents, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('aa6679ac-77d5-488e-99e0-feedc8829cf8', 'de31d1f4-8021-4f6a-94d3-389121aff190', 'travel', '广州', '广州，大城市中生活气息最浓厚的城市。', '行走在广州老城里，骑楼街、麻石巷、满洲窗，聆听着街坊聊天的声音，闻着小店里炒菜的香气，这是广州随处可见的风景。

有时我愿意不急着去景点，只为陌生城市的街区里游荡几个小时。

这里既有小城的闲适，又有现代大都市的效率，传统商脉与现代商业在此共生，织就独特的广府气质。', true, 1, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprints (id, release_id, city_key, title, summary, contents, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('22efc57b-263c-44b7-a058-b936bf3dd7ba', 'de31d1f4-8021-4f6a-94d3-389121aff190', 'music', '深圳', '深圳，一座被山海拥抱着奔跑的城市。', '从广州过来，新，就是我对深圳的第一印象，这里没有古老的城墙，只有各式各样的都市高楼。

深圳湾看海，梧桐山爬山，山海环绕，以及大大小小的公园，是深圳特有的城市景观。

改革开放的前沿，一座平均年龄极年轻的城市。山海连城，高楼与自然共生。没有历史包袱，只有创新基因。', true, 2, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprints (id, release_id, city_key, title, summary, contents, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('eddbff5b-224d-4228-97ab-5e0376a641a1', 'de31d1f4-8021-4f6a-94d3-389121aff190', 'photo', '西安', '西安，一座让千年古韵与现代繁华温柔相拥的城市。', '西安的老城区以钟楼为中心，涵盖城墙周边的大片区域。这里历史沉淀深厚，市井气息浓郁，我喜欢穿梭在大街小巷之间感受平民生活的温度，在浏览园林景致中感受历史古韵。

而城市的另一面，则以行政中心为原点向周围铺展。高楼鳞次栉比，商场连绵不绝，与城墙内的古朴沉静遥相对望，构成一幅时空交错的独特画卷。', true, 3, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprints (id, release_id, city_key, title, summary, contents, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('282dfaf7-7594-4ced-b454-07cd9a2229cd', 'de31d1f4-8021-4f6a-94d3-389121aff190', 'coffee', '上海', '上海，一座在黄浦江两岸折叠时空的城市。', '外滩的百年建筑与陆家嘴的摩天楼群隔江相望，梧桐老洋房与石库门弄堂共存于同一片街区。

这里既有金融中心的快节奏，也有街角咖啡馆的慢时光。

中西合璧的海派文化，让传统与现代并肩而立，织就一幅独特的城市天际线。', true, 4, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprints (id, release_id, city_key, title, summary, contents, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('17257d1e-6459-493e-805f-a314d0df298f', 'de31d1f4-8021-4f6a-94d3-389121aff190', 'read', '北京', '北京，千年古都，国家心脏。', '红墙黄瓦与摩天楼群共存，胡同烟火与CBD繁华交织。

历史厚度与现代速度在此碰撞，政治文化中心与国际化都市并行。

四季分明，底蕴深厚，是梦想与现实持续交锋的北方重镇。', true, 5, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);

ALTER TABLE ONLY public.footprints
    ADD CONSTRAINT footprints_release_id_fkey FOREIGN KEY (release_id) REFERENCES public.content_releases(id) ON DELETE CASCADE;

-- ============================================================================
-- 09. footprint_resources（足迹资源）
-- ============================================================================

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

ALTER TABLE ONLY public.footprint_resources
    ADD CONSTRAINT footprint_resources_pkey PRIMARY KEY (id);

CREATE INDEX idx_footprint_resources_resource ON public.footprint_resources USING btree (resource_id);

CREATE UNIQUE INDEX uq_footprint_resources_order ON public.footprint_resources USING btree (footprint_id, sort_order) WHERE (deleted_at IS NULL);

CREATE UNIQUE INDEX uq_footprint_resources_resource ON public.footprint_resources USING btree (footprint_id, resource_id) WHERE (deleted_at IS NULL);

-- footprint_resources 初始数据
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('5c120f2c-baaa-463a-9cd7-e4df7f731cfc', '2d23ba6a-2c82-4351-826f-fdc4095aae3d', '8dd096de-3db8-4941-ad75-eabee4e03633', 0, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('a3ec0175-dee5-4f7c-95be-659c3998735b', '2d23ba6a-2c82-4351-826f-fdc4095aae3d', '8f48cfe1-978d-4636-b693-cdfd9d5aaf56', 1, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('5fb2f27e-3c06-4c20-9afc-922066bf7256', '2d23ba6a-2c82-4351-826f-fdc4095aae3d', '0afa7af5-725f-4e61-b0c8-04d37ab148b9', 2, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('a8dd10ff-ec54-40f9-9d4c-f16801d4e9c0', '2d23ba6a-2c82-4351-826f-fdc4095aae3d', 'efefaae1-db49-473b-a22e-7ff05ecfbb62', 3, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('cea81006-dd8a-4ffb-b587-9c578b9a1881', '2d23ba6a-2c82-4351-826f-fdc4095aae3d', 'b0a6a34e-45d5-4b5b-be36-ab9084fb83df', 4, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('f1185033-49e5-4a2f-97e1-7372d622aed5', '2d23ba6a-2c82-4351-826f-fdc4095aae3d', 'ceed1b03-ddf3-4ac7-97ec-01e6f6de42a8', 5, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('85c962e5-6566-4287-9c4c-720114fb75ac', '2d23ba6a-2c82-4351-826f-fdc4095aae3d', '76e34059-adf8-4280-aa9c-545b6b9d27b8', 6, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('e8a424f7-f993-428d-934f-5777646f428d', '2d23ba6a-2c82-4351-826f-fdc4095aae3d', '207dec49-3a28-4af1-8a29-b8196a7069d0', 7, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('6f008c55-69e3-4413-aba4-15f01ab360d9', '2d23ba6a-2c82-4351-826f-fdc4095aae3d', '70e9bfbf-f11e-4c22-8994-42605e806621', 8, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('fd3ddeca-087f-4c26-aab6-365e1bdee6fd', 'aa6679ac-77d5-488e-99e0-feedc8829cf8', '9643f1ff-cc52-4bbf-b9c6-aa657cd1078e', 0, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('021dbb14-8c0c-408a-a514-abae6a39cc66', 'aa6679ac-77d5-488e-99e0-feedc8829cf8', 'ce487f10-8f9f-45be-bc23-1860f040854d', 1, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('415c46ed-870e-48e1-bde7-2c741fd40cc3', 'aa6679ac-77d5-488e-99e0-feedc8829cf8', 'f59535ef-234e-444e-a799-7af526962eba', 2, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('c778e9fa-f404-495b-a454-e5412db79e5c', 'aa6679ac-77d5-488e-99e0-feedc8829cf8', 'cc48b9e0-f3d3-4980-b386-bfce181fd793', 3, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('e86eff93-f12f-47e5-8bfa-9871b8d0d6a7', 'aa6679ac-77d5-488e-99e0-feedc8829cf8', 'c64a2eee-a122-422d-a58a-25f629d0b80d', 4, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('eafa1329-8a96-45e7-b28b-c07b9ae46dc3', 'aa6679ac-77d5-488e-99e0-feedc8829cf8', '7c1fd1d0-3eac-478a-8713-6c65a85efdac', 5, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('a098bd5c-a26c-4e93-8400-1a8f9df1ecb3', '22efc57b-263c-44b7-a058-b936bf3dd7ba', '27815e56-3d57-4d20-86e3-78279115ba87', 0, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('e7df4226-ba34-4f3b-9457-6898ec4f74cc', '22efc57b-263c-44b7-a058-b936bf3dd7ba', '2e6ee498-f690-4cff-9a1c-983417013ce4', 1, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('7bd6d3dd-92cd-4b91-be18-65b024aff88b', '22efc57b-263c-44b7-a058-b936bf3dd7ba', '82915b3f-e053-4679-b57f-aa0c87f461fa', 2, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('85b14cb7-941c-4f0c-a964-bf3a7d6c8659', '22efc57b-263c-44b7-a058-b936bf3dd7ba', '4e10c48b-8447-4a2c-9db2-91f088df037b', 3, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('20f7aefa-664f-41ab-b362-e149d4915950', '22efc57b-263c-44b7-a058-b936bf3dd7ba', '47d6a325-b0c6-4ab3-b10f-bb87716acdbe', 4, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('9e6cc7b3-89f0-4f1b-9afd-b5a12a585b4f', '22efc57b-263c-44b7-a058-b936bf3dd7ba', '4b57bb78-37ba-49cf-82b3-04dfcd5a794b', 5, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('be6c8c86-b22c-4e31-8904-298e6bcadbfa', '22efc57b-263c-44b7-a058-b936bf3dd7ba', '889405d9-e6b5-47ae-a4d7-f978b390d7a9', 6, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('07b57636-cbef-4592-9917-648b534c9e4f', 'eddbff5b-224d-4228-97ab-5e0376a641a1', 'd3910fb4-a1ab-4153-931d-87d84893d62c', 0, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('78d1e1b1-ee71-40d3-a732-6a46230f413f', 'eddbff5b-224d-4228-97ab-5e0376a641a1', '457fdd0f-2a53-4acf-a102-378d56b25f9b', 1, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('73538dda-dc04-4a2a-9a1c-604217fe779e', 'eddbff5b-224d-4228-97ab-5e0376a641a1', 'e2853460-a24b-438f-99d1-cb06c5123ffd', 2, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('0f8e2501-f7a9-40c8-a0ca-51e2568b83f8', 'eddbff5b-224d-4228-97ab-5e0376a641a1', '81bf69f5-23d2-484f-bb1d-35085fcc68b6', 3, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('6c010ed7-fadd-4758-a63d-53c9a0ef060c', 'eddbff5b-224d-4228-97ab-5e0376a641a1', 'bdd1af10-1578-45dd-bb8f-3fe91f141f29', 4, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('46f3b049-b8c6-405b-bccd-2173dac8d10f', 'eddbff5b-224d-4228-97ab-5e0376a641a1', '18478fc2-e6fd-4bc5-87cc-4a34646b373c', 5, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('a5448644-a8ca-43c9-911f-6cc9624bbd66', '282dfaf7-7594-4ced-b454-07cd9a2229cd', 'cfdefefd-f50a-4c44-8195-0e59dedbc3be', 0, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('58f0e178-aec0-4f9a-9d0c-c2f48c4387f6', '282dfaf7-7594-4ced-b454-07cd9a2229cd', '73d5f633-1732-40ac-b5f0-797cdc308a05', 1, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('a9476da0-1d81-410b-9f43-596c7ca13e0a', '282dfaf7-7594-4ced-b454-07cd9a2229cd', '06c1b717-05c4-4882-8601-7c35f6bed6b7', 2, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('5b9052c9-5250-4da2-b8af-e98fdbc8ca09', '282dfaf7-7594-4ced-b454-07cd9a2229cd', '00591d27-ab26-4295-9e6f-3ce988b5a38d', 3, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('58b8a258-5b9f-4050-b024-5d6f9cc4c2f0', '282dfaf7-7594-4ced-b454-07cd9a2229cd', '2780f138-274a-43b2-97c5-939b5b01b8bc', 4, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('76310ae2-34a4-41c2-8117-32bf6586e828', '282dfaf7-7594-4ced-b454-07cd9a2229cd', 'd832949a-bf1f-4803-87f8-4410c3ecbf4e', 5, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('156f176b-54c2-4e68-b9ef-3b5762997b25', '17257d1e-6459-493e-805f-a314d0df298f', '842d16f0-0ada-4bc0-b63c-2b92f3545f6f', 0, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('00acb1d3-7ed4-4714-8b89-84c387d86d19', '17257d1e-6459-493e-805f-a314d0df298f', '2e0e9185-e407-4412-9601-820210a49f5c', 1, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('61d53b12-988c-46c3-9b5a-1ddc424ee5e8', '17257d1e-6459-493e-805f-a314d0df298f', 'c34714fd-c984-4fb4-b301-305c4dffb8b2', 2, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('6b7d8cbd-2f96-4c0e-a98f-b000968af7ca', '17257d1e-6459-493e-805f-a314d0df298f', '46accbe7-3d42-41c2-b81a-210776fd7b01', 3, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);
INSERT INTO public.footprint_resources (id, footprint_id, resource_id, sort_order, created_at, updated_at, deleted_at) VALUES ('8c18dc40-4d98-430d-8eb8-de6e94856d09', '17257d1e-6459-493e-805f-a314d0df298f', 'b1b8ffa1-1b70-41f5-9bc6-26c7f2a68090', 4, '2026-08-11 22:24:53.147913+08', '2026-08-11 22:24:53.147913+08', NULL);

ALTER TABLE ONLY public.footprint_resources
    ADD CONSTRAINT footprint_resources_footprint_id_fkey FOREIGN KEY (footprint_id) REFERENCES public.footprints(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.footprint_resources
    ADD CONSTRAINT footprint_resources_resource_id_fkey FOREIGN KEY (resource_id) REFERENCES public.resources(id) ON DELETE RESTRICT;

-- ============================================================================
-- 10. hobbies（兴趣爱好）
-- ============================================================================

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

ALTER TABLE ONLY public.hobbies
    ADD CONSTRAINT hobbies_pkey PRIMARY KEY (id);

CREATE INDEX idx_hobbies_release_order ON public.hobbies USING btree (release_id, sort_order, hobby_key) WHERE (deleted_at IS NULL);

CREATE UNIQUE INDEX uq_hobbies_release_key ON public.hobbies USING btree (release_id, hobby_key) WHERE (deleted_at IS NULL);

-- hobbies 初始数据
INSERT INTO public.hobbies (id, release_id, hobby_key, title, description, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('6a1f5400-041d-4f84-9d42-86b3a04246ad', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 'counter-strike-2', 'Travel', '与其说是旅行，不如说是生活。自然观光，爬山看海，city walk，美食探店等等，这些构成了我对美好生活的期待。', true, 0, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobbies (id, release_id, hobby_key, title, description, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('dca2f2c0-d1f3-4b66-8746-1463015f80e0', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 'apex', 'Coding', '静下心专注的做某些事情能让我感到充实，想做些自己感兴趣的项目，想做些让生活更便利更美好', true, 1, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobbies (id, release_id, hobby_key, title, description, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('0f5485e4-cbe3-46be-8645-0e0cdb52b408', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 'delta-force', 'Sport', '爬山看风景，骑车兜下风，或者在市区里走上一整天', true, 2, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobbies (id, release_id, hobby_key, title, description, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('06f3aaf0-f47f-4382-8657-0658e98c8658', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 'valorant', 'Game', '从小学三年级一直延续到现在，游戏的是我曾经最大的爱好', true, 3, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobbies (id, release_id, hobby_key, title, description, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('4cc6f27f-7e4f-4e22-8111-d0640d8dff32', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 'overwatch-2', 'Social or Family', '家人和朋友，是构成我以上所有爱好的基础，我会珍惜每一次相遇', true, 4, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);

ALTER TABLE ONLY public.hobbies
    ADD CONSTRAINT hobbies_release_id_fkey FOREIGN KEY (release_id) REFERENCES public.content_releases(id) ON DELETE CASCADE;

-- ============================================================================
-- 11. hobby_resources（兴趣资源）
-- ============================================================================

CREATE TABLE public.hobby_resources (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    hobby_id uuid NOT NULL,
    resource_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone
);

ALTER TABLE ONLY public.hobby_resources
    ADD CONSTRAINT hobby_resources_pkey PRIMARY KEY (id);

CREATE INDEX idx_hobby_resources_resource ON public.hobby_resources USING btree (resource_id);

CREATE UNIQUE INDEX uq_hobby_resources_hobby ON public.hobby_resources USING btree (hobby_id) WHERE (deleted_at IS NULL);

-- hobby_resources 初始数据
INSERT INTO public.hobby_resources (id, hobby_id, resource_id, created_at, updated_at, deleted_at) VALUES ('8d67b5c4-d1be-490e-8bf7-71d0392488cf', '6a1f5400-041d-4f84-9d42-86b3a04246ad', '75af93fa-1b78-43da-8e11-e1e4d39f2603', '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_resources (id, hobby_id, resource_id, created_at, updated_at, deleted_at) VALUES ('ba340589-d00b-41da-a882-dab19ebe428c', 'dca2f2c0-d1f3-4b66-8746-1463015f80e0', '61078cfe-24dc-4904-9c87-912d774bca62', '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_resources (id, hobby_id, resource_id, created_at, updated_at, deleted_at) VALUES ('1843b219-1700-4897-85c0-22f94a2069ea', '0f5485e4-cbe3-46be-8645-0e0cdb52b408', 'e94c3f43-ff43-4590-85a6-8e5b9a76ccca', '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_resources (id, hobby_id, resource_id, created_at, updated_at, deleted_at) VALUES ('034e0ac4-c8e0-4309-badc-6d8c5c18d099', '06f3aaf0-f47f-4382-8657-0658e98c8658', '232ea6d4-b90b-48b0-8a6f-d1e7f1085933', '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_resources (id, hobby_id, resource_id, created_at, updated_at, deleted_at) VALUES ('6ccc262b-7b10-4866-846f-92652d50092e', '4cc6f27f-7e4f-4e22-8111-d0640d8dff32', '73ddaea2-c208-420a-a5a7-faca1d9e64c1', '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);

ALTER TABLE ONLY public.hobby_resources
    ADD CONSTRAINT hobby_resources_hobby_id_fkey FOREIGN KEY (hobby_id) REFERENCES public.hobbies(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.hobby_resources
    ADD CONSTRAINT hobby_resources_resource_id_fkey FOREIGN KEY (resource_id) REFERENCES public.resources(id) ON DELETE RESTRICT;

-- ============================================================================
-- 12. hobby_time_points（兴趣时间点）
-- ============================================================================

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

ALTER TABLE ONLY public.hobby_time_points
    ADD CONSTRAINT hobby_time_points_pkey PRIMARY KEY (id);

CREATE UNIQUE INDEX uq_hobby_time_points_release_age ON public.hobby_time_points USING btree (release_id, age) WHERE (deleted_at IS NULL);

-- hobby_time_points 初始数据
INSERT INTO public.hobby_time_points (id, release_id, age, hobby1, hobby2, hobby3, hobby4, hobby5, created_at, updated_at, deleted_at) VALUES ('85772df6-194f-4f4c-bc59-8129cfd2f8cd', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', -1, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_time_points (id, release_id, age, hobby1, hobby2, hobby3, hobby4, hobby5, created_at, updated_at, deleted_at) VALUES ('ae2b3b1b-e54c-4380-be64-154ea4c5c0b7', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 0, 0.0, 0.0, 0.0, 0.0, 10.0, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_time_points (id, release_id, age, hobby1, hobby2, hobby3, hobby4, hobby5, created_at, updated_at, deleted_at) VALUES ('57161c6e-1aae-44ef-91cf-bc235f5a474b', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 1, 0.5, 0.0, 0.0, 0.0, 9.5, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_time_points (id, release_id, age, hobby1, hobby2, hobby3, hobby4, hobby5, created_at, updated_at, deleted_at) VALUES ('015238b8-6314-4e49-b259-c35fe0d27f65', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 2, 1.0, 0.0, 0.0, 0.0, 9.0, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_time_points (id, release_id, age, hobby1, hobby2, hobby3, hobby4, hobby5, created_at, updated_at, deleted_at) VALUES ('4b6edebb-a72a-4d77-bc9e-8c1e5f770537', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 3, 2.0, 0.0, 0.0, 0.0, 8.0, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_time_points (id, release_id, age, hobby1, hobby2, hobby3, hobby4, hobby5, created_at, updated_at, deleted_at) VALUES ('28b61cd9-5327-45c2-895e-42a107ab4888', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 4, 3.0, 0.0, 0.0, 0.0, 7.0, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_time_points (id, release_id, age, hobby1, hobby2, hobby3, hobby4, hobby5, created_at, updated_at, deleted_at) VALUES ('e3064b5c-9188-420d-8d1f-50b0d3366c37', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 5, 4.0, 0.0, 0.0, 0.0, 6.0, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_time_points (id, release_id, age, hobby1, hobby2, hobby3, hobby4, hobby5, created_at, updated_at, deleted_at) VALUES ('10b2df55-9bbf-437e-b9ee-6e7f9a45e63a', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 6, 4.5, 0.0, 0.0, 0.0, 5.5, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_time_points (id, release_id, age, hobby1, hobby2, hobby3, hobby4, hobby5, created_at, updated_at, deleted_at) VALUES ('ae4cd1e2-5f97-4b22-8484-53e3ff6454b0', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 7, 5.0, 0.0, 0.0, 0.0, 5.0, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_time_points (id, release_id, age, hobby1, hobby2, hobby3, hobby4, hobby5, created_at, updated_at, deleted_at) VALUES ('e7b8e0e7-6764-4b94-a9a4-38d0f7376318', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 8, 4.5, 0.0, 0.0, 0.0, 5.5, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_time_points (id, release_id, age, hobby1, hobby2, hobby3, hobby4, hobby5, created_at, updated_at, deleted_at) VALUES ('7e47b58a-335e-4de1-805c-cacde66db5be', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 9, 4.0, 0.0, 1.0, 0.0, 5.0, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_time_points (id, release_id, age, hobby1, hobby2, hobby3, hobby4, hobby5, created_at, updated_at, deleted_at) VALUES ('db3fccf9-6d3a-4487-90fa-1a63a1e9fbb1', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 10, 3.7, 0.0, 1.5, 0.0, 4.8, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_time_points (id, release_id, age, hobby1, hobby2, hobby3, hobby4, hobby5, created_at, updated_at, deleted_at) VALUES ('a3cc8e5b-dd8b-43cc-9832-96960419e878', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 11, 3.4, 0.0, 2.0, 0.0, 4.6, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_time_points (id, release_id, age, hobby1, hobby2, hobby3, hobby4, hobby5, created_at, updated_at, deleted_at) VALUES ('d450d08d-31e6-4fe8-8cf2-07447acda755', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 12, 3.1, 0.0, 2.5, 0.0, 4.4, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_time_points (id, release_id, age, hobby1, hobby2, hobby3, hobby4, hobby5, created_at, updated_at, deleted_at) VALUES ('0823df2e-239c-4465-8805-dc9752ab7451', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 13, 2.9, 0.0, 3.0, 0.0, 4.1, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_time_points (id, release_id, age, hobby1, hobby2, hobby3, hobby4, hobby5, created_at, updated_at, deleted_at) VALUES ('964e5e48-6097-4ab0-be59-f6a1ad697a5c', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 14, 2.7, 0.0, 3.2, 0.0, 4.1, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_time_points (id, release_id, age, hobby1, hobby2, hobby3, hobby4, hobby5, created_at, updated_at, deleted_at) VALUES ('7a393a3b-3a79-49ff-a298-1e2ed7c5e6fa', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 15, 2.5, 0.0, 3.4, 0.0, 4.1, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_time_points (id, release_id, age, hobby1, hobby2, hobby3, hobby4, hobby5, created_at, updated_at, deleted_at) VALUES ('6b1059ea-80f8-4743-9224-0527e38c791d', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 16, 2.3, 0.0, 3.6, 0.0, 4.1, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_time_points (id, release_id, age, hobby1, hobby2, hobby3, hobby4, hobby5, created_at, updated_at, deleted_at) VALUES ('228f26b5-942e-4ac1-a152-2ab6c2845ae6', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 17, 2.1, 0.0, 3.9, 0.0, 4.0, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_time_points (id, release_id, age, hobby1, hobby2, hobby3, hobby4, hobby5, created_at, updated_at, deleted_at) VALUES ('f6ef4299-fa62-4621-a990-c25b144cc0ef', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 18, 2.0, 0.0, 4.1, 0.0, 3.9, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_time_points (id, release_id, age, hobby1, hobby2, hobby3, hobby4, hobby5, created_at, updated_at, deleted_at) VALUES ('d9d6a310-3556-4d7f-9d9e-deca1fa8f67e', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 19, 1.9, 0.5, 3.6, 0.5, 3.5, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_time_points (id, release_id, age, hobby1, hobby2, hobby3, hobby4, hobby5, created_at, updated_at, deleted_at) VALUES ('b23a79f6-2d61-4c9e-8f9f-a6b58b599f32', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 20, 1.8, 1.0, 3.2, 0.7, 3.3, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_time_points (id, release_id, age, hobby1, hobby2, hobby3, hobby4, hobby5, created_at, updated_at, deleted_at) VALUES ('5ecfb4d1-112d-4d4d-a800-953bada94265', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 21, 1.7, 1.5, 2.8, 1.0, 3.0, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_time_points (id, release_id, age, hobby1, hobby2, hobby3, hobby4, hobby5, created_at, updated_at, deleted_at) VALUES ('d660963f-aac0-4ef7-bf54-7a43beec5e34', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 22, 1.6, 1.5, 2.6, 2.0, 2.3, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_time_points (id, release_id, age, hobby1, hobby2, hobby3, hobby4, hobby5, created_at, updated_at, deleted_at) VALUES ('97658063-87f3-451d-a070-99aea5b29abc', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 23, 1.5, 2.0, 2.2, 3.0, 1.3, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_time_points (id, release_id, age, hobby1, hobby2, hobby3, hobby4, hobby5, created_at, updated_at, deleted_at) VALUES ('9949b574-4796-40e4-851a-7ed8e28fa5d2', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 24, 1.4, 2.2, 2.0, 3.0, 1.4, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_time_points (id, release_id, age, hobby1, hobby2, hobby3, hobby4, hobby5, created_at, updated_at, deleted_at) VALUES ('231ae5d0-b2f2-41a7-8806-f462ed755497', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 25, 1.3, 2.0, 2.0, 3.0, 1.7, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_time_points (id, release_id, age, hobby1, hobby2, hobby3, hobby4, hobby5, created_at, updated_at, deleted_at) VALUES ('78e26f5b-1269-40cc-b9ff-2c71462e0af0', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 26, 1.2, 2.0, 1.5, 3.5, 1.8, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_time_points (id, release_id, age, hobby1, hobby2, hobby3, hobby4, hobby5, created_at, updated_at, deleted_at) VALUES ('da190910-e1f8-4463-8414-fb1fc91a124e', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', 27, 1.1, 2.0, 1.0, 4.0, 1.9, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);

ALTER TABLE ONLY public.hobby_time_points
    ADD CONSTRAINT hobby_time_points_release_id_fkey FOREIGN KEY (release_id) REFERENCES public.content_releases(id) ON DELETE CASCADE;

-- ============================================================================
-- 13. hobby_time_tags（兴趣时间标签）
-- ============================================================================

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
    CONSTRAINT hobby_time_tags_data_key_check CHECK (((data_key)::text = ANY (ARRAY[('爱好1'::character varying)::text, ('爱好2'::character varying)::text, ('爱好3'::character varying)::text, ('爱好4'::character varying)::text, ('爱好5'::character varying)::text]))),
    CONSTRAINT hobby_time_tags_label_scale_check CHECK (((label_scale >= 0.5) AND (label_scale <= 3.0))),
    CONSTRAINT hobby_time_tags_label_x_check CHECK (((label_x >= 0) AND (label_x <= 500))),
    CONSTRAINT hobby_time_tags_label_y_check CHECK (((label_y >= 0) AND (label_y <= 300))),
    CONSTRAINT hobby_time_tags_sort_order_check CHECK ((sort_order >= 0))
);

ALTER TABLE ONLY public.hobby_time_tags
    ADD CONSTRAINT hobby_time_tags_pkey PRIMARY KEY (id);

CREATE UNIQUE INDEX uq_hobby_time_tags_release_key ON public.hobby_time_tags USING btree (release_id, data_key) WHERE (deleted_at IS NULL);

CREATE UNIQUE INDEX uq_hobby_time_tags_release_order ON public.hobby_time_tags USING btree (release_id, sort_order) WHERE (deleted_at IS NULL);

-- hobby_time_tags 初始数据
INSERT INTO public.hobby_time_tags (id, release_id, data_key, name, color, label_x, label_y, label_scale, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('3134cfc7-d330-4694-92a5-382eb9e021eb', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', '爱好1', 'Sport', '#f8b659', 110, 240, 1.5, true, 0, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_time_tags (id, release_id, data_key, name, color, label_x, label_y, label_scale, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('a8a5e260-046f-47b5-b168-0a700b043ffb', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', '爱好2', 'Travel', '#57caff', 410, 240, 1.3, true, 1, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_time_tags (id, release_id, data_key, name, color, label_x, label_y, label_scale, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('c8b86b1d-ad92-4dda-9d31-4514c9bfa346', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', '爱好3', 'Game', '#656afb', 300, 170, 1.5, true, 2, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_time_tags (id, release_id, data_key, name, color, label_x, label_y, label_scale, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('e531b2ae-e5c4-4922-926f-2391a2a4d193', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', '爱好4', 'Coding', '#33ff77', 410, 110, 1.5, true, 3, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);
INSERT INTO public.hobby_time_tags (id, release_id, data_key, name, color, label_x, label_y, label_scale, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('8c354175-8e75-43c9-b91c-25bc898ffff4', 'a2f87024-1aaf-4687-afa1-325a1c7b9f57', '爱好5', 'Social or Family', '#70fff5', 63, 100, 1.5, true, 4, '2026-08-26 23:52:43.840646+08', '2026-08-26 23:52:43.840646+08', NULL);

ALTER TABLE ONLY public.hobby_time_tags
    ADD CONSTRAINT hobby_time_tags_release_id_fkey FOREIGN KEY (release_id) REFERENCES public.content_releases(id) ON DELETE CASCADE;

-- ============================================================================
-- 14. home_images（首页图片）
-- ============================================================================

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

ALTER TABLE ONLY public.home_images
    ADD CONSTRAINT home_images_pkey PRIMARY KEY (id);

CREATE INDEX idx_home_images_resource ON public.home_images USING btree (image_resource_id);

CREATE UNIQUE INDEX uq_home_images_release_order ON public.home_images USING btree (release_id, sort_order) WHERE (deleted_at IS NULL);

-- home_images 初始数据
INSERT INTO public.home_images (id, release_id, image_resource_id, alt_text, object_position, sort_order, created_at, updated_at, deleted_at) VALUES ('318e186b-9263-40f1-8549-ebde6c437dc7', '897c1c16-c070-461a-8d93-109d24c17979', '17a0aca0-a554-5d7c-811f-87950438bd0b', '香港太平山城市远景', '50% 35%', 0, '2026-08-13 22:11:04.814812+08', '2026-08-13 22:11:04.814812+08', NULL);
INSERT INTO public.home_images (id, release_id, image_resource_id, alt_text, object_position, sort_order, created_at, updated_at, deleted_at) VALUES ('a9438491-8e88-4b3d-b2ee-112d1eccbb15', '897c1c16-c070-461a-8d93-109d24c17979', '39c26cd2-109b-5c9a-99b1-0dcb7d19865a', '蓝天下飞翔的海鸥', '50% 42%', 1, '2026-08-13 22:11:04.814812+08', '2026-08-13 22:11:04.814812+08', NULL);
INSERT INTO public.home_images (id, release_id, image_resource_id, alt_text, object_position, sort_order, created_at, updated_at, deleted_at) VALUES ('6f3366e1-24f7-4785-8be3-774c415eb982', '897c1c16-c070-461a-8d93-109d24c17979', 'cd1316ac-c206-584c-982d-e024a084ec2d', '落日晚霞山景', '50% 50%', 2, '2026-08-13 22:11:04.814812+08', '2026-08-13 22:11:04.814812+08', NULL);
INSERT INTO public.home_images (id, release_id, image_resource_id, alt_text, object_position, sort_order, created_at, updated_at, deleted_at) VALUES ('a69311c3-9300-40e6-8a80-95f3e55ce21d', '897c1c16-c070-461a-8d93-109d24c17979', '266331b3-1978-5495-ad36-362647692ada', '海面与云层', '50% 50%', 3, '2026-08-13 22:11:04.814812+08', '2026-08-13 22:11:04.814812+08', NULL);
INSERT INTO public.home_images (id, release_id, image_resource_id, alt_text, object_position, sort_order, created_at, updated_at, deleted_at) VALUES ('4976fdf7-bd47-4767-b97a-a0c9ded64876', '897c1c16-c070-461a-8d93-109d24c17979', '16760407-ecf2-5587-8d75-000a4c91686d', '夜色城市灯光', '50% 45%', 4, '2026-08-13 22:11:04.814812+08', '2026-08-13 22:11:04.814812+08', NULL);
INSERT INTO public.home_images (id, release_id, image_resource_id, alt_text, object_position, sort_order, created_at, updated_at, deleted_at) VALUES ('efaf5e98-02c9-44d0-afd3-128a8875cdb4', '897c1c16-c070-461a-8d93-109d24c17979', '931c1f55-66e5-536a-ad42-2ceec8cf5e5d', '海边公路与云', '50% 50%', 5, '2026-08-13 22:11:04.814812+08', '2026-08-13 22:11:04.814812+08', NULL);
INSERT INTO public.home_images (id, release_id, image_resource_id, alt_text, object_position, sort_order, created_at, updated_at, deleted_at) VALUES ('fe32c123-6e5e-4425-bc0e-0652812c813f', '6e5ce8e2-4351-4763-9ddb-8b5dbe528226', '17a0aca0-a554-5d7c-811f-87950438bd0b', '香港太平山城市远景', '50% 35%', 0, '2026-08-27 00:01:08.752216+08', '2026-08-27 00:01:08.752216+08', NULL);
INSERT INTO public.home_images (id, release_id, image_resource_id, alt_text, object_position, sort_order, created_at, updated_at, deleted_at) VALUES ('09597da4-f81d-483b-8a6f-f1dd558b9f6d', '6e5ce8e2-4351-4763-9ddb-8b5dbe528226', '39c26cd2-109b-5c9a-99b1-0dcb7d19865a', '蓝天下飞翔的海鸥', '50% 42%', 1, '2026-08-27 00:01:08.752216+08', '2026-08-27 00:01:08.752216+08', NULL);
INSERT INTO public.home_images (id, release_id, image_resource_id, alt_text, object_position, sort_order, created_at, updated_at, deleted_at) VALUES ('07f3df02-3615-4b4a-96c5-31a09c14f601', '6e5ce8e2-4351-4763-9ddb-8b5dbe528226', 'cd1316ac-c206-584c-982d-e024a084ec2d', '落日晚霞山景', '50% 50%', 2, '2026-08-27 00:01:08.752216+08', '2026-08-27 00:01:08.752216+08', NULL);
INSERT INTO public.home_images (id, release_id, image_resource_id, alt_text, object_position, sort_order, created_at, updated_at, deleted_at) VALUES ('ec7df3f2-88d5-4c4d-8b40-3b9e6f25beff', '6e5ce8e2-4351-4763-9ddb-8b5dbe528226', '266331b3-1978-5495-ad36-362647692ada', '海面与云层', '50% 50%', 3, '2026-08-27 00:01:08.752216+08', '2026-08-27 00:01:08.752216+08', NULL);
INSERT INTO public.home_images (id, release_id, image_resource_id, alt_text, object_position, sort_order, created_at, updated_at, deleted_at) VALUES ('406806c1-ee6e-425e-ad4b-046847be77c6', '6e5ce8e2-4351-4763-9ddb-8b5dbe528226', '16760407-ecf2-5587-8d75-000a4c91686d', '夜色城市灯光', '50% 45%', 4, '2026-08-27 00:01:08.752216+08', '2026-08-27 00:01:08.752216+08', NULL);
INSERT INTO public.home_images (id, release_id, image_resource_id, alt_text, object_position, sort_order, created_at, updated_at, deleted_at) VALUES ('96598446-e230-4722-ad6c-e2bb62cbb055', '6e5ce8e2-4351-4763-9ddb-8b5dbe528226', '931c1f55-66e5-536a-ad42-2ceec8cf5e5d', '海边公路与云', '50% 50%', 5, '2026-08-27 00:01:08.752216+08', '2026-08-27 00:01:08.752216+08', NULL);

ALTER TABLE ONLY public.home_images
    ADD CONSTRAINT home_images_image_resource_id_fkey FOREIGN KEY (image_resource_id) REFERENCES public.resources(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.home_images
    ADD CONSTRAINT home_images_release_id_fkey FOREIGN KEY (release_id) REFERENCES public.content_releases(id) ON DELETE CASCADE;

-- ============================================================================
-- 15. mylab_cards（实验室卡片）
-- ============================================================================

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
    CONSTRAINT mylab_cards_card_type_check CHECK (((card_type)::text = ANY (ARRAY[('PROJECT'::character varying)::text, ('ARTICLE'::character varying)::text]))),
    CONSTRAINT mylab_cards_project_show_order_check CHECK (((project_show_order IS NULL) OR (project_show_order >= 0))),
    CONSTRAINT mylab_cards_sort_order_check CHECK ((sort_order >= 0))
);

ALTER TABLE ONLY public.mylab_cards
    ADD CONSTRAINT mylab_cards_pkey PRIMARY KEY (id);

CREATE INDEX idx_mylab_cards_release_order ON public.mylab_cards USING btree (release_id, sort_order, post_key) WHERE (deleted_at IS NULL);

CREATE UNIQUE INDEX uq_mylab_cards_project_order ON public.mylab_cards USING btree (release_id, project_show_order) WHERE (((card_type)::text = 'PROJECT'::text) AND (deleted_at IS NULL));

CREATE UNIQUE INDEX uq_mylab_cards_release_key ON public.mylab_cards USING btree (release_id, post_key) WHERE (deleted_at IS NULL);

-- mylab_cards 初始数据
INSERT INTO public.mylab_cards (id, release_id, post_key, card_title, card_summary, post_date, enabled, sort_order, card_type, project_show_order, project_contents, created_at, updated_at, deleted_at) VALUES ('19789b13-080c-4db0-94dd-d605a14d91f9', 'b061ab16-f7ee-4806-a4af-7628124f5082', 'project-gm1', 'Moth and Bat：项目研究记录', '48 小时 GameJam 作品，关于夜色中两种生物的相会。', '2024-01-01', true, 0, 'PROJECT', 0, '这是一款关于夜晚相遇的解谜游戏。

Unity、C#、Aseprite', '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_cards (id, release_id, post_key, card_title, card_summary, post_date, enabled, sort_order, card_type, project_show_order, project_contents, created_at, updated_at, deleted_at) VALUES ('656b86f7-7118-41fb-a265-03cdce4cb6bf', 'b061ab16-f7ee-4806-a4af-7628124f5082', 'project-gm2', 'Naughty Cat：项目研究记录', '一只总想搞破坏的猫与一个不肯关机的扫地机器人。', '2023-01-01', true, 1, 'PROJECT', 1, '一款轻松幽默的平台跳跃游戏。

Godot、GDScript', '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_cards (id, release_id, post_key, card_title, card_summary, post_date, enabled, sort_order, card_type, project_show_order, project_contents, created_at, updated_at, deleted_at) VALUES ('622192ed-c358-4700-a0b9-5e3c55e9898d', 'b061ab16-f7ee-4806-a4af-7628124f5082', 'project-gm3', 'Naughty Boy：项目研究记录', '规则与违抗之间的游戏化实验，关于儿童行为心理学的隐喻。', '2023-01-01', true, 2, 'PROJECT', 2, '探索规则边界的叙事游戏。

Phaser、JavaScript', '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_cards (id, release_id, post_key, card_title, card_summary, post_date, enabled, sort_order, card_type, project_show_order, project_contents, created_at, updated_at, deleted_at) VALUES ('ae1e9096-ef22-4219-b24b-98b99f9982c6', 'b061ab16-f7ee-4806-a4af-7628124f5082', 'project-gm4', 'Ring of Elysium：项目研究记录', '参与腾讯北极光工作室《无限法则》的玩法与系统设计。', '2022-01-01', true, 3, 'PROJECT', 3, '作为玩法设计师参与开发的大逃杀游戏。

Unreal Engine、C++、Lua', '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_cards (id, release_id, post_key, card_title, card_summary, post_date, enabled, sort_order, card_type, project_show_order, project_contents, created_at, updated_at, deleted_at) VALUES ('fdedf6c4-8a5d-41d2-8c9e-26d36af4b107', 'b061ab16-f7ee-4806-a4af-7628124f5082', 'project-gm5', 'Moodlog：项目研究记录', '一个极简的情绪记录工具，专注输入体验与一年后的回看。', '2024-01-01', true, 4, 'PROJECT', 4, '帮助你记录情绪变化的日常工具。

React、TypeScript、Supabase', '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_cards (id, release_id, post_key, card_title, card_summary, post_date, enabled, sort_order, card_type, project_show_order, project_contents, created_at, updated_at, deleted_at) VALUES ('257bd927-938b-4a56-b0db-c1e27ba5ea68', 'b061ab16-f7ee-4806-a4af-7628124f5082', 'project-gm6', 'Beat Lab：项目研究记录', '浏览器内的鼓机与音序器，使用 Web Audio API 实时合成。', '2023-01-01', true, 5, 'PROJECT', 5, '在线音乐创作工具。

Vue、Web Audio API、Tone.js', '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_cards (id, release_id, post_key, card_title, card_summary, post_date, enabled, sort_order, card_type, project_show_order, project_contents, created_at, updated_at, deleted_at) VALUES ('554cb3ba-4e9e-493b-a21e-31eb149bd00b', 'b061ab16-f7ee-4806-a4af-7628124f5082', 'blog-docker-deploy', '个人博客 Docker + Nginx 部署全流程记录', '从 Dockerfile 多阶段构建到 nginx SPA 回退与 gzip 配置，把博客塞进容器的完整折腾过程。', '2026-07-28', true, 6, 'ARTICLE', NULL, NULL, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_cards (id, release_id, post_key, card_title, card_summary, post_date, enabled, sort_order, card_type, project_show_order, project_contents, created_at, updated_at, deleted_at) VALUES ('87ebd737-3150-47c1-97e4-bcb76121353c', 'b061ab16-f7ee-4806-a4af-7628124f5082', 'vue-gsap-hero', '用 GSAP 给首页 Hero 做电影感动效', 'ScrollTrigger 驱动的滚动叙事：分镜、视差与滚动提示文字的入场编排。', '2026-07-15', true, 7, 'ARTICLE', NULL, NULL, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_cards (id, release_id, post_key, card_title, card_summary, post_date, enabled, sort_order, card_type, project_show_order, project_contents, created_at, updated_at, deleted_at) VALUES ('0baca4db-11fa-485a-bb69-ca6ed0378f3b', 'b061ab16-f7ee-4806-a4af-7628124f5082', 'tailwind-migration', '项目迁移 Tailwind CSS v4 的坑', 'v4 改为 CSS-first 配置后，postcss 插件与 @theme 写法的迁移笔记。', '2026-06-12', true, 8, 'ARTICLE', NULL, NULL, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_cards (id, release_id, post_key, card_title, card_summary, post_date, enabled, sort_order, card_type, project_show_order, project_contents, created_at, updated_at, deleted_at) VALUES ('37cf7d85-1ef0-40b3-917a-93c6a958a6fa', 'b061ab16-f7ee-4806-a4af-7628124f5082', 'vue-composable-mouse-tilt', '封装一个 useMouseTilt 组合式函数', '用 requestAnimationFrame 节流鼠标事件，给卡片做跟随视角的 3D 倾斜。', '2026-05-06', true, 9, 'ARTICLE', NULL, NULL, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_cards (id, release_id, post_key, card_title, card_summary, post_date, enabled, sort_order, card_type, project_show_order, project_contents, created_at, updated_at, deleted_at) VALUES ('a03ee04e-5c86-4324-9ed7-449fa66324a9', 'b061ab16-f7ee-4806-a4af-7628124f5082', 'first-post', 'MyLab 开张：为什么单独开一个实验记录页', '项目展示放在首页，零散的学习与折腾记录集中收在这里，方便检索与回顾。', '2026-04-01', true, 10, 'ARTICLE', NULL, NULL, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);

ALTER TABLE ONLY public.mylab_cards
    ADD CONSTRAINT mylab_cards_release_id_fkey FOREIGN KEY (release_id) REFERENCES public.content_releases(id) ON DELETE CASCADE;

-- ============================================================================
-- 16. mylab_card_tags（实验室卡片标签）
-- ============================================================================

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

ALTER TABLE ONLY public.mylab_card_tags
    ADD CONSTRAINT mylab_card_tags_pkey PRIMARY KEY (id);

CREATE INDEX idx_mylab_card_tags_tag ON public.mylab_card_tags USING btree (tag_id, card_id) WHERE (deleted_at IS NULL);

CREATE UNIQUE INDEX uq_mylab_card_tags_order ON public.mylab_card_tags USING btree (card_id, sort_order) WHERE (deleted_at IS NULL);

CREATE UNIQUE INDEX uq_mylab_card_tags_pair ON public.mylab_card_tags USING btree (card_id, tag_id) WHERE (deleted_at IS NULL);

-- mylab_card_tags 初始数据
INSERT INTO public.mylab_card_tags (id, card_id, tag_id, sort_order, created_at, updated_at, deleted_at) VALUES ('7479373c-3f38-4a4f-9f1a-fb9bbb9b8521', '19789b13-080c-4db0-94dd-d605a14d91f9', 'cd3776a7-8d5f-5c3f-b7d2-b81fc8974d7a', 0, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_card_tags (id, card_id, tag_id, sort_order, created_at, updated_at, deleted_at) VALUES ('55413812-450c-456b-ba37-8bd856b9c9c4', '19789b13-080c-4db0-94dd-d605a14d91f9', '9b7ab32a-5c1f-566b-a8dc-48bdf9a6fc16', 1, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_card_tags (id, card_id, tag_id, sort_order, created_at, updated_at, deleted_at) VALUES ('7577266a-e71b-46c1-a58c-68a338cf8385', '19789b13-080c-4db0-94dd-d605a14d91f9', '100b7029-067a-59db-8c2b-f9c524fb520c', 2, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_card_tags (id, card_id, tag_id, sort_order, created_at, updated_at, deleted_at) VALUES ('b2890c23-16e7-45c9-8d87-733600d52318', '19789b13-080c-4db0-94dd-d605a14d91f9', 'f153754c-e92a-5d82-9642-250ef1972aa8', 3, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_card_tags (id, card_id, tag_id, sort_order, created_at, updated_at, deleted_at) VALUES ('f66c530e-5ee8-47cb-adb9-8cd12487e6e5', '656b86f7-7118-41fb-a265-03cdce4cb6bf', 'cd3776a7-8d5f-5c3f-b7d2-b81fc8974d7a', 0, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_card_tags (id, card_id, tag_id, sort_order, created_at, updated_at, deleted_at) VALUES ('0105ab30-a298-4130-a220-434c1a9a3f7a', '656b86f7-7118-41fb-a265-03cdce4cb6bf', '2d9db0ad-a7fb-5bcf-99ff-040ba892267c', 1, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_card_tags (id, card_id, tag_id, sort_order, created_at, updated_at, deleted_at) VALUES ('26d2d1be-c470-4067-9040-1c051c8baf18', '656b86f7-7118-41fb-a265-03cdce4cb6bf', '0fc6f305-8fae-53fd-ac1a-85d060a4f2bf', 2, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_card_tags (id, card_id, tag_id, sort_order, created_at, updated_at, deleted_at) VALUES ('5309ee9e-037c-44cb-94c4-63e5e01947ae', '622192ed-c358-4700-a0b9-5e3c55e9898d', 'cd3776a7-8d5f-5c3f-b7d2-b81fc8974d7a', 0, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_card_tags (id, card_id, tag_id, sort_order, created_at, updated_at, deleted_at) VALUES ('9281b78f-b96e-4a75-a108-a4cc614e6997', '622192ed-c358-4700-a0b9-5e3c55e9898d', '6310e463-4ad4-59f3-8cf3-e735e3fd3691', 1, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_card_tags (id, card_id, tag_id, sort_order, created_at, updated_at, deleted_at) VALUES ('ea518564-dc07-4feb-bb69-e7c2962c5091', '622192ed-c358-4700-a0b9-5e3c55e9898d', 'faba2f26-a83f-575a-9c0e-590251dc6909', 2, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_card_tags (id, card_id, tag_id, sort_order, created_at, updated_at, deleted_at) VALUES ('04771517-27b5-4e18-a45f-404676a881e3', 'ae1e9096-ef22-4219-b24b-98b99f9982c6', '16649271-892d-57bf-8555-a43662e53956', 0, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_card_tags (id, card_id, tag_id, sort_order, created_at, updated_at, deleted_at) VALUES ('e3ae4852-aced-442e-bdac-c1d11bf493a3', 'ae1e9096-ef22-4219-b24b-98b99f9982c6', '85dd4c28-cbe0-52b4-88bf-82cd376f20f3', 1, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_card_tags (id, card_id, tag_id, sort_order, created_at, updated_at, deleted_at) VALUES ('70376bd2-2a42-4b30-850e-74f2f96b7497', 'ae1e9096-ef22-4219-b24b-98b99f9982c6', '902ac023-ea59-51e6-b308-75aae2fefa14', 2, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_card_tags (id, card_id, tag_id, sort_order, created_at, updated_at, deleted_at) VALUES ('8e696fae-a4bf-4774-9b6e-e800276b097c', 'fdedf6c4-8a5d-41d2-8c9e-26d36af4b107', '40685b33-1982-589e-8c09-2df8b75b5655', 0, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_card_tags (id, card_id, tag_id, sort_order, created_at, updated_at, deleted_at) VALUES ('809c8231-3eb8-44a3-ba86-8d89de8fdb84', 'fdedf6c4-8a5d-41d2-8c9e-26d36af4b107', '11894919-dbfa-5d3d-8a2f-3bfa1580c4b6', 1, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_card_tags (id, card_id, tag_id, sort_order, created_at, updated_at, deleted_at) VALUES ('5f9338cf-bfcd-492e-8090-08643f6702d0', 'fdedf6c4-8a5d-41d2-8c9e-26d36af4b107', '1bba22a4-d4c5-5f5e-8dcf-bce71d13388a', 2, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_card_tags (id, card_id, tag_id, sort_order, created_at, updated_at, deleted_at) VALUES ('4ecf6e8f-c3f7-4f1a-817f-f22100775158', '257bd927-938b-4a56-b0db-c1e27ba5ea68', 'e64d08a8-eedf-5d6c-be92-91a51a9c294f', 0, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_card_tags (id, card_id, tag_id, sort_order, created_at, updated_at, deleted_at) VALUES ('c1f00d1d-db3b-4d46-8ed7-06f1b0adf104', '257bd927-938b-4a56-b0db-c1e27ba5ea68', 'c144f9fa-8ffb-5225-a51c-2b68aaeb276c', 1, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_card_tags (id, card_id, tag_id, sort_order, created_at, updated_at, deleted_at) VALUES ('9ba3c2bd-7b9e-4891-9399-8522b4bbaa5a', '257bd927-938b-4a56-b0db-c1e27ba5ea68', 'b08090e1-19eb-598d-bbee-1ff39bcf31a0', 2, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_card_tags (id, card_id, tag_id, sort_order, created_at, updated_at, deleted_at) VALUES ('e605096e-15f0-4f23-a49c-530abc957f74', '554cb3ba-4e9e-493b-a21e-31eb149bd00b', 'b820842b-8c3e-5dba-a955-393633ab888e', 0, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_card_tags (id, card_id, tag_id, sort_order, created_at, updated_at, deleted_at) VALUES ('f4597a87-0fc2-4912-a2fb-2c06121943c1', '554cb3ba-4e9e-493b-a21e-31eb149bd00b', '85538585-342a-58bb-b4d5-37fe10d2f05e', 1, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_card_tags (id, card_id, tag_id, sort_order, created_at, updated_at, deleted_at) VALUES ('831affe1-60d0-46de-8ce4-5204b3ef8a5a', '554cb3ba-4e9e-493b-a21e-31eb149bd00b', '61dc6897-31dd-5b1f-bc22-b32e1ad4c51c', 2, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_card_tags (id, card_id, tag_id, sort_order, created_at, updated_at, deleted_at) VALUES ('60d7aed5-47af-4842-86ba-6048c637b9e1', '87ebd737-3150-47c1-97e4-bcb76121353c', 'e64d08a8-eedf-5d6c-be92-91a51a9c294f', 0, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_card_tags (id, card_id, tag_id, sort_order, created_at, updated_at, deleted_at) VALUES ('93fe1644-be88-4354-91e3-744083c341c1', '87ebd737-3150-47c1-97e4-bcb76121353c', 'bed13764-9b69-54c0-b976-a9a0f6a594ae', 1, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_card_tags (id, card_id, tag_id, sort_order, created_at, updated_at, deleted_at) VALUES ('d94e4f42-4cfc-44d9-b4fa-685d9a6e167a', '87ebd737-3150-47c1-97e4-bcb76121353c', 'be50c0a5-d841-5287-80cf-b3272fa0277b', 2, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_card_tags (id, card_id, tag_id, sort_order, created_at, updated_at, deleted_at) VALUES ('92ddc488-c13a-4984-a6cc-25a7b6c18129', '0baca4db-11fa-485a-bb69-ca6ed0378f3b', 'd4f70c09-16a3-54a1-b715-dc608b90859e', 0, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_card_tags (id, card_id, tag_id, sort_order, created_at, updated_at, deleted_at) VALUES ('1b8dd5a1-6c24-4550-924a-d1345868e081', '0baca4db-11fa-485a-bb69-ca6ed0378f3b', 'be50c0a5-d841-5287-80cf-b3272fa0277b', 1, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_card_tags (id, card_id, tag_id, sort_order, created_at, updated_at, deleted_at) VALUES ('4110d2dd-b82e-44d5-8924-fe9009b4ccc0', '0baca4db-11fa-485a-bb69-ca6ed0378f3b', '4ac8673b-86ef-5af1-a70a-e446cc2cf799', 2, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_card_tags (id, card_id, tag_id, sort_order, created_at, updated_at, deleted_at) VALUES ('32680229-7948-43aa-a11b-2814a1e5977e', '37cf7d85-1ef0-40b3-917a-93c6a958a6fa', 'e64d08a8-eedf-5d6c-be92-91a51a9c294f', 0, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_card_tags (id, card_id, tag_id, sort_order, created_at, updated_at, deleted_at) VALUES ('6de94f3c-7508-47d2-9b7c-516f91bb0cc8', '37cf7d85-1ef0-40b3-917a-93c6a958a6fa', 'be50c0a5-d841-5287-80cf-b3272fa0277b', 1, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_card_tags (id, card_id, tag_id, sort_order, created_at, updated_at, deleted_at) VALUES ('7e1f07da-b2ac-460d-816f-a00c921bad72', 'a03ee04e-5c86-4324-9ed7-449fa66324a9', 'ce23d581-aa67-5c91-a808-0fba88586935', 0, '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);

ALTER TABLE ONLY public.mylab_card_tags
    ADD CONSTRAINT mylab_card_tags_card_id_fkey FOREIGN KEY (card_id) REFERENCES public.mylab_cards(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.mylab_card_tags
    ADD CONSTRAINT mylab_card_tags_tag_id_fkey FOREIGN KEY (tag_id) REFERENCES public.mylab_tags(id) ON DELETE RESTRICT;

-- ============================================================================
-- 17. mylab_resources（实验室资源）
-- ============================================================================

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

ALTER TABLE ONLY public.mylab_resources
    ADD CONSTRAINT mylab_resources_pkey PRIMARY KEY (id);

CREATE INDEX idx_mylab_resources_content ON public.mylab_resources USING btree (content_resource_id);

CREATE INDEX idx_mylab_resources_image ON public.mylab_resources USING btree (image_resource_id);

CREATE UNIQUE INDEX uq_mylab_resources_card ON public.mylab_resources USING btree (card_id) WHERE (deleted_at IS NULL);

-- mylab_resources 初始数据
INSERT INTO public.mylab_resources (id, card_id, image_resource_id, content_resource_id, created_at, updated_at, deleted_at) VALUES ('0dd4b0d2-a4d4-42ad-8fc9-4882c891a3b1', '19789b13-080c-4db0-94dd-d605a14d91f9', 'edc8f7b4-861b-5df8-8fe2-10fb34579671', 'a7ee90e0-9b3d-5dca-a5c5-0b8e119fc594', '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_resources (id, card_id, image_resource_id, content_resource_id, created_at, updated_at, deleted_at) VALUES ('5773f435-cac1-4de3-98d5-0f8e23b6f908', '656b86f7-7118-41fb-a265-03cdce4cb6bf', '66c98ded-1585-53e3-81bb-d3d81cd07551', '513d2877-761e-51f0-ba14-648dd9d9b4e1', '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_resources (id, card_id, image_resource_id, content_resource_id, created_at, updated_at, deleted_at) VALUES ('f952126e-8b54-4b25-961f-d69fd03878d8', '622192ed-c358-4700-a0b9-5e3c55e9898d', 'fa390e9e-11ae-578f-9cda-c59ce887e5b3', '247a975b-8bfc-5065-9887-dae2df61df79', '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_resources (id, card_id, image_resource_id, content_resource_id, created_at, updated_at, deleted_at) VALUES ('65cdc3f5-f254-42ac-858a-bf0fea6cc103', 'ae1e9096-ef22-4219-b24b-98b99f9982c6', '9d1b7101-100b-5f3f-9185-11a59d08da77', 'c4901a3e-ec1e-5a78-8703-3c006996fb3e', '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_resources (id, card_id, image_resource_id, content_resource_id, created_at, updated_at, deleted_at) VALUES ('2b117700-efd7-4af8-a9e4-8bd4b725b562', 'fdedf6c4-8a5d-41d2-8c9e-26d36af4b107', '51decfbf-b194-5795-a31d-4b7dd377d0bb', '454666a1-482b-5687-abbf-769c3c676d6e', '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_resources (id, card_id, image_resource_id, content_resource_id, created_at, updated_at, deleted_at) VALUES ('32cee89a-03b4-4572-b36f-7d8969026849', '257bd927-938b-4a56-b0db-c1e27ba5ea68', '5e60c20f-9e5c-5d58-b7a9-121bf18696c8', '47993a1b-d360-54b6-b869-42438a3f05d7', '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_resources (id, card_id, image_resource_id, content_resource_id, created_at, updated_at, deleted_at) VALUES ('14ff909a-2585-4fa3-b3cc-b60e97e09392', '554cb3ba-4e9e-493b-a21e-31eb149bd00b', 'edc8f7b4-861b-5df8-8fe2-10fb34579671', '0efb8ec1-d08c-5296-a226-ff241e9a1c3e', '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_resources (id, card_id, image_resource_id, content_resource_id, created_at, updated_at, deleted_at) VALUES ('43047790-5cbb-4cc4-a9f2-9d8ccf26e52e', '87ebd737-3150-47c1-97e4-bcb76121353c', '66c98ded-1585-53e3-81bb-d3d81cd07551', '5eaf8bb5-1a8a-5791-a9fd-66a8dda4ce66', '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_resources (id, card_id, image_resource_id, content_resource_id, created_at, updated_at, deleted_at) VALUES ('a5e56cbb-f5e3-46c3-b72d-185869aa9782', '0baca4db-11fa-485a-bb69-ca6ed0378f3b', '9d1b7101-100b-5f3f-9185-11a59d08da77', '37ec099c-2dca-593f-a9db-3eb8f0de6f0b', '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_resources (id, card_id, image_resource_id, content_resource_id, created_at, updated_at, deleted_at) VALUES ('cfb995bf-fd1b-43a6-af2f-95d8f56cb6c9', '37cf7d85-1ef0-40b3-917a-93c6a958a6fa', '5e60c20f-9e5c-5d58-b7a9-121bf18696c8', 'ee612e27-b895-5946-beaa-6a9b272855f8', '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);
INSERT INTO public.mylab_resources (id, card_id, image_resource_id, content_resource_id, created_at, updated_at, deleted_at) VALUES ('34a7b239-f92a-43bd-82b2-6cfa029ae9c5', 'a03ee04e-5c86-4324-9ed7-449fa66324a9', '66c98ded-1585-53e3-81bb-d3d81cd07551', '2d0214f8-6e4f-5c3e-ac39-b54e4fd9de57', '2026-08-26 23:54:12.93658+08', '2026-08-26 23:54:12.93658+08', NULL);

ALTER TABLE ONLY public.mylab_resources
    ADD CONSTRAINT mylab_resources_card_id_fkey FOREIGN KEY (card_id) REFERENCES public.mylab_cards(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.mylab_resources
    ADD CONSTRAINT mylab_resources_content_resource_id_fkey FOREIGN KEY (content_resource_id) REFERENCES public.resources(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.mylab_resources
    ADD CONSTRAINT mylab_resources_image_resource_id_fkey FOREIGN KEY (image_resource_id) REFERENCES public.resources(id) ON DELETE RESTRICT;

-- ============================================================================
-- 18. skills（技能）
-- ============================================================================

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
    CONSTRAINT skills_level_code_check CHECK (((level_code IS NULL) OR ((level_code)::text = ANY (ARRAY[('novice'::character varying)::text, ('competent'::character varying)::text, ('proficient'::character varying)::text])))),
    CONSTRAINT skills_percentage_check CHECK (((percentage >= 0) AND (percentage <= 100))),
    CONSTRAINT skills_sort_order_check CHECK ((sort_order >= 0))
);

ALTER TABLE ONLY public.skills
    ADD CONSTRAINT skills_pkey PRIMARY KEY (id);

CREATE INDEX idx_skills_icon_resource ON public.skills USING btree (icon_resource_id);

CREATE INDEX idx_skills_release_order ON public.skills USING btree (release_id, sort_order, skill_key) WHERE (deleted_at IS NULL);

CREATE UNIQUE INDEX uq_skills_release_key ON public.skills USING btree (release_id, skill_key) WHERE (deleted_at IS NULL);

-- skills 初始数据
INSERT INTO public.skills (id, release_id, skill_key, name, percentage, level_code, level_text, icon_resource_id, bar_style, is_new, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('51457a05-3062-4ee5-afac-8792a218c3d3', 'f6d1b717-651e-48d8-a0d3-8ceeb1666d36', 'java-spring-boot', 'Java / Spring Boot', 80, 'proficient', '熟练', 'ecae2d77-5b15-5918-93d7-12ef0e0544db', 'coral', false, true, 0, '2026-08-11 14:45:20.156552+08', '2026-08-11 14:45:20.156552+08', NULL);
INSERT INTO public.skills (id, release_id, skill_key, name, percentage, level_code, level_text, icon_resource_id, bar_style, is_new, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('22789985-c0e7-4422-ac2d-c56c26fcbd82', 'f6d1b717-651e-48d8-a0d3-8ceeb1666d36', 'csharp-dotnet', 'C# / .NET', 80, 'proficient', '熟练', '106e0cb3-c95b-5d3f-96dd-997169ac6510', 'coral', false, true, 1, '2026-08-11 14:45:20.156552+08', '2026-08-11 14:45:20.156552+08', NULL);
INSERT INTO public.skills (id, release_id, skill_key, name, percentage, level_code, level_text, icon_resource_id, bar_style, is_new, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('0e0df5db-614e-4b5e-abdb-425348a5f901', 'f6d1b717-651e-48d8-a0d3-8ceeb1666d36', 'sql', 'MySQL', 70, 'competent', '掌握', '9b936246-207d-5841-a65b-a9f4b8d1aacb', 'teal', false, true, 2, '2026-08-11 14:45:20.156552+08', '2026-08-11 14:45:20.156552+08', NULL);
INSERT INTO public.skills (id, release_id, skill_key, name, percentage, level_code, level_text, icon_resource_id, bar_style, is_new, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('6ae47e56-6727-4361-bed4-e767a957f2b0', 'f6d1b717-651e-48d8-a0d3-8ceeb1666d36', 'docker', 'Docker', 70, 'competent', '掌握', '63df3f0a-bda9-5de0-a068-d77d75916815', 'teal', false, true, 3, '2026-08-11 14:45:20.156552+08', '2026-08-11 14:45:20.156552+08', NULL);
INSERT INTO public.skills (id, release_id, skill_key, name, percentage, level_code, level_text, icon_resource_id, bar_style, is_new, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('9b7a9531-b2df-4ca7-9545-7ed41ca17b7c', 'f6d1b717-651e-48d8-a0d3-8ceeb1666d36', 'javascript-typescript', 'JavaScript / TypeScript', 30, 'novice', '入门', 'c31fb74c-cd2a-52b2-bb35-e817627271f3', 'gray-white', false, true, 4, '2026-08-11 14:45:20.156552+08', '2026-08-11 14:45:20.156552+08', NULL);
INSERT INTO public.skills (id, release_id, skill_key, name, percentage, level_code, level_text, icon_resource_id, bar_style, is_new, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('33fbcd1e-7f29-4ff6-8456-6a50c07d94b9', 'f6d1b717-651e-48d8-a0d3-8ceeb1666d36', 'react-vue', 'React / Vue', 30, 'novice', '入门', '2bb66285-e985-5cf8-bdc8-0d77264a195f', 'gray-white', false, true, 5, '2026-08-11 14:45:20.156552+08', '2026-08-11 14:45:20.156552+08', NULL);
INSERT INTO public.skills (id, release_id, skill_key, name, percentage, level_code, level_text, icon_resource_id, bar_style, is_new, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('97fe561b-a156-409d-8398-7a32a3266908', 'f6d1b717-651e-48d8-a0d3-8ceeb1666d36', 'python', 'Python', 30, 'novice', '入门', '53587a9f-f3a2-584d-ae4e-a8c7a2fa62a3', 'gray-white', false, true, 6, '2026-08-11 14:45:20.156552+08', '2026-08-11 14:45:20.156552+08', NULL);

ALTER TABLE ONLY public.skills
    ADD CONSTRAINT skills_icon_resource_id_fkey FOREIGN KEY (icon_resource_id) REFERENCES public.resources(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.skills
    ADD CONSTRAINT skills_release_id_fkey FOREIGN KEY (release_id) REFERENCES public.content_releases(id) ON DELETE CASCADE;

-- ============================================================================
-- 19. vibe_tools（工具偏好）
-- ============================================================================

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

ALTER TABLE ONLY public.vibe_tools
    ADD CONSTRAINT vibe_tools_pkey PRIMARY KEY (id);

CREATE INDEX idx_vibe_tools_release_order ON public.vibe_tools USING btree (release_id, sort_order, tool_key) WHERE (deleted_at IS NULL);

CREATE UNIQUE INDEX uq_vibe_tools_release_key ON public.vibe_tools USING btree (release_id, tool_key) WHERE (deleted_at IS NULL);

-- vibe_tools 初始数据
INSERT INTO public.vibe_tools (id, release_id, tool_key, name, percentage, description, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('9c7561ae-e130-4b2a-98a6-3c2446c83866', 'e5d78b76-992b-4ed5-9b8c-9d187a628573', 'codex', 'Codex', 80, '代码编写主力，用户意图理解力强，用于执行关键任务', true, 0, '2026-08-13 13:32:14.22007+08', '2026-08-13 13:32:14.22007+08', NULL);
INSERT INTO public.vibe_tools (id, release_id, tool_key, name, percentage, description, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('6a5c82ce-72d1-4b12-9343-fda44d6efb47', 'e5d78b76-992b-4ed5-9b8c-9d187a628573', 'kimi', 'Kimi', 70, '代码编写辅助，codex的替补，执行相对边缘的任务', true, 1, '2026-08-13 13:32:14.22007+08', '2026-08-13 13:32:14.22007+08', NULL);
INSERT INTO public.vibe_tools (id, release_id, tool_key, name, percentage, description, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('ccef2b0a-cf35-4e66-ab37-219fa59b7654', 'e5d78b76-992b-4ed5-9b8c-9d187a628573', 'cursor', 'Cursor', 60, '前代码编写主力，目前用作项目分析，方案编写以及知识问答等', true, 2, '2026-08-13 13:32:14.22007+08', '2026-08-13 13:32:14.22007+08', NULL);
INSERT INTO public.vibe_tools (id, release_id, tool_key, name, percentage, description, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('94f0a311-53dd-40f9-822f-4123cdb19d42', 'e5d78b76-992b-4ed5-9b8c-9d187a628573', 'claude-code', 'Claude Code', 60, '前代码编写主力，生成代码质量高，执行复杂任务', true, 3, '2026-08-13 13:32:14.22007+08', '2026-08-13 13:32:14.22007+08', NULL);
INSERT INTO public.vibe_tools (id, release_id, tool_key, name, percentage, description, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('f3ee5ce8-3a21-4328-a833-135343e39faf', 'e5d78b76-992b-4ed5-9b8c-9d187a628573', 'deepseek', 'DeepSeek', 40, '作为国产模型探索，主要的API调用，以及一些日常辅助问答', true, 4, '2026-08-13 13:32:14.22007+08', '2026-08-13 13:32:14.22007+08', NULL);
INSERT INTO public.vibe_tools (id, release_id, tool_key, name, percentage, description, enabled, sort_order, created_at, updated_at, deleted_at) VALUES ('b38a7717-ce69-4484-8c46-3a3376bd0070', 'e5d78b76-992b-4ed5-9b8c-9d187a628573', 'chatgpt', 'ChatGPT', 20, '图片素材生成，以及日常辅助问答(暗黑版)', true, 5, '2026-08-13 13:32:14.22007+08', '2026-08-13 13:32:14.22007+08', NULL);

ALTER TABLE ONLY public.vibe_tools
    ADD CONSTRAINT vibe_tools_release_id_fkey FOREIGN KEY (release_id) REFERENCES public.content_releases(id) ON DELETE CASCADE;

-- ============================================================================
-- 20. mylab_engagement_stats（实验室互动统计）
-- ============================================================================

CREATE TABLE public.mylab_engagement_stats (
    post_key character varying(96) NOT NULL,
    view_count bigint DEFAULT 0 NOT NULL,
    like_count bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT mylab_engagement_stats_like_count_check CHECK ((like_count >= 0)),
    CONSTRAINT mylab_engagement_stats_view_count_check CHECK ((view_count >= 0))
);

ALTER TABLE ONLY public.mylab_engagement_stats
    ADD CONSTRAINT mylab_engagement_stats_pkey PRIMARY KEY (post_key);



-- mylab_engagement_stats 初始数据
INSERT INTO public.mylab_engagement_stats (post_key, view_count, like_count, created_at, updated_at) VALUES ('project-gm1', 8, 1, '2026-08-10 21:24:49.05799+08', '2026-08-13 20:39:14.881587+08');
INSERT INTO public.mylab_engagement_stats (post_key, view_count, like_count, created_at, updated_at) VALUES ('blog-docker-deploy', 1, 0, '2026-08-26 23:44:49.582671+08', '2026-08-26 23:44:49.582671+08');
INSERT INTO public.mylab_engagement_stats (post_key, view_count, like_count, created_at, updated_at) VALUES ('vue-composable-mouse-tilt', 1, 0, '2026-08-27 00:09:49.669141+08', '2026-08-27 00:09:49.669141+08');
INSERT INTO public.mylab_engagement_stats (post_key, view_count, like_count, created_at, updated_at) VALUES ('first-post', 1, 0, '2026-08-27 00:09:49.669141+08', '2026-08-27 00:09:49.669141+08');
INSERT INTO public.mylab_engagement_stats (post_key, view_count, like_count, created_at, updated_at) VALUES ('tailwind-migration', 1, 0, '2026-08-27 00:09:49.669141+08', '2026-08-27 00:09:49.669141+08');
INSERT INTO public.mylab_engagement_stats (post_key, view_count, like_count, created_at, updated_at) VALUES ('vue-gsap-hero', 1, 0, '2026-08-27 00:09:49.669141+08', '2026-08-27 00:09:49.669141+08');
INSERT INTO public.mylab_engagement_stats (post_key, view_count, like_count, created_at, updated_at) VALUES ('project-gm6', 1, 0, '2026-08-27 00:10:49.681238+08', '2026-08-27 00:10:49.681238+08');
INSERT INTO public.mylab_engagement_stats (post_key, view_count, like_count, created_at, updated_at) VALUES ('project-gm5', 1, 0, '2026-08-27 00:10:49.681238+08', '2026-08-27 00:10:49.681238+08');

-- ============================================================================
-- 21. site_daily_stats（站点每日统计）
-- ============================================================================

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

ALTER TABLE ONLY public.site_daily_stats
    ADD CONSTRAINT site_daily_stats_pkey PRIMARY KEY (stat_date);



-- site_daily_stats 初始数据
INSERT INTO public.site_daily_stats (stat_date, visit_count, view_count, like_count, created_at, updated_at) VALUES ('2026-08-10', 4, 3, 2, '2026-08-10 21:24:49.05799+08', '2026-08-10 21:57:09.469897+08');
INSERT INTO public.site_daily_stats (stat_date, visit_count, view_count, like_count, created_at, updated_at) VALUES ('2026-08-11', 8, 1, 0, '2026-08-11 14:29:31.228112+08', '2026-08-11 23:47:00.134519+08');
INSERT INTO public.site_daily_stats (stat_date, visit_count, view_count, like_count, created_at, updated_at) VALUES ('2026-08-12', 4, 2, 0, '2026-08-12 00:33:00.132072+08', '2026-08-12 22:18:18.630517+08');
INSERT INTO public.site_daily_stats (stat_date, visit_count, view_count, like_count, created_at, updated_at) VALUES ('2026-08-13', 4, 2, 0, '2026-08-13 13:11:43.973572+08', '2026-08-13 22:11:06.034628+08');
INSERT INTO public.site_daily_stats (stat_date, visit_count, view_count, like_count, created_at, updated_at) VALUES ('2026-08-14', 1, 0, 0, '2026-08-14 01:55:32.061146+08', '2026-08-14 01:55:32.061146+08');
INSERT INTO public.site_daily_stats (stat_date, visit_count, view_count, like_count, created_at, updated_at) VALUES ('2026-08-16', 4, 0, 0, '2026-08-16 20:12:30.596956+08', '2026-08-16 22:53:27.776964+08');
INSERT INTO public.site_daily_stats (stat_date, visit_count, view_count, like_count, created_at, updated_at) VALUES ('2026-08-17', 1, 0, 0, '2026-08-17 13:08:32.004659+08', '2026-08-17 13:08:32.004659+08');
INSERT INTO public.site_daily_stats (stat_date, visit_count, view_count, like_count, created_at, updated_at) VALUES ('2026-08-25', 1, 0, 0, '2026-08-25 19:23:59.898381+08', '2026-08-25 19:23:59.898381+08');
INSERT INTO public.site_daily_stats (stat_date, visit_count, view_count, like_count, created_at, updated_at) VALUES ('2026-08-26', 1, 1, 0, '2026-08-26 23:25:49.521815+08', '2026-08-26 23:44:49.582671+08');
INSERT INTO public.site_daily_stats (stat_date, visit_count, view_count, like_count, created_at, updated_at) VALUES ('2026-08-27', 0, 6, 0, '2026-08-27 00:09:49.669141+08', '2026-08-27 00:10:49.681238+08');

-- ============================================================================
-- 22. site_traffic_stats（站点流量统计）
-- ============================================================================

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

ALTER TABLE ONLY public.site_traffic_stats
    ADD CONSTRAINT site_traffic_stats_pkey PRIMARY KEY (id);



-- site_traffic_stats 初始数据
INSERT INTO public.site_traffic_stats (id, visit_count, total_view_count, total_like_count, created_at, updated_at) VALUES (1, 28, 15, 1, '2026-08-10 21:22:45.82724+08', '2026-08-27 00:10:49.681238+08');
