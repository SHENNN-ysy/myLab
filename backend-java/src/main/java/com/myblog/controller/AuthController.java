package com.myblog.controller;

import com.myblog.application.model.dto.AuthDtos;
import com.myblog.common.exception.ValidationException;
import com.myblog.common.result.Result;
import com.myblog.application.service.auth.AuthService;
import com.myblog.application.port.TokenService;
import com.myblog.common.security.CurrentUser;
import jakarta.validation.Valid;
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
public class AuthController {

    private final AuthService auth;
    private final TokenService tokens;

    public AuthController(AuthService auth, TokenService tokens) {
        this.auth = auth;
        this.tokens = tokens;
    }

    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody AuthDtos.Login body) {
        return Result.ok(auth.login(body.username(), body.password()));
    }

    @PostMapping("/refresh")
    public Result<?> refresh(@Valid @RequestBody AuthDtos.Refresh body) {
        return Result.ok(auth.refresh(body.refreshToken()));
    }

    @GetMapping("/me")
    public Result<?> me(@AuthenticationPrincipal CurrentUser user) {
        return Result.ok(auth.publicUser(auth.current(user.id())));
    }

    @PostMapping("/logout")
    public Result<?> logout() {
        return Result.ok(null, "logged out");
    }

    @PostMapping("/logout-token")
    public Result<?> logoutToken(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null) {
            throw new ValidationException("token is required");
        }
        tokens.revoke(token);
        return Result.ok(null, "token revoked");
    }

    @PutMapping("/password")
    public Result<?> password(@AuthenticationPrincipal CurrentUser user,
                              @Valid @RequestBody AuthDtos.PasswordChange body) {
        auth.change(user.id(), body.oldPassword(), body.newPassword());
        return Result.ok(null, "password updated");
    }
}
