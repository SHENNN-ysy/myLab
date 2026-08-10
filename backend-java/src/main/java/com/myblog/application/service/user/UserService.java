package com.myblog.application.service.user;

import com.myblog.application.model.command.user.UserCommands;
import com.myblog.application.model.vo.UserOutVO;
import com.myblog.common.result.PageResult;
import com.myblog.common.security.CurrentUser;

import java.util.UUID;

/**
 * 用户管理用例接口：后台用户的分页查询与增改删。
 */
public interface UserService {

    /**
     * 分页列出用户（仅管理员）。
     */
    PageResult<UserOutVO> page(CurrentUser actor, long page, long size);

    /**
     * 创建用户（仅超级管理员）。
     */
    UserOutVO create(CurrentUser actor, UserCommands.Create command);

    /**
     * 更新用户的角色、启用状态或密码。
     */
    UserOutVO update(CurrentUser actor, UUID id, UserCommands.Update command);

    /**
     * 删除用户（仅超级管理员）。
     */
    void delete(CurrentUser actor, UUID id);
}
