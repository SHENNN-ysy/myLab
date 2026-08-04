package com.myblog.controller;

import com.myblog.common.result.PageResult;
import com.myblog.common.result.Result;
import com.myblog.application.service.visit.VisitService;
import com.myblog.application.model.command.visit.RecordVisit;
import com.myblog.common.security.CurrentUser;
import com.myblog.application.model.vo.VisitStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/visits")
@Tag(name = "访问统计")
public class VisitController {

    private final VisitService visits;

    public VisitController(VisitService visits) {
        this.visits = visits;
    }

    @PostMapping("/logs/track")
    @Operation(summary = "记录一次前台访问", description = "公开接口；根据 IP、User-Agent 和 X-Page-Path 写入访问记录并累计统计。")
    public Result<?> track(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded == null ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
        visits.track(new RecordVisit(
                ip,
                request.getHeader("User-Agent"),
                request.getHeader("X-Page-Path") == null ? request.getRequestURI() : request.getHeader("X-Page-Path"),
                request.getHeader("Referer")));
        return Result.ok(null, "访问已记录");
    }

    @GetMapping("/stats")
    @Operation(summary = "查询访问统计", security = @SecurityRequirement(name = "bearerAuth"))
    public Result<VisitStatsVO> stats(@AuthenticationPrincipal CurrentUser actor,
                                      @RequestParam(required = false) String date) {
        return Result.ok(visits.stats(actor, date));
    }

    @GetMapping("/logs")
    @Operation(summary = "分页查询访问日志", security = @SecurityRequirement(name = "bearerAuth"))
    public Result<PageResult<?>> logs(@AuthenticationPrincipal CurrentUser actor,
                                      @RequestParam(defaultValue = "1") long page,
                                      @RequestParam(name = "page_size", defaultValue = "20") long size,
                                      @RequestParam(required = false) OffsetDateTime start,
                                      @RequestParam(required = false) OffsetDateTime end) {
        return Result.ok(visits.logs(actor, page, size, start, end));
    }

    @DeleteMapping("/logs/{id}")
    @Operation(summary = "删除指定访问日志", security = @SecurityRequirement(name = "bearerAuth"))
    public Result<Map<String, Integer>> delete(@AuthenticationPrincipal CurrentUser actor,
                                               @PathVariable UUID id) {
        return Result.ok(visits.delete(actor, id), "访问日志已删除");
    }

    @PostMapping("/logs/batch-delete")
    @Operation(summary = "按截止时间批量删除访问日志", security = @SecurityRequirement(name = "bearerAuth"))
    public Result<Map<String, Integer>> batchDelete(@AuthenticationPrincipal CurrentUser actor,
                                                    @RequestParam OffsetDateTime cutoff) {
        return Result.ok(visits.batchDelete(actor, cutoff));
    }

    @DeleteMapping("/logs")
    @Operation(summary = "清空访问日志", security = @SecurityRequirement(name = "bearerAuth"))
    public Result<Map<String, Integer>> clear(@AuthenticationPrincipal CurrentUser actor) {
        return Result.ok(visits.clear(actor), "访问日志已清空");
    }
}
