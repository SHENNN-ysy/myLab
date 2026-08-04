package com.myblog.controller;

import com.myblog.application.model.dto.AuthDtos;
import com.myblog.common.exception.ValidationException;
import com.myblog.common.result.Result;
import com.myblog.application.service.auth.AuthService;
import com.myblog.application.port.TokenService;
import com.myblog.common.security.CurrentUser;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "认证")
public class AuthController {

    private final AuthService auth;
    private final TokenService tokens;

    public AuthController(AuthService auth, TokenService tokens) {
        this.auth = auth;
        this.tokens = tokens;
    }

    @PostMapping("/login")
    @Operation(summary = "管理员登录", description = "使用用户名和密码换取 access_token 与 refresh_token。")
    public Result<?> login(@Valid @RequestBody AuthDtos.Login body) {
        return Result.ok(auth.login(body.username(), body.password()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新访问令牌", description = "使用 refresh_token 获取一组新的令牌。")
    public Result<?> refresh(@Valid @RequestBody AuthDtos.Refresh body) {
        return Result.ok(auth.refresh(body.refreshToken()));
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前管理员信息", security = @SecurityRequirement(name = "bearerAuth"))
    public Result<?> me(@AuthenticationPrincipal CurrentUser user) {
        return Result.ok(auth.publicUser(auth.current(user.id())));
    }

    @PostMapping("/logout")
    @Operation(summary = "退出当前登录", description = "无状态 JWT 客户端退出接口。", security = @SecurityRequirement(name = "bearerAuth"))
    public Result<?> logout() {
        return Result.ok(null, "已退出登录");
    }

    @PostMapping("/logout-token")
    @Operation(summary = "吊销指定令牌", security = @SecurityRequirement(name = "bearerAuth"))
    public Result<?> logoutToken(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null) {
            throw new ValidationException("token 为必填字段");
        }
        tokens.revoke(token);
        return Result.ok(null, "令牌已吊销");
    }

    @PutMapping("/password")
    @Operation(summary = "修改当前管理员密码", security = @SecurityRequirement(name = "bearerAuth"))
    public Result<?> password(@AuthenticationPrincipal CurrentUser user,
                              @Valid @RequestBody AuthDtos.PasswordChange body) {
        auth.change(user.id(), body.oldPassword(), body.newPassword());
        return Result.ok(null, "密码已更新");
    }
}
