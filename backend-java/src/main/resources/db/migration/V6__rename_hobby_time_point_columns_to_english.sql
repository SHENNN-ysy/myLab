-- 将 hobby_time_points 的五个数值列由中文「爱好1~爱好5」改为英文 hobby1~hobby5。
-- 数据库列名统一英文风格；JSON 数据键（爱好1~爱好5）属于 API 契约，不受影响。
-- RENAME COLUMN 会自动更新 ck_hobby_time_points_total 等约束表达式，数据不受影响。
-- 仅当中文旧列存在时执行，重复执行为空操作。
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'hobby_time_points' AND column_name = '爱好1'
    ) THEN
        ALTER TABLE hobby_time_points RENAME COLUMN "爱好1" TO hobby1;
        ALTER TABLE hobby_time_points RENAME COLUMN "爱好2" TO hobby2;
        ALTER TABLE hobby_time_points RENAME COLUMN "爱好3" TO hobby3;
        ALTER TABLE hobby_time_points RENAME COLUMN "爱好4" TO hobby4;
        ALTER TABLE hobby_time_points RENAME COLUMN "爱好5" TO hobby5;
    END IF;
END $$;
