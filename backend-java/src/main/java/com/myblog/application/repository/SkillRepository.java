package com.myblog.application.repository;

import com.myblog.application.model.entity.Skill;
import com.myblog.common.result.PageResult;

import java.util.UUID;

public interface SkillRepository {
    PageResult<Skill> findPage(long page, long size);
    Skill findById(UUID id);
    void add(Skill skill);
    void save(Skill skill);
    boolean remove(UUID id);
}
