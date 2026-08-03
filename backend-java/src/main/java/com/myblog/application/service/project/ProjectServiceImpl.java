package com.myblog.application.service.project;

import com.myblog.application.model.entity.Project;
import com.myblog.common.exception.NotFoundException;
import com.myblog.common.exception.ValidationException;
import com.myblog.application.repository.ProjectRepository;
import com.myblog.application.model.command.project.ProjectUpsert;
import com.myblog.common.security.CurrentUser;
import com.myblog.common.result.PageResult;
import com.myblog.common.security.Authorization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projects;

    public ProjectServiceImpl(ProjectRepository projects) {
        this.projects = projects;
    }

    @Override
    public PageResult<Project> list(CurrentUser actor, long page, long size, String tag, Integer year) {
        return projects.findPage(page, size, tag, year);
    }

    @Override
    @Transactional
    public Project create(CurrentUser actor, ProjectUpsert command) {
        Authorization.requireAdmin(actor);
        if (command.title() == null || command.slug() == null || command.year() == null) {
            throw new ValidationException("title, slug and year are required");
        }
        Project project = new Project();
        apply(project, command);
        initialize(project);
        projects.add(project);
        return project;
    }

    @Override
    @Transactional
    public Project update(CurrentUser actor, UUID id, ProjectUpsert command) {
        Authorization.requireAdmin(actor);
        Project project = require(id);
        apply(project, command);
        project.setUpdatedAt(OffsetDateTime.now());
        projects.save(project);
        return project;
    }

    @Override
    @Transactional
    public void delete(CurrentUser actor, UUID id) {
        Authorization.requireAdmin(actor);
        if (!projects.remove(id)) throw new NotFoundException("project not found");
    }

    private Project require(UUID id) {
        Project project = projects.findById(id);
        if (project == null) throw new NotFoundException("project not found");
        return project;
    }

    private static void initialize(Project project) {
        project.setId(UUID.randomUUID());
        OffsetDateTime now = OffsetDateTime.now();
        project.setCreatedAt(now);
        project.setUpdatedAt(now);
    }

    private static void apply(Project project, ProjectUpsert command) {
        if (command.title() != null) project.setTitle(command.title());
        if (command.slug() != null) project.setSlug(command.slug());
        if (command.description() != null) project.setDescription(command.description());
        if (command.content() != null) project.setContent(command.content());
        if (command.tag() != null) project.setTag(command.tag());
        if (command.year() != null) project.setYear(command.year());
        if (command.imageUrl() != null) project.setImageUrl(command.imageUrl());
        if (command.projectUrl() != null) project.setProjectUrl(command.projectUrl());
        if (command.repoUrl() != null) project.setRepoUrl(command.repoUrl());
        if (command.tech() != null) project.setTech(command.tech());
        if (command.orderNum() != null) project.setOrderNum(command.orderNum());
    }
}
