DO $$
DECLARE
    table_count INTEGER;
    module_count INTEGER;
    publication_count INTEGER;
    module_keys TEXT[];
BEGIN
    SELECT COUNT(*) INTO table_count
    FROM information_schema.tables
    WHERE table_schema = 'public' AND table_type = 'BASE TABLE';

    SELECT COUNT(*), ARRAY_AGG(module_key ORDER BY module_key)
    INTO module_count, module_keys
    FROM content_modules;

    SELECT COUNT(*) INTO publication_count
    FROM content_publications;

    IF table_count <> 5 THEN
        RAISE EXCEPTION 'expected 5 business tables, got %', table_count;
    END IF;
    IF module_count <> 7 OR module_keys <> ARRAY['footprints','hobbies','mylab','projects','skills','support','vibe'] THEN
        RAISE EXCEPTION 'expected seven content modules, got %', module_keys;
    END IF;
    IF publication_count <> 7 THEN
        RAISE EXCEPTION 'expected 7 initial publications, got %', publication_count;
    END IF;
    IF EXISTS (
        SELECT 1
        FROM content_modules
        WHERE module_key = 'about'
           OR draft_data ?| ARRAY['title','highlight','description','profile','ingredients','points','panel_title','current_location']
    ) THEN
        RAISE EXCEPTION 'baseline contains a removed module or unmanaged page fields';
    END IF;
END $$;

SELECT
    (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE') AS business_tables,
    (SELECT COUNT(*) FROM content_modules) AS content_modules,
    (SELECT COUNT(*) FROM content_publications) AS initial_publications;
