package com.myblog.controller;

import com.myblog.application.model.dto.EngagementDtos;
import com.myblog.application.service.engagement.EngagementService;
import com.myblog.application.service.engagement.VisitorIdentityService;
import com.myblog.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/** 匿名访客可用的内容互动和全站访问接口。 */
@RestController
@RequestMapping("/api/v1/public")
public class PublicEngagementController {
    private final EngagementService engagement;
    private final VisitorIdentityService identities;

    public PublicEngagementController(EngagementService engagement, VisitorIdentityService identities) {
        this.engagement = engagement;
        this.identities = identities;
    }

    @GetMapping("/mylab/engagement")
    public ResponseEntity<Result<List<EngagementDtos.EngagementSummary>>> engagement(
            @RequestParam(name = "post_keys") String postKeys) {
        List<String> values = Arrays.asList(postKeys.split(",", -1));
        return noStore(Result.ok(engagement.engagement(values)));
    }

    @PostMapping("/mylab/{postKey}/views")
    public ResponseEntity<Result<EngagementDtos.EngagementView>> registerView(
            @PathVariable String postKey,
            @CookieValue(name = VisitorIdentityService.COOKIE_NAME, required = false) String visitorToken,
            HttpServletRequest request,
            HttpServletResponse response) {
        EngagementDtos.VisitorIdentity visitor = visitor(visitorToken, request, response);
        return noStore(Result.ok(engagement.registerView(visitor.visitorHash(), postKey)));
    }

    @PutMapping("/mylab/{postKey}/likes")
    public ResponseEntity<Result<EngagementDtos.EngagementView>> like(
            @PathVariable String postKey,
            @CookieValue(name = VisitorIdentityService.COOKIE_NAME, required = false) String visitorToken,
            HttpServletRequest request,
            HttpServletResponse response) {
        EngagementDtos.VisitorIdentity visitor = visitor(visitorToken, request, response);
        return noStore(Result.ok(engagement.like(visitor.visitorHash(), postKey)));
    }

    @DeleteMapping("/mylab/{postKey}/likes")
    public ResponseEntity<Result<EngagementDtos.EngagementView>> unlike(
            @PathVariable String postKey,
            @CookieValue(name = VisitorIdentityService.COOKIE_NAME, required = false) String visitorToken,
            HttpServletRequest request,
            HttpServletResponse response) {
        EngagementDtos.VisitorIdentity visitor = visitor(visitorToken, request, response);
        return noStore(Result.ok(engagement.unlike(visitor.visitorHash(), postKey)));
    }

    @PostMapping("/analytics/visits")
    public ResponseEntity<Result<EngagementDtos.SiteStatisticsView>> registerVisit(
            @CookieValue(name = VisitorIdentityService.COOKIE_NAME, required = false) String visitorToken,
            HttpServletRequest request,
            HttpServletResponse response) {
        EngagementDtos.VisitorIdentity visitor = visitor(visitorToken, request, response);
        return noStore(Result.ok(engagement.registerVisit(visitor.visitorHash())));
    }

    @GetMapping("/analytics/summary")
    public ResponseEntity<Result<EngagementDtos.SiteStatisticsView>> summary() {
        return noStore(Result.ok(engagement.siteStatistics()));
    }

    private EngagementDtos.VisitorIdentity visitor(String token, HttpServletRequest request,
                                                     HttpServletResponse response) {
        EngagementDtos.VisitorIdentity visitor = identities.resolve(token);
        if (visitor.issued()) {
            boolean secure = identities.cookieSecure() || request.isSecure();
            ResponseCookie cookie = ResponseCookie.from(VisitorIdentityService.COOKIE_NAME, visitor.token())
                    .httpOnly(true)
                    .secure(secure)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(Duration.ofHours(72))
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        return visitor;
    }

    private static <T> ResponseEntity<Result<T>> noStore(Result<T> value) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(value);
    }
}
