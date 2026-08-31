package com.myblog.application.service.content;

import com.myblog.application.model.dto.ContentDtos;
import com.myblog.application.model.entity.ContentRelease;
import com.myblog.application.port.ObjectStorage;
import com.myblog.application.repository.ContentReleaseRepository;
import com.myblog.application.repository.FileRepository;
import com.myblog.application.repository.MylabTagRepository;
import com.myblog.application.repository.MylabPublicRepository;
import com.myblog.common.exception.ConflictException;
import com.myblog.common.exception.NotFoundException;
import com.myblog.common.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentModuleServiceImplTest {
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

    @Test
    void removedProjectModuleIsRejected() {
        assertThatThrownBy(() -> service.publicModule("projects"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void savingExistingDraftRequiresOptimisticLockTimestamp() {
        ContentRelease draft = release("skills", "DRAFT");
        when(releases.findDraft("skills")).thenReturn(draft);

        assertThatThrownBy(() -> service.saveDraft(admin, "skills",
                new ContentDtos.SaveDraft(null, "测试版本", "测试版本描述", Map.of("items", List.of()))))
                .isInstanceOfSatisfying(ConflictException.class,
                        exception -> assertThat(exception.getDetail()).contains("expected_updated_at"));
    }

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
    }

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
    }

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

    @Test
    void deletingArchivedVersionSoftDeletesRelease() {
        ContentRelease archived = release("vibe", "ARCHIVED");
        when(releases.findVersion("vibe", 1)).thenReturn(archived);

        service.deleteVersion(admin, "vibe", 1);

        verify(releases).softDeleteVersion(any(ContentRelease.class), any(OffsetDateTime.class));
    }

    @Test
    void deletingPublishedVersionIsRejected() {
        ContentRelease published = release("vibe", "PUBLISHED");
        when(releases.findVersion("vibe", 1)).thenReturn(published);

        assertThatThrownBy(() -> service.deleteVersion(admin, "vibe", 1))
                .isInstanceOf(ConflictException.class);
        verify(releases, never()).softDeleteVersion(any(), any());
    }

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
