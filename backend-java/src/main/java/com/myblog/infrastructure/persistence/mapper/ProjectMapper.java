package com.myblog.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myblog.application.model.entity.Project;
import com.myblog.application.repository.ProjectRepository;
import com.myblog.common.result.PageResult;

import java.util.UUID;

public interface ProjectMapper extends BaseMapper<Project>, ProjectRepository {
    @Override
    default PageResult<Project> findPage(long page, long size, String tag, Integer year) {
        QueryWrapper<Project> query = new QueryWrapper<Project>()
                .eq(tag != null, "tag", tag).eq(year != null, "year", year)
                .orderByAsc("order_num");
        Page<Project> result = selectPage(new Page<>(page, size), query);
        return PageResult.of(result.getRecords(), page, size, result.getTotal());
    }
    @Override default Project findById(UUID id) { return selectById(id); }
    @Override default void add(Project project) { insert(project); }
    @Override default void save(Project project) { updateById(project); }
    @Override default boolean remove(UUID id) { return deleteById(id) > 0; }
}
