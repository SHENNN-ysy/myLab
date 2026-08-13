package com.myblog.controller;

import com.myblog.application.model.dto.EngagementDtos;
import com.myblog.application.service.engagement.EngagementService;
import com.myblog.common.result.Result;
import com.myblog.common.security.CurrentUser;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 管理端全站统计与趋势接口。数据实时聚合，响应禁用缓存。 */
@RestController
@RequestMapping("/api/v1/admin/analytics")
public class AdminAnalyticsController {
    private final EngagementService engagement;

    public AdminAnalyticsController(EngagementService engagement) {
        this.engagement = engagement;
    }

    @GetMapping("/summary")
    public ResponseEntity<Result<EngagementDtos.SiteStatisticsView>> summary(
            @AuthenticationPrincipal CurrentUser actor) {
        return noStore(Result.ok(engagement.adminSummary(actor)));
    }

    @GetMapping("/trends")
    public ResponseEntity<Result<EngagementDtos.AnalyticsTrendView>> trends(
            @AuthenticationPrincipal CurrentUser actor,
            @RequestParam(defaultValue = "30") int days) {
        return noStore(Result.ok(engagement.trends(actor, days)));
    }

    /** 统计数据实时计算，禁止缓存以保证后台看到最新结果。 */
    private static <T> ResponseEntity<Result<T>> noStore(Result<T> value) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(value);
    }
}
