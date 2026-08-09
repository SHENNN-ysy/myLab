package com.myblog.application.service.content;

import com.myblog.application.model.dto.ContentDtos;
import com.myblog.application.model.entity.ContentRelease;
import com.myblog.application.port.ObjectStorage;
import com.myblog.application.repository.ContentReleaseRepository;
import com.myblog.application.repository.FileRepository;
import com.myblog.application.repository.MylabTagRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentModuleServiceImplTest {
    @Mock ContentReleaseRepository releases;
    @Mock MylabTagRepository tags;
    @Mock FileRepository resources;
    @Mock ObjectStorage storage;

    private ContentModuleServiceImpl service;
    private CurrentUser admin;

    @BeforeEach
    void setUp() {
        service = new ContentModuleServiceImpl(releases, tags, resources, storage);
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
                new ContentDtos.SaveDraft(null, Map.of("items", List.of()))))
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
        when(releases.readData(published)).thenReturn(Map.of(
                "tags", List.of(),
                "cards", List.of(Map.of(
                        "post_key", "article-one", "card_type", "ARTICLE", "enabled", true))));

        Map<String, Object> result = (Map<String, Object>) service.publicModule("mylab");

        assertThat((List<?>) result.get("cards")).hasSize(1);
    }

    private ContentRelease release(String module, String state) {
        ContentRelease release = new ContentRelease();
        release.setId(UUID.randomUUID());
        release.setModuleKey(module);
        release.setVersionNo(1);
        release.setState(state);
        release.setUpdatedAt(OffsetDateTime.now());
        return release;
    }
}
