package com.myblog.application.service.content;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myblog.application.model.entity.ContentModule;
import com.myblog.application.model.entity.ContentPublication;
import com.myblog.application.repository.ContentModuleRepository;
import com.myblog.application.repository.VisitRepository;
import com.myblog.common.exception.NotFoundException;
import com.myblog.common.exception.ValidationException;
import com.myblog.common.exception.ConflictException;
import com.myblog.common.enumeration.ErrorCode;
import com.myblog.common.json.JacksonObjectMapper;
import com.myblog.common.security.Authorization;
import com.myblog.common.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ContentModuleServiceImpl implements ContentModuleService {
    private static final Set<String> KEYS = Set.of(
            "skills", "projects", "footprints", "hobbies", "vibe", "mylab", "support");
    private static final ObjectMapper OM = JacksonObjectMapper.get();

    private final ContentModuleRepository modules;
    private final VisitRepository visits;

    public ContentModuleServiceImpl(ContentModuleRepository modules, VisitRepository visits) {
        this.modules = modules;
        this.visits = visits;
    }

    @Override
    public Map<String, Object> publicContent() {
        Map<String, Object> result = new LinkedHashMap<>();
        modules.findAll().stream()
                .filter(module -> !"offline".equals(module.getStatus()) && module.getPublishedData() != null)
                .forEach(module -> result.put(module.getModuleKey(), publicData(module)));
        return result;
    }

    @Override
    public Object publicModule(String moduleKey) {
        ContentModule module = require(moduleKey);
        if ("offline".equals(module.getStatus()) || module.getPublishedData() == null) {
            throw new NotFoundException(ErrorCode.CONTENT_MODULE_OFFLINE, moduleKey);
        }
        return publicData(module);
    }

    @Override
    public List<ContentModule> list(CurrentUser actor) {
        Authorization.requireAdmin(actor);
        return modules.findAll();
    }

    @Override
    public ContentModule getDraft(CurrentUser actor, String moduleKey) {
        Authorization.requireAdmin(actor);
        return require(moduleKey);
    }

    @Override
    @Transactional
    public ContentModule saveDraft(CurrentUser actor, String moduleKey, Object data) {
        Authorization.requireAdmin(actor);
        ContentModule module = require(moduleKey);
        validate(moduleKey, data, false);
        if ("footprints".equals(moduleKey)) validateFootprintIdsUnchanged(module.getDraftData(), data);
        module.setDraftData(data);
        module.setDraftVersion(module.getDraftVersion() + 1);
        if (!"offline".equals(module.getStatus())) module.setStatus("draft");
        module.setUpdatedAt(OffsetDateTime.now());
        modules.save(module);
        return module;
    }

    @Override
    @Transactional
    public ContentModule publish(CurrentUser actor, String moduleKey) {
        Authorization.requireAdmin(actor);
        ContentModule module = require(moduleKey);
        validate(moduleKey, module.getDraftData(), true);
        return createPublication(module, module.getDraftData(), actor.id());
    }

    @Override
    @Transactional
    public ContentModule offline(CurrentUser actor, String moduleKey) {
        Authorization.requireAdmin(actor);
        ContentModule module = require(moduleKey);
        module.setStatus("offline");
        module.setUpdatedAt(OffsetDateTime.now());
        modules.save(module);
        return module;
    }

    @Override
    public List<ContentPublication> versions(CurrentUser actor, String moduleKey) {
        Authorization.requireAdmin(actor);
        require(moduleKey);
        return modules.findVersions(moduleKey);
    }

    @Override
    @Transactional
    public ContentModule rollback(CurrentUser actor, String moduleKey, int version) {
        Authorization.requireAdmin(actor);
        ContentModule module = require(moduleKey);
        ContentPublication target = modules.findVersion(moduleKey, version);
        if (target == null) throw new NotFoundException(ErrorCode.CONTENT_VERSION_NOT_FOUND,
                moduleKey + " v" + version);
        validate(moduleKey, target.getData(), true);
        if ("footprints".equals(moduleKey)) validateFootprintIdsUnchanged(module.getDraftData(), target.getData());
        module.setDraftData(target.getData());
        module.setDraftVersion(module.getDraftVersion() + 1);
        return createPublication(module, target.getData(), actor.id());
    }

    private ContentModule createPublication(ContentModule module, Object data, UUID actorId) {
        int version = module.getPublishedVersion() + 1;
        OffsetDateTime now = OffsetDateTime.now();
        ContentPublication publication = new ContentPublication();
        publication.setId(UUID.randomUUID());
        publication.setModuleKey(module.getModuleKey());
        publication.setVersion(version);
        publication.setData(data);
        publication.setPublishedBy(actorId);
        publication.setPublishedAt(now);
        modules.addPublication(publication);

        module.setPublishedData(data);
        module.setPublishedVersion(version);
        module.setStatus("published");
        module.setPublishedAt(now);
        module.setUpdatedAt(now);
        modules.save(module);
        return module;
    }

    @SuppressWarnings("unchecked")
    private Object publicData(ContentModule module) {
        if (!"support".equals(module.getModuleKey())) return module.getPublishedData();
        Map<String, Object> stored = OM.convertValue(module.getPublishedData(), LinkedHashMap.class);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("visit_count", number(stored.get("visit_base")) + visits.countSessions());
        result.put("like_count", number(stored.get("like_count")));
        result.put("page_view_count", number(stored.get("page_view_base")) + visits.countAll());
        return result;
    }

    private ContentModule require(String moduleKey) {
        if (!KEYS.contains(moduleKey)) throw new NotFoundException(ErrorCode.CONTENT_MODULE_NOT_FOUND, moduleKey);
        ContentModule module = modules.findByKey(moduleKey);
        if (module == null) throw new NotFoundException(ErrorCode.CONTENT_MODULE_NOT_FOUND, moduleKey);
        return module;
    }

    private void validate(String key, Object value, boolean publishing) {
        JsonNode root = OM.valueToTree(value);
        if (!root.isObject()) throw contentValidation("模块内容必须是 JSON 对象");
        validateRootFields(key, root);
        switch (key) {
            case "skills" -> validatePercentItems("skills", requireArray(root, "items"), publishing);
            case "projects" -> validateProjects(root.path("items"), publishing);
            case "footprints" -> validateFootprintDetails(requireArray(root, "details"), publishing);
            case "hobbies" -> validateHobbies(requireArray(root, "cards"), publishing);
            case "vibe" -> validatePercentItems("vibe", requireArray(root, "tools"), publishing);
            case "mylab" -> {
                requireArray(root, "tags");
                requireArray(root, "posts");
                validateLab(root, publishing);
            }
            case "support" -> validateSupport(root);
            default -> { }
        }
    }

    private void validateRootFields(String key, JsonNode root) {
        Set<String> allowed = switch (key) {
            case "skills", "projects" -> Set.of("items");
            case "footprints" -> Set.of("details");
            case "hobbies" -> Set.of("cards");
            case "vibe" -> Set.of("tools");
            case "mylab" -> Set.of("tags", "posts");
            case "support" -> Set.of("visit_base", "like_count", "page_view_base");
            default -> Set.of();
        };
        var fields = root.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            if (!allowed.contains(field)) throw contentValidation(key + " 不允许字段：" + field);
        }
    }

    private JsonNode requireArray(JsonNode root, String field) {
        JsonNode value = root.path(field);
        if (!value.isArray()) throw contentValidation(field + " 必须是数组");
        return value;
    }

    private void validatePercentItems(String moduleKey, JsonNode items, boolean publishing) {
        Set<String> allowed = "skills".equals(moduleKey)
                ? Set.of("id", "name", "percentage", "level", "level_text", "icon", "bar_style", "is_new", "enabled")
                : Set.of("id", "name", "percentage", "description", "enabled");
        Set<String> ids = new HashSet<>();
        for (JsonNode item : items) {
            validateItemFields(moduleKey, item, allowed);
            String id = requireText(item, "id");
            if (!ids.add(id)) throw contentValidation(moduleKey + " 集合项 ID 不能重复");
            int percentage = item.path("percentage").asInt(-1);
            if (percentage < 0 || percentage > 100) {
                throw contentValidation("percentage 必须在 0 到 100 之间");
            }
            if (publishing && item.path("enabled").asBoolean(true)) {
                requireText(item, "name");
            }
        }
    }

    private void validateProjects(JsonNode items, boolean publishing) {
        if (!items.isArray()) throw contentValidation("items 必须是数组");
        JsonNode publishedPosts = OM.createArrayNode();
        if (publishing) {
            ContentModule lab = require("mylab");
            if ("offline".equals(lab.getStatus()) || lab.getPublishedData() == null) {
                throw contentDependency("发布项目之前必须先发布 myLab");
            }
            publishedPosts = OM.valueToTree(lab.getPublishedData()).path("posts");
        }
        Set<String> ids = new HashSet<>();
        for (JsonNode item : items) {
            validateItemFields("projects", item, Set.of(
                    "id", "card_title", "card_summary", "detail_title", "detail_summary", "tag", "accent",
                    "year", "image", "image_alt", "paragraphs", "tech", "images", "lab_post_id", "enabled"));
            String id = requireText(item, "id");
            if (!ids.add(id)) throw contentValidation("projects 集合项 ID 不能重复");
            if (publishing && item.path("enabled").asBoolean(true)) {
                requireText(item, "card_title");
                requireText(item, "detail_title");
                String postId = item.path("lab_post_id").asText("");
                boolean exists = false;
                if (publishedPosts.isArray()) {
                    for (JsonNode post : publishedPosts) {
                        if (postId.equals(post.path("id").asText()) && post.path("enabled").asBoolean(true)) {
                            exists = true;
                            break;
                        }
                    }
                }
                if (!exists) throw contentDependency("已启用项目必须关联已发布的 myLab 记录");
            }
        }
    }

    private void validateFootprintDetails(JsonNode details, boolean publishing) {
        Set<String> ids = new HashSet<>();
        for (JsonNode detail : details) {
            validateItemFields("footprints", detail, Set.of(
                    "id", "title", "summary", "paragraphs", "images", "cta_text", "cta_url"));
            String id = requireText(detail, "id");
            if (!ids.add(id)) throw contentValidation("城市详情 ID 不能重复");
            if (publishing) requireText(detail, "title");
        }
    }

    private void validateHobbies(JsonNode cards, boolean publishing) {
        long enabled = 0;
        Set<String> ids = new HashSet<>();
        for (JsonNode card : cards) {
            validateItemFields("hobbies", card, Set.of(
                    "id", "title", "description", "image", "image_alt", "enabled"));
            String id = requireText(card, "id");
            if (!ids.add(id)) throw contentValidation("hobbies 集合项 ID 不能重复");
            if (card.path("enabled").asBoolean(true)) {
                enabled++;
                if (publishing) requireText(card, "title");
            }
        }
        if (enabled > 5) throw contentValidation("最多只能启用五张爱好卡片");
    }

    private void validateFootprintIdsUnchanged(Object currentData, Object nextData) {
        JsonNode current = OM.valueToTree(currentData).path("details");
        JsonNode next = OM.valueToTree(nextData).path("details");
        if (!footprintIds(current).equals(footprintIds(next))) {
            throw contentValidation("城市 ID 由前端固定配置，不能新增、删除或修改");
        }
    }

    private Set<String> footprintIds(JsonNode details) {
        Set<String> ids = new HashSet<>();
        if (details.isArray()) details.forEach(detail -> ids.add(detail.path("id").asText("")));
        return ids;
    }

    private void validateItemFields(String moduleKey, JsonNode item, Set<String> allowed) {
        if (!item.isObject()) throw contentValidation(moduleKey + " 集合项必须是 JSON 对象");
        var fields = item.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            if (!allowed.contains(field)) throw contentValidation(moduleKey + " 集合项不允许字段：" + field);
        }
    }

    private void validateLab(JsonNode root, boolean publishing) {
        JsonNode posts = root.path("posts");
        if (!posts.isArray()) return;
        if (!publishing) return;
        Set<String> enabledTags = new HashSet<>();
        JsonNode tags = root.path("tags");
        if (tags.isArray()) {
            for (JsonNode tag : tags) {
                if (!tag.path("enabled").asBoolean(true)) continue;
                String name = requireText(tag, "name");
                if (!enabledTags.add(name)) throw contentValidation("myLab 标签名称不能重复");
            }
        }
        Set<String> ids = new HashSet<>();
        for (JsonNode post : posts) {
            if (!post.path("enabled").asBoolean(true)) continue;
            String id = requireText(post, "id");
            if (!ids.add(id)) throw contentValidation("myLab 记录 ID 不能重复");
            requireText(post, "title");
            JsonNode postTags = post.path("tags");
            if (postTags.isArray()) {
                for (JsonNode tag : postTags) {
                    if (!enabledTags.contains(tag.asText())) {
                        throw contentValidation("myLab 记录只能引用已启用标签");
                    }
                }
            }
            JsonNode sections = post.path("sections");
            if (sections.isArray()) for (JsonNode section : sections) requireText(section, "heading");
        }
        validatePublishedProjectReferences(ids);
    }

    private void validatePublishedProjectReferences(Set<String> labPostIds) {
        ContentModule projects = modules.findByKey("projects");
        if (projects == null || "offline".equals(projects.getStatus()) || projects.getPublishedData() == null) return;
        JsonNode items = OM.valueToTree(projects.getPublishedData()).path("items");
        if (!items.isArray()) return;
        for (JsonNode item : items) {
            if (!item.path("enabled").asBoolean(true)) continue;
            if (!labPostIds.contains(item.path("lab_post_id").asText())) {
                throw contentDependency("该 myLab 记录正被已发布项目引用");
            }
        }
    }

    private void validateSupport(JsonNode root) {
        if (root.path("like_count").asLong(-1) < 0) throw contentValidation("点赞数不能为负数");
        if (root.path("visit_base").asLong(-1) < 0 || root.path("page_view_base").asLong(-1) < 0) {
            throw contentValidation("访问量和浏览量基数不能为负数");
        }
    }

    private String requireText(JsonNode node, String field) {
        String value = node.path(field).asText("").trim();
        if (value.isEmpty()) throw contentValidation(field + " 为必填字段");
        return value;
    }

    private static ValidationException contentValidation(String detail) {
        return new ValidationException(ErrorCode.CONTENT_VALIDATION_FAILED, detail);
    }

    private static ConflictException contentDependency(String detail) {
        return new ConflictException(ErrorCode.CONTENT_DEPENDENCY_CONFLICT, detail);
    }

    private long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }
}
