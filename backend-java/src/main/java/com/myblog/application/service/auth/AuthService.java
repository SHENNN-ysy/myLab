package com.myblog.application.service.auth;

import com.myblog.application.model.entity.User;
import com.myblog.application.model.vo.AuthResultVO;
import com.myblog.application.model.vo.TokenPairVO;
import com.myblog.application.model.vo.UserPublicVO;

import java.util.UUID;

public interface AuthService {

    AuthResultVO login(String username, String password);

    TokenPairVO refresh(String token);

    User current(UUID id);

    UserPublicVO publicUser(User user);

    void change(UUID id, String oldPassword, String newPassword);

    void ensureAdmin();

    String hash(String password);
}
