package com.myblog.controller;

import com.myblog.application.model.dto.AuthDtos;
import com.myblog.common.result.Result;
import com.myblog.application.service.auth.AuthService;
import com.myblog.application.port.SessionService;
import com.myblog.common.security.CurrentUser;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：管理员登录、会话注销、当前用户信息查询及账号信息修改。
 * Bearer 令牌对应 Redis 会话，业务逻辑委托给 {@link AuthService} 与 {@link SessionService}。
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "认证")
public class AuthController {

    // 认证应用服务：登录、用户查询、改密
    private final AuthService auth;
    // 会话服务：负责当前会话注销
    private final SessionService sessions;

    public AuthController(AuthService auth, SessionService sessions) {
        this.auth = auth;
        this.sessions = sessions;
    }

    /**
     * 管理员登录：用户名密码校验通过后返回单个 Redis 会话令牌。
     */
    @PostMapping("/login")
    @Operation(summary = "管理员登录", description = "使用用户名和密码创建 Redis 会话令牌。")
    public Result<?> login(@Valid @RequestBody AuthDtos.Login body) {
        return Result.ok(auth.login(body.username(), body.password()));
    }

    /**
     * 获取当前登录管理员的信息。
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前管理员信息", security = @SecurityRequirement(name = "bearerAuth"))
    public Result<?> me(@AuthenticationPrincipal CurrentUser user) {
        return Result.ok(auth.publicUser(auth.current(user.id())));
    }

    /**
     * 退出当前登录：删除请求头 Bearer 令牌对应的 Redis 会话。
     */
    @PostMapping("/logout")
    @Operation(summary = "退出当前登录", description = "注销当前 Redis 会话令牌。",
            security = @SecurityRequirement(name = "bearerAuth"))
    public Result<?> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        sessions.revoke(authorization.substring(7).trim());
        return Result.ok(null, "已退出登录");
    }

    /**
     * 修改当前登录管理员的密码（需校验旧密码）。
     */
    @PutMapping("/password")
    @Operation(summary = "修改当前管理员密码", security = @SecurityRequirement(name = "bearerAuth"))
    public Result<?> password(@AuthenticationPrincipal CurrentUser user,
                              @Valid @RequestBody AuthDtos.PasswordChange body) {
        auth.change(user.id(), body.oldPassword(), body.newPassword());
        return Result.ok(null, "密码已更新");
    }

    /**
     * 修改当前登录管理员的账号名称，并可同时修改密码。
     */
    @PutMapping("/account")
    @Operation(summary = "修改当前管理员账号信息", security = @SecurityRequirement(name = "bearerAuth"))
    public Result<?> account(@AuthenticationPrincipal CurrentUser user,
                             @Valid @RequestBody AuthDtos.AccountUpdate body) {
        return Result.ok(auth.updateAccount(user.id(), body.username(), body.oldPassword(), body.newPassword()));
    }
}
