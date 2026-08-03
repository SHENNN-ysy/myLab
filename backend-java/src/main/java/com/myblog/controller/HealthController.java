package com.myblog.controller;

import com.myblog.application.service.system.SystemService;
import com.myblog.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final SystemService system;

    public HealthController(SystemService system) {
        this.system = system;
    }

    @GetMapping
    public Result<Map<String, Object>> health() {
        return Result.ok(system.health());
    }
}
