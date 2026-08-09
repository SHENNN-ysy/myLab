package com.myblog.application.service.content;

import com.myblog.application.model.dto.ContentDtos;
import com.myblog.common.security.CurrentUser;

import java.util.List;
import java.util.Map;

public interface ContentModuleService {
    Map<String, Object> publicContent();
    Object publicModule(String moduleKey);
    Object publicMylabDetail(String postKey);
    List<ContentDtos.ModuleView> list(CurrentUser actor);
    ContentDtos.ModuleView get(CurrentUser actor, String moduleKey);
    ContentDtos.ModuleView saveDraft(CurrentUser actor, String moduleKey, ContentDtos.SaveDraft command);
    ContentDtos.ModuleView publish(CurrentUser actor, String moduleKey);
    ContentDtos.ModuleView offline(CurrentUser actor, String moduleKey);
    List<ContentDtos.VersionView> versions(CurrentUser actor, String moduleKey);
    ContentDtos.VersionView version(CurrentUser actor, String moduleKey, int versionNo);
    ContentDtos.ModuleView restore(CurrentUser actor, String moduleKey, int versionNo);
    void deleteDraft(CurrentUser actor, String moduleKey);
}
