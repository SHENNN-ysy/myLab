package com.myblog.controller;

import com.myblog.application.model.dto.ContentDtos;
import com.myblog.application.service.content.ContentModuleService;
import com.myblog.common.result.Result;
import com.myblog.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/content")
@Tag(name = "内容管理")
@SecurityRequirement(name = "bearerAuth")
public class AdminContentController {
    private final ContentModuleService content;

    public AdminContentController(ContentModuleService content) {
        this.content = content;
    }

    @GetMapping
    public Result<List<ContentDtos.ModuleView>> list(@AuthenticationPrincipal CurrentUser actor) {
        return Result.ok(content.list(actor));
    }

    @GetMapping("/{moduleKey}")
    public Result<ContentDtos.ModuleView> get(@AuthenticationPrincipal CurrentUser actor,
                                              @PathVariable String moduleKey) {
        return Result.ok(content.get(actor, moduleKey));
    }

    @PutMapping("/{moduleKey}")
    @Operation(summary = "完整保存模块草稿", description = "请求体包含 expected_updated_at 和 data。")
    public Result<ContentDtos.ModuleView> save(@AuthenticationPrincipal CurrentUser actor,
                                               @PathVariable String moduleKey,
                                               @RequestBody ContentDtos.SaveDraft command) {
        return Result.ok(content.saveDraft(actor, moduleKey, command), "草稿已保存");
    }

    @PostMapping("/{moduleKey}/publish")
    public Result<ContentDtos.ModuleView> publish(@AuthenticationPrincipal CurrentUser actor,
                                                  @PathVariable String moduleKey) {
        return Result.ok(content.publish(actor, moduleKey), "内容已发布");
    }

    @PostMapping("/{moduleKey}/offline")
    public Result<ContentDtos.ModuleView> offline(@AuthenticationPrincipal CurrentUser actor,
                                                  @PathVariable String moduleKey) {
        return Result.ok(content.offline(actor, moduleKey), "内容已下线");
    }

    @GetMapping("/{moduleKey}/versions")
    public Result<List<ContentDtos.VersionView>> versions(@AuthenticationPrincipal CurrentUser actor,
                                                          @PathVariable String moduleKey) {
        return Result.ok(content.versions(actor, moduleKey));
    }

    @GetMapping("/{moduleKey}/versions/{versionNo}")
    public Result<ContentDtos.VersionView> version(@AuthenticationPrincipal CurrentUser actor,
                                                   @PathVariable String moduleKey,
                                                   @PathVariable int versionNo) {
        return Result.ok(content.version(actor, moduleKey, versionNo));
    }

    @PostMapping("/{moduleKey}/versions/{versionNo}/restore")
    public Result<ContentDtos.ModuleView> restore(@AuthenticationPrincipal CurrentUser actor,
                                                  @PathVariable String moduleKey,
                                                  @PathVariable int versionNo) {
        return Result.ok(content.restore(actor, moduleKey, versionNo), "历史版本已恢复为草稿");
    }

    @DeleteMapping("/{moduleKey}/draft")
    public Result<?> deleteDraft(@AuthenticationPrincipal CurrentUser actor,
                                 @PathVariable String moduleKey) {
        content.deleteDraft(actor, moduleKey);
        return Result.ok(null, "草稿已放弃");
    }
}
