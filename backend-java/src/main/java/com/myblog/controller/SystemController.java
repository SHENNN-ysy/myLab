package com.myblog.controller;

import com.myblog.application.service.system.SystemService;
import com.myblog.common.result.Result;
import com.myblog.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 系统信息接口（管理端）：提供应用静态信息与实时运行状态查询。
 * 业务逻辑委托给 {@link SystemService}。
 */
@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "系统")
@SecurityRequirement(name = "bearerAuth")
public class SystemController {

    // 系统信息应用服务
    private final SystemService system;

    public SystemController(SystemService system) {
        this.system = system;
    }

    /**
     * 查询系统静态信息（应用、JVM、操作系统、构建信息等）。
     */
    @GetMapping("/static")
    @Operation(summary = "查询系统静态信息", description = "返回应用、JVM、操作系统和构建等基本信息。")
    public Result<Map<String, Object>> stat(@AuthenticationPrincipal CurrentUser actor) throws Exception {
        return Result.ok(system.staticInfo(actor));
    }

    /**
     * 查询系统实时运行状态（内存、线程、运行时间等）。
     */
    @GetMapping("/dynamic")
    @Operation(summary = "查询系统运行状态", description = "返回实时内存、线程和运行时间等信息。")
    public Result<Map<String, Object>> dynamic(@AuthenticationPrincipal CurrentUser actor) {
        return Result.ok(system.dynamicInfo(actor));
    }
}
