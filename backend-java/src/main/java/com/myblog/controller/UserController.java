package com.myblog.controller;

import com.myblog.common.result.PageResult;
import com.myblog.common.result.Result;
import com.myblog.application.service.user.UserService;
import com.myblog.application.model.command.user.UserCommands;
import com.myblog.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "管理员")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping
    @Operation(summary = "分页查询管理员账号")
    public Result<PageResult<?>> list(@AuthenticationPrincipal CurrentUser actor,
                                      @RequestParam(defaultValue = "1") long page,
                                      @RequestParam(name = "page_size", defaultValue = "20") long size) {
        return Result.ok(users.page(actor, page, size));
    }

    @PostMapping
    @Operation(summary = "创建管理员账号", description = "仅 superadmin 可执行。")
    public Result<?> create(@AuthenticationPrincipal CurrentUser actor,
                            @RequestBody UserCommands.Create command) {
        return Result.ok(users.create(actor, command));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新管理员账号")
    public Result<?> update(@AuthenticationPrincipal CurrentUser actor,
                            @PathVariable UUID id,
                            @RequestBody UserCommands.Update command) {
        return Result.ok(users.update(actor, id, command));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除管理员账号", description = "仅 superadmin 可执行。")
    public Result<?> delete(@AuthenticationPrincipal CurrentUser actor, @PathVariable UUID id) {
        users.delete(actor, id);
        return Result.ok(null, "管理员账号已删除");
    }
}
