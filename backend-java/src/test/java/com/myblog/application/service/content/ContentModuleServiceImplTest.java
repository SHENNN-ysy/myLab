package com.myblog.application.service.content;

import com.myblog.application.model.dto.ContentDtos;
import com.myblog.application.model.entity.ContentRelease;
import com.myblog.application.model.event.PublishedContentChangedEvent;
import com.myblog.application.port.ObjectStorage;
import com.myblog.application.repository.ContentReleaseRepository;
import com.myblog.application.repository.FileRepository;
import com.myblog.application.repository.MylabTagRepository;
import com.myblog.application.repository.MylabPublicRepository;
import com.myblog.common.exception.ConflictException;
import com.myblog.common.exception.NotFoundException;
import com.myblog.common.security.CurrentUser;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * ContentModuleServiceImpl 单测：覆盖草稿乐观锁、发布/恢复/归档/删除版本的状态流转，
 * 以及公开内容缓存与对象存储签名 URL 的生成策略。
 */
@ExtendWith(MockitoExtension.class)
class ContentModuleServiceImplTest {
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
        // 桩：公开缓存默认直接执行回源回调，等价于无缓存场景
        lenient().when(publicCache.readAll(any())).thenAnswer(invocation ->
                ((Supplier<Map<String, Object>>) invocation.getArgument(0)).get());
        lenient().when(publicCache.readMylabDetail(any(), any())).thenAnswer(invocation ->
                ((Supplier<Map<String, Object>>) invocation.getArgument(1)).get());
        service = new ContentModuleServiceImpl(releases, tags, resources, storage, mylabPublic,
                publicCache, events);
        admin = new CurrentUser(UUID.randomUUID(), "admin", "admin");
    }

    /** 已下线的 projects 模块按不存在处理。 */
    @Test
    void removedProjectModuleIsRejected() {
        assertThatThrownBy(() -> service.publicModule("projects"))
                .isInstanceOf(NotFoundException.class);
    }

    /** 更新既有草稿缺少 expected_updated_at 乐观锁时间戳时报冲突。 */
    @Test
    void savingExistingDraftRequiresOptimisticLockTimestamp() {
        ContentRelease draft = release("skills", "DRAFT");
        when(releases.findDraft("skills")).thenReturn(draft);

        assertThatThrownBy(() -> service.saveDraft(admin, "skills",
                new ContentDtos.SaveDraft(null, "测试版本", "测试版本描述", Map.of("items", List.of()))))
                .isInstanceOfSatisfying(ConflictException.class,
                        exception -> assertThat(exception.getDetail()).contains("expected_updated_at"));
    }

    /** 发布会归档当前线上版本并发布草稿，同时发出缓存失效事件。 */
    @Test
    void publishingArchivesCurrentAndPublishesDraft() {
        ContentRelease draft = release("skills", "DRAFT");
        ContentRelease current = release("skills", "PUBLISHED");
        when(releases.findDraft("skills")).thenReturn(draft);
        when(releases.findCurrent("skills")).thenReturn(current);
        when(releases.readData(draft)).thenReturn(Map.of("items", List.of()));

        service.publish(admin, "skills");

        verify(releases).publish(any(ContentRelease.class), any(ContentRelease.class),
                any(UUID.class), any(OffsetDateTime.class));
        verify(events).publishEvent(argThat((Object event) -> event instanceof PublishedContentChangedEvent changed
                && "skills".equals(changed.moduleKey())));
    }

    /** 公开 MyLab 只读取已发布版本：直查 PG 摘要，不走公开缓存。 */
    @Test
    @SuppressWarnings("unchecked")
    void publicMylabUsesOnlyPublishedRelease() {
        ContentRelease published = release("mylab", "PUBLISHED");
        when(releases.findPublished("mylab")).thenReturn(published);
        when(mylabPublic.readSummary(published.getId())).thenReturn(Map.of(
                "tags", List.of(),
                "cards", List.of(Map.of(
                        "post_key", "article-one", "card_type", "ARTICLE", "enabled", true))));

        Map<String, Object> result = (Map<String, Object>) service.publicModule("mylab");

        assertThat((List<?>) result.get("cards")).hasSize(1);
        verify(publicCache, never()).readAll(any());
        verify(publicCache, never()).readMylabDetail(any(), any());
    }

    /** 站内相对路径资源原样返回，不生成对象存储 URL。 */
    @Test
    @SuppressWarnings("unchecked")
    void siteRelativeResourceNeverUsesObjectStorageUrl() {
        ContentRelease published = release("home", "PUBLISHED");
        when(releases.findPublished("home")).thenReturn(published);
        when(releases.readData(published)).thenReturn(Map.of(
                "images", List.of(Map.of("image_object_key", "/assets/hero/hero-1.webp"))));
        Map<String, Object> result = (Map<String, Object>) service.publicModule("home");
        List<Map<String, Object>> images = (List<Map<String, Object>>) result.get("images");

        assertThat(images.getFirst().get("image_url")).isEqualTo("/assets/hero/hero-1.webp");
        verify(storage, never()).publicUrl(any());
    }

    /** 缓存命中时仍为每次响应重新生成对象存储签名 URL，而非复用缓存值。 */
    @Test
    @SuppressWarnings("unchecked")
    void cachedRawContentRegeneratesObjectStorageUrlForEveryResponse() {
        Map<String, Object> cached = Map.of("home", Map.of(
                "images", List.of(Map.of("image_object_key", "hero/image.png"))));
        doReturn(cached).when(publicCache).readAll(any());
        when(storage.configured()).thenReturn(true);
        // 桩连续返回两个不同的签名 URL，验证每次响应都重新签名
        when(storage.publicUrl("hero/image.png")).thenReturn("https://signed.example/one",
                "https://signed.example/two");

        Map<String, Object> first = service.publicContent();
        Map<String, Object> second = service.publicContent();

        List<Map<String, Object>> firstImages = (List<Map<String, Object>>)
                ((Map<String, Object>) first.get("home")).get("images");
        List<Map<String, Object>> secondImages = (List<Map<String, Object>>)
                ((Map<String, Object>) second.get("home")).get("images");
        assertThat(firstImages.getFirst().get("image_url")).isEqualTo("https://signed.example/one");
        assertThat(secondImages.getFirst().get("image_url")).isEqualTo("https://signed.example/two");
        verify(releases, never()).findPublished(any());
    }

    /** 从线上版本新建草稿时重新生成业务行 ID，不复用已发布的行 ID。 */
    @Test
    void creatingDraftDoesNotReusePublishedBusinessRowIds() {
        ContentRelease current = release("vibe", "PUBLISHED");
        UUID publishedRowId = UUID.randomUUID();
        when(releases.findCurrent("vibe")).thenReturn(current);
        when(releases.nextVersion("vibe")).thenReturn(2);

        service.saveDraft(admin, "vibe", new ContentDtos.SaveDraft(null, "测试版本", "测试版本描述", Map.of(
                "tools", List.of(Map.of(
                        "row_id", publishedRowId,
                        "tool_key", "cursor",
                        "percentage", 80,
                        "enabled", false)))));

        verify(releases).replaceData(any(ContentRelease.class), argThat(data ->
                !String.valueOf(data).contains(publishedRowId.toString())));
    }

    /** 无草稿时恢复历史版本：原记录原地转为草稿，不复制数据。 */
    @Test
    void restoringVersionSwitchesHistoricalReleaseToDraftWithoutCopyingData() {
        ContentRelease source = release("vibe", "ARCHIVED");
        when(releases.findVersion("vibe", 1)).thenReturn(source);
        when(releases.findDraft("vibe")).thenReturn(null);

        service.restore(admin, "vibe", 1);

        verify(releases).restoreAsDraft(argThat(release -> release.getId().equals(source.getId())),
                org.mockito.ArgumentMatchers.isNull(), any(OffsetDateTime.class));
        verify(releases, never()).add(any());
        verify(releases, never()).replaceData(any(), any());
    }

    /** 已有草稿时恢复：原草稿转为归档，历史版本原地转为草稿。 */
    @Test
    void restoringWithExistingDraftArchivesCurrentDraftAndActivatesSource() {
        ContentRelease draft = release("vibe", "DRAFT");
        ContentRelease source = release("vibe", "ARCHIVED");
        when(releases.findDraft("vibe")).thenReturn(draft);
        when(releases.findVersion("vibe", 1)).thenReturn(source);

        service.restore(admin, "vibe", 1);

        verify(releases, never()).add(any());
        verify(releases).restoreAsDraft(
                argThat(release -> release.getId().equals(source.getId())),
                argThat(release -> release.getId().equals(draft.getId())),
                any(OffsetDateTime.class));
        verify(releases, never()).replaceData(any(), any());
    }

    /** 删除归档版本执行软删除。 */
    @Test
    void deletingArchivedVersionSoftDeletesRelease() {
        ContentRelease archived = release("vibe", "ARCHIVED");
        when(releases.findVersion("vibe", 1)).thenReturn(archived);

        service.deleteVersion(admin, "vibe", 1);

        verify(releases).softDeleteVersion(any(ContentRelease.class), any(OffsetDateTime.class));
    }

    /** 线上版本未下线，直接删除报冲突。 */
    @Test
    void deletingPublishedVersionIsRejected() {
        ContentRelease published = release("vibe", "PUBLISHED");
        when(releases.findVersion("vibe", 1)).thenReturn(published);

        assertThatThrownBy(() -> service.deleteVersion(admin, "vibe", 1))
                .isInstanceOf(ConflictException.class);
        verify(releases, never()).softDeleteVersion(any(), any());
    }

    /** 删除不存在或仍是草稿的版本按不存在处理。 */
    @Test
    void deletingMissingOrDraftVersionIsNotFound() {
        when(releases.findVersion("vibe", 1)).thenReturn(null);
        assertThatThrownBy(() -> service.deleteVersion(admin, "vibe", 1))
                .isInstanceOf(NotFoundException.class);

        ContentRelease draft = release("vibe", "DRAFT");
        when(releases.findVersion("vibe", 2)).thenReturn(draft);
        assertThatThrownBy(() -> service.deleteVersion(admin, "vibe", 2))
                .isInstanceOf(NotFoundException.class);
        verify(releases, never()).softDeleteVersion(any(), any());
    }

    /** 草稿归档原地转为归档版本，不触发缓存失效事件。 */
    @Test
    void archivingDraftConvertsItToArchivedVersion() {
        ContentRelease draft = release("vibe", "DRAFT");
        // 第一次 findDraft 返回草稿供归档，第二次（view 重读）返回 null 模拟归档后状态
        when(releases.findDraft("vibe")).thenReturn(draft, (ContentRelease) null);
        when(releases.findVersions("vibe")).thenReturn(List.of());

        service.archiveDraft(admin, "vibe");

        verify(releases).archiveDraft(argThat(release -> release.getId().equals(draft.getId())),
                any(OffsetDateTime.class));
        // 归档不影响线上内容，不应触发缓存失效事件
        verify(events, never()).publishEvent(any());
    }

    /** 无草稿可归档时报不存在。 */
    @Test
    void archivingWithoutDraftIsNotFound() {
        when(releases.findDraft("vibe")).thenReturn(null);

        assertThatThrownBy(() -> service.archiveDraft(admin, "vibe"))
                .isInstanceOf(NotFoundException.class);
        verify(releases, never()).archiveDraft(any(), any());
    }

    private ContentRelease release(String module, String state) {
        ContentRelease release = new ContentRelease();
        release.setId(UUID.randomUUID());
        release.setModuleKey(module);
        release.setVersionNo(1);
        release.setVersionName("测试版本");
        release.setVersionDescription("测试版本描述");
        release.setState(state);
        release.setUpdatedAt(OffsetDateTime.now());
        return release;
    }
}
