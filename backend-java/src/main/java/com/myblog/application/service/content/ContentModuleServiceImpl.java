package com.myblog.application.service.content;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.myblog.application.model.dto.ContentDtos;
import com.myblog.application.model.entity.ContentRelease;
import com.myblog.application.model.entity.FileRecord;
import com.myblog.application.model.entity.MylabTag;
import com.myblog.application.port.ObjectStorage;
import com.myblog.application.repository.ContentReleaseRepository;
import com.myblog.application.repository.FileRepository;
import com.myblog.application.repository.MylabTagRepository;
import com.myblog.common.enumeration.ErrorCode;
import com.myblog.common.exception.ConflictException;
import com.myblog.common.exception.NotFoundException;
import com.myblog.common.exception.ValidationException;
import com.myblog.common.json.JacksonObjectMapper;
import com.myblog.common.security.Authorization;
import com.myblog.common.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 内容模块服务实现：版本化内容系统的核心。
 * 每个模块同一时刻至多一个草稿（DRAFT）和一个线上版本（PUBLISHED），发布即生成不可变历史版本；
 * 模块内容以 JSON 存储，保存/发布前按模块分别做结构校验，读取时把对象存储 key 转成可访问 URL。
 */
@Service
public class ContentModuleServiceImpl implements ContentModuleService {
    private static final List<String> KEYS = List.of("home", "about", "skills", "footprints", "hobbies", "vibe", "mylab"); // 支持的内容模块清单
    private static final Set<String> TIME_KEYS = Set.of("爱好1", "爱好2", "爱好3", "爱好4", "爱好5"); // hobbies 时间分布图的五个维度
    private static final ObjectMapper OM = JacksonObjectMapper.get();

    private final ContentReleaseRepository releases;
    private final MylabTagRepository tags;
    private final FileRepository resources;
    private final ObjectStorage storage;

    public ContentModuleServiceImpl(ContentReleaseRepository releases, MylabTagRepository tags,
                                    FileRepository resources, ObjectStorage storage) {
        this.releases = releases;
        this.tags = tags;
        this.resources = resources;
        this.storage = storage;
    }

    /**
     * 汇总所有模块的已发布内容；跳过未发布模块，输出经公开化处理（过滤停用项、补 URL）的数据。
     */
    @Override
    public Map<String, Object> publicContent() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : KEYS) {
            ContentRelease release = releases.findPublished(key);
            if (release != null) result.put(key, publicData(key, releases.readData(release)));
        }
        return result;
    }

    /**
     * 读取单个模块的已发布内容；模块不存在或从未发布/已下线时抛异常。
     */
    @Override
    public Object publicModule(String moduleKey) {
        requireKey(moduleKey);
        ContentRelease release = releases.findPublished(moduleKey);
        if (release == null) throw new NotFoundException(ErrorCode.CONTENT_MODULE_OFFLINE, moduleKey);
        return publicData(moduleKey, releases.readData(release));
    }

    /**
     * 从已发布的 mylab 模块中按 post_key 取单张卡片的公开详情。
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object publicMylabDetail(String postKey) {
        Map<String, Object> root = (Map<String, Object>) publicModule("mylab");
        List<Map<String, Object>> cards = (List<Map<String, Object>>) root.getOrDefault("cards", List.of());
        return cards.stream().filter(card -> postKey.equals(card.get("post_key")))
                .findFirst().orElseThrow(() -> new NotFoundException(ErrorCode.RESOURCE_NOT_FOUND, postKey));
    }

    /**
     * 列出全部模块的管理视图（仅管理员）。
     */
    @Override
    public List<ContentDtos.ModuleView> list(CurrentUser actor) {
        Authorization.requireAdmin(actor);
        return KEYS.stream().map(this::view).toList();
    }

    /**
     * 获取单个模块的管理视图（仅管理员）。
     */
    @Override
    public ContentDtos.ModuleView get(CurrentUser actor, String moduleKey) {
        Authorization.requireAdmin(actor);
        requireKey(moduleKey);
        return view(moduleKey);
    }

    /**
     * 保存草稿：先按草稿态校验数据，再对模块加行锁串行化写操作。
     * 无草稿时新建一版草稿（数据剥离 row_id）；已有草稿时用 expected_updated_at 做乐观并发校验，防止覆盖他人修改。
     */
    @Override
    @Transactional
    public ContentDtos.ModuleView saveDraft(CurrentUser actor, String moduleKey, ContentDtos.SaveDraft command) {
        Authorization.requireAdmin(actor);
        requireKey(moduleKey);
        if (command == null || command.data() == null) throw validation("data 为必填字段");
        validate(moduleKey, command.data(), false);

        releases.lockModule(moduleKey);
        ContentRelease draft = releases.findDraft(moduleKey);
        Object draftData = command.data();
        OffsetDateTime now = OffsetDateTime.now();
        if (draft == null) {
            ContentRelease current = releases.findCurrent(moduleKey);
            draft = new ContentRelease();
            draft.setId(UUID.randomUUID());
            draft.setModuleKey(moduleKey);
            draft.setVersionNo(releases.nextVersion(moduleKey));
            draft.setState("DRAFT");
            draft.setSourceReleaseId(current == null ? null : current.getId());
            draft.setCreatedAt(now);
            draft.setUpdatedAt(now);
            releases.add(draft);
            draftData = withoutRowIds(draftData);
        } else {
            if (command.expectedUpdatedAt() == null) throw conflict("缺少 expected_updated_at，无法确认草稿版本");
            // touchDraft 以 expected_updated_at 为条件做 CAS 式更新，失败说明草稿已被并发修改
            if (!releases.touchDraft(draft.getId(), command.expectedUpdatedAt(), now)) {
                throw conflict("草稿已被其他操作修改，请刷新后重试");
            }
            draft.setUpdatedAt(now);
        }
        releases.replaceData(draft, draftData);
        return view(moduleKey);
    }

    /**
     * 发布：要求存在草稿，按发布态做更严格的完整校验后，将草稿转为新版本并替换线上版本。
     */
    @Override
    @Transactional
    public ContentDtos.ModuleView publish(CurrentUser actor, String moduleKey) {
        Authorization.requireAdmin(actor);
        requireKey(moduleKey);
        releases.lockModule(moduleKey);
        ContentRelease draft = releases.findDraft(moduleKey);
        if (draft == null) throw conflict("当前模块没有可发布草稿");
        Object data = releases.readData(draft);
        validate(moduleKey, data, true);
        releases.publish(draft, releases.findCurrent(moduleKey), actor.id(), OffsetDateTime.now());
        return view(moduleKey);
    }

    /**
     * 下线当前已发布版本；没有线上版本时抛冲突异常。
     */
    @Override
    @Transactional
    public ContentDtos.ModuleView offline(CurrentUser actor, String moduleKey) {
        Authorization.requireAdmin(actor);
        requireKey(moduleKey);
        releases.lockModule(moduleKey);
        ContentRelease current = releases.findPublished(moduleKey);
        if (current == null) throw conflict("当前模块没有已发布版本");
        releases.offline(current, OffsetDateTime.now());
        return view(moduleKey);
    }

    /**
     * 列出模块全部历史版本（仅管理员）。
     */
    @Override
    public List<ContentDtos.VersionView> versions(CurrentUser actor, String moduleKey) {
        Authorization.requireAdmin(actor);
        requireKey(moduleKey);
        return releases.findVersions(moduleKey).stream().map(this::versionView).toList();
    }

    /**
     * 查看指定版本号的历史版本；草稿态版本不对外可见。
     */
    @Override
    public ContentDtos.VersionView version(CurrentUser actor, String moduleKey, int versionNo) {
        Authorization.requireAdmin(actor);
        requireKey(moduleKey);
        ContentRelease release = releases.findVersion(moduleKey, versionNo);
        if (release == null || "DRAFT".equals(release.getState())) {
            throw new NotFoundException(ErrorCode.CONTENT_VERSION_NOT_FOUND, moduleKey + " v" + versionNo);
        }
        return versionView(release);
    }

    /**
     * 回滚：以指定历史版本的内容为底新建草稿；存在未处理草稿时要求先保存或放弃。
     */
    @Override
    @Transactional
    public ContentDtos.ModuleView restore(CurrentUser actor, String moduleKey, int versionNo) {
        Authorization.requireAdmin(actor);
        requireKey(moduleKey);
        releases.lockModule(moduleKey);
        if (releases.findDraft(moduleKey) != null) throw conflict("请先保存或放弃当前草稿");
        ContentRelease source = releases.findVersion(moduleKey, versionNo);
        if (source == null || "DRAFT".equals(source.getState())) {
            throw new NotFoundException(ErrorCode.CONTENT_VERSION_NOT_FOUND, moduleKey + " v" + versionNo);
        }
        Object data = releases.readData(source);
        OffsetDateTime now = OffsetDateTime.now();
        ContentRelease draft = new ContentRelease();
        draft.setId(UUID.randomUUID());
        draft.setModuleKey(moduleKey);
        draft.setVersionNo(releases.nextVersion(moduleKey));
        draft.setState("DRAFT");
        draft.setSourceReleaseId(source.getId());
        draft.setCreatedAt(now);
        draft.setUpdatedAt(now);
        releases.add(draft);
        releases.replaceData(draft, withoutRowIds(data));
        return view(moduleKey);
    }

    /**
     * 删除当前草稿；没有草稿时抛 NotFoundException。
     */
    @Override
    @Transactional
    public void deleteDraft(CurrentUser actor, String moduleKey) {
        Authorization.requireAdmin(actor);
        requireKey(moduleKey);
        ContentRelease draft = releases.findDraft(moduleKey);
        if (draft == null) throw new NotFoundException(ErrorCode.CONTENT_VERSION_NOT_FOUND, "当前草稿");
        releases.deleteDraft(draft);
    }

    /**
     * 软删除指定历史版本：级联标记其模块子表数据行以解除资源引用；线上发布态版本不可删除。
     */
    @Override
    @Transactional
    public void deleteVersion(CurrentUser actor, String moduleKey, int versionNo) {
        Authorization.requireAdmin(actor);
        requireKey(moduleKey);
        releases.lockModule(moduleKey);
        ContentRelease release = releases.findVersion(moduleKey, versionNo);
        if (release == null || "DRAFT".equals(release.getState())) {
            throw new NotFoundException(ErrorCode.CONTENT_VERSION_NOT_FOUND, moduleKey + " v" + versionNo);
        }
        if ("PUBLISHED".equals(release.getState())) {
            throw conflict("线上版本不可删除，请先下线或发布新版本");
        }
        releases.softDeleteVersion(release, OffsetDateTime.now());
    }

    /**
     * 组装模块管理视图：草稿优先，无草稿时草稿侧回退展示线上内容，并推导整体状态。
     */
    private ContentDtos.ModuleView view(String moduleKey) {
        ContentRelease draft = releases.findDraft(moduleKey);
        ContentRelease current = releases.findCurrent(moduleKey);
        Object draftRaw = draft == null ? (current == null ? emptyData(moduleKey) : releases.readData(current)) : releases.readData(draft);
        Object draftData = adminData(moduleKey, draftRaw);
        Object publishedData = current == null ? null : adminData(moduleKey, releases.readData(current));
        // 有草稿一律视为 draft；无草稿且无线上版本也视为 draft，否则取线上版本状态
        String status = draft != null ? "draft" : current == null ? "draft" : current.getState().toLowerCase();
        return new ContentDtos.ModuleView(moduleKey, draft == null ? null : draft.getId(), current == null ? null : current.getId(),
                draftData, publishedData, draft == null ? null : draft.getVersionNo(),
                current == null ? null : current.getVersionNo(), status,
                draft == null ? null : draft.getUpdatedAt(), current == null ? null : current.getPublishedAt());
    }

    /**
     * 组装历史版本视图。
     */
    private ContentDtos.VersionView versionView(ContentRelease release) {
        return new ContentDtos.VersionView(release.getId(), release.getModuleKey(), release.getVersionNo(),
                release.getState(), adminData(release.getModuleKey(), releases.readData(release)),
                release.getSourceReleaseId(), release.getPublishedAt());
    }

    /**
     * 各模块无任何版本时的初始空数据结构，保证前端总能拿到固定字段。
     */
    private Object emptyData(String moduleKey) {
        return switch (moduleKey) {
            case "home" -> Map.of("images", List.of());
            case "about" -> Map.of(
                    "profile", Map.of("bullets", List.of()),
                    "ingredients", Map.of(), "bubbles", List.of());
            case "skills" -> Map.of("items", List.of());
            case "footprints" -> Map.of("details", List.of());
            case "hobbies" -> Map.of("cards", List.of(), "time_tags", List.of(), "time_points", List.of());
            case "vibe" -> Map.of("tools", List.of());
            case "mylab" -> Map.of("tags", tags.findAll(false), "cards", List.of());
            default -> Map.of();
        };
    }

    /**
     * 校验模块 key 合法，非法时抛 NotFoundException。
     */
    private void requireKey(String moduleKey) {
        if (!KEYS.contains(moduleKey)) throw new NotFoundException(ErrorCode.CONTENT_MODULE_NOT_FOUND, moduleKey);
    }

    /**
     * 按模块分发结构校验；publishing 为 true 时套用发布态的严格规则（必填、数量上限等）。
     */
    private void validate(String moduleKey, Object value, boolean publishing) {
        JsonNode root = OM.valueToTree(value);
        if (!root.isObject()) throw validation("模块内容必须是 JSON 对象");
        switch (moduleKey) {
            case "home" -> validateHome(requireArray(root, "images"), publishing);
            case "about" -> validateAbout(root, publishing);
            case "skills" -> validateSkills(requireArray(root, "items"), publishing);
            case "footprints" -> validateFootprints(array(root, "details", "items"), publishing);
            case "hobbies" -> validateHobbies(root, publishing);
            case "vibe" -> validateVibe(requireArray(root, "tools"), publishing);
            case "mylab" -> validateMylab(array(root, "cards", "posts"), publishing);
            default -> throw validation("未知内容模块");
        }
    }

    private void validateHome(JsonNode images, boolean publishing) {
        if (publishing && images.size() != 6) throw validation("首页发布时必须恰好配置六张图片");
        Set<UUID> resourceIds = new HashSet<>();
        for (JsonNode image : images) {
            UUID resourceId = uuid(image, "image_resource_id");
            if (resourceId != null) {
                if (!resourceIds.add(resourceId)) throw validation("首页图片资源不能重复");
                requireResource(resourceId, "image/");
            }
            if (publishing && resourceId == null) throw validation("首页图片必须选择图片资源");
            if (publishing) requireText(image, "alt");
        }
    }

    private void validateAbout(JsonNode root, boolean publishing) {
        JsonNode profile = root.path("profile");
        JsonNode ingredients = root.path("ingredients");
        JsonNode bubbles = root.path("bubbles");
        if (!profile.isObject() || !ingredients.isObject() || !bubbles.isArray()) {
            throw validation("about 必须包含 profile、ingredients 和 bubbles");
        }
        JsonNode bullets = profile.path("bullets");
        if (!bullets.isArray()) throw validation("profile.bullets 必须是数组");
        UUID avatarId = uuid(profile, "avatar_resource_id");
        if (avatarId != null) requireResource(avatarId, "image/");
        if (publishing) {
            if (avatarId == null) throw validation("关于我头像必须选择图片资源");
            requireText(profile, "title");
            requireText(profile, "avatar_alt");
            requireText(profile, "intro");
            requireText(profile, "outro");
            requireText(ingredients, "title");
            requireText(ingredients, "description");
            if (bullets.size() != 3) throw validation("个人简介条目必须恰好三条");
            for (JsonNode bullet : bullets) {
                if (!bullet.isTextual() || bullet.asText().isBlank()) throw validation("个人简介条目不能为空");
            }
        }
        for (JsonNode bubble : bubbles) {
            if (publishing) requireText(bubble, "text");
            String size = text(bubble, "size");
            if (size != null && !Set.of("big", "mid").contains(size)) throw validation("气泡 size 只允许 big 或 mid");
            validateColor(bubble, "background_color", publishing);
            validateColor(bubble, "text_color", publishing);
            validateColor(bubble, "glow_color", publishing);
        }
    }

    private void validateSkills(JsonNode items, boolean publishing) {
        Set<String> keys = new HashSet<>();
        int enabledCount = 0;
        for (JsonNode item : items) {
            uniqueKey(keys, item, "skill_key", "id");
            int percentage = item.path("percentage").asInt(-1);
            if (percentage < 0 || percentage > 100) throw validation("技能 percentage 必须在 0 到 100 之间");
            String levelCode = firstText(item, "level_code", "level");
            if (levelCode != null && !Set.of("novice", "competent", "proficient").contains(levelCode)) {
                throw validation("level_code 不合法");
            }
            UUID iconId = uuid(item, "icon_resource_id");
            if (iconId != null) requireResource(iconId, "image/");
            if (enabled(item)) {
                enabledCount++;
            }
            if (publishing && enabled(item)) {
                requireText(item, "name");
                if (levelCode == null || text(item, "level_text") == null) {
                    throw validation("已启用技能必须填写 level_code 和 level_text");
                }
                if (iconId == null) throw validation("已启用技能必须选择图标资源");
            }
        }
        if (publishing && enabledCount > 8) throw validation("最多只能启用八张技术栈卡片");
    }

    private void validateFootprints(JsonNode items, boolean publishing) {
        if (!items.isArray()) throw validation("details 必须是数组");
        Set<String> keys = new HashSet<>();
        int enabledCount = 0;
        for (JsonNode item : items) {
            uniqueKey(keys, item, "city_key", "id");
            validateResourceIds(item.path("resource_ids"), "image/");
            if (enabled(item)) enabledCount++;
            if (publishing && enabled(item)) {
                requireText(item, "title");
                requireText(item, "summary");
                if (firstText(item, "contents") == null && !item.path("paragraphs").isArray()) {
                    throw validation("已启用足迹必须填写 contents");
                }
            }
        }
        if (publishing && enabledCount > 6) throw validation("最多只能启用六条足迹");
    }

    private void validateHobbies(JsonNode root, boolean publishing) {
        JsonNode items = requireArray(root, "cards");
        Set<String> keys = new HashSet<>();
        int enabled = 0;
        for (JsonNode item : items) {
            uniqueKey(keys, item, "hobby_key", "id");
            UUID resourceId = uuid(item, "resource_id", "image_resource_id");
            if (resourceId != null) requireResource(resourceId, "image/");
            if (enabled(item)) {
                enabled++;
                if (publishing) {
                    requireText(item, "title");
                    requireText(item, "description");
                    if (resourceId == null) throw validation("已启用爱好必须选择图片资源");
                }
            }
        }
        if (publishing && enabled > 5) throw validation("最多只能启用五张爱好卡片");

        JsonNode timeTags = requireArray(root, "time_tags");
        Set<String> timeKeys = new HashSet<>();
        int enabledTags = 0;
        for (JsonNode tag : timeTags) {
            String dataKey = text(tag, "data_key");
            if (dataKey == null || !TIME_KEYS.contains(dataKey)) throw validation("Time 标签 data_key 不合法");
            if (!timeKeys.add(dataKey)) throw validation("Time 标签 data_key 不能重复");
            int x = tag.path("label_x").asInt(-1);
            int y = tag.path("label_y").asInt(-1);
            double scale = tag.path("label_scale").asDouble(-1);
            if (x < 0 || x > 500 || y < 0 || y > 300 || scale < 0.5 || scale > 3) {
                throw validation("Time 标签位置或缩放超出允许范围");
            }
            validateColor(tag, "color", publishing || enabled(tag));
            if (enabled(tag)) {
                enabledTags++;
                if (publishing) requireText(tag, "name");
            }
        }
        if (publishing && enabledTags > 5) throw validation("Time 标签最多只能启用五条");

        JsonNode points = requireArray(root, "time_points");
        Set<Integer> ages = new HashSet<>();
        for (JsonNode point : points) {
            int age = point.path("age").asInt(Integer.MIN_VALUE);
            if (age < -1 || age > 27 || !ages.add(age)) throw validation("Time 年龄必须在 -1 到 27 且不能重复");
            JsonNode values = point.path("values");
            if (!values.isObject()) throw validation("Time 年龄数据 values 必须是对象");
            double total = 0;
            for (String key : TIME_KEYS) {
                JsonNode value = values.path(key);
                if (!value.isNumber() || value.asDouble() < 0 || value.asDouble() > 10) {
                    throw validation("Time 年龄数据必须包含五项 0 到 10 的数值");
                }
                total += value.asDouble();
            }
            if (Math.abs(total - 10) > 0.001) throw validation("Time 年龄数据每行合计必须为 10");
        }
        if (publishing && (ages.size() != 29 || !ages.contains(-1) || !ages.contains(27))) {
            throw validation("Time 年龄数据必须完整覆盖 -1 到 27");
        }
    }

    private void validateVibe(JsonNode items, boolean publishing) {
        Set<String> keys = new HashSet<>();
        int enabledCount = 0;
        for (JsonNode item : items) {
            uniqueKey(keys, item, "tool_key", "id");
            int percentage = item.path("percentage").asInt(-1);
            if (percentage < 0 || percentage > 100) throw validation("工具 percentage 必须在 0 到 100 之间");
            if (enabled(item)) enabledCount++;
            if (publishing && enabled(item)) {
                requireText(item, "name");
                requireText(item, "description");
            }
        }
        if (publishing && enabledCount > 6) throw validation("最多只能启用六个 Vibe Coding 工具");
    }

    private void validateMylab(JsonNode cards, boolean publishing) {
        if (!cards.isArray()) throw validation("cards 必须是数组");
        Set<String> keys = new HashSet<>();
        Set<Integer> projectOrders = new HashSet<>();
        for (JsonNode card : cards) {
            String key = uniqueKey(keys, card, "post_key", "id");
            // 未显式声明 card_type 时按 post_key 前缀推断：project- 开头视为 PROJECT，其余为 ARTICLE
            String type = Objects.requireNonNullElse(firstText(card, "card_type"),
                    key.startsWith("project-") ? "PROJECT" : "ARTICLE").toUpperCase();
            if (!Set.of("PROJECT", "ARTICLE").contains(type)) throw validation("card_type 只允许 PROJECT 或 ARTICLE");
            Integer projectOrder = card.hasNonNull("project_show_order") ? card.path("project_show_order").asInt() : null;
            String projectContents = firstText(card, "project_contents", "project_content");
            if ("PROJECT".equals(type)) {
                if (projectOrder != null && projectOrder < 0) throw validation("PROJECT 必须填写非负 project_show_order");
                if (projectOrder != null && !projectOrders.add(projectOrder)) throw validation("PROJECT 的 project_show_order 不能重复");
                if (publishing && projectOrder == null) throw validation("已发布 PROJECT 必须填写 project_show_order");
                if (publishing && (projectContents == null || projectContents.isBlank())) {
                    throw validation("已发布 PROJECT 必须填写 project_contents");
                }
            } else if (projectOrder != null || projectContents != null) {
                throw validation("ARTICLE 不能填写项目侧边栏字段");
            }

            List<UUID> tagIds = uuidList(card.path("tag_ids"));
            if (new HashSet<>(tagIds).size() != tagIds.size()) throw validation("tag_ids 不能重复");
            if (tags.findActiveByIds(tagIds).size() != tagIds.size()) {
                throw validation("卡片只能引用当前启用且未删除的标签");
            }
            UUID imageId = uuid(card, "image_resource_id");
            UUID contentId = uuid(card, "content_resource_id");
            if (imageId != null) requireResource(imageId, "image/");
            if (contentId != null) requireResource(contentId, "text/markdown", "text/plain");
            if (publishing && enabled(card)) {
                if (firstText(card, "card_title", "title") == null || firstText(card, "card_summary", "summary") == null) {
                    throw validation("已启用 MyLab 卡片必须填写标题和摘要");
                }
                if (contentId == null) throw validation("已启用 MyLab 卡片必须选择 Markdown 正文资源");
            }
        }
    }

    /**
     * 公开化数据：在管理视图数据基础上过滤 enabled=false 的条目，mylab 卡片额外把 tag_ids 展开为标签名。
     */
    @SuppressWarnings("unchecked")
    private Object publicData(String moduleKey, Object raw) {
        Map<String, Object> root = (Map<String, Object>) adminData(moduleKey, raw);
        String field = switch (moduleKey) {
            case "home" -> "images";
            case "skills" -> "items";
            case "footprints" -> "details";
            case "hobbies" -> "cards";
            case "vibe" -> "tools";
            case "mylab" -> "cards";
            default -> null;
        };
        if (field == null) return root;
        List<Map<String, Object>> source = (List<Map<String, Object>>) root.getOrDefault(field, List.of());
        List<Map<String, Object>> visible = new ArrayList<>();
        Map<String, String> tagNames = new LinkedHashMap<>();
        if ("mylab".equals(moduleKey)) {
            List<Map<String, Object>> activeTags = (List<Map<String, Object>>) root.getOrDefault("tags", List.of());
            activeTags.forEach(tag -> tagNames.put(String.valueOf(tag.get("id")), String.valueOf(tag.get("name"))));
        }
        for (Map<String, Object> item : source) {
            if (Boolean.FALSE.equals(item.get("enabled"))) continue;
            Map<String, Object> result = new LinkedHashMap<>(item);
            if ("mylab".equals(moduleKey)) {
                List<?> ids = (List<?>) result.getOrDefault("tag_ids", List.of());
                result.put("tags", ids.stream().map(String::valueOf).map(tagNames::get).filter(Objects::nonNull).toList());
            }
            visible.add(result);
        }
        root.put(field, visible);
        if ("hobbies".equals(moduleKey)) {
            List<Map<String, Object>> timeTags = (List<Map<String, Object>>) root.getOrDefault("time_tags", List.of());
            root.put("time_tags", timeTags.stream().filter(tag -> !Boolean.FALSE.equals(tag.get("enabled"))).toList());
        }
        return root;
    }

    /**
     * 管理侧数据加工：把各模块引用到的对象存储 key 转换为可访问 URL 字段。
     */
    @SuppressWarnings("unchecked")
    private Object adminData(String moduleKey, Object raw) {
        Object source = raw == null ? emptyData(moduleKey) : raw;
        Map<String, Object> root = OM.convertValue(source, LinkedHashMap.class);
        switch (moduleKey) {
            case "home" -> ((List<Map<String, Object>>) root.getOrDefault("images", List.of()))
                    .forEach(image -> putUrl(image, "image_object_key", "image_url"));
            case "about" -> {
                Map<String, Object> profile = (Map<String, Object>) root.get("profile");
                if (profile != null) putUrl(profile, "avatar_object_key", "avatar_url");
            }
            case "skills" -> ((List<Map<String, Object>>) root.getOrDefault("items", List.of()))
                    .forEach(item -> putUrl(item, "icon_object_key", "icon_url"));
            case "footprints" -> ((List<Map<String, Object>>) root.getOrDefault("details", List.of()))
                    .forEach(item -> {
                        List<Map<String, Object>> linked = (List<Map<String, Object>>) item.getOrDefault("resources", List.of());
                        linked.forEach(resource -> putUrl(resource, "object_key", "url"));
                        item.put("resource_ids", linked.stream().map(resource -> resource.get("id")).toList());
                        item.put("images", linked.stream().map(resource -> resource.get("url")).filter(Objects::nonNull).toList());
                    });
            case "hobbies" -> ((List<Map<String, Object>>) root.getOrDefault("cards", List.of()))
                    .forEach(item -> {
                        putUrl(item, "image_object_key", "image_url");
                        if (item.get("image_url") != null) item.put("image", item.get("image_url"));
                    });
            case "mylab" -> ((List<Map<String, Object>>) root.getOrDefault("cards", List.of()))
                    .forEach(item -> {
                        putUrl(item, "image_object_key", "image_url");
                        putUrl(item, "content_object_key", "markdown_url");
                    });
            default -> { }
        }
        return root;
    }

    private void putUrl(Map<String, Object> item, String source, String target) {
        String key = (String) item.get(source);
        String url = url(key);
        if (url != null) item.put(target, url);
    }

    /**
     * 对象 key 转 URL：已是站内路径或完整 URL 时原样返回，否则走对象存储的公开地址；未配置存储时返回 key 本身。
     */
    private String url(String objectKey) {
        if (objectKey == null) return null;
        if (objectKey.startsWith("/") || objectKey.startsWith("http://") || objectKey.startsWith("https://")) {
            return objectKey;
        }
        return storage.configured() ? storage.publicUrl(objectKey) : objectKey;
    }

    /**
     * 递归剥离前端带来的 row_id 字段，避免行级标识随草稿/回滚数据落库。
     */
    private Object withoutRowIds(Object value) {
        JsonNode root = OM.valueToTree(value);
        removeRowIds(root);
        return OM.convertValue(root, Object.class);
    }

    private void removeRowIds(JsonNode node) {
        if (node instanceof ObjectNode object) {
            object.remove("row_id");
            object.elements().forEachRemaining(this::removeRowIds);
            return;
        }
        if (node.isArray()) node.elements().forEachRemaining(this::removeRowIds);
    }

    private void validateResourceIds(JsonNode ids, String mimePrefix) {
        if (ids == null || !ids.isArray()) return;
        Set<UUID> unique = new HashSet<>();
        for (JsonNode id : ids) {
            UUID value = UUID.fromString(id.asText());
            if (!unique.add(value)) throw validation("resource_ids 不能重复");
            requireResource(value, mimePrefix);
        }
    }

    /**
     * 校验引用的文件资源存在且未删除，且媒体类型符合字段要求（mime 前缀如 "image/" 表示按前缀匹配）。
     */
    private FileRecord requireResource(UUID id, String... mimeTypes) {
        FileRecord resource = resources.findById(id);
        if (resource == null || resource.getDeletedAt() != null) throw validation("资源不存在或已删除：" + id);
        boolean supported = false;
        for (String type : mimeTypes) {
            if (type.endsWith("/") ? resource.getMimeType().startsWith(type) : type.equals(resource.getMimeType())) {
                supported = true;
                break;
            }
        }
        if (!supported) throw validation("资源媒体类型不符合字段要求：" + id);
        return resource;
    }

    private JsonNode requireArray(JsonNode root, String field) {
        JsonNode value = root.path(field);
        if (!value.isArray()) throw validation(field + " 必须是数组");
        return value;
    }

    private JsonNode array(JsonNode root, String preferred, String fallback) {
        JsonNode result = root.path(preferred);
        return result.isArray() ? result : root.path(fallback);
    }

    /**
     * 取条目的唯一标识（优先 preferred 字段，回退 fallback），并校验非空、不重复。
     */
    private String uniqueKey(Set<String> keys, JsonNode node, String preferred, String fallback) {
        String value = firstText(node, preferred, fallback);
        if (value == null || value.isBlank()) throw validation(preferred + " 为必填字段");
        if (!keys.add(value)) throw validation(preferred + " 不能重复");
        return value;
    }

    private void requireText(JsonNode node, String field) {
        if (text(node, field) == null) throw validation(field + " 为必填字段");
    }

    private void validateColor(JsonNode node, String field, boolean required) {
        String value = text(node, field);
        if (required && value == null) throw validation(field + " 为必填字段");
        if (value != null && !value.matches("^#[0-9A-Fa-f]{6}$")) {
            throw validation(field + " 必须使用 #RRGGBB 格式");
        }
    }

    private String text(JsonNode node, String field) {
        String value = node.path(field).asText("").trim();
        return value.isEmpty() ? null : value;
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (value != null) return value;
        }
        return null;
    }

    private boolean enabled(JsonNode node) {
        return node.path("enabled").asBoolean(true);
    }

    private UUID uuid(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = node.path(field).asText("").trim();
            if (!value.isEmpty()) {
                try { return UUID.fromString(value); }
                catch (IllegalArgumentException exception) { throw validation(field + " 必须是 UUID"); }
            }
        }
        return null;
    }

    private List<UUID> uuidList(JsonNode node) {
        if (!node.isArray()) return List.of();
        List<UUID> result = new ArrayList<>();
        node.forEach(value -> {
            try { result.add(UUID.fromString(value.asText())); }
            catch (IllegalArgumentException exception) { throw validation("tag_ids 必须包含 UUID"); }
        });
        return result;
    }

    private static ValidationException validation(String detail) {
        return new ValidationException(ErrorCode.CONTENT_VALIDATION_FAILED, detail);
    }

    private static ConflictException conflict(String detail) {
        return new ConflictException(ErrorCode.CONTENT_DEPENDENCY_CONFLICT, detail);
    }
}
