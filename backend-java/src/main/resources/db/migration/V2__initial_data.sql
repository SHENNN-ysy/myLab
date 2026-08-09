-- 五个模块各自从版本 1 开始。业务内容由后台创建草稿后维护。
INSERT INTO content_releases (id, module_key, version_no, state, published_by, published_at)
SELECT uuid_generate_v4(), module_key, 1, 'DRAFT', NULL, NULL
FROM (VALUES
    ('footprints'),
    ('hobbies'),
    ('mylab'),
    ('skills'),
    ('vibe')
) AS modules(module_key)
ON CONFLICT (module_key, version_no) DO NOTHING;

-- 全局标签不参与内容版本管理。
INSERT INTO mylab_tags (tag_key, name, sort_order)
VALUES
    ('java', 'Java', 10),
    ('vue', 'Vue', 20),
    ('typescript', 'TypeScript', 30),
    ('docker', 'Docker', 40),
    ('algorithm', '算法', 50)
ON CONFLICT DO NOTHING;
