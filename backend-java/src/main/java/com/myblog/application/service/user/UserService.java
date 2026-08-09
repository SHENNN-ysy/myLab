package com.myblog.application.service.user;

import com.myblog.application.model.command.user.UserCommands;
import com.myblog.application.model.vo.UserOutVO;
import com.myblog.common.result.PageResult;
import com.myblog.common.security.CurrentUser;

import java.util.UUID;

public interface UserService {

    PageResult<UserOutVO> page(CurrentUser actor, long page, long size);

    UserOutVO create(CurrentUser actor, UserCommands.Create command);

    UserOutVO update(CurrentUser actor, UUID id, UserCommands.Update command);

    void delete(CurrentUser actor, UUID id);
}
