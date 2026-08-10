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

/**
 * 内容管理接口（管理端）：模块化内容的草稿保存、发布/下线、版本查询与回滚。
 * 业务逻辑委托给 {@link ContentModuleService}。
 */
@RestController
@RequestMapping("/api/v1/admin/content")
@Tag(name = "内容管理")
@SecurityRequirement(name = "bearerAuth")
public class AdminContentController {
    // 内容模块应用服务，承载草稿/发布/版本等业务逻辑
    private final ContentModuleService content;

    public AdminContentController(ContentModuleService content) {
        this.content = content;
    }

    /**
     * 列出全部内容模块的管理视图。
     */
    @GetMapping
    public Result<List<ContentDtos.ModuleView>> list(@AuthenticationPrincipal CurrentUser actor) {
        return Result.ok(content.list(actor));
    }

    /**
     * 获取指定模块的管理视图（含草稿与发布状态）。
     *
     * @param moduleKey 模块标识
     */
    @GetMapping("/{moduleKey}")
    public Result<ContentDtos.ModuleView> get(@AuthenticationPrincipal CurrentUser actor,
                                              @PathVariable String moduleKey) {
        return Result.ok(content.get(actor, moduleKey));
    }

    /**
     * 完整保存模块草稿，请求体携带 expected_updated_at 与 data，整体覆盖式保存。
     */
    @PutMapping("/{moduleKey}")
    @Operation(summary = "完整保存模块草稿", description = "请求体包含 expected_updated_at 和 data。")
    public Result<ContentDtos.ModuleView> save(@AuthenticationPrincipal CurrentUser actor,
                                               @PathVariable String moduleKey,
                                               @RequestBody ContentDtos.SaveDraft command) {
        return Result.ok(content.saveDraft(actor, moduleKey, command), "草稿已保存");
    }

    /**
     * 发布指定模块的当前草稿，生成新的内容版本。
     */
    @PostMapping("/{moduleKey}/publish")
    public Result<ContentDtos.ModuleView> publish(@AuthenticationPrincipal CurrentUser actor,
                                                  @PathVariable String moduleKey) {
        return Result.ok(content.publish(actor, moduleKey), "内容已发布");
    }

    /**
     * 下线指定模块的已发布内容。
     */
    @PostMapping("/{moduleKey}/offline")
    public Result<ContentDtos.ModuleView> offline(@AuthenticationPrincipal CurrentUser actor,
                                                  @PathVariable String moduleKey) {
        return Result.ok(content.offline(actor, moduleKey), "内容已下线");
    }

    /**
     * 查询指定模块的历史版本列表。
     */
    @GetMapping("/{moduleKey}/versions")
    public Result<List<ContentDtos.VersionView>> versions(@AuthenticationPrincipal CurrentUser actor,
                                                          @PathVariable String moduleKey) {
        return Result.ok(content.versions(actor, moduleKey));
    }

    /**
     * 查询指定模块某一历史版本的详情。
     *
     * @param versionNo 版本号
     */
    @GetMapping("/{moduleKey}/versions/{versionNo}")
    public Result<ContentDtos.VersionView> version(@AuthenticationPrincipal CurrentUser actor,
                                                   @PathVariable String moduleKey,
                                                   @PathVariable int versionNo) {
        return Result.ok(content.version(actor, moduleKey, versionNo));
    }

    /**
     * 将指定历史版本恢复为当前草稿。
     */
    @PostMapping("/{moduleKey}/versions/{versionNo}/restore")
    public Result<ContentDtos.ModuleView> restore(@AuthenticationPrincipal CurrentUser actor,
                                                  @PathVariable String moduleKey,
                                                  @PathVariable int versionNo) {
        return Result.ok(content.restore(actor, moduleKey, versionNo), "历史版本已恢复为草稿");
    }

    /**
     * 放弃指定模块的当前草稿。
     */
    @DeleteMapping("/{moduleKey}/draft")
    public Result<?> deleteDraft(@AuthenticationPrincipal CurrentUser actor,
                                 @PathVariable String moduleKey) {
        content.deleteDraft(actor, moduleKey);
        return Result.ok(null, "草稿已放弃");
    }
}
