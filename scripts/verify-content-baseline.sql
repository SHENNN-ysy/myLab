DO $$
DECLARE
    business_table_count INTEGER;
    release_count INTEGER;
    tag_count INTEGER;
    module_keys TEXT[];
BEGIN
    SELECT COUNT(*) INTO business_table_count
    FROM information_schema.tables
    WHERE table_schema = 'public'
      AND table_type = 'BASE TABLE'
      AND table_name IN (
          'users', 'resources', 'content_releases', 'footprints', 'hobbies', 'skills', 'vibe_tools',
          'mylab_tags', 'mylab_cards', 'mylab_card_tags', 'footprint_resources', 'hobby_resources', 'mylab_resources'
      );

    SELECT COUNT(*), ARRAY_AGG(module_key ORDER BY module_key)
    INTO release_count, module_keys
    FROM content_releases
    WHERE state = 'DRAFT' AND version_no = 1;

    SELECT COUNT(*) INTO tag_count FROM mylab_tags WHERE deleted_at IS NULL;

    IF business_table_count <> 13 THEN
        RAISE EXCEPTION 'expected 13 business tables, got %', business_table_count;
    END IF;
    IF release_count <> 5 OR module_keys <> ARRAY['footprints','hobbies','mylab','skills','vibe'] THEN
        RAISE EXCEPTION 'expected five initial draft releases, got %', module_keys;
    END IF;
    IF tag_count <> 5 THEN
        RAISE EXCEPTION 'expected five initial MyLab tags, got %', tag_count;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'mylab_card_tags'
    ) THEN
        RAISE EXCEPTION 'mylab_card_tags table is missing';
    END IF;
    IF (
        SELECT COUNT(*) FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'mylab_card_tags'
          AND column_name IN ('id', 'card_id', 'tag_id', 'sort_order', 'created_at', 'updated_at', 'deleted_at')
    ) <> 7 THEN
        RAISE EXCEPTION 'mylab_card_tags management columns are incomplete';
    END IF;
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'mylab_cards' AND column_name = 'tag_ids'
    ) THEN
        RAISE EXCEPTION 'legacy mylab_cards.tag_ids column still exists';
    END IF;
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name IN ('visit_logs', 'support_stats', 'content_modules', 'content_publications', 'projects')
    ) THEN
        RAISE EXCEPTION 'removed legacy tables still exist';
    END IF;
END $$;

SELECT
    (SELECT COUNT(*) FROM information_schema.tables
     WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
       AND table_name IN (
           'users', 'resources', 'content_releases', 'footprints', 'hobbies', 'skills', 'vibe_tools',
           'mylab_tags', 'mylab_cards', 'mylab_card_tags', 'footprint_resources', 'hobby_resources', 'mylab_resources'
       )) AS business_tables,
    (SELECT COUNT(*) FROM content_releases) AS initial_releases,
    (SELECT COUNT(*) FROM mylab_tags WHERE deleted_at IS NULL) AS initial_tags;
