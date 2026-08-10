package com.myblog.application.repository;

import com.myblog.application.model.entity.MylabTag;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Mylab 标签仓储接口：标签实体的持久化抽象，由基础设施层实现。
 */
public interface MylabTagRepository {
    /**
     * 查询全部标签。
     *
     * @param includeDisabled 是否包含已禁用的标签
     */
    List<MylabTag> findAll(boolean includeDisabled);
    /**
     * 按 ID 批量查询处于启用状态的标签。
     */
    List<MylabTag> findActiveByIds(Collection<UUID> ids);
    /**
     * 按 ID 查询标签，不存在时返回 null。
     */
    MylabTag findById(UUID id);
    /**
     * 标签 key 或名称是否已被占用。
     *
     * @param excludedId 校验时需排除的自身 ID（用于编辑场景），可为 null
     */
    boolean keyOrNameExists(String key, String name, UUID excludedId);
    /**
     * 新增标签。
     */
    void add(MylabTag tag);
    /**
     * 更新已有标签。
     */
    void save(MylabTag tag);
    /**
     * 按 ID 删除标签。
     *
     * @return 删除成功返回 true，标签不存在返回 false
     */
    boolean remove(UUID id);
}
