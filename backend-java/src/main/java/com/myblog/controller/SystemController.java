package com.myblog.controller;

import com.myblog.application.service.system.SystemService;
import com.myblog.common.result.Result;
import com.myblog.common.security.CurrentUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    private final SystemService system;

    public SystemController(SystemService system) {
        this.system = system;
    }

    @GetMapping("/static")
    public Result<Map<String, Object>> stat(@AuthenticationPrincipal CurrentUser actor) throws Exception {
        return Result.ok(system.staticInfo(actor));
    }

    @GetMapping("/dynamic")
    public Result<Map<String, Object>> dynamic(@AuthenticationPrincipal CurrentUser actor) {
        return Result.ok(system.dynamicInfo(actor));
    }
}
