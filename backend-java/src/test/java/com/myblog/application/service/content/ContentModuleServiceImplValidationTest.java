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

    private ContentModuleServiceImpl service;
    private CurrentUser admin;

    @BeforeEach
    void setUp() {
        service = new ContentModuleServiceImpl(releases, tags, resources, storage, mylabPublic);
        admin = new CurrentUser(UUID.randomUUID(), "admin", "admin");
    }

    // ---------- home ----------

    @Test
    void homePublishRequiresExactlySixImages() {
        stubDraft("home", Map.of("images",
                List.of(Map.of("alt", "a"), Map.of("alt", "b"), Map.of("alt", "c"),
                        Map.of("alt", "d"), Map.of("alt", "e"))));

        assertThatThrownBy(() -> service.publish(admin, "home"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("六张图片"));
    }

    @Test
    void homePublishRequiresImageResource() {
        stubDraft("home", Map.of("images", sixImagesWithoutResources()));

        assertThatThrownBy(() -> service.publish(admin, "home"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("必须选择图片资源"));
    }

    @Test
    void homePublishRequiresAltText() {
        UUID id = UUID.randomUUID();
        when(resources.findById(id)).thenReturn(file(id, "image/webp"));
        List<Map<String, Object>> images = new ArrayList<>();
        images.add(Map.of("image_resource_id", id.toString()));
        for (int i = 1; i < 6; i++) {
            images.add(Map.of("alt", "图" + i));
        }
        stubDraft("home", Map.of("images", images));

        assertThatThrownBy(() -> service.publish(admin, "home"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("alt"));
    }

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

    @Test
    void homeRejectsDuplicateImageResource() {
        when(resources.findById(IMAGE_ID)).thenReturn(file(IMAGE_ID, "image/webp"));
        Map<String, Object> image = Map.of("image_resource_id", IMAGE_ID.toString());

        assertThatThrownBy(() -> saveHomeDraft(Map.of("images", List.of(image, image))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("不能重复"));
    }

    @Test
    void homeRejectsMalformedUuid() {
        assertThatThrownBy(() -> saveHomeDraft(Map.of("images",
                List.of(Map.of("image_resource_id", "not-a-uuid")))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("必须是 UUID"));
    }

    @Test
    void homeRejectsUnknownResource() {
        assertThatThrownBy(() -> saveHomeDraft(Map.of("images",
                List.of(Map.of("image_resource_id", IMAGE_ID.toString())))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("资源不存在或已删除"));
    }

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

    @Test
    void homeRejectsNonImageResource() {
        when(resources.findById(IMAGE_ID)).thenReturn(file(IMAGE_ID, "text/plain"));

        assertThatThrownBy(() -> saveHomeDraft(Map.of("images",
                List.of(Map.of("image_resource_id", IMAGE_ID.toString())))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("媒体类型不符合"));
    }

    // ---------- about ----------

    @Test
    void aboutRequiresProfileIngredientsAndBubbles() {
        assertThatThrownBy(() -> saveAboutDraft(Map.of()))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("profile"));
    }

    @Test
    void aboutRequiresBulletsArray() {
        assertThatThrownBy(() -> saveAboutDraft(Map.of(
                "profile", Map.of("bullets", "not-an-array"),
                "ingredients", Map.of(), "bubbles", List.of())))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("bullets"));
    }

    @Test
    void aboutDraftAcceptsValidStructure() {
        when(resources.findById(any(UUID.class))).thenAnswer(
                invocation -> file(invocation.getArgument(0), "image/webp"));

        service.saveDraft(admin, "about", new ContentDtos.SaveDraft(
                null, "测试版本", "测试版本描述", validAboutData()));

        verify(releases).add(any());
    }

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

    @Test
    void aboutPublishSucceedsWithValidData() {
        stubDraft("about", validAboutData());
        when(resources.findById(any(UUID.class))).thenAnswer(
                invocation -> file(invocation.getArgument(0), "image/webp"));

        service.publish(admin, "about");

        verify(releases).publish(any(), any(), any(), any());
    }

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

    @Test
    void skillsRejectsDuplicateKey() {
        Map<String, Object> item = Map.of("skill_key", "java", "percentage", 80, "enabled", false);

        assertThatThrownBy(() -> saveSkillsDraft(Map.of("items", List.of(item, item))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("不能重复"));
    }

    @Test
    void skillsRejectsMissingKey() {
        assertThatThrownBy(() -> saveSkillsDraft(Map.of("items",
                List.of(Map.of("percentage", 80)))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("必填"));
    }

    @Test
    void skillsRejectsPercentageOutOfRange() {
        assertThatThrownBy(() -> saveSkillsDraft(Map.of("items",
                List.of(Map.of("skill_key", "java", "percentage", 101)))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("0 到 100"));
    }

    @Test
    void skillsRejectsInvalidLevelCode() {
        assertThatThrownBy(() -> saveSkillsDraft(Map.of("items",
                List.of(Map.of("skill_key", "java", "percentage", 80, "level_code", "master")))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("level_code 不合法"));
    }

    @Test
    void skillsPublishRequiresNameForEnabledItem() {
        stubDraft("skills", Map.of("items",
                List.of(Map.of("skill_key", "java", "percentage", 80, "enabled", true))));

        assertThatThrownBy(() -> service.publish(admin, "skills"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("name"));
    }

    @Test
    void skillsPublishRequiresLevelFields() {
        stubDraft("skills", Map.of("items", List.of(Map.of(
                "skill_key", "java", "percentage", 80, "enabled", true,
                "name", "Java", "level_code", "proficient"))));

        assertThatThrownBy(() -> service.publish(admin, "skills"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("level_code 和 level_text"));
    }

    @Test
    void skillsPublishRequiresIcon() {
        stubDraft("skills", Map.of("items", List.of(Map.of(
                "skill_key", "java", "percentage", 80, "enabled", true,
                "name", "Java", "level_code", "proficient", "level_text", "熟练"))));

        assertThatThrownBy(() -> service.publish(admin, "skills"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("图标资源"));
    }

    @Test
    void skillsPublishLimitsEightEnabledItems() {
        when(resources.findById(IMAGE_ID)).thenReturn(file(IMAGE_ID, "image/webp"));
        List<Map<String, Object>> items = new ArrayList<>();
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

    @Test
    void footprintsRequiresDetailsArray() {
        assertThatThrownBy(() -> saveFootprintsDraft(Map.of("details", "not-an-array")))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("details 必须是数组"));
    }

    @Test
    void footprintsRejectsDuplicateCityKey() {
        Map<String, Object> item = Map.of("city_key", "beijing", "enabled", false);

        assertThatThrownBy(() -> saveFootprintsDraft(Map.of("details", List.of(item, item))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("不能重复"));
    }

    @Test
    void footprintsRejectsDuplicateResourceIds() {
        when(resources.findById(IMAGE_ID)).thenReturn(file(IMAGE_ID, "image/webp"));
        Map<String, Object> item = Map.of("city_key", "beijing", "enabled", false,
                "resource_ids", List.of(IMAGE_ID.toString(), IMAGE_ID.toString()));

        assertThatThrownBy(() -> saveFootprintsDraft(Map.of("details", List.of(item))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("resource_ids 不能重复"));
    }

    @Test
    void footprintsPublishRequiresTitle() {
        stubDraft("footprints", Map.of("details",
                List.of(Map.of("city_key", "beijing", "enabled", true))));

        assertThatThrownBy(() -> service.publish(admin, "footprints"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("title"));
    }

    @Test
    void footprintsPublishRequiresContents() {
        stubDraft("footprints", Map.of("details", List.of(Map.of(
                "city_key", "beijing", "enabled", true, "title", "北京", "summary", "首都"))));

        assertThatThrownBy(() -> service.publish(admin, "footprints"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("contents"));
    }

    @Test
    void footprintsPublishLimitsSixEnabledItems() {
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

    @Test
    void hobbiesRequiresCardsArray() {
        assertThatThrownBy(() -> saveHobbiesDraft(Map.of()))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("cards 必须是数组"));
    }

    @Test
    void hobbiesRejectsDuplicateHobbyKey() {
        Map<String, Object> card = Map.of("hobby_key", "reading", "enabled", false);

        assertThatThrownBy(() -> saveHobbiesDraft(hobbiesData(List.of(card, card), List.of(), List.of())))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("不能重复"));
    }

    @Test
    void hobbiesPublishRequiresTitle() {
        stubDraft("hobbies", hobbiesData(
                List.of(Map.of("hobby_key", "reading", "enabled", true)), List.of(), List.of()));

        assertThatThrownBy(() -> service.publish(admin, "hobbies"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("title"));
    }

    @Test
    void hobbiesPublishRequiresImageResource() {
        stubDraft("hobbies", hobbiesData(List.of(Map.of(
                "hobby_key", "reading", "enabled", true, "title", "阅读", "description", "读书")),
                List.of(), List.of()));

        assertThatThrownBy(() -> service.publish(admin, "hobbies"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("必须选择图片资源"));
    }

    @Test
    void hobbiesPublishLimitsFiveCards() {
        when(resources.findById(IMAGE_ID)).thenReturn(file(IMAGE_ID, "image/webp"));
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

    @Test
    void hobbiesRejectsInvalidTimeTagKey() {
        assertThatThrownBy(() -> saveHobbiesDraft(hobbiesData(List.of(),
                List.of(Map.of("data_key", "其他", "label_x", 1, "label_y", 1, "label_scale", 1.0)),
                List.of())))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("data_key 不合法"));
    }

    @Test
    void hobbiesRejectsDuplicateTimeTagKey() {
        Map<String, Object> timeTag = Map.of(
                "data_key", "爱好1", "label_x", 1, "label_y", 1, "label_scale", 1.0, "enabled", false);

        assertThatThrownBy(() -> saveHobbiesDraft(hobbiesData(List.of(), List.of(timeTag, timeTag), List.of())))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("不能重复"));
    }

    @Test
    void hobbiesRejectsOutOfRangeLabelPosition() {
        assertThatThrownBy(() -> saveHobbiesDraft(hobbiesData(List.of(),
                List.of(Map.of("data_key", "爱好1", "label_x", 600, "label_y", 1, "label_scale", 1.0)),
                List.of())))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("超出允许范围"));
    }

    @Test
    void hobbiesRequiresColorForEnabledTimeTag() {
        assertThatThrownBy(() -> saveHobbiesDraft(hobbiesData(List.of(),
                List.of(Map.of("data_key", "爱好1", "label_x", 1, "label_y", 1,
                        "label_scale", 1.0, "enabled", true)),
                List.of())))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("color"));
    }

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

    @Test
    void hobbiesRejectsAgeOutOfRange() {
        assertThatThrownBy(() -> saveHobbiesDraft(hobbiesData(List.of(), List.of(),
                List.of(Map.of("age", 28, "values", Map.of())))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("-1 到 27"));
    }

    @Test
    void hobbiesRejectsDuplicateAge() {
        Map<String, Object> point = Map.of("age", 0, "values", fullTimeValues(2.0));

        assertThatThrownBy(() -> saveHobbiesDraft(hobbiesData(List.of(), List.of(), List.of(point, point))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("不能重复"));
    }

    @Test
    void hobbiesRequiresValuesObject() {
        assertThatThrownBy(() -> saveHobbiesDraft(hobbiesData(List.of(), List.of(),
                List.of(Map.of("age", 0, "values", "not-an-object")))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("values 必须是对象"));
    }

    @Test
    void hobbiesRequiresFiveNumericValues() {
        assertThatThrownBy(() -> saveHobbiesDraft(hobbiesData(List.of(), List.of(),
                List.of(Map.of("age", 0, "values", Map.of("爱好1", 2.0))))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("五项 0 到 10"));
    }

    @Test
    void hobbiesRequiresRowTotalOfTen() {
        assertThatThrownBy(() -> saveHobbiesDraft(hobbiesData(List.of(), List.of(),
                List.of(Map.of("age", 0, "values", fullTimeValues(1.0))))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("合计必须为 10"));
    }

    @Test
    void hobbiesPublishRequiresFullAgeCoverage() {
        stubDraft("hobbies", hobbiesData(List.of(), List.of(),
                List.of(Map.of("age", 0, "values", fullTimeValues(2.0)))));

        assertThatThrownBy(() -> service.publish(admin, "hobbies"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("完整覆盖 -1 到 27"));
    }

    @Test
    void hobbiesPublishSucceedsWithValidData() {
        when(resources.findById(IMAGE_ID)).thenReturn(file(IMAGE_ID, "image/webp"));
        stubDraft("hobbies", validHobbiesData(IMAGE_ID));

        service.publish(admin, "hobbies");

        verify(releases).publish(any(), any(), any(), any());
    }

    // ---------- vibe ----------

    @Test
    void vibeRejectsDuplicateToolKey() {
        Map<String, Object> tool = Map.of("tool_key", "cursor", "percentage", 80, "enabled", false);

        assertThatThrownBy(() -> saveVibeDraft(Map.of("tools", List.of(tool, tool))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("不能重复"));
    }

    @Test
    void vibeRejectsMissingPercentage() {
        assertThatThrownBy(() -> saveVibeDraft(Map.of("tools", List.of(Map.of("tool_key", "cursor")))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("0 到 100"));
    }

    @Test
    void vibePublishRequiresName() {
        stubDraft("vibe", Map.of("tools",
                List.of(Map.of("tool_key", "cursor", "percentage", 80, "enabled", true))));

        assertThatThrownBy(() -> service.publish(admin, "vibe"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("name"));
    }

    @Test
    void vibePublishRequiresDescription() {
        stubDraft("vibe", Map.of("tools", List.of(Map.of(
                "tool_key", "cursor", "percentage", 80, "enabled", true, "name", "Cursor"))));

        assertThatThrownBy(() -> service.publish(admin, "vibe"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("description"));
    }

    @Test
    void vibePublishLimitsSixEnabledTools() {
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

    @Test
    void vibePublishSucceedsWithValidData() {
        stubDraft("vibe", Map.of("tools", List.of(Map.of(
                "tool_key", "cursor", "percentage", 80, "enabled", true,
                "name", "Cursor", "description", "AI 编辑器"))));

        service.publish(admin, "vibe");

        verify(releases).publish(any(), any(), any(), any());
    }

    // ---------- mylab ----------

    @Test
    void mylabRequiresCardsArray() {
        assertThatThrownBy(() -> saveMylabDraft(Map.of()))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("cards 必须是数组"));
    }

    @Test
    void mylabRejectsDuplicatePostKey() {
        Map<String, Object> card = Map.of("post_key", "article-a", "enabled", false);

        assertThatThrownBy(() -> saveMylabDraft(Map.of("cards", List.of(card, card))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("不能重复"));
    }

    @Test
    void mylabRejectsInvalidCardType() {
        assertThatThrownBy(() -> saveMylabDraft(Map.of("cards",
                List.of(Map.of("post_key", "article-a", "card_type", "VIDEO")))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("PROJECT 或 ARTICLE"));
    }

    @Test
    void mylabRejectsNegativeProjectOrder() {
        assertThatThrownBy(() -> saveMylabDraft(Map.of("cards",
                List.of(Map.of("post_key", "project-a", "project_show_order", -1,
                        "project_contents", "介绍")))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("非负"));
    }

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
    void mylabPublishRequiresProjectOrder() {
        stubDraft("mylab", Map.of("cards", List.of(Map.of(
                "post_key", "project-a", "project_contents", "介绍", "enabled", false))));

        assertThatThrownBy(() -> service.publish(admin, "mylab"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("project_show_order"));
    }

    @Test
    void mylabPublishRequiresProjectContents() {
        stubDraft("mylab", Map.of("cards", List.of(Map.of(
                "post_key", "project-a", "project_show_order", 1, "enabled", false))));

        assertThatThrownBy(() -> service.publish(admin, "mylab"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("project_contents"));
    }

    @Test
    void mylabRejectsArticleWithProjectFields() {
        assertThatThrownBy(() -> saveMylabDraft(Map.of("cards",
                List.of(Map.of("post_key", "article-a", "project_show_order", 1)))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("ARTICLE 不能填写项目侧边栏字段"));
    }

    @Test
    void mylabRejectsDuplicateTagIds() {
        assertThatThrownBy(() -> saveMylabDraft(Map.of("cards", List.of(Map.of(
                "post_key", "article-a", "enabled", false,
                "tag_ids", List.of(TAG_ID.toString(), TAG_ID.toString()))))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("tag_ids 不能重复"));
    }

    @Test
    void mylabRejectsMalformedTagId() {
        assertThatThrownBy(() -> saveMylabDraft(Map.of("cards", List.of(Map.of(
                "post_key", "article-a", "enabled", false, "tag_ids", List.of("not-a-uuid"))))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("tag_ids 必须包含 UUID"));
    }

    @Test
    void mylabRejectsInactiveTagReference() {
        when(tags.findActiveByIds(any())).thenReturn(List.of());

        assertThatThrownBy(() -> saveMylabDraft(Map.of("cards", List.of(Map.of(
                "post_key", "article-a", "enabled", false,
                "tag_ids", List.of(TAG_ID.toString()))))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("启用且未删除的标签"));
    }

    @Test
    void mylabRejectsOversizedMarkdown() {
        assertThatThrownBy(() -> saveMylabDraft(Map.of("cards", List.of(Map.of(
                "post_key", "article-a", "enabled", false,
                "markdown_content", "x".repeat(ContentModuleServiceImpl.MAX_MYLAB_MARKDOWN_CHARACTERS + 1))))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("500000"));
    }

    @Test
    void mylabPublishRequiresTitleAndSummary() {
        stubDraft("mylab", Map.of("cards",
                List.of(Map.of("post_key", "article-a", "enabled", true))));

        assertThatThrownBy(() -> service.publish(admin, "mylab"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("标题和摘要"));
    }

    @Test
    void mylabPublishRequiresMarkdownContent() {
        stubDraft("mylab", Map.of("cards", List.of(Map.of(
                "post_key", "article-a", "enabled", true,
                "card_title", "标题", "card_summary", "摘要"))));

        assertThatThrownBy(() -> service.publish(admin, "mylab"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("Markdown 正文"));
    }

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

    private static List<Map<String, Object>> sixImagesWithoutResources() {
        List<Map<String, Object>> images = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            images.add(Map.of("alt", "图" + i));
        }
        return images;
    }

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

    private static Map<String, Object> fullTimeValues(double value) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String key : List.of("爱好1", "爱好2", "爱好3", "爱好4", "爱好5")) {
            values.put(key, value);
        }
        return values;
    }
}
