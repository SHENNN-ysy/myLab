package com.myblog.application.port;

import com.myblog.application.model.entity.User;
import com.myblog.application.model.vo.TokenPairVO;

public interface TokenService {

    TokenPairVO pair(User user);

    TokenClaims parse(String token, String expectedType);

    void revoke(String token);
}
