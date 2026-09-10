package com.myblog.application.service.content;

import com.myblog.application.model.dto.ContentDtos;
import com.myblog.application.model.entity.MylabTag;
import com.myblog.application.repository.MylabTagRepository;
import com.myblog.common.exception.ConflictException;
import com.myblog.common.exception.ForbiddenException;
import com.myblog.common.exception.NotFoundException;
import com.myblog.common.exception.ValidationException;
import com.myblog.common.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MylabTagService 单测：覆盖 MyLab 标签增删改查的管理员权限控制、字段校验、
 * 默认值与去空格处理，以及键名唯一性冲突检测。
 */
@ExtendWith(MockitoExtension.class)
class MylabTagServiceTest {
    @Mock MylabTagRepository tags;

    private MylabTagService service;
    private CurrentUser admin;
    private CurrentUser viewer;

    @BeforeEach
    void setUp() {
        service = new MylabTagService(tags);
        admin = new CurrentUser(UUID.randomUUID(), "admin", "admin");
        viewer = new CurrentUser(UUID.randomUUID(), "guest", "viewer");
    }

    /** 非管理员查询标签列表被拒绝。 */
    @Test
    void listRequiresAdmin() {
        assertThatThrownBy(() -> service.list(viewer))
                .isInstanceOf(ForbiddenException.class);
    }

    /** 管理员查询返回全部标签，包括已禁用项。 */
    @Test
    void listReturnsAllTagsIncludingDisabled() {
        MylabTag tag = persistedTag("demo", "演示");
        when(tags.findAll(true)).thenReturn(List.of(tag));

        assertThat(service.list(admin)).containsExactly(tag);
    }

    /** 非管理员创建标签被拒绝。 */
    @Test
    void createRequiresAdmin() {
        assertThatThrownBy(() -> service.create(viewer, new ContentDtos.TagWrite("demo", "演示", null, null)))
                .isInstanceOf(ForbiddenException.class);
    }

    /** 命令为空或必填字段为空白时报参数错误。 */
    @Test
    void createRejectsMissingCommandOrBlankFields() {
        assertThatThrownBy(() -> service.create(admin, null))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.create(admin, new ContentDtos.TagWrite("  ", "演示", null, null)))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.create(admin, new ContentDtos.TagWrite("demo", null, null, null)))
                .isInstanceOf(ValidationException.class);
    }

    /** 排序值为负报参数错误。 */
    @Test
    void createRejectsNegativeSortOrder() {
        assertThatThrownBy(() -> service.create(admin, new ContentDtos.TagWrite("demo", "演示", null, -1)))
                .isInstanceOf(ValidationException.class);
    }

    /** 键或名称与已有标签重复时报冲突，且不入库。 */
    @Test
    void createRejectsDuplicatedKeyOrName() {
        when(tags.keyOrNameExists("demo", "演示", null)).thenReturn(true);

        assertThatThrownBy(() -> service.create(admin, new ContentDtos.TagWrite(" demo ", " 演示 ", null, null)))
                .isInstanceOf(ConflictException.class);
        verify(tags, never()).add(any());
    }

    /** 创建时去除首尾空格，并填充默认启用状态、排序值与时间戳。 */
    @Test
    void createAppliesDefaultsAndTrimsFields() {
        MylabTag result = service.create(admin, new ContentDtos.TagWrite(" demo ", " 演示 ", null, null));

        ArgumentCaptor<MylabTag> added = ArgumentCaptor.forClass(MylabTag.class);
        verify(tags).add(added.capture());
        MylabTag tag = added.getValue();
        assertThat(tag.getId()).isNotNull();
        assertThat(tag.getTagKey()).isEqualTo("demo");
        assertThat(tag.getName()).isEqualTo("演示");
        assertThat(tag.getEnabled()).isTrue();
        assertThat(tag.getSortOrder()).isZero();
        assertThat(tag.getCreatedAt()).isNotNull();
        assertThat(tag.getUpdatedAt()).isNotNull();
        assertThat(result).isSameAs(tag);
    }

    /** 显式传入的启用状态与排序值不被默认值覆盖。 */
    @Test
    void createKeepsExplicitEnabledAndSortOrder() {
        MylabTag result = service.create(admin, new ContentDtos.TagWrite("demo", "演示", false, 5));

        assertThat(result.getEnabled()).isFalse();
        assertThat(result.getSortOrder()).isEqualTo(5);
    }

    /** 非管理员更新标签被拒绝。 */
    @Test
    void updateRequiresAdmin() {
        assertThatThrownBy(() -> service.update(viewer, UUID.randomUUID(),
                new ContentDtos.TagWrite("demo", "演示", null, null)))
                .isInstanceOf(ForbiddenException.class);
    }

    /** 更新不存在的标签报不存在。 */
    @Test
    void updateRejectsUnknownTag() {
        UUID id = UUID.randomUUID();
        when(tags.findById(id)).thenReturn(null);

        assertThatThrownBy(() -> service.update(admin, id, new ContentDtos.TagWrite("demo", "演示", null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    /** 更新先做字段校验；唯一性冲突检测排除标签自身。 */
    @Test
    void updateValidatesCommandAndConflictsExcludingItself() {
        MylabTag tag = persistedTag("demo", "演示");
        when(tags.findById(tag.getId())).thenReturn(tag);

        assertThatThrownBy(() -> service.update(admin, tag.getId(), new ContentDtos.TagWrite("demo", " ", null, null)))
                .isInstanceOf(ValidationException.class);

        when(tags.keyOrNameExists("other", "其他", tag.getId())).thenReturn(true);
        assertThatThrownBy(() -> service.update(admin, tag.getId(), new ContentDtos.TagWrite("other", "其他", null, null)))
                .isInstanceOf(ConflictException.class);
        verify(tags, never()).save(any());
    }

    /** 未传字段保持原值，仅更新传入字段并刷新更新时间。 */
    @Test
    void updateKeepsUnsetFieldsAndRefreshesTimestamp() {
        MylabTag tag = persistedTag("demo", "演示");
        tag.setEnabled(false);
        tag.setSortOrder(7);
        OffsetDateTime previousUpdate = tag.getUpdatedAt();
        when(tags.findById(tag.getId())).thenReturn(tag);

        MylabTag result = service.update(admin, tag.getId(), new ContentDtos.TagWrite(" new-key ", " 新名称 ", null, null));

        assertThat(result.getTagKey()).isEqualTo("new-key");
        assertThat(result.getName()).isEqualTo("新名称");
        assertThat(result.getEnabled()).isFalse();
        assertThat(result.getSortOrder()).isEqualTo(7);
        assertThat(result.getUpdatedAt()).isAfterOrEqualTo(previousUpdate);
        verify(tags).keyOrNameExists("new-key", "新名称", tag.getId());
        verify(tags).save(tag);
    }

    /** 显式传入的启用状态与排序值（含 false/0）生效。 */
    @Test
    void updateAppliesExplicitEnabledAndSortOrder() {
        MylabTag tag = persistedTag("demo", "演示");
        when(tags.findById(tag.getId())).thenReturn(tag);

        MylabTag result = service.update(admin, tag.getId(), new ContentDtos.TagWrite("demo", "演示", true, 0));

        assertThat(result.getEnabled()).isTrue();
        assertThat(result.getSortOrder()).isZero();
    }

    /** 非管理员删除标签被拒绝。 */
    @Test
    void deleteRequiresAdmin() {
        assertThatThrownBy(() -> service.delete(viewer, UUID.randomUUID()))
                .isInstanceOf(ForbiddenException.class);
    }

    /** 删除不存在的标签报不存在。 */
    @Test
    void deleteRejectsUnknownTag() {
        UUID id = UUID.randomUUID();
        when(tags.remove(id)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(admin, id))
                .isInstanceOf(NotFoundException.class);
    }

    /** 删除已有标签时调用仓储移除。 */
    @Test
    void deleteRemovesExistingTag() {
        UUID id = UUID.randomUUID();
        when(tags.remove(id)).thenReturn(true);

        service.delete(admin, id);

        verify(tags).remove(id);
    }

    private MylabTag persistedTag(String key, String name) {
        MylabTag tag = new MylabTag();
        tag.setId(UUID.randomUUID());
        tag.setTagKey(key);
        tag.setName(name);
        tag.setEnabled(true);
        tag.setSortOrder(0);
        OffsetDateTime now = OffsetDateTime.now();
        tag.setCreatedAt(now);
        tag.setUpdatedAt(now);
        return tag;
    }
}
