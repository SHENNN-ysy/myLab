-- 为内容版本补充可读名称和描述，并允许未发布草稿在被替换后归档。

ALTER TABLE public.content_releases
    ADD COLUMN version_name character varying(120),
    ADD COLUMN version_description text;

UPDATE public.content_releases
SET version_name = CASE state
        WHEN 'DRAFT' THEN '草稿版本 ' || version_no
        ELSE '发布版本 ' || version_no
    END,
    version_description = CASE state
        WHEN 'DRAFT' THEN '迁移前保存的草稿版本'
        ELSE '迁移前创建的内容版本'
    END;

ALTER TABLE public.content_releases
    ALTER COLUMN version_name SET NOT NULL,
    ALTER COLUMN version_description SET NOT NULL,
    DROP CONSTRAINT ck_content_release_publication,
    ADD CONSTRAINT ck_content_release_publication CHECK (
        state IN ('DRAFT', 'ARCHIVED')
        OR (published_by IS NOT NULL AND published_at IS NOT NULL)
    ),
    ADD CONSTRAINT ck_content_release_version_name CHECK (
        length(btrim(version_name)) BETWEEN 1 AND 120
    ),
    ADD CONSTRAINT ck_content_release_version_description CHECK (
        length(btrim(version_description)) BETWEEN 1 AND 2000
    );

ALTER TABLE public.content_releases
    DROP CONSTRAINT content_releases_source_release_id_fkey,
    ADD CONSTRAINT content_releases_source_release_id_fkey
        FOREIGN KEY (source_release_id) REFERENCES public.content_releases(id) ON DELETE SET NULL;
