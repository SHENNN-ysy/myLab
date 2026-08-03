package com.myblog.application.service.project;

import com.myblog.application.model.entity.Project;
import com.myblog.application.model.command.project.ProjectUpsert;
import com.myblog.common.result.PageResult;
import com.myblog.common.security.CurrentUser;

import java.util.UUID;

public interface ProjectService {

    PageResult<Project> list(CurrentUser actor, long page, long size, String tag, Integer year);

    Project create(CurrentUser actor, ProjectUpsert command);

    Project update(CurrentUser actor, UUID id, ProjectUpsert command);

    void delete(CurrentUser actor, UUID id);
}
