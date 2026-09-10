package com.myblog.application.service.content;

import com.myblog.application.model.dto.ContentDtos;
import com.myblog.application.model.entity.ContentRelease;
import com.myblog.application.model.entity.FileRecord;
import com.myblog.application.model.entity.MylabTag;
import com.myblog.application.model.event.PublishedContentChangedEvent;
import com.myblog.application.port.ObjectStorage;
import com.myblog.application.repository.ContentReleaseRepository;
import com.myblog.application.repository.FileRepository;
import com.myblog.application.repository.MylabTagRepository;
import com.myblog.application.repository.MylabPublicRepository;
import com.myblog.common.exception.ConflictException;
import com.myblog.common.exception.ForbiddenException;
import com.myblog.common.exception.NotFoundException;
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
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 针对 ContentModuleServiceImpl 的覆盖率补充测试：公开读写路径、管理视图、
 * URL 加工以及各模块的草稿态/发布态结构校验分支。
 */
@ExtendWith(MockitoExtension.class)
class ContentModuleServiceImplCoverageTest {
    private static final UUID AVATAR_ID = UUID.randomUUID();
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
    @SuppressWarnings("unchecked")
    void setUp() {
        // 缓存桩直接透传回源 supplier，让用例聚焦服务层逻辑而非缓存
        lenient().when(publicCache.readAll(any())).thenAnswer(invocation ->
                ((Supplier<Map<String, Object>>) invocation.getArgument(0)).get());
        lenient().when(publicCache.readMylabDetail(any(), any())).thenAnswer(invocation ->
                ((Supplier<Map<String, Object>>) invocation.getArgument(1)).get());
        service = new ContentModuleServiceImpl(releases, tags, resources, storage, mylabPublic,
                publicCache, events);
        admin = new CurrentUser(UUID.randomUUID(), "admin", "admin");
    }

    // ---------- 公开读取路径 ----------

    /** 聚合接口只汇总已发布模块；mylab 卡片过滤禁用项并将 tag_ids 映射为标签名。 */
    @Test
    @SuppressWarnings("unchecked")
    void publicContentAggregatesOnlyPublishedModules() {
        // 兜底桩：未单独桩的模块一律视为无已发布版本
        when(releases.findPublished(any())).thenReturn(null);
        ContentRelease home = release("home", "PUBLISHED");
        when(releases.findPublished("home")).thenReturn(home);
        when(releases.readData(home)).thenReturn(Map.of("images", List.of()));
        ContentRelease about = release("about", "PUBLISHED");
        when(releases.findPublished("about")).thenReturn(about);
        when(releases.readData(about)).thenReturn(Map.of("profile", Map.of()));
        ContentRelease mylab = release("mylab", "PUBLISHED");
        when(releases.findPublished("mylab")).thenReturn(mylab);
        when(mylabPublic.readSummary(mylab.getId())).thenReturn(Map.of(
                "tags", List.of(Map.of("id", TAG_ID.toString(), "name", "Java")),
                "cards", List.of(
                        Map.of("post_key", "article-a", "enabled", true,
                                "tag_ids", List.of(TAG_ID.toString())),
                        Map.of("post_key", "article-b", "enabled", false))));

        Map<String, Object> result = service.publicContent();

        assertThat(result).containsOnlyKeys("home", "about", "mylab");
        Map<String, Object> mylabData = (Map<String, Object>) result.get("mylab");
        List<Map<String, Object>> cards = (List<Map<String, Object>>) mylabData.get("cards");
        assertThat(cards).hasSize(1);
        assertThat((List<String>) cards.getFirst().get("tags")).containsExactly("Java");
    }

    /** 模块无已发布版本时按未上线处理，抛 NotFoundException。 */
    @Test
    void publicModuleWithoutPublishedReleaseIsOffline() {
        assertThatThrownBy(() -> service.publicModule("about"))
                .isInstanceOf(NotFoundException.class);
    }

    /** 按 post_key 返回已发布 mylab 卡片的公开详情。 */
    @Test
    @SuppressWarnings("unchecked")
    void publicMylabDetailReturnsMatchingCard() {
        ContentRelease published = release("mylab", "PUBLISHED");
        when(releases.findPublished("mylab")).thenReturn(published);
        when(mylabPublic.readDetail(published.getId(), "article-a")).thenReturn(Map.of(
                "tags", List.of(),
                "cards", List.of(Map.of("post_key", "article-a", "card_title", "标题", "enabled", true))));

        Map<String, Object> card = (Map<String, Object>) service.publicMylabDetail("article-a");

        assertThat(card.get("card_title")).isEqualTo("标题");
    }

    /** post_key 不存在时抛 NotFoundException。 */
    @Test
    void publicMylabDetailRejectsUnknownPostKey() {
        ContentRelease published = release("mylab", "PUBLISHED");
        when(releases.findPublished("mylab")).thenReturn(published);
        when(mylabPublic.readDetail(published.getId(), "missing")).thenReturn(null);

        assertThatThrownBy(() -> service.publicMylabDetail("missing"))
                .isInstanceOf(NotFoundException.class);
    }

    /** 公开 hobbies 数据过滤 enabled=false 的卡片和时间标签。 */
    @Test
    @SuppressWarnings("unchecked")
    void publicHobbiesFiltersDisabledCardsAndTimeTags() {
        ContentRelease published = release("hobbies", "PUBLISHED");
        when(releases.findPublished("hobbies")).thenReturn(published);
        when(releases.readData(published)).thenReturn(Map.of(
                "cards", List.of(
                        Map.of("hobby_key", "a", "enabled", true),
                        Map.of("hobby_key", "b", "enabled", false)),
                "time_tags", List.of(
                        Map.of("data_key", "爱好1", "enabled", true),
                        Map.of("data_key", "爱好2", "enabled", false)),
                "time_points", List.of()));

        Map<String, Object> result = (Map<String, Object>) service.publicModule("hobbies");

        assertThat((List<?>) result.get("cards")).hasSize(1);
        assertThat((List<?>) result.get("time_tags")).hasSize(1);
    }

    // ---------- 管理视图 ----------

    /** skills/footprints/vibe 的公开数据均过滤禁用条目。 */
    @Test
    @SuppressWarnings("unchecked")
    void publicSkillsFootprintsAndVibeFilterDisabledEntries() {
        ContentRelease skills = release("skills", "PUBLISHED");
        when(releases.findPublished("skills")).thenReturn(skills);
        when(releases.readData(skills)).thenReturn(Map.of("items", List.of(
                Map.of("skill_key", "java", "enabled", true),
                Map.of("skill_key", "go", "enabled", false))));
        ContentRelease footprints = release("footprints", "PUBLISHED");
        when(releases.findPublished("footprints")).thenReturn(footprints);
        when(releases.readData(footprints)).thenReturn(Map.of("details", List.of(
                Map.of("city_key", "beijing", "enabled", true),
                Map.of("city_key", "shanghai", "enabled", false))));
        ContentRelease vibe = release("vibe", "PUBLISHED");
        when(releases.findPublished("vibe")).thenReturn(vibe);
        when(releases.readData(vibe)).thenReturn(Map.of("tools", List.of(
                Map.of("tool_key", "cursor", "enabled", true),
                Map.of("tool_key", "copilot", "enabled", false))));

        Map<String, Object> skillsData = (Map<String, Object>) service.publicModule("skills");
        Map<String, Object> footprintsData = (Map<String, Object>) service.publicModule("footprints");
        Map<String, Object> vibeData = (Map<String, Object>) service.publicModule("vibe");

        assertThat((List<?>) skillsData.get("items")).hasSize(1);
        assertThat((List<?>) footprintsData.get("details")).hasSize(1);
        assertThat((List<?>) vibeData.get("tools")).hasSize(1);
    }

    /** 管理列表固定返回七个模块，无任何版本时状态均为 draft。 */
    @Test
    void listReturnsAllSevenModules() {
        List<ContentDtos.ModuleView> views = service.list(admin);

        assertThat(views).hasSize(7);
        assertThat(views).allSatisfy(view -> assertThat(view.status()).isEqualTo("draft"));
        assertThat(views.stream().map(ContentDtos.ModuleView::moduleKey))
                .containsExactly("home", "about", "skills", "footprints", "hobbies", "vibe", "mylab");
    }

    /** 模块无任何版本记录时返回空草稿视图。 */
    @Test
    @SuppressWarnings("unchecked")
    void getWithoutAnyReleaseReturnsEmptyDraftView() {
        ContentDtos.ModuleView view = service.get(admin, "skills");

        assertThat(view.status()).isEqualTo("draft");
        assertThat(view.draftReleaseId()).isNull();
        assertThat(view.publishedData()).isNull();
        assertThat((List<Object>) ((Map<String, Object>) view.draftData()).get("items")).isEmpty();
    }

    /** 无草稿时草稿视图回退为线上已发布数据，并加工出 image_url。 */
    @Test
    @SuppressWarnings("unchecked")
    void getFallsBackToPublishedDataWhenNoDraft() {
        ContentRelease current = release("home", "PUBLISHED");
        when(releases.findCurrent("home")).thenReturn(current);
        when(releases.readData(current)).thenReturn(Map.of(
                "images", List.of(Map.of("image_object_key", "/assets/a.webp"))));

        ContentDtos.ModuleView view = service.get(admin, "home");

        assertThat(view.status()).isEqualTo("published");
        assertThat(view.draftReleaseId()).isNull();
        assertThat(view.publishedVersion()).isEqualTo(1);
        List<Map<String, Object>> images =
                (List<Map<String, Object>>) ((Map<String, Object>) view.draftData()).get("images");
        assertThat(images.getFirst().get("image_url")).isEqualTo("/assets/a.webp");
    }

    /** 当前版本为 OFFLINE 时管理视图状态显示 offline。 */
    @Test
    void getReflectsOfflineStateOfCurrentRelease() {
        ContentRelease current = release("vibe", "OFFLINE");
        when(releases.findCurrent("vibe")).thenReturn(current);
        when(releases.readData(current)).thenReturn(Map.of("tools", List.of()));

        ContentDtos.ModuleView view = service.get(admin, "vibe");

        assertThat(view.status()).isEqualTo("offline");
    }

    /** 非 admin 角色调用全部管理接口均被拒绝（ForbiddenException）。 */
    @Test
    void managementEndpointsRequireAdmin() {
        CurrentUser viewer = new CurrentUser(UUID.randomUUID(), "bob", "viewer");

        assertThatThrownBy(() -> service.list(viewer)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.get(viewer, "home")).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.saveDraft(viewer, "home",
                new ContentDtos.SaveDraft(null, "测试版本", "测试版本描述", Map.of("images", List.of()))))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.publish(viewer, "home")).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.offline(viewer, "home")).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.versions(viewer, "home")).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.version(viewer, "home", 1)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.restore(viewer, "home", 1)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.deleteDraft(viewer, "home")).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.deleteVersion(viewer, "home", 1))
                .isInstanceOf(ForbiddenException.class);
    }

    // ---------- 草稿保存 ----------

    /** 首次保存创建 DRAFT 版本，并关联当前线上版本作为来源。 */
    @Test
    void saveDraftCreatesDraftLinkedToCurrentVersion() {
        ContentRelease current = release("skills", "PUBLISHED");
        when(releases.findCurrent("skills")).thenReturn(current);

        service.saveDraft(admin, "skills", new ContentDtos.SaveDraft(
                null, "技术栈更新", "调整技术栈内容", Map.of("items", List.of())));

        verify(releases).add(argThat(draft -> "DRAFT".equals(draft.getState())
                && "skills".equals(draft.getModuleKey())
                && "技术栈更新".equals(draft.getVersionName())
                && "调整技术栈内容".equals(draft.getVersionDescription())
                && current.getId().equals(draft.getSourceReleaseId())));
        verify(releases).replaceData(any(ContentRelease.class), any());
    }

    /** 无线上版本时新建草稿不关联来源版本。 */
    @Test
    void saveDraftCreatesDraftWithoutSourceWhenNothingPublished() {
        service.saveDraft(admin, "vibe", new ContentDtos.SaveDraft(
                null, "测试版本", "测试版本描述", Map.of("tools", List.of())));

        verify(releases).add(argThat(draft -> draft.getSourceReleaseId() == null));
    }

    /** 期望时间戳匹配时原地更新已有草稿（乐观锁成功），不新建版本。 */
    @Test
    void saveDraftUpdatesExistingDraftWhenTimestampMatches() {
        ContentRelease draft = release("skills", "DRAFT");
        when(releases.findDraft("skills")).thenReturn(draft);
        when(releases.updateDraft(any(UUID.class), any(), any(OffsetDateTime.class), any(), any())).thenReturn(true);
        when(releases.readData(draft)).thenReturn(Map.of("items", List.of()));

        ContentDtos.ModuleView view = service.saveDraft(admin, "skills",
                new ContentDtos.SaveDraft(OffsetDateTime.now(), "更新名称", "更新描述", Map.of("items", List.of())));

        verify(releases).replaceData(argThat(r -> r.getId().equals(draft.getId())), any());
        verify(releases, never()).add(any());
        assertThat(view.draftReleaseId()).isEqualTo(draft.getId());
    }

    /** 乐观锁更新失败（草稿被并发修改）时抛 ConflictException 并提示刷新重试。 */
    @Test
    void saveDraftFailsWhenDraftModifiedConcurrently() {
        ContentRelease draft = release("skills", "DRAFT");
        when(releases.findDraft("skills")).thenReturn(draft);
        when(releases.updateDraft(any(UUID.class), any(), any(OffsetDateTime.class), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.saveDraft(admin, "skills",
                new ContentDtos.SaveDraft(OffsetDateTime.now(), "更新名称", "更新描述", Map.of("items", List.of()))))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.getDetail()).contains("刷新后重试"));
    }

    /** data 缺失或非 JSON 对象时抛 ValidationException。 */
    @Test
    void saveDraftRejectsMissingOrNonObjectData() {
        assertThatThrownBy(() -> service.saveDraft(admin, "home", null))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.saveDraft(admin, "home",
                new ContentDtos.SaveDraft(null, "测试版本", "测试版本描述", null)))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.saveDraft(admin, "home",
                new ContentDtos.SaveDraft(null, "测试版本", "测试版本描述", List.of("not-an-object"))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("JSON 对象"));
    }

    /** 版本名或版本描述为空白时分别抛 ValidationException。 */
    @Test
    void saveDraftRequiresVersionNameAndDescription() {
        Object data = Map.of("images", List.of());
        assertThatThrownBy(() -> service.saveDraft(admin, "home",
                new ContentDtos.SaveDraft(null, " ", "描述", data)))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("version_name"));
        assertThatThrownBy(() -> service.saveDraft(admin, "home",
                new ContentDtos.SaveDraft(null, "名称", " ", data)))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getDetail()).contains("version_description"));
    }

    // ---------- 发布 / 下线 ----------

    /** 没有可发布草稿时发布抛 ConflictException。 */
    @Test
    void publishWithoutDraftConflicts() {
        assertThatThrownBy(() -> service.publish(admin, "home"))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.getDetail()).contains("没有可发布草稿"));
    }

    /** 没有已发布版本时下线抛 ConflictException，且不触发下线操作。 */
    @Test
    void offlineWithoutPublishedReleaseConflicts() {
        assertThatThrownBy(() -> service.offline(admin, "home"))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.getDetail()).contains("没有已发布版本"));
        verify(releases, never()).offline(any(), any());
    }

    /** 下线将当前已发布版本置为 OFFLINE，并发布内容变更事件。 */
    @Test
    void offlineMarksCurrentReleaseOffline() {
        ContentRelease current = release("home", "PUBLISHED");
        when(releases.findPublished("home")).thenReturn(current);

        service.offline(admin, "home");

        verify(releases).offline(argThat(r -> r.getId().equals(current.getId())), any(OffsetDateTime.class));
        verify(events).publishEvent(argThat((Object event) -> event instanceof PublishedContentChangedEvent changed
                && "home".equals(changed.moduleKey())));
    }

    // ---------- 历史版本 ----------

    /** 历史列表返回全部版本视图，保留版本号与状态。 */
    @Test
    void versionsReturnsAllVersionViews() {
        ContentRelease v1 = release("vibe", "ARCHIVED");
        ContentRelease v2 = release("vibe", "PUBLISHED");
        v2.setVersionNo(2);
        when(releases.findVersions("vibe")).thenReturn(List.of(v1, v2));
        when(releases.readData(any(ContentRelease.class))).thenReturn(Map.of("tools", List.of()));

        List<ContentDtos.VersionView> views = service.versions(admin, "vibe");

        assertThat(views).hasSize(2);
        assertThat(views.stream().map(ContentDtos.VersionView::versionNo)).containsExactly(1, 2);
        assertThat(views.stream().map(ContentDtos.VersionView::state))
                .containsExactly("ARCHIVED", "PUBLISHED");
    }

    /** 按版本号读取历史归档版本。 */
    @Test
    void versionReturnsHistoricalRelease() {
        ContentRelease archived = release("vibe", "ARCHIVED");
        when(releases.findVersion("vibe", 1)).thenReturn(archived);
        when(releases.readData(archived)).thenReturn(Map.of("tools", List.of()));

        ContentDtos.VersionView view = service.version(admin, "vibe", 1);

        assertThat(view.state()).isEqualTo("ARCHIVED");
        assertThat(view.versionNo()).isEqualTo(1);
    }

    /** 版本号不存在抛 NotFoundException；草稿版本也可按版本号查看。 */
    @Test
    void versionRejectsMissingAndReturnsDraftRelease() {
        assertThatThrownBy(() -> service.version(admin, "vibe", 1))
                .isInstanceOf(NotFoundException.class);
        ContentRelease draft = release("vibe", "DRAFT");
        when(releases.findVersion("vibe", 2)).thenReturn(draft);
        when(releases.readData(draft)).thenReturn(Map.of("tools", List.of()));
        assertThat(service.version(admin, "vibe", 2).state()).isEqualTo("DRAFT");
    }

    /** 恢复来源不存在、为草稿或为已发布版本时分别拒绝，且不创建新版本。 */
    @Test
    void restoreRejectsMissingDraftOrPublishedSource() {
        assertThatThrownBy(() -> service.restore(admin, "vibe", 1))
                .isInstanceOf(NotFoundException.class);
        when(releases.findVersion("vibe", 2)).thenReturn(release("vibe", "DRAFT"));
        assertThatThrownBy(() -> service.restore(admin, "vibe", 2))
                .isInstanceOf(ConflictException.class);
        when(releases.findVersion("vibe", 3)).thenReturn(release("vibe", "PUBLISHED"));
        assertThatThrownBy(() -> service.restore(admin, "vibe", 3))
                .isInstanceOf(ConflictException.class);
        verify(releases, never()).add(any());
    }

    /** 删除已存在的草稿。 */
    @Test
    void deleteDraftRemovesExistingDraft() {
        ContentRelease draft = release("home", "DRAFT");
        when(releases.findDraft("home")).thenReturn(draft);

        service.deleteDraft(admin, "home");

        verify(releases).deleteDraft(argThat(r -> r.getId().equals(draft.getId())));
    }

    /** 无草稿时删除抛 NotFoundException，且不执行删除。 */
    @Test
    void deleteDraftWithoutDraftIsNotFound() {
        assertThatThrownBy(() -> service.deleteDraft(admin, "home"))
                .isInstanceOf(NotFoundException.class);
        verify(releases, never()).deleteDraft(any());
    }

    // ---------- URL 加工 ----------

    /** 配置对象存储时 object key 被展开为完整公开 URL。 */
    @Test
    @SuppressWarnings("unchecked")
    void objectStorageKeyIsExpandedToPublicUrl() {
        ContentRelease published = release("home", "PUBLISHED");
        when(releases.findPublished("home")).thenReturn(published);
        when(releases.readData(published)).thenReturn(Map.of(
                "images", List.of(Map.of("image_object_key", "home/hero.webp"))));
        when(storage.configured()).thenReturn(true);
        when(storage.publicUrl("home/hero.webp")).thenReturn("https://cdn.example.com/home/hero.webp");

        Map<String, Object> result = (Map<String, Object>) service.publicModule("home");

        List<Map<String, Object>> images = (List<Map<String, Object>>) result.get("images");
        assertThat(images.getFirst().get("image_url")).isEqualTo("https://cdn.example.com/home/hero.webp");
    }

    /** 未配置对象存储时 object key 原样返回。 */
    @Test
    @SuppressWarnings("unchecked")
    void objectKeyIsReturnedAsIsWhenStorageNotConfigured() {
        ContentRelease published = release("home", "PUBLISHED");
        when(releases.findPublished("home")).thenReturn(published);
        when(releases.readData(published)).thenReturn(Map.of(
                "images", List.of(Map.of("image_object_key", "home/hero.webp"))));

        Map<String, Object> result = (Map<String, Object>) service.publicModule("home");

        List<Map<String, Object>> images = (List<Map<String, Object>>) result.get("images");
        assertThat(images.getFirst().get("image_url")).isEqualTo("home/hero.webp");
        verify(storage, never()).publicUrl(any());
    }

    /** 已是 http(s) 绝对地址的 key 保持原样，不再加工。 */
    @Test
    @SuppressWarnings("unchecked")
    void absoluteHttpUrlIsKeptAsIs() {
        ContentRelease published = release("home", "PUBLISHED");
        when(releases.findPublished("home")).thenReturn(published);
        when(releases.readData(published)).thenReturn(Map.of(
                "images", List.of(Map.of("image_object_key", "https://cdn.example.com/a.webp"))));

        Map<String, Object> result = (Map<String, Object>) service.publicModule("home");

        List<Map<String, Object>> images = (List<Map<String, Object>>) result.get("images");
        assertThat(images.getFirst().get("image_url")).isEqualTo("https://cdn.example.com/a.webp");
        verify(storage, never()).publicUrl(any());
    }

    /** about 管理视图将 avatar_object_key 映射为 avatar_url。 */
    @Test
    @SuppressWarnings("unchecked")
    void aboutAdminDataMapsAvatarUrl() {
        ContentRelease current = release("about", "PUBLISHED");
        when(releases.findCurrent("about")).thenReturn(current);
        when(releases.readData(current)).thenReturn(Map.of(
                "profile", Map.of("avatar_object_key", "/assets/avatar.webp"),
                "ingredients", Map.of(), "bubbles", List.of()));

        ContentDtos.ModuleView view = service.get(admin, "about");

        Map<String, Object> profile =
                (Map<String, Object>) ((Map<String, Object>) view.draftData()).get("profile");
        assertThat(profile.get("avatar_url")).isEqualTo("/assets/avatar.webp");
    }

    /** skills 管理视图将 icon_object_key 映射为 icon_url。 */
    @Test
    @SuppressWarnings("unchecked")
    void skillsAdminDataMapsIconUrl() {
        ContentRelease current = release("skills", "PUBLISHED");
        when(releases.findCurrent("skills")).thenReturn(current);
        when(releases.readData(current)).thenReturn(Map.of(
                "items", List.of(Map.of("skill_key", "java", "icon_object_key", "/assets/java.webp"))));

        ContentDtos.ModuleView view = service.get(admin, "skills");

        List<Map<String, Object>> items =
                (List<Map<String, Object>>) ((Map<String, Object>) view.draftData()).get("items");
        assertThat(items.getFirst().get("icon_url")).isEqualTo("/assets/java.webp");
    }

    /** footprints 管理视图从 resources 派生 resource_ids 与 images 列表。 */
    @Test
    @SuppressWarnings("unchecked")
    void footprintsAdminDataDerivesResourceIdsAndImages() {
        String resourceId = UUID.randomUUID().toString();
        ContentRelease current = release("footprints", "PUBLISHED");
        when(releases.findCurrent("footprints")).thenReturn(current);
        when(releases.readData(current)).thenReturn(Map.of(
                "details", List.of(Map.of("city_key", "beijing",
                        "resources", List.of(Map.of("id", resourceId, "object_key", "/assets/bj.webp"))))));

        ContentDtos.ModuleView view = service.get(admin, "footprints");

        List<Map<String, Object>> details =
                (List<Map<String, Object>>) ((Map<String, Object>) view.draftData()).get("details");
        assertThat((List<Object>) details.getFirst().get("resource_ids")).containsExactly(resourceId);
        assertThat((List<Object>) details.getFirst().get("images")).containsExactly("/assets/bj.webp");
    }

    /** hobbies 管理视图同时输出 image_url 与 image 别名。 */
    @Test
    @SuppressWarnings("unchecked")
    void hobbiesAdminDataCopiesImageAlias() {
        ContentRelease current = release("hobbies", "PUBLISHED");
        when(releases.findCurrent("hobbies")).thenReturn(current);
        when(releases.readData(current)).thenReturn(Map.of(
                "cards", List.of(Map.of("hobby_key", "reading", "image_object_key", "/assets/book.webp")),
                "time_tags", List.of(), "time_points", List.of()));

        ContentDtos.ModuleView view = service.get(admin, "hobbies");

        List<Map<String, Object>> cards =
                (List<Map<String, Object>>) ((Map<String, Object>) view.draftData()).get("cards");
        assertThat(cards.getFirst().get("image_url")).isEqualTo("/assets/book.webp");
        assertThat(cards.getFirst().get("image")).isEqualTo("/assets/book.webp");
    }

    /** mylab 管理视图保留卡片 Markdown 正文。 */
    @Test
    @SuppressWarnings("unchecked")
    void mylabAdminDataKeepsMarkdownContent() {
        ContentRelease current = release("mylab", "PUBLISHED");
        when(releases.findCurrent("mylab")).thenReturn(current);
        when(releases.readData(current)).thenReturn(Map.of(
                "cards", List.of(Map.of("post_key", "article-a", "markdown_content", "# 正文"))));

        ContentDtos.ModuleView view = service.get(admin, "mylab");

        List<Map<String, Object>> cards =
                (List<Map<String, Object>>) ((Map<String, Object>) view.draftData()).get("cards");
        assertThat(cards.getFirst().get("markdown_content")).isEqualTo("# 正文");
    }

    // ---------- 测试辅助 ----------

    // 构造指定模块与状态的版本实体，版本号固定为 1
    private ContentRelease release(String module, String state) {
        ContentRelease release = new ContentRelease();
        release.setId(UUID.randomUUID());
        release.setModuleKey(module);
        release.setVersionNo(1);
        release.setState(state);
        release.setUpdatedAt(OffsetDateTime.now());
        return release;
    }

    static FileRecord file(UUID id, String mimeType) {
        FileRecord record = new FileRecord();
        record.setId(id);
        record.setMimeType(mimeType);
        return record;
    }

    static MylabTag tag(UUID id, String name) {
        MylabTag tag = new MylabTag();
        tag.setId(id);
        tag.setTagKey(name);
        tag.setName(name);
        tag.setEnabled(true);
        return tag;
    }

    static Map<String, Object> validAboutData() {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("avatar_resource_id", AVATAR_ID.toString());
        profile.put("title", "关于我");
        profile.put("avatar_alt", "头像");
        profile.put("intro", "你好");
        profile.put("outro", "谢谢");
        profile.put("bullets", List.of("一", "二", "三"));
        Map<String, Object> ingredients = new LinkedHashMap<>();
        ingredients.put("title", "配料");
        ingredients.put("description", "描述");
        Map<String, Object> bubble = new LinkedHashMap<>();
        bubble.put("text", "气泡");
        bubble.put("size", "big");
        bubble.put("background_color", "#AABBCC");
        bubble.put("text_color", "#112233");
        bubble.put("glow_color", "#445566");
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("profile", profile);
        root.put("ingredients", ingredients);
        root.put("bubbles", List.of(bubble));
        return root;
    }

    static Map<String, Object> validHobbiesData(UUID cardImageId) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("hobby_key", "reading");
        card.put("resource_id", cardImageId.toString());
        card.put("title", "阅读");
        card.put("description", "读书");
        card.put("enabled", true);
        Map<String, Object> timeTag = new LinkedHashMap<>();
        timeTag.put("data_key", "爱好1");
        timeTag.put("label_x", 10);
        timeTag.put("label_y", 20);
        timeTag.put("label_scale", 1.0);
        timeTag.put("color", "#FFAA00");
        timeTag.put("name", "阅读");
        timeTag.put("enabled", true);
        List<Map<String, Object>> points = new ArrayList<>();
        // 魔法值：构造 -1~27 岁共 29 个时间点，5 个爱好取值均为 2.0
        for (int age = -1; age <= 27; age++) {
            Map<String, Object> values = new LinkedHashMap<>();
            for (String key : List.of("爱好1", "爱好2", "爱好3", "爱好4", "爱好5")) {
                values.put(key, 2.0);
            }
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("age", age);
            point.put("values", values);
            points.add(point);
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("cards", List.of(card));
        root.put("time_tags", List.of(timeTag));
        root.put("time_points", points);
        return root;
    }
}
