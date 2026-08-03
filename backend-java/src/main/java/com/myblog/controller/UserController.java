package com.myblog.controller;

import com.myblog.common.result.PageResult;
import com.myblog.common.result.Result;
import com.myblog.application.service.user.UserService;
import com.myblog.application.model.command.user.UserCommands;
import com.myblog.common.security.CurrentUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping
    public Result<PageResult<?>> list(@AuthenticationPrincipal CurrentUser actor,
                                      @RequestParam(defaultValue = "1") long page,
                                      @RequestParam(name = "page_size", defaultValue = "20") long size) {
        return Result.ok(users.page(actor, page, size));
    }

    @PostMapping
    public Result<?> create(@AuthenticationPrincipal CurrentUser actor,
                            @RequestBody UserCommands.Create command) {
        return Result.ok(users.create(actor, command));
    }

    @PutMapping("/{id}")
    public Result<?> update(@AuthenticationPrincipal CurrentUser actor,
                            @PathVariable UUID id,
                            @RequestBody UserCommands.Update command) {
        return Result.ok(users.update(actor, id, command));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@AuthenticationPrincipal CurrentUser actor, @PathVariable UUID id) {
        users.delete(actor, id);
        return Result.ok(null, "user deleted");
    }
}
