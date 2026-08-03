package com.myblog.application.repository;

import com.myblog.application.model.entity.Project;
import com.myblog.common.result.PageResult;

import java.util.UUID;

public interface ProjectRepository {
    PageResult<Project> findPage(long page, long size, String tag, Integer year);
    Project findById(UUID id);
    void add(Project project);
    void save(Project project);
    boolean remove(UUID id);
}
