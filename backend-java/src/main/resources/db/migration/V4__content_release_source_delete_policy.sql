-- 恢复历史版本为草稿后，放弃该草稿时允许清空其他版本的溯源指针。

ALTER TABLE public.content_releases
    DROP CONSTRAINT content_releases_source_release_id_fkey,
    ADD CONSTRAINT content_releases_source_release_id_fkey
        FOREIGN KEY (source_release_id) REFERENCES public.content_releases(id) ON DELETE SET NULL;
