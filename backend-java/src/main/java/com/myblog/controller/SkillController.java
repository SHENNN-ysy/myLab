package com.myblog.controller;

import com.myblog.application.model.command.skill.SkillUpsert;
import com.myblog.application.model.entity.Skill;
import com.myblog.application.service.skill.SkillService;
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
@RequestMapping("/api/v1/skills")
public class SkillController {

    private final SkillService skills;

    public SkillController(SkillService skills) {
        this.skills = skills;
    }

    @GetMapping
    public Result<?> list(@AuthenticationPrincipal CurrentUser actor,
                          @RequestParam(defaultValue = "1") long page,
                          @RequestParam(name = "page_size", defaultValue = "100") long size) {
        return Result.ok(skills.list(actor, page, size));
    }

    @PostMapping
    public Result<Skill> create(@AuthenticationPrincipal CurrentUser actor,
                                @RequestBody SkillUpsert body) {
        return Result.ok(skills.create(actor, body));
    }

    @PutMapping("/{id}")
    public Result<Skill> update(@AuthenticationPrincipal CurrentUser actor,
                                @PathVariable UUID id,
                                @RequestBody SkillUpsert body) {
        return Result.ok(skills.update(actor, id, body));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@AuthenticationPrincipal CurrentUser actor, @PathVariable UUID id) {
        skills.delete(actor, id);
        return Result.ok(null, "skill deleted");
    }
}
