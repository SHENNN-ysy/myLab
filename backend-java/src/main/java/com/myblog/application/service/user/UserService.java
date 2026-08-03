package com.myblog.application.service.user;

import com.myblog.application.model.entity.User;
import com.myblog.application.model.command.user.UserCommands;
import com.myblog.common.result.PageResult;
import com.myblog.common.security.CurrentUser;

import java.util.UUID;

public interface UserService {

    PageResult<User> page(CurrentUser actor, long page, long size);

    User create(CurrentUser actor, UserCommands.Create command);

    User update(CurrentUser actor, UUID id, UserCommands.Update command);

    void delete(CurrentUser actor, UUID id);
}
