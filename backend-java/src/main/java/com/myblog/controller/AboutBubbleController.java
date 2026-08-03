package com.myblog.controller;

import com.myblog.application.model.command.about.AboutBubbleUpsert;
import com.myblog.application.model.entity.AboutBubble;
import com.myblog.application.service.about.AboutBubbleService;
import com.myblog.common.result.Result;
import com.myblog.common.security.CurrentUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/about-bubbles")
public class AboutBubbleController {

    private final AboutBubbleService bubbles;

    public AboutBubbleController(AboutBubbleService bubbles) {
        this.bubbles = bubbles;
    }

    @GetMapping
    public Result<List<AboutBubble>> get() {
        return Result.ok(bubbles.list());
    }

    @PostMapping
    public Result<AboutBubble> create(@AuthenticationPrincipal CurrentUser actor,
                                      @RequestBody AboutBubbleUpsert body) {
        return Result.ok(bubbles.create(actor, body));
    }

    @PutMapping("/{id}")
    public Result<AboutBubble> update(@AuthenticationPrincipal CurrentUser actor,
                                      @PathVariable UUID id,
                                      @RequestBody AboutBubbleUpsert body) {
        return Result.ok(bubbles.update(actor, id, body));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@AuthenticationPrincipal CurrentUser actor, @PathVariable UUID id) {
        bubbles.delete(actor, id);
        return Result.ok(null, "bubble deleted");
    }
}
