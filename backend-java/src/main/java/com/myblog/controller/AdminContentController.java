package com.myblog.controller;

import com.myblog.application.model.entity.ContentModule;
import com.myblog.application.model.entity.ContentPublication;
import com.myblog.application.service.content.ContentModuleService;
import com.myblog.common.result.Result;
import com.myblog.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    @Operation(summary = "查询全部内容模块草稿")
    public Result<List<ContentModule>> list(@AuthenticationPrincipal CurrentUser actor) {
        return Result.ok(content.list(actor));
    }

    @GetMapping("/{moduleKey}")
    @Operation(summary = "查询指定内容模块草稿")
    public Result<ContentModule> get(@AuthenticationPrincipal CurrentUser actor,
                                     @Parameter(description = "内容模块标识：skills、projects、footprints、hobbies、vibe、mylab、support", required = true) @PathVariable String moduleKey) {
        return Result.ok(content.getDraft(actor, moduleKey));
    }

    @PutMapping("/{moduleKey}")
    @Operation(summary = "保存内容模块草稿", description = "保存后不会立即影响前台；前台继续读取上一版已发布快照。七类结构见 SkillsContent、ProjectsContent、FootprintsContent、HobbiesContent、VibeContent、MyLabContent、SupportDraftContent。")
    public Result<ContentModule> save(@AuthenticationPrincipal CurrentUser actor,
                                      @Parameter(description = "内容模块标识：skills、projects、footprints、hobbies、vibe、mylab、support", required = true) @PathVariable String moduleKey,
                                      @RequestBody Object data) {
        return Result.ok(content.saveDraft(actor, moduleKey, data), "草稿已保存");
    }

    @PostMapping("/{moduleKey}/publish")
    @Operation(summary = "发布内容模块", description = "校验草稿并生成不可变的线上版本快照。")
    public Result<ContentModule> publish(@AuthenticationPrincipal CurrentUser actor,
                                         @PathVariable String moduleKey) {
        return Result.ok(content.publish(actor, moduleKey), "内容已发布");
    }

    @PostMapping("/{moduleKey}/offline")
    @Operation(summary = "下线内容模块", description = "下线后公开内容接口不再返回该模块。")
    public Result<ContentModule> offline(@AuthenticationPrincipal CurrentUser actor,
                                         @PathVariable String moduleKey) {
        return Result.ok(content.offline(actor, moduleKey), "内容已下线");
    }

    @GetMapping("/{moduleKey}/versions")
    @Operation(summary = "查询内容模块发布历史")
    public Result<List<ContentPublication>> versions(@AuthenticationPrincipal CurrentUser actor,
                                                      @PathVariable String moduleKey) {
        return Result.ok(content.versions(actor, moduleKey));
    }

    @PostMapping("/{moduleKey}/rollback/{version}")
    @Operation(summary = "回滚并发布历史版本", description = "读取指定历史快照并作为一个新版本重新发布。")
    public Result<ContentModule> rollback(@AuthenticationPrincipal CurrentUser actor,
                                          @PathVariable String moduleKey,
                                          @PathVariable int version) {
        return Result.ok(content.rollback(actor, moduleKey, version), "内容已回滚并发布");
    }
}
