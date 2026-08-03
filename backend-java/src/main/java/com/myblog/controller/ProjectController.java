package com.myblog.controller;

import com.myblog.application.model.command.project.ProjectUpsert;
import com.myblog.application.model.entity.Project;
import com.myblog.application.service.project.ProjectService;
import com.myblog.common.result.Result;
import com.myblog.common.security.CurrentUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projects;

    public ProjectController(ProjectService projects) {
        this.projects = projects;
    }

    @GetMapping
    public Result<?> list(@AuthenticationPrincipal CurrentUser actor,
                          @RequestParam(defaultValue = "1") long page,
                          @RequestParam(name = "page_size", defaultValue = "20") long size,
                          @RequestParam(required = false) String tag,
                          @RequestParam(required = false) Integer year) {
        return Result.ok(projects.list(actor, page, size, tag, year));
    }

    @PostMapping
    public Result<Project> create(@AuthenticationPrincipal CurrentUser actor,
                                  @RequestBody ProjectUpsert body) {
        return Result.ok(projects.create(actor, body));
    }

    @PutMapping("/{id}")
    public Result<Project> update(@AuthenticationPrincipal CurrentUser actor,
                                  @PathVariable UUID id,
                                  @RequestBody ProjectUpsert body) {
        return Result.ok(projects.update(actor, id, body));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@AuthenticationPrincipal CurrentUser actor, @PathVariable UUID id) {
        projects.delete(actor, id);
        return Result.ok(null, "project deleted");
    }
}
