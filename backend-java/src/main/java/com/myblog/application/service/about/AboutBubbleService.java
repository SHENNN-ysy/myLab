package com.myblog.application.service.about;

import com.myblog.application.model.entity.AboutBubble;
import com.myblog.application.model.command.about.AboutBubbleUpsert;
import com.myblog.common.security.CurrentUser;

import java.util.List;
import java.util.UUID;

public interface AboutBubbleService {

    List<AboutBubble> list();

    AboutBubble create(CurrentUser actor, AboutBubbleUpsert command);

    AboutBubble update(CurrentUser actor, UUID id, AboutBubbleUpsert command);

    void delete(CurrentUser actor, UUID id);
}
