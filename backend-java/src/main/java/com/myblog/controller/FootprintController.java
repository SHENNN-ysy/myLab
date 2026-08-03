package com.myblog.controller;

import com.myblog.application.model.command.footprint.FootprintUpsert;
import com.myblog.application.model.entity.Footprint;
import com.myblog.application.service.footprint.FootprintService;
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
@RequestMapping("/api/v1/footprints")
public class FootprintController {

    private final FootprintService footprints;

    public FootprintController(FootprintService footprints) {
        this.footprints = footprints;
    }

    @GetMapping
    public Result<List<Footprint>> get() {
        return Result.ok(footprints.list());
    }

    @PostMapping
    public Result<Footprint> create(@AuthenticationPrincipal CurrentUser actor,
                                    @RequestBody FootprintUpsert body) {
        return Result.ok(footprints.create(actor, body));
    }

    @PutMapping("/{id}")
    public Result<Footprint> update(@AuthenticationPrincipal CurrentUser actor,
                                    @PathVariable UUID id,
                                    @RequestBody FootprintUpsert body) {
        return Result.ok(footprints.update(actor, id, body));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@AuthenticationPrincipal CurrentUser actor, @PathVariable UUID id) {
        footprints.delete(actor, id);
        return Result.ok(null, "footprint deleted");
    }
}
