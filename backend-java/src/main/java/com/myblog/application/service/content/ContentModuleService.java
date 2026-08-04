package com.myblog.application.service.content;

import com.myblog.application.model.entity.ContentModule;
import com.myblog.application.model.entity.ContentPublication;
import com.myblog.common.security.CurrentUser;

import java.util.List;
import java.util.Map;

public interface ContentModuleService {
    Map<String, Object> publicContent();
    Object publicModule(String moduleKey);
    List<ContentModule> list(CurrentUser actor);
    ContentModule getDraft(CurrentUser actor, String moduleKey);
    ContentModule saveDraft(CurrentUser actor, String moduleKey, Object data);
    ContentModule publish(CurrentUser actor, String moduleKey);
    ContentModule offline(CurrentUser actor, String moduleKey);
    List<ContentPublication> versions(CurrentUser actor, String moduleKey);
    ContentModule rollback(CurrentUser actor, String moduleKey, int version);
}
