-- 将 hobby_time_points 的五个数值列由英文改为「爱好1~爱好5」，与数据键命名对齐：
-- study→爱好1、music→爱好2、game→爱好3、coding→爱好4、social→爱好5。
-- RENAME COLUMN 会自动更新 ck_hobby_time_points_total 等约束表达式，数据不受影响。
-- 仅当旧列存在时执行，全新安装（V1 已是新列名）为空操作，可安全重复执行。
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'hobby_time_points' AND column_name = 'study'
    ) THEN
        ALTER TABLE hobby_time_points RENAME COLUMN study  TO "爱好1";
        ALTER TABLE hobby_time_points RENAME COLUMN music  TO "爱好2";
        ALTER TABLE hobby_time_points RENAME COLUMN game   TO "爱好3";
        ALTER TABLE hobby_time_points RENAME COLUMN coding TO "爱好4";
        ALTER TABLE hobby_time_points RENAME COLUMN social TO "爱好5";
    END IF;
END $$;
