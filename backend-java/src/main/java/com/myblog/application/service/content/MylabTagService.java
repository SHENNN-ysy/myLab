package com.myblog.application.service.content;

import com.myblog.application.model.dto.ContentDtos;
import com.myblog.application.model.entity.MylabTag;
import com.myblog.application.model.event.PublishedContentChangedEvent;
import com.myblog.application.repository.MylabTagRepository;
import com.myblog.common.enumeration.ErrorCode;
import com.myblog.common.exception.ConflictException;
import com.myblog.common.exception.NotFoundException;
import com.myblog.common.exception.ValidationException;
import com.myblog.common.security.Authorization;
import com.myblog.common.security.CurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * MyLab 标签管理服务：标签的查询与增删改，标签供 mylab 模块卡片引用。
 */
@Service
@Slf4j
public class MylabTagService {
    private final MylabTagRepository tags;
    private final ApplicationEventPublisher events;

    public MylabTagService(MylabTagRepository tags, ApplicationEventPublisher events) {
        this.tags = tags;
        this.events = events;
    }

    /**
     * 列出全部标签（含已停用），仅管理员可用。
     */
    public List<MylabTag> list(CurrentUser actor) {
        Authorization.requireAdmin(actor);
        return tags.findAll(true);
    }

    /**
     * 新建标签：校验必填与唯一性，enabled 默认启用；展示顺序由前端按引用次数计算。
     */
    @Transactional
    public MylabTag create(CurrentUser actor, ContentDtos.TagWrite command) {
        Authorization.requireAdmin(actor);
        validate(command, null);
        OffsetDateTime now = OffsetDateTime.now();
        MylabTag tag = new MylabTag();
        tag.setId(UUID.randomUUID());
        tag.setTagKey(command.tagKey().trim());
        tag.setName(command.name().trim());
        tag.setEnabled(Objects.requireNonNullElse(command.enabled(), true));
        tag.setCreatedAt(now);
        tag.setUpdatedAt(now);
        tags.add(tag);
        publishTagChanged();
        log.info("MyLab 标签已创建：operator={}, tagId={}", actor.username(), tag.getId());
        return tag;
    }

    /**
     * 更新标签：未传 enabled 时保持原值。
     */
    @Transactional
    public MylabTag update(CurrentUser actor, UUID id, ContentDtos.TagWrite command) {
        Authorization.requireAdmin(actor);
        MylabTag tag = tags.findById(id);
        if (tag == null) throw new NotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "MyLab 标签");
        validate(command, id);
        tag.setTagKey(command.tagKey().trim());
        tag.setName(command.name().trim());
        tag.setEnabled(Objects.requireNonNullElse(command.enabled(), tag.getEnabled()));
        tag.setUpdatedAt(OffsetDateTime.now());
        tags.save(tag);
        publishTagChanged();
        log.info("MyLab 标签已更新：operator={}, tagId={}", actor.username(), tag.getId());
        return tag;
    }

    /**
     * 删除标签，不存在时抛 NotFoundException。
     */
    @Transactional
    public void delete(CurrentUser actor, UUID id) {
        Authorization.requireAdmin(actor);
        if (!tags.remove(id)) throw new NotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "MyLab 标签");
        publishTagChanged();
        log.info("MyLab 标签已删除：operator={}, tagId={}", actor.username(), id);
    }

    /** 事务提交后清理 MyLab 公开内容与详情缓存，避免继续展示旧标签。 */
    private void publishTagChanged() {
        events.publishEvent(new PublishedContentChangedEvent("mylab"));
    }

    /**
     * 校验标签入参：tag_key/name 必填、标识与名称全局唯一（excludedId 用于更新时排除自身）。
     */
    private void validate(ContentDtos.TagWrite command, UUID excludedId) {
        if (command == null || command.tagKey() == null || command.tagKey().isBlank()
                || command.name() == null || command.name().isBlank()) {
            throw new ValidationException(ErrorCode.CONTENT_VALIDATION_FAILED, "tag_key 和 name 为必填字段");
        }
        if (tags.keyOrNameExists(command.tagKey().trim(), command.name().trim(), excludedId)) {
            throw new ConflictException(ErrorCode.RESOURCE_CONFLICT, "标签标识或名称已存在");
        }
    }
}
