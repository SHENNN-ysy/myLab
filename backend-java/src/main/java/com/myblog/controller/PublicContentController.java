package com.myblog.controller;

import com.myblog.application.service.content.ContentModuleService;
import com.myblog.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/public")
@Tag(name = "公开内容")
public class PublicContentController {
    private final ContentModuleService content;

    public PublicContentController(ContentModuleService content) {
        this.content = content;
    }

    @GetMapping("/content")
    @Operation(summary = "获取全部已发布内容", description = "博客前台一次性读取全部在线内容模块。已下线模块不会出现在结果中。")
    public Result<Map<String, Object>> content() {
        return Result.ok(content.publicContent());
    }

    @GetMapping("/content/{moduleKey}")
    @Operation(summary = "获取指定已发布内容模块")
    public Result<Object> module(
            @Parameter(description = "模块标识：skills、projects、footprints、hobbies、vibe、mylab、support", required = true)
            @PathVariable String moduleKey) {
        return Result.ok(content.publicModule(moduleKey));
    }

    @GetMapping("/support")
    @Operation(summary = "获取支持页统计", description = "仅返回计算后的访问量、后台维护的点赞数和计算后的浏览量。")
    public Result<Object> support() {
        return Result.ok(content.publicModule("support"));
    }
}
