-- 初始化内容模块基础记录。除 support 外，其余模块将在下方填充集合数据后统一发布。
INSERT INTO content_modules (module_key, draft_data)
VALUES
    ('skills', '{"items":[]}'::jsonb),
    ('projects', '{"items":[]}'::jsonb),
    ('footprints', '{"details":[]}'::jsonb),
    ('hobbies', '{"cards":[]}'::jsonb),
    ('vibe', '{"tools":[]}'::jsonb),
    ('mylab', '{"tags":[],"posts":[]}'::jsonb)
ON CONFLICT (module_key) DO NOTHING;

INSERT INTO content_modules (
    module_key,
    draft_data,
    published_data,
    published_version,
    status,
    published_at
)
VALUES (
    'support',
    '{"visit_base":12847,"like_count":1023,"page_view_base":68921}'::jsonb,
    '{"visit_base":12847,"like_count":1023,"page_view_base":68921}'::jsonb,
    1,
    'published',
    NOW()
)
ON CONFLICT (module_key) DO NOTHING;

-- 填充当前博客前台需要远程维护的集合数据。

UPDATE content_modules
SET draft_data = $content$
{"items":[{"id":"skill-1","name":"C# / .NET","percentage":80,"level":"proficient","level_text":"熟练","icon":"grid","bar_style":"coral","is_new":false,"enabled":true},{"id":"skill-2","name":"Java / Spring Boot","percentage":80,"level":"proficient","level_text":"熟练","icon":"server","bar_style":"coral","is_new":false,"enabled":true},{"id":"skill-3","name":"Docker","percentage":70,"level":"competent","level_text":"熟练","icon":"box","bar_style":"teal","is_new":false,"enabled":true},{"id":"skill-4","name":"SQL","percentage":70,"level":"competent","level_text":"熟练","icon":"shield","bar_style":"teal","is_new":false,"enabled":true},{"id":"skill-5","name":"JavaScript / TypeScript","percentage":30,"level":"novice","level_text":"入门","icon":"terminal","bar_style":"teal","is_new":false,"enabled":true},{"id":"skill-6","name":"React / Vue","percentage":30,"level":"novice","level_text":"入门","icon":"smartphone","bar_style":"teal","is_new":false,"enabled":true},{"id":"skill-7","name":"Python","percentage":30,"level":"novice","level_text":"入门","icon":"pen","bar_style":"teal","is_new":false,"enabled":true}]}
$content$::jsonb, updated_at = NOW()
WHERE module_key = 'skills'
  AND jsonb_typeof(draft_data->'items') = 'array'
  AND jsonb_array_length(draft_data->'items') = 0;

UPDATE content_modules
SET draft_data = $content$
{"items":[{"id":"gm1","card_title":"Moth and Bat","card_summary":"48 小时 GameJam 作品，关于夜色中两种生物的相会。","detail_title":"Moth and Bat","detail_summary":"48 小时 GameJam 作品，关于夜色中两种生物的相会。","tag":"GameJam","accent":false,"year":2024,"image":"https://picsum.photos/seed/gm1/600/375","image_alt":"Moth and Bat","paragraphs":["这是一款关于夜晚相遇的解谜游戏。玩家扮演一只飞蛾，在月光下寻找答案。"],"tech":["Unity","C#","Aseprite"],"images":[],"lab_post_id":"project-gm1","enabled":true},{"id":"gm2","card_title":"Naughty Cat","card_summary":"一只总想搞破坏的猫与一个不肯关机的扫地机器人。","detail_title":"Naughty Cat","detail_summary":"一只总想搞破坏的猫与一个不肯关机的扫地机器人。","tag":"GameJam","accent":false,"year":2023,"image":"https://picsum.photos/seed/gm2/600/375","image_alt":"Naughty Cat","paragraphs":["一款轻松幽默的平台跳跃游戏。"],"tech":["Godot","GDScript"],"images":[],"lab_post_id":"project-gm2","enabled":true},{"id":"gm3","card_title":"Naughty Boy","card_summary":"规则与违抗之间的游戏化实验，关于儿童行为心理学的隐喻。","detail_title":"Naughty Boy","detail_summary":"规则与违抗之间的游戏化实验，关于儿童行为心理学的隐喻。","tag":"GameJam","accent":false,"year":2023,"image":"https://picsum.photos/seed/gm3/600/375","image_alt":"Naughty Boy","paragraphs":["探索规则边界的叙事游戏。"],"tech":["Phaser","JavaScript"],"images":[],"lab_post_id":"project-gm3","enabled":true},{"id":"gm4","card_title":"Ring of Elysium","card_summary":"参与腾讯北极光工作室《无限法则》的玩法与系统设计。","detail_title":"Ring of Elysium","detail_summary":"参与腾讯北极光工作室《无限法则》的玩法与系统设计。","tag":"商业项目","accent":true,"year":2022,"image":"https://picsum.photos/seed/gm4/600/375","image_alt":"Ring of Elysium","paragraphs":["作为玩法设计师参与开发的大逃杀游戏。"],"tech":["Unreal Engine","C++","Lua"],"images":[],"lab_post_id":"project-gm4","enabled":true},{"id":"gm5","card_title":"Moodlog","card_summary":"一个极简的情绪记录工具，专注输入体验与一年后的回看。","detail_title":"Moodlog","detail_summary":"一个极简的情绪记录工具，专注输入体验与一年后的回看。","tag":"独立工具","accent":false,"year":2024,"image":"https://picsum.photos/seed/gm5/600/375","image_alt":"Moodlog","paragraphs":["帮助你记录情绪变化的日常工具。"],"tech":["React","TypeScript","Supabase"],"images":[],"lab_post_id":"project-gm5","enabled":true},{"id":"gm6","card_title":"Beat Lab","card_summary":"浏览器内的鼓机与音序器，使用 Web Audio API 实时合成。","detail_title":"Beat Lab","detail_summary":"浏览器内的鼓机与音序器，使用 Web Audio API 实时合成。","tag":"Web 实验","accent":false,"year":2023,"image":"https://picsum.photos/seed/gm6/600/375","image_alt":"Beat Lab","paragraphs":["在线音乐创作工具。"],"tech":["Vue","Web Audio API","Tone.js"],"images":[],"lab_post_id":"project-gm6","enabled":true}]}
$content$::jsonb, updated_at = NOW()
WHERE module_key = 'projects'
  AND jsonb_typeof(draft_data->'items') = 'array'
  AND jsonb_array_length(draft_data->'items') = 0;

UPDATE content_modules
SET draft_data = $content$
{"details":[{"id":"photo","title":"西安 · 城市足迹","summary":"34.34°N · 108.94°E · 古城墙 · 钟楼 · 园林","paragraphs":["这里记录我在西安的一次认真抵达。","沿途关注的场景包括：古城墙 · 钟楼 · 园林。"],"images":[],"cta_text":"查看更多","cta_url":""},{"id":"hike","title":"昆明 · 城市足迹","summary":"24.88°N · 102.83°E · 我的家乡 · 滇池 · 翠湖","paragraphs":["这里记录我在昆明的一次认真抵达。","沿途关注的场景包括：我的家乡 · 滇池 · 翠湖。"],"images":[],"cta_text":"查看更多","cta_url":""},{"id":"coffee","title":"上海 · 城市足迹","summary":"31.21°N · 121.47°E · 外滩 · city walk","paragraphs":["这里记录我在上海的一次认真抵达。","沿途关注的场景包括：外滩 · city walk。"],"images":[],"cta_text":"查看更多","cta_url":""},{"id":"travel","title":"广州 · 城市足迹","summary":"23.13°N · 113.26°E · 美食 · 人文","paragraphs":["这里记录我在广州的一次认真抵达。","沿途关注的场景包括：美食 · 人文。"],"images":[],"cta_text":"查看更多","cta_url":""},{"id":"music","title":"深圳 · 城市足迹","summary":"22.54°N · 114.06°E · 深圳湾 · city walk","paragraphs":["这里记录我在深圳的一次认真抵达。","沿途关注的场景包括：深圳湾 · city walk。"],"images":[],"cta_text":"查看更多","cta_url":""},{"id":"read","title":"北京 · 城市足迹","summary":"39.91°N · 116.39°E · 文化 · 历史 · city walk","paragraphs":["这里记录我在北京的一次认真抵达。","沿途关注的场景包括：文化 · 历史 · city walk。"],"images":[],"cta_text":"查看更多","cta_url":""}]}
$content$::jsonb, updated_at = NOW()
WHERE module_key = 'footprints'
  AND jsonb_typeof(draft_data->'details') = 'array'
  AND jsonb_array_length(draft_data->'details') = 0;

UPDATE content_modules
SET draft_data = $content$
{"cards":[{"id":"Study","title":"Counter-Strike 2","description":"最喜欢它纯粹又残酷的博弈感，每一颗道具、每一次 peek 都要为团队节奏负责。","image":"./game_posters/cs2.jpg","image_alt":"Counter-Strike 2","enabled":true},{"id":"Music","title":"Apex 英雄","description":"机动性和临场决策很迷人，打赢一波混战时有很强的节奏感。","image":"./game_posters/apex.jpg","image_alt":"Apex 英雄","enabled":true},{"id":"Game","title":"三角洲行动","description":"偏战术、偏压迫的枪线体验，适合认真研究路线、信息和团队配合。","image":"./game_posters/delta-force.jpg","image_alt":"三角洲行动","enabled":true},{"id":"Coding","title":"无畏契约","description":"技能和枪法互相牵制，回合制的紧张感很足。","image":"./game_posters/the-finals.jpg","image_alt":"无畏契约","enabled":true},{"id":"Social","title":"守望先锋 2","description":"英雄机制和团战节奏变化很快，重点是团队位置和技能交换。","image":"./game_posters/overwatch2.jpeg","image_alt":"守望先锋 2","enabled":true}]}
$content$::jsonb, updated_at = NOW()
WHERE module_key = 'hobbies'
  AND jsonb_typeof(draft_data->'cards') = 'array'
  AND jsonb_array_length(draft_data->'cards') = 0;

UPDATE content_modules
SET draft_data = $content$
{"tools":[{"id":"tool-1","name":"Cursor","percentage":80,"description":"代码编写主力，执行明确任务，性价比高","enabled":true},{"id":"tool-2","name":"Codex","percentage":80,"description":"代码编写主力，用户意图理解力强，执行需求模糊的任务","enabled":true},{"id":"tool-3","name":"Claude Code","percentage":60,"description":"代码编写辅助，生成代码质量高，执行复杂任务","enabled":true},{"id":"tool-4","name":"Kimi","percentage":60,"description":"我最初使用的AI工具，目前作为日常辅助问答以及API调用","enabled":true},{"id":"tool-5","name":"DeepSeek","percentage":40,"description":"有时疑似被Kimi拉黑，作为国产模型探索以及kimi的替代","enabled":true},{"id":"tool-6","name":"ChatGPT","percentage":20,"description":"图片素材生成，以及日常辅助问答(暗黑版)","enabled":true}]}
$content$::jsonb, updated_at = NOW()
WHERE module_key = 'vibe'
  AND jsonb_typeof(draft_data->'tools') = 'array'
  AND jsonb_array_length(draft_data->'tools') = 0;

UPDATE content_modules
SET draft_data = $content$
{"tags":[{"id":"tag-1","name":"GameJam","enabled":true},{"id":"tag-2","name":"Unity","enabled":true},{"id":"tag-3","name":"C#","enabled":true},{"id":"tag-4","name":"Aseprite","enabled":true},{"id":"tag-5","name":"Godot","enabled":true},{"id":"tag-6","name":"GDScript","enabled":true},{"id":"tag-7","name":"Phaser","enabled":true},{"id":"tag-8","name":"JavaScript","enabled":true},{"id":"tag-9","name":"商业项目","enabled":true},{"id":"tag-10","name":"Unreal Engine","enabled":true},{"id":"tag-11","name":"C++","enabled":true},{"id":"tag-12","name":"Lua","enabled":true},{"id":"tag-13","name":"独立工具","enabled":true},{"id":"tag-14","name":"React","enabled":true},{"id":"tag-15","name":"TypeScript","enabled":true},{"id":"tag-16","name":"Supabase","enabled":true},{"id":"tag-17","name":"Web 实验","enabled":true},{"id":"tag-18","name":"Vue","enabled":true},{"id":"tag-19","name":"Web Audio API","enabled":true},{"id":"tag-20","name":"Tone.js","enabled":true},{"id":"tag-21","name":"Docker","enabled":true},{"id":"tag-22","name":"Nginx","enabled":true},{"id":"tag-23","name":"运维","enabled":true},{"id":"tag-24","name":"GSAP","enabled":true},{"id":"tag-25","name":"前端","enabled":true},{"id":"tag-26","name":"Leetcode","enabled":true},{"id":"tag-27","name":"算法","enabled":true},{"id":"tag-28","name":"Tailwind","enabled":true},{"id":"tag-29","name":"工程化","enabled":true},{"id":"tag-30","name":"树莓派","enabled":true},{"id":"tag-31","name":"硬件","enabled":true},{"id":"tag-32","name":"DP","enabled":true},{"id":"tag-33","name":"随笔","enabled":true}],"posts":[{"id":"project-gm1","date":"2024-01-01","title":"Moth and Bat 项目记录","tags":["GameJam","Unity","C#","Aseprite"],"summary":"48 小时 GameJam 作品，关于夜色中两种生物的相会。","image":"https://picsum.photos/seed/gm1/600/375","image_alt":"Moth and Bat","sections":[{"heading":"项目概述","paragraphs":["这是一款关于夜晚相遇的解谜游戏。玩家扮演一只飞蛾，在月光下寻找答案。","主要技术：Unity、C#、Aseprite"]}],"enabled":true},{"id":"project-gm2","date":"2023-01-01","title":"Naughty Cat 项目记录","tags":["GameJam","Godot","GDScript"],"summary":"一只总想搞破坏的猫与一个不肯关机的扫地机器人。","image":"https://picsum.photos/seed/gm2/600/375","image_alt":"Naughty Cat","sections":[{"heading":"项目概述","paragraphs":["一款轻松幽默的平台跳跃游戏。","主要技术：Godot、GDScript"]}],"enabled":true},{"id":"project-gm3","date":"2023-01-01","title":"Naughty Boy 项目记录","tags":["GameJam","Phaser","JavaScript"],"summary":"规则与违抗之间的游戏化实验，关于儿童行为心理学的隐喻。","image":"https://picsum.photos/seed/gm3/600/375","image_alt":"Naughty Boy","sections":[{"heading":"项目概述","paragraphs":["探索规则边界的叙事游戏。","主要技术：Phaser、JavaScript"]}],"enabled":true},{"id":"project-gm4","date":"2022-01-01","title":"Ring of Elysium 项目记录","tags":["商业项目","Unreal Engine","C++","Lua"],"summary":"参与腾讯北极光工作室《无限法则》的玩法与系统设计。","image":"https://picsum.photos/seed/gm4/600/375","image_alt":"Ring of Elysium","sections":[{"heading":"项目概述","paragraphs":["作为玩法设计师参与开发的大逃杀游戏。","主要技术：Unreal Engine、C++、Lua"]}],"enabled":true},{"id":"project-gm5","date":"2024-01-01","title":"Moodlog 项目记录","tags":["独立工具","React","TypeScript","Supabase"],"summary":"一个极简的情绪记录工具，专注输入体验与一年后的回看。","image":"https://picsum.photos/seed/gm5/600/375","image_alt":"Moodlog","sections":[{"heading":"项目概述","paragraphs":["帮助你记录情绪变化的日常工具。","主要技术：React、TypeScript、Supabase"]}],"enabled":true},{"id":"project-gm6","date":"2023-01-01","title":"Beat Lab 项目记录","tags":["Web 实验","Vue","Web Audio API","Tone.js"],"summary":"浏览器内的鼓机与音序器，使用 Web Audio API 实时合成。","image":"https://picsum.photos/seed/gm6/600/375","image_alt":"Beat Lab","sections":[{"heading":"项目概述","paragraphs":["在线音乐创作工具。","主要技术：Vue、Web Audio API、Tone.js"]}],"enabled":true},{"id":"blog-docker-deploy","date":"2026-07-28","title":"个人博客 Docker + Nginx 部署全流程记录","tags":["Docker","Nginx","运维"],"summary":"从 Dockerfile 多阶段构建到 nginx SPA 回退与 gzip 配置，把博客塞进容器的完整折腾过程。","image":"https://picsum.photos/seed/gm5/600/375","sections":[{"heading":"多阶段构建","paragraphs":["构建阶段用 node 镜像安装依赖并执行 vite build，产物只有 dist 一个目录；运行阶段直接换成 nginx:alpine，把 dist 拷进去即可，最终镜像不到 30MB。","需要注意的是 .dockerignore 别忘了排除 node_modules 和本地 dist，否则构建上下文会非常大，CI 上每次都要多传几百 MB。"]},{"heading":"Nginx 配置要点","paragraphs":["SPA 最关键的是 try_files $uri $uri/ /index.html，否则刷新非根路由直接 404。静态资源带 hash 的文件可以放心加一年的强缓存。","gzip 开启后对文本类资源收益明显，同时建议把 /assets 下的 immutable 缓存和 index.html 的 no-cache 分开配置，避免发版后用户拿到旧页面。"]},{"heading":"部署与回滚","paragraphs":["镜像打上时间戳 tag 再推 latest，出问题时 docker run 指回上一个 tag 就能秒级回滚，比重新构建靠谱得多。"]}],"image_alt":"个人博客 Docker + Nginx 部署全流程记录","enabled":true},{"id":"vue-gsap-hero","date":"2026-07-15","title":"用 GSAP 给首页 Hero 做电影感动效","tags":["Vue","GSAP","前端"],"summary":"ScrollTrigger 驱动的滚动叙事：分镜、视差与滚动提示文字的入场编排。","image":"https://picsum.photos/seed/gm6/600/375","sections":[{"heading":"分镜思路","paragraphs":["先把 Hero 拆成几个“镜头”：开场定格、文字入场、视差拉开、提示滚动。每个镜头对应 timeline 上的一段，而不是一堆零散的 tween。","这样做的好处是节奏可调——改一个镜头的时长不会影响其他镜头的编排，整体叙事结构始终清晰。"]},{"heading":"ScrollTrigger 实践","paragraphs":["scrub 模式让动画进度和滚动位置绑定，配合 pin 把 Hero 钉在视口内，用户滚动时实际上是在“播放”这段分镜。","在 Vue 里要注意在 onBeforeUnmount 中清理 ScrollTrigger 实例，否则路由切换后残留的触发器会造成奇怪的滚动偏移。"]},{"heading":"性能与降级","paragraphs":["所有动画只操作 transform 和 opacity，配合 will-change 交给合成层；prefers-reduced-motion 用户直接呈现最终态，不播放过程动画。"]}],"image_alt":"用 GSAP 给首页 Hero 做电影感动效","enabled":true},{"id":"leetcode-binary-search","date":"2026-06-30","title":"二分查找的几种边界写法整理","tags":["Leetcode","算法"],"summary":"闭区间 / 左闭右开两种模板的循环不变量对比，附几道经典题的应用。","sections":[{"heading":"两种区间定义","paragraphs":["闭区间 [left, right] 写法里循环条件是 left <= right，收缩时 right = mid - 1；左闭右开 [left, right) 则是 left < right，收缩时 right = mid。","两种写法本质等价，但混用是几乎所有 bug 的来源——选定一种区间定义后，循环条件、中点收缩和返回值必须自洽。"]},{"heading":"循环不变量","paragraphs":["写二分最关键的是时刻维护“答案一定在当前区间内”这个不变量。每次收缩区间时都要问自己：被丢掉的那一半里有没有可能存在答案？","找左边界和右边界是对称的两套模板，建议背一套然后镜像推导，比硬记两套不容易混。"]},{"heading":"经典题应用","paragraphs":["搜索旋转排序数组的关键是判断哪一半有序；寻找峰值利用“往高处走必有峰”的性质；而“答案单调可判定”的题目（如爱吃香蕉的珂珂）则是二分答案的典型套路。"]}],"image":"","image_alt":"二分查找的几种边界写法整理","enabled":true},{"id":"tailwind-migration","date":"2026-06-12","title":"项目迁移 Tailwind CSS v4 的坑","tags":["Tailwind","前端","工程化"],"summary":"v4 改为 CSS-first 配置后，postcss 插件与 @theme 写法的迁移笔记。","image":"https://picsum.photos/seed/gm3/600/375","sections":[{"heading":"配置方式的变化","paragraphs":["v4 最大的变化是配置从 tailwind.config.js 迁到了 CSS 里：@import \"tailwindcss\" 加上 @theme 块定义设计变量，JS 配置文件不再是必需品。","PostCSS 插件也独立成了 @tailwindcss/postcss，老项目里直接升级包名不改配置的话，构建会直接报插件找不到。"]},{"heading":"迁移中踩到的坑","paragraphs":["自定义色板要改成 @theme 里的 --color-* 变量才能继续用 bg-* 类名；部分默认工具类的命名有调整，升级后建议全量过一遍页面。","另外 v4 的内容扫描变成了自动探测，monorepo 里如果有不在默认规则下的目录，需要用 @source 显式声明。"]}],"image_alt":"项目迁移 Tailwind CSS v4 的坑","enabled":true},{"id":"raspberry-pi-nas","date":"2026-05-20","title":"树莓派搭家用 NAS：Samba 与硬盘休眠","tags":["树莓派","运维","硬件"],"summary":"Samba 共享配置、挂载点权限，以及 hdparm 让闲置硬盘自动休眠省电。","sections":[{"heading":"Samba 共享配置","paragraphs":["安装 samba 后在 smb.conf 里声明共享目录，重点是 valid users 和 create mask 的组合——新建文件的权限不对，Windows 侧就会出现能看不能写。","挂载点建议用 systemd 的 .mount 单元管理，配合 nofail 选项，避免硬盘没插好时系统启动卡住。"]},{"heading":"硬盘休眠","paragraphs":["机械盘 7x24 转着既费电又伤盘，hdparm -S 设置闲置超时后就能自动停转。注意有些 USB 硬盘盒的桥接芯片不支持直通休眠指令，需要换 hdparm 的 -S 参数或用 hd-idle 兜底。","验证方式很简单：hdparm -C 查看状态，standby 即表示已经睡着了。"]}],"image":"","image_alt":"树莓派搭家用 NAS：Samba 与硬盘休眠","enabled":true},{"id":"vue-composable-mouse-tilt","date":"2026-05-06","title":"封装一个 useMouseTilt 组合式函数","tags":["Vue","前端"],"summary":"用 requestAnimationFrame 节流鼠标事件，给卡片做跟随视角的 3D 倾斜。","sections":[{"heading":"为什么需要节流","paragraphs":["mousemove 的触发频率远高于屏幕刷新率，直接在回调里改样式会造成大量无效计算。用 rAF 把事件攒到下一帧统一处理，一帧最多执行一次。","进一步还可以对目标角度做 lerp 平滑，卡片跟随鼠标时会有弹簧般的质感，而不是生硬地瞬移。"]},{"heading":"封装思路","paragraphs":["组合式函数接收一个模板引用和倾斜角度上限，返回当前的 rotateX / rotateY；内部用 useEventListener 托管监听，组件卸载时自动清理。","注意透视要加在父容器上（perspective），倾斜元素自身只做 rotate，这样多个卡片并排时视觉焦点才统一。"]}],"image":"","image_alt":"封装一个 useMouseTilt 组合式函数","enabled":true},{"id":"leetcode-dp-notes","date":"2026-04-18","title":"动态规划刷题小结：从背包到区间 DP","tags":["Leetcode","算法","DP"],"summary":"状态定义优先还是转移优先？整理了自己刷 DP 题时的思考 checklist。","sections":[{"heading":"状态定义优先","paragraphs":["我的经验是先想清楚 dp[i] 到底代表什么——“以 i 结尾”还是“前 i 个”——这两种定义决定了初始化和答案的位置，想不清楚就先拿小样例手推。","背包问题的核心是遍历顺序：01 背包倒序、完全背包正序，理解了“每个物品能用几次”就不会再背错。"]},{"heading":"区间 DP 的套路","paragraphs":["区间 DP 的标志是 dp[l][r] 表示区间 [l, r] 上的最优解，枚举顺序必须按区间长度从小到大，保证转移时子区间已经算完。","石子合并、戳气球这类题都有一个共同的“最后一次操作”视角：枚举最后合并/戳破的位置 k，把区间拆成两段独立子问题。"]},{"heading":"我的 checklist","paragraphs":["一看数据范围猜复杂度，二定状态含义，三写转移方程，四推初始值，五定遍历顺序。五步走不通就回头重新定义状态，而不是硬调转移。"]}],"image":"","image_alt":"动态规划刷题小结：从背包到区间 DP","enabled":true},{"id":"first-post","date":"2026-04-01","title":"MyLab 开张：为什么单独开一个实验记录页","tags":["随笔"],"summary":"项目展示放在首页，零散的学习与折腾记录集中收在这里，方便检索与回顾。","sections":[{"heading":"为什么单独开一页","paragraphs":["首页的项目展示区适合放完整、成体系的作品，但日常学习中大量零散的记录——一次部署踩坑、一个算法模板、一个小工具的封装——没有合适的地方放。","MyLab 就是给这些内容准备的：不打分、不追求完美，只要求未来的自己能检索到、看得懂。"]},{"heading":"记录的原则","paragraphs":["每条记录都带标签和日期，方便按主题聚合；正文尽量写清“当时为什么这么做”，因为半年后再看，决策的理由比操作步骤更值钱。"]}],"image":"","image_alt":"MyLab 开张：为什么单独开一个实验记录页","enabled":true}]}
$content$::jsonb, updated_at = NOW()
WHERE module_key = 'mylab'
  AND jsonb_typeof(draft_data->'posts') = 'array'
  AND jsonb_array_length(draft_data->'posts') = 0;

-- 发布本次写入的六个集合模块；support 已在插入时发布。
UPDATE content_modules
SET published_data = draft_data,
    published_version = 1,
    status = 'published',
    published_at = COALESCE(published_at, NOW()),
    updated_at = NOW()
WHERE published_data IS NULL
  AND published_version = 0
  AND status = 'draft'
  AND (
      (module_key = 'skills' AND draft_data->'items'->0->>'id' = 'skill-1')
      OR (module_key = 'projects' AND draft_data->'items'->0->>'id' = 'gm1')
      OR (module_key = 'footprints' AND draft_data->'details'->0->>'id' = 'photo')
      OR (module_key = 'hobbies' AND draft_data->'cards'->0->>'id' = 'Study')
      OR (module_key = 'vibe' AND draft_data->'tools'->0->>'id' = 'tool-1')
      OR (module_key = 'mylab' AND draft_data->'posts'->0->>'id' = 'project-gm1')
  );

-- 保存所有初始发布快照，确保后台可以从版本 1 开始回滚。
INSERT INTO content_publications (module_key, version, data, published_at)
SELECT module_key,
       published_version,
       published_data,
       COALESCE(published_at, NOW())
FROM content_modules
WHERE published_data IS NOT NULL
  AND published_version > 0
ON CONFLICT (module_key, version) DO NOTHING;
