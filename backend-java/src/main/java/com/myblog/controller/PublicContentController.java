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

/**
 * 公开内容接口：面向访客提供已发布内容的只读访问，无需认证。
 * 业务逻辑委托给 {@link ContentModuleService}。
 */
@RestController
@RequestMapping("/api/v1/public")
@Tag(name = "公开内容")
public class PublicContentController {
    // 内容模块应用服务
    private final ContentModuleService content;

    public PublicContentController(ContentModuleService content) {
        this.content = content;
    }

    /**
     * 获取全部模块的已发布内容。
     */
    @GetMapping("/content")
    @Operation(summary = "获取全部已发布内容")
    public Result<Map<String, Object>> content() {
        return Result.ok(content.publicContent());
    }

    /**
     * 按模块标识获取该模块的已发布内容。
     *
     * @param moduleKey 模块标识
     */
    @GetMapping("/content/{moduleKey}")
    public Result<Object> module(@PathVariable String moduleKey) {
        return Result.ok(content.publicModule(moduleKey));
    }

    /**
     * 按文章标识获取 MyLab 已发布文章详情。
     *
     * @param postKey 文章标识
     */
    @GetMapping("/mylab/{postKey}")
    public Result<Object> mylabDetail(@PathVariable String postKey) {
        return Result.ok(content.publicMylabDetail(postKey));
    }
}
