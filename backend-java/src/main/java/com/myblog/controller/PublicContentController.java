package com.myblog.controller;

import com.myblog.application.service.content.ContentModuleService;
import com.myblog.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "获取全部已发布内容")
    public Result<Map<String, Object>> content() {
        return Result.ok(content.publicContent());
    }

    @GetMapping("/content/{moduleKey}")
    public Result<Object> module(@PathVariable String moduleKey) {
        return Result.ok(content.publicModule(moduleKey));
    }

    @GetMapping("/mylab/{postKey}")
    public Result<Object> mylabDetail(@PathVariable String postKey) {
        return Result.ok(content.publicMylabDetail(postKey));
    }
}
