package com.myblog.application.repository;

import com.myblog.application.model.entity.ContentRelease;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 内容发布仓储接口：版本化内容（草稿/当前/历史版本）的持久化抽象。
 * 约定：每个 moduleKey 对应一条内容线，同一时刻至多存在一个草稿；发布时草稿转正并生成新的版本号。
 * 职责分工：业务校验（字段合法性、模块结构）在应用层完成，实现侧只做
 * JSON 快照与关系表之间的双向装配及状态机流转。
 */
public interface ContentReleaseRepository {
    /**
     * 查询模块的当前草稿，无草稿时返回 null。
     */
    ContentRelease findDraft(String moduleKey);
    /**
     * 查询模块当前线上生效的版本。
     */
    ContentRelease findCurrent(String moduleKey);
    /**
     * 查询模块最近一次已发布的版本。
     */
    ContentRelease findPublished(String moduleKey);
    /**
     * 按版本号查询模块的指定历史版本。
     */
    ContentRelease findVersion(String moduleKey, int versionNo);
    /**
     * 查询模块的全部版本。
     */
    List<ContentRelease> findVersions(String moduleKey);
    /**
     * 对模块加锁，用于串行化同一模块的并发编辑/发布操作。
     */
    void lockModule(String moduleKey);
    /**
     * 获取模块的下一个版本号。
     */
    int nextVersion(String moduleKey);
    /**
     * 新增一条内容版本记录。
     */
    void add(ContentRelease release);
    /**
     * 基于乐观锁更新草稿的修改时间。
     *
     * @param expectedUpdatedAt 调用方持有的旧时间戳，用于并发冲突检测
     * @return 时间戳匹配并更新成功返回 true，否则返回 false
     */
    boolean touchDraft(UUID releaseId, OffsetDateTime expectedUpdatedAt, OffsetDateTime nextUpdatedAt);
    /**
     * 覆盖指定版本的内容数据。
     */
    void replaceData(ContentRelease release, Object data);
    /**
     * 读取指定版本的内容数据。
     */
    Object readData(ContentRelease release);
    /**
     * 发布草稿：将原当前版本下线，草稿转正为新的当前版本。
     */
    void publish(ContentRelease draft, ContentRelease current, UUID actorId, OffsetDateTime now);
    /**
     * 将当前生效版本下线。
     */
    void offline(ContentRelease current, OffsetDateTime now);
    /**
     * 删除草稿。
     */
    void deleteDraft(ContentRelease draft);
    /**
     * 软删除历史版本及其在各模块子表中的数据行（仅非发布态版本可调用）。
     */
    void softDeleteVersion(ContentRelease release, OffsetDateTime now);
}
