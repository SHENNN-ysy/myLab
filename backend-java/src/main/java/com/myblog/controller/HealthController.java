package com.myblog.controller;

import com.myblog.application.service.system.SystemService;
import com.myblog.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "健康检查")
public class HealthController {

    private final SystemService system;

    public HealthController(SystemService system) {
        this.system = system;
    }

    @GetMapping
    @Operation(summary = "查询应用健康状态", description = "公开接口，返回应用、数据库和缓存等基础健康信息。")
    public Result<Map<String, Object>> health() {
        return Result.ok(system.health());
    }
}
