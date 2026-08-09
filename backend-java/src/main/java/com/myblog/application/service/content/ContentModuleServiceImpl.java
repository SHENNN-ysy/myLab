package com.myblog.application.service.content;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@Service
public class ContentModuleServiceImpl implements ContentModuleService {
    private static final List<String> KEYS = List.of("skills", "footprints", "hobbies", "vibe", "mylab");
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

    @Override
    public Map<String, Object> publicContent() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : KEYS) {
            ContentRelease release = releases.findPublished(key);
            if (release != null) result.put(key, publicData(key, releases.readData(release)));
        }
        return result;
    }

    @Override
    public Object publicModule(String moduleKey) {
        requireKey(moduleKey);
        ContentRelease release = releases.findPublished(moduleKey);
        if (release == null) throw new NotFoundException(ErrorCode.CONTENT_MODULE_OFFLINE, moduleKey);
        return publicData(moduleKey, releases.readData(release));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object publicMylabDetail(String postKey) {
        Map<String, Object> root = (Map<String, Object>) publicModule("mylab");
        List<Map<String, Object>> cards = (List<Map<String, Object>>) root.getOrDefault("cards", List.of());
        return cards.stream().filter(card -> postKey.equals(card.get("post_key")))
                .findFirst().orElseThrow(() -> new NotFoundException(ErrorCode.RESOURCE_NOT_FOUND, postKey));
    }

    @Override
    public List<ContentDtos.ModuleView> list(CurrentUser actor) {
        Authorization.requireAdmin(actor);
        return KEYS.stream().map(this::view).toList();
    }

    @Override
    public ContentDtos.ModuleView get(CurrentUser actor, String moduleKey) {
        Authorization.requireAdmin(actor);
        requireKey(moduleKey);
        return view(moduleKey);
    }

    @Override
    @Transactional
    public ContentDtos.ModuleView saveDraft(CurrentUser actor, String moduleKey, ContentDtos.SaveDraft command) {
        Authorization.requireAdmin(actor);
        requireKey(moduleKey);
        if (command == null || command.data() == null) throw validation("data 为必填字段");
        validate(moduleKey, command.data(), false);

        releases.lockModule(moduleKey);
        ContentRelease draft = releases.findDraft(moduleKey);
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
        } else {
            if (command.expectedUpdatedAt() == null) throw conflict("缺少 expected_updated_at，无法确认草稿版本");
            if (!releases.touchDraft(draft.getId(), command.expectedUpdatedAt(), now)) {
                throw conflict("草稿已被其他操作修改，请刷新后重试");
            }
            draft.setUpdatedAt(now);
        }
        releases.replaceData(draft, command.data());
        return view(moduleKey);
    }

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

    @Override
    public List<ContentDtos.VersionView> versions(CurrentUser actor, String moduleKey) {
        Authorization.requireAdmin(actor);
        requireKey(moduleKey);
        return releases.findVersions(moduleKey).stream().map(this::versionView).toList();
    }

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
        releases.replaceData(draft, data);
        return view(moduleKey);
    }

    @Override
    @Transactional
    public void deleteDraft(CurrentUser actor, String moduleKey) {
        Authorization.requireAdmin(actor);
        requireKey(moduleKey);
        ContentRelease draft = releases.findDraft(moduleKey);
        if (draft == null) throw new NotFoundException(ErrorCode.CONTENT_VERSION_NOT_FOUND, "当前草稿");
        releases.deleteDraft(draft);
    }

    private ContentDtos.ModuleView view(String moduleKey) {
        ContentRelease draft = releases.findDraft(moduleKey);
        ContentRelease current = releases.findCurrent(moduleKey);
        Object draftData = draft == null ? (current == null ? emptyData(moduleKey) : releases.readData(current)) : releases.readData(draft);
        Object publishedData = current == null ? null : releases.readData(current);
        String status = draft != null ? "draft" : current == null ? "draft" : current.getState().toLowerCase();
        return new ContentDtos.ModuleView(moduleKey, draft == null ? null : draft.getId(), current == null ? null : current.getId(),
                draftData, publishedData, draft == null ? null : draft.getVersionNo(),
                current == null ? null : current.getVersionNo(), status,
                draft == null ? null : draft.getUpdatedAt(), current == null ? null : current.getPublishedAt());
    }

    private ContentDtos.VersionView versionView(ContentRelease release) {
        return new ContentDtos.VersionView(release.getId(), release.getModuleKey(), release.getVersionNo(),
                release.getState(), releases.readData(release), release.getSourceReleaseId(), release.getPublishedAt());
    }

    private Object emptyData(String moduleKey) {
        return switch (moduleKey) {
            case "skills" -> Map.of("items", List.of());
            case "footprints" -> Map.of("details", List.of());
            case "hobbies" -> Map.of("cards", List.of());
            case "vibe" -> Map.of("tools", List.of());
            case "mylab" -> Map.of("tags", tags.findAll(false), "cards", List.of());
            default -> Map.of();
        };
    }

    private void requireKey(String moduleKey) {
        if (!KEYS.contains(moduleKey)) throw new NotFoundException(ErrorCode.CONTENT_MODULE_NOT_FOUND, moduleKey);
    }

    private void validate(String moduleKey, Object value, boolean publishing) {
        JsonNode root = OM.valueToTree(value);
        if (!root.isObject()) throw validation("模块内容必须是 JSON 对象");
        switch (moduleKey) {
            case "skills" -> validateSkills(requireArray(root, "items"), publishing);
            case "footprints" -> validateFootprints(array(root, "details", "items"), publishing);
            case "hobbies" -> validateHobbies(requireArray(root, "cards"), publishing);
            case "vibe" -> validateVibe(requireArray(root, "tools"), publishing);
            case "mylab" -> validateMylab(array(root, "cards", "posts"), publishing);
            default -> throw validation("未知内容模块");
        }
    }

    private void validateSkills(JsonNode items, boolean publishing) {
        Set<String> keys = new HashSet<>();
        for (JsonNode item : items) {
            uniqueKey(keys, item, "skill_key", "id");
            int percentage = item.path("percentage").asInt(-1);
            if (percentage < 0 || percentage > 100) throw validation("技能 percentage 必须在 0 到 100 之间");
            if (publishing && enabled(item)) {
                requireText(item, "name");
                if (firstText(item, "level_code", "level") == null || text(item, "level_text") == null) {
                    throw validation("已启用技能必须填写 level_code 和 level_text");
                }
            }
        }
    }

    private void validateFootprints(JsonNode items, boolean publishing) {
        if (!items.isArray()) throw validation("details 必须是数组");
        Set<String> keys = new HashSet<>();
        for (JsonNode item : items) {
            uniqueKey(keys, item, "city_key", "id");
            validateResourceIds(item.path("resource_ids"), "image/");
            if (publishing && enabled(item)) {
                requireText(item, "title");
                requireText(item, "summary");
                if (firstText(item, "contents") == null && !item.path("paragraphs").isArray()) {
                    throw validation("已启用足迹必须填写 contents");
                }
            }
        }
    }

    private void validateHobbies(JsonNode items, boolean publishing) {
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
    }

    private void validateVibe(JsonNode items, boolean publishing) {
        Set<String> keys = new HashSet<>();
        for (JsonNode item : items) {
            uniqueKey(keys, item, "tool_key", "id");
            int percentage = item.path("percentage").asInt(-1);
            if (percentage < 0 || percentage > 100) throw validation("工具 percentage 必须在 0 到 100 之间");
            if (publishing && enabled(item)) {
                requireText(item, "name");
                requireText(item, "description");
            }
        }
    }

    private void validateMylab(JsonNode cards, boolean publishing) {
        if (!cards.isArray()) throw validation("cards 必须是数组");
        Set<String> keys = new HashSet<>();
        Set<Integer> projectOrders = new HashSet<>();
        for (JsonNode card : cards) {
            String key = uniqueKey(keys, card, "post_key", "id");
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

    @SuppressWarnings("unchecked")
    private Object publicData(String moduleKey, Object raw) {
        Map<String, Object> root = OM.convertValue(raw, LinkedHashMap.class);
        String field = switch (moduleKey) {
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
            if ("hobbies".equals(moduleKey)) putUrl(result, "resource_object_key", "image");
            if ("footprints".equals(moduleKey)) {
                List<Map<String, Object>> resources = (List<Map<String, Object>>) result.getOrDefault("resources", List.of());
                result.put("images", resources.stream().map(resource -> url((String) resource.get("object_key"))).filter(Objects::nonNull).toList());
            }
            if ("mylab".equals(moduleKey)) {
                putUrl(result, "image_object_key", "image");
                putUrl(result, "content_object_key", "markdown_url");
                List<?> ids = (List<?>) result.getOrDefault("tag_ids", List.of());
                result.put("tags", ids.stream().map(String::valueOf).map(tagNames::get).filter(Objects::nonNull).toList());
            }
            visible.add(result);
        }
        root.put(field, visible);
        return root;
    }

    private void putUrl(Map<String, Object> item, String source, String target) {
        String key = (String) item.get(source);
        String url = url(key);
        if (url != null) item.put(target, url);
    }

    private String url(String objectKey) {
        if (objectKey == null) return null;
        return storage.configured() ? storage.publicUrl(objectKey) : objectKey;
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

    private String uniqueKey(Set<String> keys, JsonNode node, String preferred, String fallback) {
        String value = firstText(node, preferred, fallback);
        if (value == null || value.isBlank()) throw validation(preferred + " 为必填字段");
        if (!keys.add(value)) throw validation(preferred + " 不能重复");
        return value;
    }

    private void requireText(JsonNode node, String field) {
        if (text(node, field) == null) throw validation(field + " 为必填字段");
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
