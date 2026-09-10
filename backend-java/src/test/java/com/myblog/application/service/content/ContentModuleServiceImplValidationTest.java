package com.myblog.application.service.content;

import com.myblog.application.model.dto.ContentDtos;
import com.myblog.application.model.entity.ContentRelease;
import com.myblog.application.port.ObjectStorage;
import com.myblog.application.repository.ContentReleaseRepository;
import com.myblog.application.repository.FileRepository;
import com.myblog.application.repository.MylabTagRepository;
import com.myblog.application.repository.MylabPublicRepository;
import com.myblog.common.exception.ValidationException;
import com.myblog.common.security.CurrentUser;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.myblog.application.service.content.ContentModuleServiceImplCoverageTest.file;
import static com.myblog.application.service.content.ContentModuleServiceImplCoverageTest.tag;
import static com.myblog.application.service.content.ContentModuleServiceImplCoverageTest.validAboutData;
import static com.myblog.application.service.content.ContentModuleServiceImplCoverageTest.validHobbiesData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ContentModuleServiceImpl 各模块结构校验分支测试：
 * 草稿态走 saveDraft（宽松校验），发布态走 publish（严格校验）。
 */
@ExtendWith(MockitoExtension.class)
class ContentModuleServiceImplValidationTest {
    private static final UUID IMAGE_ID = UUID.randomUUID();
    private static final UUID TAG_ID = UUID.randomUUID();

    @Mock ContentReleaseRepository releases;
    @Mock MylabTagRepository tags;
    @Mock FileRepository resources;
    @Mock ObjectStorage storage;
    @Mock MylabPublicRepository mylabPublic;
    @Mock PublicContentCacheService publicCache;
    @Mock ApplicationEventPublisher events;

    private ContentModuleServiceImpl service;
    private CurrentUser admin;

    @BeforeEach
    void setUp() {
        service = new ContentModuleServiceImpl(releases, tags, resources, storage, mylabPublic,
                publicCache, events);
        admin = new CurrentUser(UUID.randomUUID(), "admin", "admin");
    }

    // ---------- home ----------

    /** 发布要求恰好六张图片：只给五张时报“六张图片”。 */
    @Test
    void homePublishRequiresExactlySixImages() {
        stubDraft("home", Map.of("images",
                List.of(Map.of("alt", "a"), Map.of("alt", "b"), Map.of("alt", "c"),
                        Map.of("alt", "d"), Map.of("alt", "e"))));

        assertThatThrownBy(() -> service.publish(admin, "home"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("六张图片"));
    }

    /** 发布时图片缺少资源引用（image_resource_id）报错。 */
    @Test
    void homePublishRequiresImageResource() {
        stubDraft("home", Map.of("images", sixImagesWithoutResources()));

        assertThatThrownBy(() -> service.publish(admin, "home"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("必须选择图片资源"));
    }

    /** 发布时图片必须带 alt 文本：第一张带资源但缺 alt，其余仅凑满六张数量。 */
    @Test
    void homePublishRequiresAltText() {
        UUID id = UUID.randomUUID();
        when(resources.findById(id)).thenReturn(file(id, "image/webp"));
        List<Map<String, Object>> images = new ArrayList<>();
        images.add(Map.of("image_resource_id", id.toString()));
        // 补足剩余五张（仅 alt），隔离出 alt 缺失的校验分支
        for (int i = 1; i < 6; i++) {
            images.add(Map.of("alt", "图" + i));
        }
        stubDraft("home", Map.of("images", images));

        assertThatThrownBy(() -> service.publish(admin, "home"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("alt"));
    }

    /** 六张带资源且带 alt 的图片可正常发布。 */
    @Test
    void homePublishSucceedsWithSixValidImages() {
        List<Map<String, Object>> images = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            UUID id = UUID.randomUUID();
            when(resources.findById(id)).thenReturn(file(id, "image/webp"));
            images.add(Map.of("image_resource_id", id.toString(), "alt", "图" + i));
        }
        stubDraft("home", Map.of("images", images));

        service.publish(admin, "home");

        verify(releases).publish(any(), any(), any(), any());
    }

    /** 草稿拒绝重复引用同一图片资源。 */
    @Test
    void homeRejectsDuplicateImageResource() {
        when(resources.findById(IMAGE_ID)).thenReturn(file(IMAGE_ID, "image/webp"));
        Map<String, Object> image = Map.of("image_resource_id", IMAGE_ID.toString());

        assertThatThrownBy(() -> saveHomeDraft(Map.of("images", List.of(image, image))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("不能重复"));
    }

    /** image_resource_id 不是合法 UUID 时报错。 */
    @Test
    void homeRejectsMalformedUuid() {
        assertThatThrownBy(() -> saveHomeDraft(Map.of("images",
                List.of(Map.of("image_resource_id", "not-a-uuid")))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("必须是 UUID"));
    }

    /** 引用不存在的资源（mock 未打桩）按“资源不存在或已删除”处理。 */
    @Test
    void homeRejectsUnknownResource() {
        assertThatThrownBy(() -> saveHomeDraft(Map.of("images",
                List.of(Map.of("image_resource_id", IMAGE_ID.toString())))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("资源不存在或已删除"));
    }

    /** 已软删除的资源视同不存在。 */
    @Test
    void homeRejectsDeletedResource() {
        var deleted = file(IMAGE_ID, "image/webp");
        deleted.setDeletedAt(OffsetDateTime.now());
        when(resources.findById(IMAGE_ID)).thenReturn(deleted);

        assertThatThrownBy(() -> saveHomeDraft(Map.of("images",
                List.of(Map.of("image_resource_id", IMAGE_ID.toString())))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("资源不存在或已删除"));
    }

    /** 非图片媒体类型（text/plain）不能作为首页图片。 */
    @Test
    void homeRejectsNonImageResource() {
        when(resources.findById(IMAGE_ID)).thenReturn(file(IMAGE_ID, "text/plain"));

        assertThatThrownBy(() -> saveHomeDraft(Map.of("images",
                List.of(Map.of("image_resource_id", IMAGE_ID.toString())))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("媒体类型不符合"));
    }

    // ---------- about ----------

    /** 草稿缺少 profile/ingredients/bubbles 结构时报错。 */
    @Test
    void aboutRequiresProfileIngredientsAndBubbles() {
        assertThatThrownBy(() -> saveAboutDraft(Map.of()))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("profile"));
    }

    /** profile.bullets 不是数组时报错。 */
    @Test
    void aboutRequiresBulletsArray() {
        assertThatThrownBy(() -> saveAboutDraft(Map.of(
                "profile", Map.of("bullets", "not-an-array"),
                "ingredients", Map.of(), "bubbles", List.of())))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("bullets"));
    }

    /** 结构合法的 about 草稿可保存。 */
    @Test
    void aboutDraftAcceptsValidStructure() {
        // 任意图片 id 均返回合法图片资源，避免逐 id 打桩；about 其他用例的同款桩同理
        when(resources.findById(any(UUID.class))).thenAnswer(
                invocation -> file(invocation.getArgument(0), "image/webp"));

        service.saveDraft(admin, "about", new ContentDtos.SaveDraft(
                null, "测试版本", "测试版本描述", validAboutData()));

        verify(releases).add(any());
    }

    /** 发布时缺少头像资源报错。 */
    @Test
    void aboutPublishRequiresAvatar() {
        Map<String, Object> data = validAboutData();
        @SuppressWarnings("unchecked")
        Map<String, Object> profile = (Map<String, Object>) data.get("profile");
        profile.remove("avatar_resource_id");
        stubDraft("about", data);

        assertThatThrownBy(() -> service.publish(admin, "about"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("头像"));
    }

    /** 发布要求恰好三条 bullets，只给两条时报错。 */
    @Test
    void aboutPublishRequiresExactlyThreeBullets() {
        Map<String, Object> data = validAboutData();
        @SuppressWarnings("unchecked")
        Map<String, Object> profile = (Map<String, Object>) data.get("profile");
        profile.put("bullets", List.of("一", "二"));
        stubDraft("about", data);
        when(resources.findById(any(UUID.class))).thenAnswer(
                invocation -> file(invocation.getArgument(0), "image/webp"));

        assertThatThrownBy(() -> service.publish(admin, "about"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("恰好三条"));
    }

    /** bullets 条目为空白串时报错。 */
    @Test
    void aboutPublishRejectsBlankBullet() {
        Map<String, Object> data = validAboutData();
        @SuppressWarnings("unchecked")
        Map<String, Object> profile = (Map<String, Object>) data.get("profile");
        profile.put("bullets", List.of("一", " ", "三"));
        stubDraft("about", data);
        when(resources.findById(any(UUID.class))).thenAnswer(
                invocation -> file(invocation.getArgument(0), "image/webp"));

        assertThatThrownBy(() -> service.publish(admin, "about"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("条目不能为空"));
    }

    /** 合法 about 数据可正常发布。 */
    @Test
    void aboutPublishSucceedsWithValidData() {
        stubDraft("about", validAboutData());
        when(resources.findById(any(UUID.class))).thenAnswer(
                invocation -> file(invocation.getArgument(0), "image/webp"));

        service.publish(admin, "about");

        verify(releases).publish(any(), any(), any(), any());
    }

    /** 气泡 size 仅允许 big 或 mid。 */
    @Test
    void aboutRejectsInvalidBubbleSize() {
        Map<String, Object> data = validAboutData();
        data.put("bubbles", List.of(Map.of("text", "气泡", "size", "huge")));
        when(resources.findById(any(UUID.class))).thenAnswer(
                invocation -> file(invocation.getArgument(0), "image/webp"));

        assertThatThrownBy(() -> saveAboutDraft(data))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("big 或 mid"));
    }

    /** 气泡背景色必须是 #RRGGBB 格式。 */
    @Test
    void aboutRejectsInvalidBubbleColor() {
        Map<String, Object> data = validAboutData();
        data.put("bubbles", List.of(Map.of("text", "气泡", "size", "big",
                "background_color", "red")));
        when(resources.findById(any(UUID.class))).thenAnswer(
                invocation -> file(invocation.getArgument(0), "image/webp"));

        assertThatThrownBy(() -> saveAboutDraft(data))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("#RRGGBB"));
    }

    /** 发布时气泡必须包含 text。 */
    @Test
    void aboutPublishRequiresBubbleText() {
        Map<String, Object> data = validAboutData();
        data.put("bubbles", List.of(Map.of("size", "mid")));
        stubDraft("about", data);
        when(resources.findById(any(UUID.class))).thenAnswer(
                invocation -> file(invocation.getArgument(0), "image/webp"));

        assertThatThrownBy(() -> service.publish(admin, "about"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("text"));
    }

    // ---------- skills ----------

    /** skill_key 重复时报错。 */
    @Test
    void skillsRejectsDuplicateKey() {
        Map<String, Object> item = Map.of("skill_key", "java", "percentage", 80, "enabled", false);

        assertThatThrownBy(() -> saveSkillsDraft(Map.of("items", List.of(item, item))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("不能重复"));
    }

    /** 缺少 skill_key 时报错。 */
    @Test
    void skillsRejectsMissingKey() {
        assertThatThrownBy(() -> saveSkillsDraft(Map.of("items",
                List.of(Map.of("percentage", 80)))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("必填"));
    }

    /** percentage 超出 0 到 100 时报错。 */
    @Test
    void skillsRejectsPercentageOutOfRange() {
        assertThatThrownBy(() -> saveSkillsDraft(Map.of("items",
                List.of(Map.of("skill_key", "java", "percentage", 101)))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("0 到 100"));
    }

    /** level_code 不在允许范围内时报错。 */
    @Test
    void skillsRejectsInvalidLevelCode() {
        assertThatThrownBy(() -> saveSkillsDraft(Map.of("items",
                List.of(Map.of("skill_key", "java", "percentage", 80, "level_code", "master")))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("level_code 不合法"));
    }

    /** 启用的技能发布时必须填 name。 */
    @Test
    void skillsPublishRequiresNameForEnabledItem() {
        stubDraft("skills", Map.of("items",
                List.of(Map.of("skill_key", "java", "percentage", 80, "enabled", true))));

        assertThatThrownBy(() -> service.publish(admin, "skills"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("name"));
    }

    /** level_code 与 level_text 必须成对填写。 */
    @Test
    void skillsPublishRequiresLevelFields() {
        stubDraft("skills", Map.of("items", List.of(Map.of(
                "skill_key", "java", "percentage", 80, "enabled", true,
                "name", "Java", "level_code", "proficient"))));

        assertThatThrownBy(() -> service.publish(admin, "skills"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("level_code 和 level_text"));
    }

    /** 启用的技能发布时必须选择图标资源。 */
    @Test
    void skillsPublishRequiresIcon() {
        stubDraft("skills", Map.of("items", List.of(Map.of(
                "skill_key", "java", "percentage", 80, "enabled", true,
                "name", "Java", "level_code", "proficient", "level_text", "熟练"))));

        assertThatThrownBy(() -> service.publish(admin, "skills"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("图标资源"));
    }

    /** 发布时启用技能最多八项，九项报错。 */
    @Test
    void skillsPublishLimitsEightEnabledItems() {
        when(resources.findById(IMAGE_ID)).thenReturn(file(IMAGE_ID, "image/webp"));
        List<Map<String, Object>> items = new ArrayList<>();
        // 造 9 条启用技能，超出八项上限
        for (int i = 0; i < 9; i++) {
            items.add(Map.of(
                    "skill_key", "skill-" + i, "percentage", 80, "enabled", true,
                    "name", "技能" + i, "level_code", "proficient", "level_text", "熟练",
                    "icon_resource_id", IMAGE_ID.toString()));
        }
        stubDraft("skills", Map.of("items", items));

        assertThatThrownBy(() -> service.publish(admin, "skills"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("八张"));
    }

    /** 合法技能数据可正常发布。 */
    @Test
    void skillsPublishSucceedsWithValidData() {
        when(resources.findById(IMAGE_ID)).thenReturn(file(IMAGE_ID, "image/webp"));
        stubDraft("skills", Map.of("items", List.of(Map.of(
                "skill_key", "java", "percentage", 80, "enabled", true,
                "name", "Java", "level_code", "proficient", "level_text", "熟练",
                "icon_resource_id", IMAGE_ID.toString()))));

        service.publish(admin, "skills");

        verify(releases).publish(any(), any(), any(), any());
    }

    // ---------- footprints ----------

    /** details 必须是数组。 */
    @Test
    void footprintsRequiresDetailsArray() {
        assertThatThrownBy(() -> saveFootprintsDraft(Map.of("details", "not-an-array")))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("details 必须是数组"));
    }

    /** city_key 重复时报错。 */
    @Test
    void footprintsRejectsDuplicateCityKey() {
        Map<String, Object> item = Map.of("city_key", "beijing", "enabled", false);

        assertThatThrownBy(() -> saveFootprintsDraft(Map.of("details", List.of(item, item))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("不能重复"));
    }

    /** 同一城市的 resource_ids 不允许重复。 */
    @Test
    void footprintsRejectsDuplicateResourceIds() {
        when(resources.findById(IMAGE_ID)).thenReturn(file(IMAGE_ID, "image/webp"));
        Map<String, Object> item = Map.of("city_key", "beijing", "enabled", false,
                "resource_ids", List.of(IMAGE_ID.toString(), IMAGE_ID.toString()));

        assertThatThrownBy(() -> saveFootprintsDraft(Map.of("details", List.of(item))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("resource_ids 不能重复"));
    }

    /** 启用的足迹发布时必须填 title。 */
    @Test
    void footprintsPublishRequiresTitle() {
        stubDraft("footprints", Map.of("details",
                List.of(Map.of("city_key", "beijing", "enabled", true))));

        assertThatThrownBy(() -> service.publish(admin, "footprints"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("title"));
    }

    /** 启用的足迹发布时必须填 contents。 */
    @Test
    void footprintsPublishRequiresContents() {
        stubDraft("footprints", Map.of("details", List.of(Map.of(
                "city_key", "beijing", "enabled", true, "title", "北京", "summary", "首都"))));

        assertThatThrownBy(() -> service.publish(admin, "footprints"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("contents"));
    }

    /** 发布时启用足迹最多六条，七条报错。 */
    @Test
    void footprintsPublishLimitsSixEnabledItems() {
        // 造 7 条启用足迹，超出六条上限
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            items.add(Map.of("city_key", "city-" + i, "enabled", true,
                    "title", "城市" + i, "summary", "简介", "contents", "正文"));
        }
        stubDraft("footprints", Map.of("details", items));

        assertThatThrownBy(() -> service.publish(admin, "footprints"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("六条足迹"));
    }

    /** 合法足迹数据可正常发布。 */
    @Test
    void footprintsPublishSucceedsWithValidData() {
        when(resources.findById(IMAGE_ID)).thenReturn(file(IMAGE_ID, "image/webp"));
        stubDraft("footprints", Map.of("details", List.of(Map.of(
                "city_key", "beijing", "enabled", true, "title", "北京", "summary", "首都",
                "contents", "正文", "resource_ids", List.of(IMAGE_ID.toString())))));

        service.publish(admin, "footprints");

        verify(releases).publish(any(), any(), any(), any());
    }

    // ---------- hobbies ----------

    /** 缺少 cards 数组时报错。 */
    @Test
    void hobbiesRequiresCardsArray() {
        assertThatThrownBy(() -> saveHobbiesDraft(Map.of()))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("cards 必须是数组"));
    }

    /** hobby_key 重复时报错。 */
    @Test
    void hobbiesRejectsDuplicateHobbyKey() {
        Map<String, Object> card = Map.of("hobby_key", "reading", "enabled", false);

        assertThatThrownBy(() -> saveHobbiesDraft(hobbiesData(List.of(card, card), List.of(), List.of())))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("不能重复"));
    }

    /** 启用的爱好卡片发布时必须填 title。 */
    @Test
    void hobbiesPublishRequiresTitle() {
        stubDraft("hobbies", hobbiesData(
                List.of(Map.of("hobby_key", "reading", "enabled", true)), List.of(), List.of()));

        assertThatThrownBy(() -> service.publish(admin, "hobbies"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("title"));
    }

    /** 启用的爱好卡片发布时必须选择图片资源。 */
    @Test
    void hobbiesPublishRequiresImageResource() {
        stubDraft("hobbies", hobbiesData(List.of(Map.of(
                "hobby_key", "reading", "enabled", true, "title", "阅读", "description", "读书")),
                List.of(), List.of()));

        assertThatThrownBy(() -> service.publish(admin, "hobbies"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("必须选择图片资源"));
    }

    /** 发布时启用爱好卡片最多五张，六张报错。 */
    @Test
    void hobbiesPublishLimitsFiveCards() {
        when(resources.findById(IMAGE_ID)).thenReturn(file(IMAGE_ID, "image/webp"));
        // 造 6 张启用卡片，超出五张上限
        List<Map<String, Object>> cards = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            cards.add(Map.of("hobby_key", "hobby-" + i, "enabled", true,
                    "title", "爱好" + i, "description", "描述",
                    "resource_id", IMAGE_ID.toString()));
        }
        stubDraft("hobbies", hobbiesData(cards, List.of(), List.of()));

        assertThatThrownBy(() -> service.publish(admin, "hobbies"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("五张爱好卡片"));
    }

    /** 时间标签 data_key 不在允许范围内时报错。 */
    @Test
    void hobbiesRejectsInvalidTimeTagKey() {
        assertThatThrownBy(() -> saveHobbiesDraft(hobbiesData(List.of(),
                List.of(Map.of("data_key", "其他", "label_x", 1, "label_y", 1, "label_scale", 1.0)),
                List.of())))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("data_key 不合法"));
    }

    /** 时间标签 data_key 重复时报错。 */
    @Test
    void hobbiesRejectsDuplicateTimeTagKey() {
        Map<String, Object> timeTag = Map.of(
                "data_key", "爱好1", "label_x", 1, "label_y", 1, "label_scale", 1.0, "enabled", false);

        assertThatThrownBy(() -> saveHobbiesDraft(hobbiesData(List.of(), List.of(timeTag, timeTag), List.of())))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("不能重复"));
    }

    /** 标签坐标 label_x 超出允许范围时报错。 */
    @Test
    void hobbiesRejectsOutOfRangeLabelPosition() {
        assertThatThrownBy(() -> saveHobbiesDraft(hobbiesData(List.of(),
                List.of(Map.of("data_key", "爱好1", "label_x", 600, "label_y", 1, "label_scale", 1.0)),
                List.of())))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("超出允许范围"));
    }

    /** 启用的时间标签必须填 color。 */
    @Test
    void hobbiesRequiresColorForEnabledTimeTag() {
        assertThatThrownBy(() -> saveHobbiesDraft(hobbiesData(List.of(),
                List.of(Map.of("data_key", "爱好1", "label_x", 1, "label_y", 1,
                        "label_scale", 1.0, "enabled", true)),
                List.of())))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("color"));
    }

    /** 启用的时间标签发布时必须填 name。 */
    @Test
    void hobbiesPublishRequiresTimeTagName() {
        stubDraft("hobbies", hobbiesData(List.of(),
                List.of(Map.of("data_key", "爱好1", "label_x", 1, "label_y", 1,
                        "label_scale", 1.0, "enabled", true, "color", "#FFAA00")),
                List.of()));

        assertThatThrownBy(() -> service.publish(admin, "hobbies"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("name"));
    }

    /** 年龄数据点只允许 -1 到 27，28 报错。 */
    @Test
    void hobbiesRejectsAgeOutOfRange() {
        assertThatThrownBy(() -> saveHobbiesDraft(hobbiesData(List.of(), List.of(),
                List.of(Map.of("age", 28, "values", Map.of())))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("-1 到 27"));
    }

    /** 年龄数据点不允许重复。 */
    @Test
    void hobbiesRejectsDuplicateAge() {
        Map<String, Object> point = Map.of("age", 0, "values", fullTimeValues(2.0));

        assertThatThrownBy(() -> saveHobbiesDraft(hobbiesData(List.of(), List.of(), List.of(point, point))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("不能重复"));
    }

    /** values 必须是对象。 */
    @Test
    void hobbiesRequiresValuesObject() {
        assertThatThrownBy(() -> saveHobbiesDraft(hobbiesData(List.of(), List.of(),
                List.of(Map.of("age", 0, "values", "not-an-object")))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("values 必须是对象"));
    }

    /** values 必须包含五项 0 到 10 的数值。 */
    @Test
    void hobbiesRequiresFiveNumericValues() {
        assertThatThrownBy(() -> saveHobbiesDraft(hobbiesData(List.of(), List.of(),
                List.of(Map.of("age", 0, "values", Map.of("爱好1", 2.0))))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("五项 0 到 10"));
    }

    /** 单行 values 合计必须为 10（5×1.0=5 触发报错）。 */
    @Test
    void hobbiesRequiresRowTotalOfTen() {
        assertThatThrownBy(() -> saveHobbiesDraft(hobbiesData(List.of(), List.of(),
                List.of(Map.of("age", 0, "values", fullTimeValues(1.0))))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("合计必须为 10"));
    }

    /** 发布要求年龄完整覆盖 -1 到 27。 */
    @Test
    void hobbiesPublishRequiresFullAgeCoverage() {
        stubDraft("hobbies", hobbiesData(List.of(), List.of(),
                List.of(Map.of("age", 0, "values", fullTimeValues(2.0)))));

        assertThatThrownBy(() -> service.publish(admin, "hobbies"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("完整覆盖 -1 到 27"));
    }

    /** 合法爱好数据可正常发布。 */
    @Test
    void hobbiesPublishSucceedsWithValidData() {
        when(resources.findById(IMAGE_ID)).thenReturn(file(IMAGE_ID, "image/webp"));
        stubDraft("hobbies", validHobbiesData(IMAGE_ID));

        service.publish(admin, "hobbies");

        verify(releases).publish(any(), any(), any(), any());
    }

    // ---------- vibe ----------

    /** tool_key 重复时报错。 */
    @Test
    void vibeRejectsDuplicateToolKey() {
        Map<String, Object> tool = Map.of("tool_key", "cursor", "percentage", 80, "enabled", false);

        assertThatThrownBy(() -> saveVibeDraft(Map.of("tools", List.of(tool, tool))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("不能重复"));
    }

    /** 缺少 percentage 时报错。 */
    @Test
    void vibeRejectsMissingPercentage() {
        assertThatThrownBy(() -> saveVibeDraft(Map.of("tools", List.of(Map.of("tool_key", "cursor")))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("0 到 100"));
    }

    /** 启用的工具发布时必须填 name。 */
    @Test
    void vibePublishRequiresName() {
        stubDraft("vibe", Map.of("tools",
                List.of(Map.of("tool_key", "cursor", "percentage", 80, "enabled", true))));

        assertThatThrownBy(() -> service.publish(admin, "vibe"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("name"));
    }

    /** 启用的工具发布时必须填 description。 */
    @Test
    void vibePublishRequiresDescription() {
        stubDraft("vibe", Map.of("tools", List.of(Map.of(
                "tool_key", "cursor", "percentage", 80, "enabled", true, "name", "Cursor"))));

        assertThatThrownBy(() -> service.publish(admin, "vibe"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("description"));
    }

    /** 发布时启用工具最多六个，七个报错。 */
    @Test
    void vibePublishLimitsSixEnabledTools() {
        // 造 7 个启用工具，超出六个上限
        List<Map<String, Object>> tools = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            tools.add(Map.of("tool_key", "tool-" + i, "percentage", 80, "enabled", true,
                    "name", "工具" + i, "description", "描述"));
        }
        stubDraft("vibe", Map.of("tools", tools));

        assertThatThrownBy(() -> service.publish(admin, "vibe"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("六个"));
    }

    /** 合法工具数据可正常发布。 */
    @Test
    void vibePublishSucceedsWithValidData() {
        stubDraft("vibe", Map.of("tools", List.of(Map.of(
                "tool_key", "cursor", "percentage", 80, "enabled", true,
                "name", "Cursor", "description", "AI 编辑器"))));

        service.publish(admin, "vibe");

        verify(releases).publish(any(), any(), any(), any());
    }

    // ---------- mylab ----------

    /** 缺少 cards 数组时报错。 */
    @Test
    void mylabRequiresCardsArray() {
        assertThatThrownBy(() -> saveMylabDraft(Map.of()))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("cards 必须是数组"));
    }

    /** post_key 重复时报错。 */
    @Test
    void mylabRejectsDuplicatePostKey() {
        Map<String, Object> card = Map.of("post_key", "article-a", "enabled", false);

        assertThatThrownBy(() -> saveMylabDraft(Map.of("cards", List.of(card, card))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("不能重复"));
    }

    /** card_type 仅允许 PROJECT 或 ARTICLE。 */
    @Test
    void mylabRejectsInvalidCardType() {
        assertThatThrownBy(() -> saveMylabDraft(Map.of("cards",
                List.of(Map.of("post_key", "article-a", "card_type", "VIDEO")))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("PROJECT 或 ARTICLE"));
    }

    /** project_show_order 不允许负数。 */
    @Test
    void mylabRejectsNegativeProjectOrder() {
        assertThatThrownBy(() -> saveMylabDraft(Map.of("cards",
                List.of(Map.of("post_key", "project-a", "project_show_order", -1,
                        "project_contents", "介绍")))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("非负"));
    }

    /** 参与展示的 PROJECT 位次不允许重复。 */
    @Test
    void mylabRejectsDuplicateProjectOrder() {
        Map<String, Object> card = Map.of("post_key", "project-a", "project_show_order", 1,
                "project_contents", "介绍");
        Map<String, Object> other = Map.of("post_key", "project-b", "project_show_order", 1,
                "project_contents", "介绍");

        assertThatThrownBy(() -> saveMylabDraft(Map.of("cards", List.of(card, other))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("不能重复"));
    }

    @Test
    void mylabPublishAllowsProjectWithoutShowOrder() {
        // 不填 project_show_order 的 PROJECT 表示不在首页展示，允许发布
        stubDraft("mylab", Map.of("cards", List.of(Map.of(
                "post_key", "project-a", "project_contents", "介绍", "enabled", false))));
        when(releases.findVersions("mylab")).thenReturn(List.of());

        service.publish(admin, "mylab");

        verify(releases).publish(any(ContentRelease.class), any(), any(UUID.class), any(OffsetDateTime.class));
    }

    @Test
    void mylabDraftAllowsMultipleProjectsWithoutShowOrder() {
        // 多个不展示的 PROJECT（排序均为空）不触发重复校验
        saveMylabDraft(Map.of("cards", List.of(
                Map.of("post_key", "project-a", "enabled", false),
                Map.of("post_key", "project-b", "enabled", false))));

        verify(releases).replaceData(any(ContentRelease.class), any());
    }

    /** 填了展示位次的 PROJECT 发布时必须填侧边栏正文。 */
    @Test
    void mylabPublishRequiresProjectContents() {
        stubDraft("mylab", Map.of("cards", List.of(Map.of(
                "post_key", "project-a", "project_show_order", 1, "enabled", false))));

        assertThatThrownBy(() -> service.publish(admin, "mylab"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("project_contents"));
    }

    /** ARTICLE 卡片不允许填写项目侧边栏字段。 */
    @Test
    void mylabRejectsArticleWithProjectFields() {
        assertThatThrownBy(() -> saveMylabDraft(Map.of("cards",
                List.of(Map.of("post_key", "article-a", "project_show_order", 1)))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("ARTICLE 不能填写项目侧边栏字段"));
    }

    /** tag_ids 不允许重复。 */
    @Test
    void mylabRejectsDuplicateTagIds() {
        assertThatThrownBy(() -> saveMylabDraft(Map.of("cards", List.of(Map.of(
                "post_key", "article-a", "enabled", false,
                "tag_ids", List.of(TAG_ID.toString(), TAG_ID.toString()))))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("tag_ids 不能重复"));
    }

    /** tag_ids 必须是合法 UUID。 */
    @Test
    void mylabRejectsMalformedTagId() {
        assertThatThrownBy(() -> saveMylabDraft(Map.of("cards", List.of(Map.of(
                "post_key", "article-a", "enabled", false, "tag_ids", List.of("not-a-uuid"))))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("tag_ids 必须包含 UUID"));
    }

    /** 引用的标签必须启用且未删除（mock 返回空表示无有效标签）。 */
    @Test
    void mylabRejectsInactiveTagReference() {
        when(tags.findActiveByIds(any())).thenReturn(List.of());

        assertThatThrownBy(() -> saveMylabDraft(Map.of("cards", List.of(Map.of(
                "post_key", "article-a", "enabled", false,
                "tag_ids", List.of(TAG_ID.toString()))))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("启用且未删除的标签"));
    }

    /** Markdown 正文超过最大长度限制时报错。 */
    @Test
    void mylabRejectsOversizedMarkdown() {
        assertThatThrownBy(() -> saveMylabDraft(Map.of("cards", List.of(Map.of(
                "post_key", "article-a", "enabled", false,
                "markdown_content", "x".repeat(ContentModuleServiceImpl.MAX_MYLAB_MARKDOWN_CHARACTERS + 1))))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("500000"));
    }

    /** 启用的卡片发布时必须填标题和摘要。 */
    @Test
    void mylabPublishRequiresTitleAndSummary() {
        stubDraft("mylab", Map.of("cards",
                List.of(Map.of("post_key", "article-a", "enabled", true))));

        assertThatThrownBy(() -> service.publish(admin, "mylab"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("标题和摘要"));
    }

    /** 启用的卡片发布时必须填 Markdown 正文。 */
    @Test
    void mylabPublishRequiresMarkdownContent() {
        stubDraft("mylab", Map.of("cards", List.of(Map.of(
                "post_key", "article-a", "enabled", true,
                "card_title", "标题", "card_summary", "摘要"))));

        assertThatThrownBy(() -> service.publish(admin, "mylab"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("Markdown 正文"));
    }

    /** 字段完整的 PROJECT 卡片可正常发布。 */
    @Test
    void mylabPublishSucceedsWithValidData() {
        when(tags.findActiveByIds(any())).thenReturn(List.of(tag(TAG_ID, "Java")));
        when(resources.findById(IMAGE_ID)).thenReturn(file(IMAGE_ID, "image/webp"));
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("post_key", "project-a");
        card.put("project_show_order", 1);
        card.put("project_contents", "项目介绍");
        card.put("card_title", "标题");
        card.put("card_summary", "摘要");
        card.put("enabled", true);
        card.put("tag_ids", List.of(TAG_ID.toString()));
        card.put("image_resource_id", IMAGE_ID.toString());
        card.put("markdown_content", "# 正文");
        stubDraft("mylab", Map.of("cards", List.of(card)));

        service.publish(admin, "mylab");

        verify(releases).publish(any(), any(), any(), any());
    }

    // ---------- 测试辅助 ----------

    /** 发布路径：准备一条携带指定数据的草稿。 */
    private void stubDraft(String module, Object data) {
        ContentRelease draft = new ContentRelease();
        draft.setId(UUID.randomUUID());
        draft.setModuleKey(module);
        draft.setVersionNo(1);
        draft.setState("DRAFT");
        draft.setUpdatedAt(OffsetDateTime.now());
        when(releases.findDraft(module)).thenReturn(draft);
        when(releases.readData(draft)).thenReturn(data);
    }

    private void saveHomeDraft(Object data) {
        service.saveDraft(admin, "home", new ContentDtos.SaveDraft(null, "测试版本", "测试版本描述", data));
    }

    private void saveAboutDraft(Object data) {
        service.saveDraft(admin, "about", new ContentDtos.SaveDraft(null, "测试版本", "测试版本描述", data));
    }

    private void saveSkillsDraft(Object data) {
        service.saveDraft(admin, "skills", new ContentDtos.SaveDraft(null, "测试版本", "测试版本描述", data));
    }

    private void saveFootprintsDraft(Object data) {
        service.saveDraft(admin, "footprints", new ContentDtos.SaveDraft(null, "测试版本", "测试版本描述", data));
    }

    private void saveHobbiesDraft(Object data) {
        service.saveDraft(admin, "hobbies", new ContentDtos.SaveDraft(null, "测试版本", "测试版本描述", data));
    }

    private void saveVibeDraft(Object data) {
        service.saveDraft(admin, "vibe", new ContentDtos.SaveDraft(null, "测试版本", "测试版本描述", data));
    }

    private void saveMylabDraft(Object data) {
        service.saveDraft(admin, "mylab", new ContentDtos.SaveDraft(null, "测试版本", "测试版本描述", data));
    }

    /** 六张只有 alt、没有资源引用的图片，用于触发“必须选择图片资源”。 */
    private static List<Map<String, Object>> sixImagesWithoutResources() {
        List<Map<String, Object>> images = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            images.add(Map.of("alt", "图" + i));
        }
        return images;
    }

    /** 组装 hobbies 草稿数据：cards、time_tags、time_points 三段。 */
    private static Map<String, Object> hobbiesData(
            List<Map<String, Object>> cards,
            List<Map<String, Object>> timeTags,
            List<Map<String, Object>> timePoints) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("cards", cards);
        root.put("time_tags", timeTags);
        root.put("time_points", timePoints);
        return root;
    }

    /** 生成五项爱好取值相同的 values；发布要求五项合计为 10，故 2.0 合法、1.0 非法。 */
    private static Map<String, Object> fullTimeValues(double value) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String key : List.of("爱好1", "爱好2", "爱好3", "爱好4", "爱好5")) {
            values.put(key, value);
        }
        return values;
    }
}
