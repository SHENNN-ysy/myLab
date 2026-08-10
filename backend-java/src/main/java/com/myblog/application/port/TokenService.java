package com.myblog.application.port;

import com.myblog.application.model.entity.User;
import com.myblog.application.model.vo.TokenPairVO;

/**
 * Token 服务端口：应用层对令牌签发、解析与吊销能力的抽象，由基础设施层（如 JWT 实现）提供。
 */
public interface TokenService {

    /**
     * 为已认证用户签发一对令牌（访问令牌 + 刷新令牌）。
     *
     * @param user 已认证的用户
     * @return 令牌对
     */
    TokenPairVO pair(User user);

    /**
     * 解析并校验令牌，返回其中携带的用户声明。
     *
     * @param token 待解析的令牌
     * @param expectedType 期望的令牌类型（访问/刷新），类型不匹配视为无效
     * @return 令牌中的用户声明
     */
    TokenClaims parse(String token, String expectedType);

    /**
     * 吊销指定令牌，使其在过期前立即失效。
     */
    void revoke(String token);
}
