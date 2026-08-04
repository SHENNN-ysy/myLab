package com.myblog.application.repository;

import com.myblog.application.model.entity.ContentModule;
import com.myblog.application.model.entity.ContentPublication;

import java.util.List;

public interface ContentModuleRepository {
    List<ContentModule> findAll();
    ContentModule findByKey(String moduleKey);
    void save(ContentModule module);
    void addPublication(ContentPublication publication);
    List<ContentPublication> findVersions(String moduleKey);
    ContentPublication findVersion(String moduleKey, int version);
}

