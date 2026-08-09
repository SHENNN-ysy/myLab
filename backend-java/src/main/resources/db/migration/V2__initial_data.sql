-- 开发环境初始数据以当前 myblog/admin 已确定内容为准。
-- UUID 使用 uuid_generate_v5 生成，重复执行时保持稳定。

CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO users (id, username, password_hash, role)
VALUES (
    uuid_generate_v5(uuid_ns_url(), 'myblog:user:admin'),
    'admin',
    crypt('Admin@123456', gen_salt('bf', 10)),
    'superadmin'
)
ON CONFLICT (username) DO NOTHING;

INSERT INTO resources (id, object_key, bucket, original_name, mime_type, size)
VALUES
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:hero-1'), '/assets/hero/hero-1.webp', 'local', 'hero-1.webp', 'image/webp', 0),
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:hero-2'), '/assets/hero/hero-2.webp', 'local', 'hero-2.webp', 'image/webp', 0),
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:hero-3'), '/assets/hero/hero-3.webp', 'local', 'hero-3.webp', 'image/webp', 0),
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:hero-4'), '/assets/hero/hero-4.webp', 'local', 'hero-4.webp', 'image/webp', 0),
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:hero-5'), '/assets/hero/hero-5.webp', 'local', 'hero-5.webp', 'image/webp', 0),
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:hero-6'), '/assets/hero/hero-6.webp', 'local', 'hero-6.webp', 'image/webp', 0),
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:avatar'), '/assets/avatar.png', 'local', 'avatar.png', 'image/png', 0),
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:skill-csharp-dotnet'), '/assets/skill-icons/csharp-dotnet.svg', 'local', 'csharp-dotnet.svg', 'image/svg+xml', 0),
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:skill-java-spring-boot'), '/assets/skill-icons/java-spring-boot.svg', 'local', 'java-spring-boot.svg', 'image/svg+xml', 0),
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:skill-docker'), '/assets/skill-icons/docker.svg', 'local', 'docker.svg', 'image/svg+xml', 0),
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:skill-sql'), '/assets/skill-icons/sql.svg', 'local', 'sql.svg', 'image/svg+xml', 0),
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:skill-javascript-typescript'), '/assets/skill-icons/javascript-typescript.svg', 'local', 'javascript-typescript.svg', 'image/svg+xml', 0),
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:skill-react-vue'), '/assets/skill-icons/react-vue.svg', 'local', 'react-vue.svg', 'image/svg+xml', 0),
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:skill-python'), '/assets/skill-icons/python.svg', 'local', 'python.svg', 'image/svg+xml', 0),
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:hobby-cs2'), '/game_posters/cs2.jpg', 'local', 'cs2.jpg', 'image/jpeg', 0),
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:hobby-apex'), '/game_posters/apex.jpg', 'local', 'apex.jpg', 'image/jpeg', 0),
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:hobby-delta-force'), '/game_posters/delta-force.jpg', 'local', 'delta-force.jpg', 'image/jpeg', 0),
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:hobby-valorant'), '/game_posters/the-finals.jpg', 'local', 'the-finals.jpg', 'image/jpeg', 0),
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:hobby-overwatch-2'), '/game_posters/overwatch2.jpeg', 'local', 'overwatch2.jpeg', 'image/jpeg', 0),
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:mylab-project-gm1'), '/mylab/project-gm1.md', 'local', 'project-gm1.md', 'text/markdown', 0),
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:mylab-project-gm2'), '/mylab/project-gm2.md', 'local', 'project-gm2.md', 'text/markdown', 0),
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:mylab-project-gm3'), '/mylab/project-gm3.md', 'local', 'project-gm3.md', 'text/markdown', 0),
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:mylab-project-gm4'), '/mylab/project-gm4.md', 'local', 'project-gm4.md', 'text/markdown', 0),
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:mylab-project-gm5'), '/mylab/project-gm5.md', 'local', 'project-gm5.md', 'text/markdown', 0),
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:mylab-project-gm6'), '/mylab/project-gm6.md', 'local', 'project-gm6.md', 'text/markdown', 0),
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:mylab-blog-docker-deploy'), '/mylab/blog-docker-deploy.md', 'local', 'blog-docker-deploy.md', 'text/markdown', 0),
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:mylab-vue-gsap-hero'), '/mylab/vue-gsap-hero.md', 'local', 'vue-gsap-hero.md', 'text/markdown', 0),
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:mylab-leetcode-binary-search'), '/mylab/leetcode-binary-search.md', 'local', 'leetcode-binary-search.md', 'text/markdown', 0),
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:mylab-tailwind-migration'), '/mylab/tailwind-migration.md', 'local', 'tailwind-migration.md', 'text/markdown', 0),
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:mylab-raspberry-pi-nas'), '/mylab/raspberry-pi-nas.md', 'local', 'raspberry-pi-nas.md', 'text/markdown', 0),
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:mylab-vue-composable-mouse-tilt'), '/mylab/vue-composable-mouse-tilt.md', 'local', 'vue-composable-mouse-tilt.md', 'text/markdown', 0),
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:mylab-leetcode-dp-notes'), '/mylab/leetcode-dp-notes.md', 'local', 'leetcode-dp-notes.md', 'text/markdown', 0),
    (uuid_generate_v5(uuid_ns_url(), 'myblog:resource:mylab-first-post'), '/mylab/first-post.md', 'local', 'first-post.md', 'text/markdown', 0)
ON CONFLICT (object_key) DO NOTHING;

INSERT INTO content_releases (id, module_key, version_no, state, published_by, published_at)
SELECT
    uuid_generate_v5(uuid_ns_url(), 'myblog:release:' || module_key || ':1'),
    module_key,
    1,
    'PUBLISHED',
    uuid_generate_v5(uuid_ns_url(), 'myblog:user:admin'),
    NOW()
FROM (VALUES ('home'), ('about'), ('skills'), ('footprints'), ('hobbies'), ('vibe'), ('mylab')) AS modules(module_key)
ON CONFLICT (module_key, version_no) DO NOTHING;

INSERT INTO home_images (id, release_id, image_resource_id, alt_text, object_position, sort_order)
SELECT
    uuid_generate_v5(uuid_ns_url(), 'myblog:home-image:' || item.key),
    uuid_generate_v5(uuid_ns_url(), 'myblog:release:home:1'),
    uuid_generate_v5(uuid_ns_url(), 'myblog:resource:' || item.key),
    item.alt_text,
    item.object_position,
    item.sort_order
FROM (VALUES
    ('hero-1', '香港太平山城市远景', '50% 35%', 0),
    ('hero-2', '蓝天下飞翔的海鸥', '50% 42%', 1),
    ('hero-3', '海面与云层', '50% 50%', 2),
    ('hero-4', '夜色城市灯光', '50% 45%', 3),
    ('hero-5', '落日晚霞山景', '50% 50%', 4),
    ('hero-6', '海边公路与云', '50% 50%', 5)
) AS item(key, alt_text, object_position, sort_order)
ON CONFLICT DO NOTHING;

INSERT INTO about_contents (
    id, release_id, profile_title, avatar_resource_id, avatar_alt, intro, outro,
    ingredients_title, ingredients_description
)
VALUES (
    uuid_generate_v5(uuid_ns_url(), 'myblog:about-content:1'),
    uuid_generate_v5(uuid_ns_url(), 'myblog:release:about:1'),
    '关于我',
    uuid_generate_v5(uuid_ns_url(), 'myblog:resource:avatar'),
    'DNSamuel',
    '你好，我是 SHENNN，目前专注于全栈开发、AI agent学习实践中...',
    '努力成长，希望成为一名AI超级个人，通过AI让生活变得更美好。',
    '我的成分',
    '之前有人想查我的成分，我认真的思考了一下，我的成分应该是这样，不过随时有可能会变就是啦'
)
ON CONFLICT DO NOTHING;

INSERT INTO about_profile_bullets (id, about_content_id, contents, sort_order)
SELECT uuid_generate_v5(uuid_ns_url(), 'myblog:about-bullet:' || sort_order),
       uuid_generate_v5(uuid_ns_url(), 'myblog:about-content:1'), contents, sort_order
FROM (VALUES
    ('上位机开发：C#/.NET，负责为实验室内若干智能装备进行上位机软件开发与维护', 0),
    ('web开发：Java/SpringBoot服务端，TypeScript/React前端，做些个人兴趣项目', 1),
    ('爱好自然观光、city walk，喜欢探索这个世界的美', 2)
) AS item(contents, sort_order)
ON CONFLICT DO NOTHING;

INSERT INTO about_bubbles (
    id, about_content_id, bubble_text, bubble_size, background_color, text_color, glow_color, sort_order
)
SELECT uuid_generate_v5(uuid_ns_url(), 'myblog:about-bubble:' || sort_order),
       uuid_generate_v5(uuid_ns_url(), 'myblog:about-content:1'), bubble_text, bubble_size,
       background_color, text_color, glow_color, sort_order
FROM (VALUES
    ('FPS牢玩家', 'big', '#FF6B6B', '#FF8A80', '#FF6B6B', 0),
    ('健身旅行者', 'big', '#2EC4B6', '#64FFDA', '#2EC4B6', 1),
    ('动物保护旅行者', 'big', '#66BB6A', '#81C784', '#66BB6A', 2),
    ('养老二次元', 'big', '#DB7093', '#F48FB1', '#DB7093', 3),
    ('游戏旅行者', 'big', '#FF8A65', '#FFAB91', '#FF8A65', 4),
    ('美食探索旅行者', 'mid', '#FF8A65', '#FFCCBC', '#FF8A65', 5),
    ('自然风光旅行者', 'mid', '#4CAF50', '#A5D6A7', '#4CAF50', 6),
    ('技术探索者', 'mid', '#5BA4E6', '#81D4FA', '#5BA4E6', 7),
    ('摄影旅行者', 'mid', '#FFB347', '#FFE082', '#FFB347', 8),
    ('city walk', 'mid', '#64B5F6', '#90CAF9', '#64B5F6', 9),
    ('电动版骑行爱好者', 'mid', '#66BB6A', '#A5D6A7', '#66BB6A', 10),
    ('吃瓜旅行者', 'mid', '#AB47BC', '#CE93D8', '#AB47BC', 11),
    ('代码强迫症', 'mid', '#26A69A', '#80CBC4', '#26A69A', 12),
    ('AI大人的爱徒', 'mid', '#00BCD4', '#4DD0E1', '#00BCD4', 13)
) AS item(bubble_text, bubble_size, background_color, text_color, glow_color, sort_order)
ON CONFLICT DO NOTHING;

INSERT INTO skills (
    id, release_id, skill_key, name, percentage, level_code, level_text,
    icon_resource_id, bar_style, is_new, enabled, sort_order
)
SELECT uuid_generate_v5(uuid_ns_url(), 'myblog:skill:' || skill_key),
       uuid_generate_v5(uuid_ns_url(), 'myblog:release:skills:1'),
       skill_key, name, percentage, level_code, level_text,
       uuid_generate_v5(uuid_ns_url(), 'myblog:resource:skill-' || skill_key),
       bar_style, is_new, TRUE, sort_order
FROM (VALUES
    ('csharp-dotnet', 'C# / .NET', 80, 'proficient', '熟练', 'coral', FALSE, 0),
    ('java-spring-boot', 'Java / Spring Boot', 80, 'proficient', '熟练', 'coral', FALSE, 1),
    ('docker', 'Docker', 70, 'competent', '熟练', 'teal', FALSE, 2),
    ('sql', 'SQL', 70, 'competent', '熟练', 'teal', FALSE, 3),
    ('javascript-typescript', 'JavaScript / TypeScript', 30, 'novice', '入门', 'coral', TRUE, 4),
    ('react-vue', 'React / Vue', 30, 'novice', '入门', 'coral', TRUE, 5),
    ('python', 'Python', 30, 'novice', '入门', 'coral', TRUE, 6)
) AS item(skill_key, name, percentage, level_code, level_text, bar_style, is_new, sort_order)
ON CONFLICT DO NOTHING;

INSERT INTO footprints (id, release_id, city_key, title, summary, contents, enabled, sort_order)
SELECT uuid_generate_v5(uuid_ns_url(), 'myblog:footprint:' || city_key),
       uuid_generate_v5(uuid_ns_url(), 'myblog:release:footprints:1'),
       city_key, title, summary, contents, TRUE, sort_order
FROM (VALUES
    ('photo', '胶片摄影 · 西安城墙', '一台 Nikon FM2，几卷 Portra 400，和一段厚重的古城墙。', E'西安是我拍胶片最密集的城市。古城墙是天然的引导线，傍晚时分，金色的光沿着砖缝流下来。\n\n我喜欢在钟楼附近反复行走，让人流、车流和老建筑在取景框里形成自己的节奏。\n\n胶片摄影对我来说不是怀旧，而是一种慢下来的观察方式。', 0),
    ('hike', '徒步 · 昆明 · 高海拔', '用脚步丈量高原，不是征服，是学会在稀薄空气里找到自己的节奏。', E'昆明周边的山路让我重新理解了“距离”这件事：地图上的短线，走起来常常是完整的一天。\n\n我喜欢徒步里那种简单的判断：补水、节奏、天气、脚下的路，每一件都真实具体。\n\n最美的风景往往不在终点，而在“再坚持一下”之后的转角。', 1),
    ('coffee', '精品咖啡 · 上海武康路', '从豆子到杯子，一杯咖啡是一段小型的时间旅行。', E'武康路是我在上海很喜欢的一段路。梧桐树影把阳光切成碎片，几家小店藏在老房子里。\n\n咖啡对我来说是一种准时开始工作的仪式，不是醒神，而是给一天一个锚点。\n\n我更在意一杯咖啡背后的风味描述、产地故事，以及它被认真对待的方式。', 2),
    ('travel', '城市漫游 · 广州西关', '不急着去景点，只在陌生城市的街区里游荡几个小时。', E'西关是广州老城里很迷人的一片：骑楼街、麻石巷、满洲窗，还有街坊聊天的声音。\n\n我喜欢在这样的地方慢慢走，听街边的生活声，闻别人家的饭菜香。\n\n城市漫游训练我对偶然的开放度：走错路，才更容易遇到没有被攻略写过的惊喜。', 3),
    ('music', '黑胶与合成器 · 深圳 OCT', '一种回放时间，一种创造时间，它们都让我暂时离开屏幕。', E'深圳的创意园区里有几家独立唱片店，是我固定会去的地方。\n\n合成器是近几年新开的坑。把一个 pad 音色调出层次，本身就是一次小创作。\n\n音乐对我而言是不被语言打扰的时间。项目做累了，切到 DAW 里乱按二十分钟，也是一种恢复。', 4),
    ('read', '独立书店 · 北京', '认识一座城市，最慢也最可靠的方式，是在它的书店里坐一个下午。', E'北京有几条书店密度很高的街区，我喜欢把它们当作城市里的临时工作台。\n\n我常常在独立书店里不急着买东西，只是翻完一本诗集，再翻完一本地理散文。\n\n比起连锁书店，独立书店更像私人策展，选品本身就是一种表达。', 5)
) AS item(city_key, title, summary, contents, sort_order)
ON CONFLICT DO NOTHING;

INSERT INTO hobbies (id, release_id, hobby_key, title, description, enabled, sort_order)
SELECT uuid_generate_v5(uuid_ns_url(), 'myblog:hobby:' || hobby_key),
       uuid_generate_v5(uuid_ns_url(), 'myblog:release:hobbies:1'),
       hobby_key, title, description, TRUE, sort_order
FROM (VALUES
    ('counter-strike-2', 'Counter-Strike 2', '最喜欢它纯粹又残酷的博弈感，每一颗道具、每一次peek都要为团队节奏负责。', 0),
    ('apex', 'Apex 英雄', '机动性和临场决策很迷人，打赢一波混战时会有非常强的爽感和节奏感。', 1),
    ('delta-force', '三角洲行动', '偏战术、偏压迫的枪线体验，适合认真研究路线、信息和团队配合。', 2),
    ('valorant', '无畏契约', '技能和枪法互相牵制，回合制的紧张感很足，残局尤其容易让人上头。', 3),
    ('overwatch-2', '守望先锋 2', '英雄机制和团战节奏变化很快，最吸引我的是团队位置和技能交换。', 4)
) AS item(hobby_key, title, description, sort_order)
ON CONFLICT DO NOTHING;

INSERT INTO hobby_resources (id, hobby_id, resource_id)
SELECT uuid_generate_v5(uuid_ns_url(), 'myblog:hobby-resource:' || hobby_key),
       uuid_generate_v5(uuid_ns_url(), 'myblog:hobby:' || hobby_key),
       uuid_generate_v5(uuid_ns_url(), 'myblog:resource:hobby-' || hobby_key)
FROM (VALUES ('counter-strike-2'), ('apex'), ('delta-force'), ('valorant'), ('overwatch-2')) AS item(hobby_key)
ON CONFLICT DO NOTHING;

INSERT INTO hobby_time_tags (id, release_id, data_key, name, color, label_x, label_y, label_scale, enabled, sort_order)
SELECT uuid_generate_v5(uuid_ns_url(), 'myblog:hobby-time-tag:' || data_key),
       uuid_generate_v5(uuid_ns_url(), 'myblog:release:hobbies:1'),
       data_key, name, color, label_x, label_y, label_scale, TRUE, sort_order
FROM (VALUES
    ('Study', 'Study', '#93C5FD', 110, 240, 1.5, 0),
    ('Music', 'Music', '#7DD3FC', 410, 232, 1.3, 1),
    ('Game', 'Game', '#67E8F9', 195, 150, 1.5, 2),
    ('Coding', 'Coding', '#5EEAD4', 340, 110, 1.5, 3),
    ('Social', 'Social or Family', '#6EE7B7', 63, 65, 1.5, 4)
) AS item(data_key, name, color, label_x, label_y, label_scale, sort_order)
ON CONFLICT DO NOTHING;

INSERT INTO hobby_time_points (id, release_id, age, study, music, game, coding, social)
SELECT uuid_generate_v5(uuid_ns_url(), 'myblog:hobby-time-point:' || age),
       uuid_generate_v5(uuid_ns_url(), 'myblog:release:hobbies:1'),
       age, study, music, game, coding, social
FROM (VALUES
    (-1,0,0,0,0,10),(0,0,0,0,0,10),(1,1,0,0,0,9),(2,2,0,0,0,8),(3,3,0,0,0,7),
    (4,4,0,0,0,6),(5,5,0,0,0,5),(6,6,0,0,0,4),(7,5.3,0,1,0,3.7),(8,4.7,0,2,0,3.3),
    (9,4,0,3,0,3),(10,3.9,0,2.9,0.3,2.9),(11,3.8,0,2.8,0.7,2.7),(12,3.7,0,2.7,1,2.6),
    (13,3.6,0,2.6,1.3,2.5),(14,3.4,0,2.4,1.7,2.5),(15,3.3,0,2.3,2,2.4),(16,3.2,0,2.2,2.3,2.3),
    (17,3.1,0,2.1,2.7,2.1),(18,3,0,2,3,2),(19,2.8,0.2,2,3,2),(20,2.6,0.4,2,3,2),
    (21,2.4,0.6,2,3,2),(22,2.2,0.8,2,3,2),(23,2,1,2,3,2),(24,2,1,2,3,2),(25,2,1,2,3,2),
    (26,2.5,0.5,1.5,3.5,2),(27,3,0,1,4,2)
) AS item(age, study, music, game, coding, social)
ON CONFLICT DO NOTHING;

INSERT INTO vibe_tools (id, release_id, tool_key, name, percentage, description, enabled, sort_order)
SELECT uuid_generate_v5(uuid_ns_url(), 'myblog:vibe:' || tool_key),
       uuid_generate_v5(uuid_ns_url(), 'myblog:release:vibe:1'),
       tool_key, name, percentage, description, TRUE, sort_order
FROM (VALUES
    ('cursor', 'Cursor', 80, '代码编写主力，执行明确任务，性价比高', 0),
    ('codex', 'Codex', 80, '代码编写主力，用户意图理解力强，执行需求模糊的任务', 1),
    ('claude-code', 'Claude Code', 60, '代码编写辅助，生成代码质量高，执行复杂任务', 2),
    ('kimi', 'Kimi', 60, '我最初使用的AI工具，目前作为日常辅助问答以及API调用', 3),
    ('deepseek', 'DeepSeek', 40, '有时疑似被Kimi拉黑，作为国产模型探索以及kimi的替代', 4),
    ('chatgpt', 'ChatGPT', 20, '图片素材生成，以及日常辅助问答(暗黑版)', 5)
) AS item(tool_key, name, percentage, description, sort_order)
ON CONFLICT DO NOTHING;

INSERT INTO mylab_tags (id, tag_key, name, enabled, sort_order)
SELECT uuid_generate_v5(uuid_ns_url(), 'myblog:mylab-tag:' || tag_key), tag_key, name, TRUE, sort_order
FROM (VALUES
    ('gamejam','GameJam',0),('unity','Unity',1),('csharp','C#',2),('aseprite','Aseprite',3),
    ('godot','Godot',4),('gdscript','GDScript',5),('phaser','Phaser',6),('javascript','JavaScript',7),
    ('commercial-project','商业项目',8),('unreal-engine','Unreal Engine',9),('cpp','C++',10),('lua','Lua',11),
    ('indie-tool','独立工具',12),('react','React',13),('typescript','TypeScript',14),('supabase','Supabase',15),
    ('web-lab','Web 实验',16),('vue','Vue',17),('web-audio-api','Web Audio API',18),('tone-js','Tone.js',19),
    ('docker','Docker',20),('nginx','Nginx',21),('ops','运维',22),('gsap','GSAP',23),('frontend','前端',24),
    ('leetcode','Leetcode',25),('algorithm','算法',26),('tailwind','Tailwind',27),('engineering','工程化',28),
    ('raspberry-pi','树莓派',29),('hardware','硬件',30),('dp','DP',31),('essay','随笔',32)
) AS item(tag_key, name, sort_order)
ON CONFLICT DO NOTHING;

INSERT INTO mylab_cards (
    id, release_id, post_key, card_title, card_summary, post_date, enabled, sort_order,
    card_type, project_show_order, project_contents
)
SELECT uuid_generate_v5(uuid_ns_url(), 'myblog:mylab-card:' || post_key),
       uuid_generate_v5(uuid_ns_url(), 'myblog:release:mylab:1'),
       post_key, card_title, card_summary, post_date::date, TRUE, sort_order,
       card_type, project_show_order, project_contents
FROM (VALUES
    ('project-gm1','Moth and Bat：项目研究记录','48 小时 GameJam 作品，关于夜色中两种生物的相会。','2024-01-01','PROJECT',0,E'这是一款关于夜晚相遇的解谜游戏。\n\nUnity、C#、Aseprite',0),
    ('project-gm2','Naughty Cat：项目研究记录','一只总想搞破坏的猫与一个不肯关机的扫地机器人。','2023-01-01','PROJECT',1,E'一款轻松幽默的平台跳跃游戏。\n\nGodot、GDScript',1),
    ('project-gm3','Naughty Boy：项目研究记录','规则与违抗之间的游戏化实验，关于儿童行为心理学的隐喻。','2023-01-01','PROJECT',2,E'探索规则边界的叙事游戏。\n\nPhaser、JavaScript',2),
    ('project-gm4','Ring of Elysium：项目研究记录','参与腾讯北极光工作室《无限法则》的玩法与系统设计。','2022-01-01','PROJECT',3,E'作为玩法设计师参与开发的大逃杀游戏。\n\nUnreal Engine、C++、Lua',3),
    ('project-gm5','Moodlog：项目研究记录','一个极简的情绪记录工具，专注输入体验与一年后的回看。','2024-01-01','PROJECT',4,E'帮助你记录情绪变化的日常工具。\n\nReact、TypeScript、Supabase',4),
    ('project-gm6','Beat Lab：项目研究记录','浏览器内的鼓机与音序器，使用 Web Audio API 实时合成。','2023-01-01','PROJECT',5,E'在线音乐创作工具。\n\nVue、Web Audio API、Tone.js',5),
    ('blog-docker-deploy','个人博客 Docker + Nginx 部署全流程记录','从 Dockerfile 多阶段构建到 nginx SPA 回退与 gzip 配置，把博客塞进容器的完整折腾过程。','2026-07-28','ARTICLE',NULL,NULL,6),
    ('vue-gsap-hero','用 GSAP 给首页 Hero 做电影感动效','ScrollTrigger 驱动的滚动叙事：分镜、视差与滚动提示文字的入场编排。','2026-07-15','ARTICLE',NULL,NULL,7),
    ('leetcode-binary-search','二分查找的几种边界写法整理','闭区间 / 左闭右开两种模板的循环不变量对比，附几道经典题的应用。','2026-06-30','ARTICLE',NULL,NULL,8),
    ('tailwind-migration','项目迁移 Tailwind CSS v4 的坑','v4 改为 CSS-first 配置后，postcss 插件与 @theme 写法的迁移笔记。','2026-06-12','ARTICLE',NULL,NULL,9),
    ('raspberry-pi-nas','树莓派搭家用 NAS：Samba 与硬盘休眠','Samba 共享配置、挂载点权限，以及 hdparm 让闲置硬盘自动休眠省电。','2026-05-20','ARTICLE',NULL,NULL,10),
    ('vue-composable-mouse-tilt','封装一个 useMouseTilt 组合式函数','用 requestAnimationFrame 节流鼠标事件，给卡片做跟随视角的 3D 倾斜。','2026-05-06','ARTICLE',NULL,NULL,11),
    ('leetcode-dp-notes','动态规划刷题小结：从背包到区间 DP','状态定义优先还是转移优先？整理了自己刷 DP 题时的思考 checklist。','2026-04-18','ARTICLE',NULL,NULL,12),
    ('first-post','MyLab 开张：为什么单独开一个实验记录页','项目展示放在首页，零散的学习与折腾记录集中收在这里，方便检索与回顾。','2026-04-01','ARTICLE',NULL,NULL,13)
) AS item(post_key, card_title, card_summary, post_date, card_type, project_show_order, project_contents, sort_order)
ON CONFLICT DO NOTHING;

INSERT INTO mylab_card_tags (id, card_id, tag_id, sort_order)
SELECT uuid_generate_v5(uuid_ns_url(), 'myblog:mylab-card-tag:' || post_key || ':' || tag_key),
       uuid_generate_v5(uuid_ns_url(), 'myblog:mylab-card:' || post_key),
       uuid_generate_v5(uuid_ns_url(), 'myblog:mylab-tag:' || tag_key),
       sort_order
FROM (VALUES
    ('project-gm1','gamejam',0),('project-gm1','unity',1),('project-gm1','csharp',2),('project-gm1','aseprite',3),
    ('project-gm2','gamejam',0),('project-gm2','godot',1),('project-gm2','gdscript',2),
    ('project-gm3','gamejam',0),('project-gm3','phaser',1),('project-gm3','javascript',2),
    ('project-gm4','commercial-project',0),('project-gm4','unreal-engine',1),('project-gm4','cpp',2),('project-gm4','lua',3),
    ('project-gm5','indie-tool',0),('project-gm5','react',1),('project-gm5','typescript',2),('project-gm5','supabase',3),
    ('project-gm6','web-lab',0),('project-gm6','vue',1),('project-gm6','web-audio-api',2),('project-gm6','tone-js',3),
    ('blog-docker-deploy','docker',0),('blog-docker-deploy','nginx',1),('blog-docker-deploy','ops',2),
    ('vue-gsap-hero','vue',0),('vue-gsap-hero','gsap',1),('vue-gsap-hero','frontend',2),
    ('leetcode-binary-search','leetcode',0),('leetcode-binary-search','algorithm',1),
    ('tailwind-migration','tailwind',0),('tailwind-migration','frontend',1),('tailwind-migration','engineering',2),
    ('raspberry-pi-nas','raspberry-pi',0),('raspberry-pi-nas','ops',1),('raspberry-pi-nas','hardware',2),
    ('vue-composable-mouse-tilt','vue',0),('vue-composable-mouse-tilt','frontend',1),
    ('leetcode-dp-notes','leetcode',0),('leetcode-dp-notes','algorithm',1),('leetcode-dp-notes','dp',2),
    ('first-post','essay',0)
) AS item(post_key, tag_key, sort_order)
ON CONFLICT DO NOTHING;

INSERT INTO mylab_resources (id, card_id, image_resource_id, content_resource_id)
SELECT uuid_generate_v5(uuid_ns_url(), 'myblog:mylab-resource:' || post_key),
       uuid_generate_v5(uuid_ns_url(), 'myblog:mylab-card:' || post_key),
       uuid_generate_v5(uuid_ns_url(), 'myblog:resource:hero-' || ((sort_order % 6) + 1)),
       uuid_generate_v5(uuid_ns_url(), 'myblog:resource:mylab-' || post_key)
FROM (VALUES
    ('project-gm1',0),('project-gm2',1),('project-gm3',2),('project-gm4',3),('project-gm5',4),('project-gm6',5),
    ('blog-docker-deploy',6),('vue-gsap-hero',7),('leetcode-binary-search',8),('tailwind-migration',9),
    ('raspberry-pi-nas',10),('vue-composable-mouse-tilt',11),('leetcode-dp-notes',12),('first-post',13)
) AS item(post_key, sort_order)
ON CONFLICT DO NOTHING;
