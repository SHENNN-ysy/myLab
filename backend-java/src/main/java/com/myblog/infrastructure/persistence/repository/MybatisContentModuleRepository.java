package com.myblog.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.myblog.application.model.entity.ContentModule;
import com.myblog.application.model.entity.ContentPublication;
import com.myblog.application.repository.ContentModuleRepository;
import com.myblog.infrastructure.persistence.mapper.ContentModuleMapper;
import com.myblog.infrastructure.persistence.mapper.ContentPublicationMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MybatisContentModuleRepository implements ContentModuleRepository {
    private final ContentModuleMapper modules;
    private final ContentPublicationMapper publications;

    public MybatisContentModuleRepository(ContentModuleMapper modules, ContentPublicationMapper publications) {
        this.modules = modules;
        this.publications = publications;
    }

    @Override
    public List<ContentModule> findAll() {
        return modules.selectList(new QueryWrapper<ContentModule>().orderByAsc("module_key"));
    }

    @Override
    public ContentModule findByKey(String moduleKey) {
        return modules.selectById(moduleKey);
    }

    @Override
    public void save(ContentModule module) {
        modules.updateById(module);
    }

    @Override
    public void addPublication(ContentPublication publication) {
        publications.insert(publication);
    }

    @Override
    public List<ContentPublication> findVersions(String moduleKey) {
        return publications.selectList(new QueryWrapper<ContentPublication>()
                .eq("module_key", moduleKey).orderByDesc("version"));
    }

    @Override
    public ContentPublication findVersion(String moduleKey, int version) {
        return publications.selectOne(new QueryWrapper<ContentPublication>()
                .eq("module_key", moduleKey).eq("version", version));
    }
}
