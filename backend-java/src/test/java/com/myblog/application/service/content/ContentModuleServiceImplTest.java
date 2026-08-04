package com.myblog.application.service.content;

import com.myblog.application.model.entity.ContentModule;
import com.myblog.application.repository.ContentModuleRepository;
import com.myblog.application.repository.VisitRepository;
import com.myblog.common.exception.ValidationException;
import com.myblog.common.exception.ConflictException;
import com.myblog.common.exception.NotFoundException;
import com.myblog.common.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentModuleServiceImplTest {
    @Mock private ContentModuleRepository modules;
    @Mock private VisitRepository visits;

    private ContentModuleServiceImpl service;
    private CurrentUser admin;

    @BeforeEach
    void setUp() {
        service = new ContentModuleServiceImpl(modules, visits);
        admin = new CurrentUser(UUID.randomUUID(), "admin", "admin");
    }

    @Test
    void publicSupportAddsRuntimeStatisticsToConfiguredBases() {
        ContentModule support = module("support", Map.of("visit_base", 100, "like_count", 9, "page_view_base", 200));
        when(modules.findAll()).thenReturn(List.of(support));
        when(visits.countSessions()).thenReturn(7L);
        when(visits.countAll()).thenReturn(13L);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) service.publicContent().get("support");

        assertThat(result).containsExactly(
                Map.entry("visit_count", 107L),
                Map.entry("like_count", 9L),
                Map.entry("page_view_count", 213L));
    }

    @Test
    void publicContentKeepsServingLastSnapshotWhileNewDraftIsPending() {
        ContentModule skills = module("skills", Map.of("items", List.of(Map.of("name", "线上技术"))));
        skills.setStatus("draft");
        skills.setDraftData(Map.of("items", List.of(Map.of("name", "草稿技术"))));
        when(modules.findAll()).thenReturn(List.of(skills));

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) service.publicContent().get("skills");

        assertThat(result).containsEntry("items", List.of(Map.of("name", "线上技术")));
    }

    @Test
    void savingPublishedContentMarksModuleAsHavingPendingDraft() {
        ContentModule skills = module("skills", Map.of("items", List.of()));
        when(modules.findByKey("skills")).thenReturn(skills);

        ContentModule result = service.saveDraft(admin, "skills", Map.of("items", List.of()));

        assertThat(result.getStatus()).isEqualTo("draft");
        assertThat(result.getPublishedData()).isEqualTo(Map.of("items", List.of()));
        verify(modules).save(skills);
    }

    @Test
    void projectCannotPublishWithoutPublishedLabRecord() {
        ContentModule projects = module("projects", Map.of(
                "items", List.of(Map.of(
                "id", "demo", "card_title", "Demo", "detail_title", "Demo detail",
                "lab_post_id", "missing", "enabled", true))));
        ContentModule lab = module("mylab", Map.of("posts", List.of()));
        when(modules.findByKey("projects")).thenReturn(projects);
        when(modules.findByKey("mylab")).thenReturn(lab);

        assertThatThrownBy(() -> service.publish(admin, "projects"))
                .isInstanceOfSatisfying(ConflictException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(12005);
                    assertThat(exception.getDetail()).isEqualTo("已启用项目必须关联已发布的 myLab 记录");
                });
    }

    @Test
    void unmanagedPageFieldsAreRejected() {
        ContentModule hobbies = module("hobbies", Map.of(
                "cards", List.of()));
        when(modules.findByKey("hobbies")).thenReturn(hobbies);

        assertThatThrownBy(() -> service.saveDraft(admin, "hobbies", Map.of(
                        "title", "不应由后端管理", "cards", List.of())))
                .isInstanceOfSatisfying(ValidationException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(12004);
                    assertThat(exception.getDetail()).contains("hobbies 不允许字段");
                });
    }

    @Test
    void publishingCreatesImmutableVersionAndUpdatesModuleSnapshot() {
        ContentModule skills = module("skills", Map.of(
                "items", List.of(Map.of("id", "java", "name", "Java", "percentage", 80))));
        when(modules.findByKey("skills")).thenReturn(skills);

        ContentModule result = service.publish(admin, "skills");

        assertThat(result.getStatus()).isEqualTo("published");
        assertThat(result.getPublishedVersion()).isEqualTo(1);
        assertThat(result.getPublishedData()).isEqualTo(result.getDraftData());
        ArgumentCaptor<com.myblog.application.model.entity.ContentPublication> version =
                ArgumentCaptor.forClass(com.myblog.application.model.entity.ContentPublication.class);
        verify(modules).addPublication(version.capture());
        assertThat(version.getValue().getVersion()).isEqualTo(1);
        verify(modules).save(skills);
    }

    @Test
    void myLabCannotRemoveRecordReferencedByPublishedProject() {
        ContentModule lab = module("mylab", Map.of(
                "tags", List.of(),
                "posts", List.of(Map.of("id", "another", "title", "Another", "enabled", true,
                        "tags", List.of(), "sections", List.of()))));
        ContentModule projects = module("projects", Map.of("items", List.of(Map.of(
                "enabled", true, "lab_post_id", "required-post"))));
        when(modules.findByKey("mylab")).thenReturn(lab);
        when(modules.findByKey("projects")).thenReturn(projects);

        assertThatThrownBy(() -> service.publish(admin, "mylab"))
                .isInstanceOfSatisfying(ConflictException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(12005);
                    assertThat(exception.getDetail()).isEqualTo("该 myLab 记录正被已发布项目引用");
                });
    }

    @Test
    void aboutModuleIsNoLongerManaged() {
        assertThatThrownBy(() -> service.publicModule("about"))
                .isInstanceOfSatisfying(NotFoundException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(12001));
    }

    @Test
    void footprintDetailIdsMustBeUnique() {
        ContentModule footprints = module("footprints", Map.of("details", List.of(
                Map.of("id", "photo", "title", "西安"),
                Map.of("id", "photo", "title", "重复西安"))));
        when(modules.findByKey("footprints")).thenReturn(footprints);

        assertThatThrownBy(() -> service.publish(admin, "footprints"))
                .isInstanceOfSatisfying(ValidationException.class,
                        exception -> assertThat(exception.getDetail()).isEqualTo("城市详情 ID 不能重复"));
    }

    @Test
    void footprintDetailIdsCannotBeChangedThroughDraftApi() {
        ContentModule footprints = module("footprints", Map.of("details", List.of(
                Map.of("id", "photo", "title", "西安"),
                Map.of("id", "hike", "title", "昆明"))));
        when(modules.findByKey("footprints")).thenReturn(footprints);

        assertThatThrownBy(() -> service.saveDraft(admin, "footprints", Map.of("details", List.of(
                        Map.of("id", "photo", "title", "西安"),
                        Map.of("id", "read", "title", "北京")))))
                .isInstanceOfSatisfying(ValidationException.class,
                        exception -> assertThat(exception.getDetail()).contains("城市 ID 由前端固定配置"));
    }

    @Test
    void removedCollectionFieldsAreRejected() {
        ContentModule projects = module("projects", Map.of("items", List.of()));
        when(modules.findByKey("projects")).thenReturn(projects);

        assertThatThrownBy(() -> service.saveDraft(admin, "projects", Map.of("items", List.of(Map.of(
                        "id", "demo", "card_title", "Demo", "repository_url", "https://example.com")))))
                .isInstanceOfSatisfying(ValidationException.class,
                        exception -> assertThat(exception.getDetail()).contains("集合项不允许字段：repository_url"));
    }

    private ContentModule module(String key, Object data) {
        ContentModule module = new ContentModule();
        module.setModuleKey(key);
        module.setDraftData(data);
        module.setPublishedData(data);
        module.setDraftVersion(1);
        module.setPublishedVersion(0);
        module.setStatus("published");
        return module;
    }
}
