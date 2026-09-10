package com.myblog.application.port;

import com.myblog.application.model.entity.User;
import com.myblog.application.model.vo.AccessTokenVO;

import java.util.UUID;

/**
 * 管理后台会话端口：签发、认证和撤销 Redis 会话令牌。
 */
public interface SessionService {

    /** 为已认证用户创建一个新的不透明会话令牌。 */
    AccessTokenVO issue(User user);

    /** 校验令牌、滑动续期并返回会话身份。 */
    SessionIdentity authenticate(String token);

    /** 撤销一个会话；会话已不存在时保持幂等。 */
    void revoke(String token);

    /** 撤销指定用户的全部会话。 */
    void revokeAll(UUID userId);
}
