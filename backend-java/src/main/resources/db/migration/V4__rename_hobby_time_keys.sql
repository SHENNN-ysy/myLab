-- 将 hobbies 时间分布图的五个数据键由英文改为「爱好1~爱好5」，保留既有数据：
-- Study→爱好1、Music→爱好2、Game→爱好3、Coding→爱好4、Social→爱好5。
-- hobby_time_points 的数值落在固定列（study/music/game/coding/social），无需迁移。
-- 对全新安装（V1/V2 已是新键）本脚本为空操作，可安全重复执行。
BEGIN;

ALTER TABLE hobby_time_tags DROP CONSTRAINT IF EXISTS hobby_time_tags_data_key_check;

UPDATE hobby_time_tags
SET data_key = CASE data_key
    WHEN 'Study'  THEN '爱好1'
    WHEN 'Music'  THEN '爱好2'
    WHEN 'Game'   THEN '爱好3'
    WHEN 'Coding' THEN '爱好4'
    WHEN 'Social' THEN '爱好5'
    ELSE data_key
END;

ALTER TABLE hobby_time_tags
    ADD CONSTRAINT hobby_time_tags_data_key_check
    CHECK (data_key IN ('爱好1', '爱好2', '爱好3', '爱好4', '爱好5'));

COMMIT;
