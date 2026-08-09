package com.myblog.application.repository;

import com.myblog.application.model.entity.MylabTag;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MylabTagRepository {
    List<MylabTag> findAll(boolean includeDisabled);
    List<MylabTag> findActiveByIds(Collection<UUID> ids);
    MylabTag findById(UUID id);
    boolean keyOrNameExists(String key, String name, UUID excludedId);
    void add(MylabTag tag);
    void save(MylabTag tag);
    boolean remove(UUID id);
}
