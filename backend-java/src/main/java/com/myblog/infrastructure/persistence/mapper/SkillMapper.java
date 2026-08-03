package com.myblog.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myblog.application.model.entity.Skill;
import com.myblog.application.repository.SkillRepository;
import com.myblog.common.result.PageResult;

import java.util.UUID;

public interface SkillMapper extends BaseMapper<Skill>, SkillRepository {
    @Override
    default PageResult<Skill> findPage(long page, long size) {
        Page<Skill> result = selectPage(new Page<>(page, size),
                new QueryWrapper<Skill>().orderByAsc("order_num"));
        return PageResult.of(result.getRecords(), page, size, result.getTotal());
    }
    @Override default Skill findById(UUID id) { return selectById(id); }
    @Override default void add(Skill skill) { insert(skill); }
    @Override default void save(Skill skill) { updateById(skill); }
    @Override default boolean remove(UUID id) { return deleteById(id) > 0; }
}
