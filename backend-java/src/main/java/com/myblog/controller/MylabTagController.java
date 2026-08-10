package com.myblog.controller;

import com.myblog.application.model.dto.ContentDtos;
import com.myblog.application.model.entity.MylabTag;
import com.myblog.application.service.content.MylabTagService;
import com.myblog.common.result.Result;
import com.myblog.common.security.CurrentUser;
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
import java.util.UUID;

/**
 * MyLab 标签管理接口（管理端）：标签的查询、创建、更新与删除。
 * 业务逻辑委托给 {@link MylabTagService}。
 */
@RestController
@RequestMapping("/api/v1/admin/mylab/tags")
@Tag(name = "MyLab 标签")
@SecurityRequirement(name = "bearerAuth")
public class MylabTagController {
    // MyLab 标签应用服务
    private final MylabTagService tags;

    public MylabTagController(MylabTagService tags) {
        this.tags = tags;
    }

    /**
     * 查询全部 MyLab 标签。
     */
    @GetMapping
    public Result<List<MylabTag>> list(@AuthenticationPrincipal CurrentUser actor) {
        return Result.ok(tags.list(actor));
    }

    /**
     * 创建 MyLab 标签。
     */
    @PostMapping
    public Result<MylabTag> create(@AuthenticationPrincipal CurrentUser actor,
                                   @RequestBody ContentDtos.TagWrite command) {
        return Result.ok(tags.create(actor, command), "标签已创建");
    }

    /**
     * 更新指定 MyLab 标签。
     */
    @PutMapping("/{id}")
    public Result<MylabTag> update(@AuthenticationPrincipal CurrentUser actor,
                                   @PathVariable UUID id,
                                   @RequestBody ContentDtos.TagWrite command) {
        return Result.ok(tags.update(actor, id, command), "标签已更新");
    }

    /**
     * 删除指定 MyLab 标签。
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@AuthenticationPrincipal CurrentUser actor, @PathVariable UUID id) {
        tags.delete(actor, id);
        return Result.ok(null, "标签已删除");
    }
}
