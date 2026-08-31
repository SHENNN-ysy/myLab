package com.myblog.application.service.content;

import com.myblog.application.model.dto.ContentDtos;
import com.myblog.common.security.CurrentUser;

import java.util.List;
import java.util.Map;

/**
 * 内容模块用例接口：站点各内容模块（home/about/skills/footprints/hobbies/vibe/mylab）的
 * 公开读取与后台草稿-发布-版本管理。采用「草稿 + 已发布」双状态、每次发布生成新版本的版本化模型。
 */
public interface ContentModuleService {
    /**
     * 读取全部模块当前已发布内容，供公开站点渲染；未发布的模块不出现在结果中。
     */
    Map<String, Object> publicContent();
    /**
     * 读取单个模块的已发布内容；未发布时抛模块下线异常。
     */
    Object publicModule(String moduleKey);
    /**
     * 按 post_key 从已发布的 mylab 模块中取单张卡片详情。
     */
    Object publicMylabDetail(String postKey);
    /**
     * 列出全部模块的管理视图（草稿与发布状态）。
     */
    List<ContentDtos.ModuleView> list(CurrentUser actor);
    /**
     * 获取单个模块的管理视图。
     */
    ContentDtos.ModuleView get(CurrentUser actor, String moduleKey);
    /**
     * 保存草稿：版本名称和描述必填，新建首版草稿或通过乐观锁更新既有草稿。
     */
    ContentDtos.ModuleView saveDraft(CurrentUser actor, String moduleKey, ContentDtos.SaveDraft command);
    /**
     * 发布当前草稿为新版本并替换线上版本。
     */
    ContentDtos.ModuleView publish(CurrentUser actor, String moduleKey);
    /**
     * 将当前已发布版本下线。
     */
    ContentDtos.ModuleView offline(CurrentUser actor, String moduleKey);
    /**
     * 列出模块全部未删除版本，包含当前线上版本和当前草稿。
     */
    List<ContentDtos.VersionView> versions(CurrentUser actor, String moduleKey);
    /**
     * 查看指定版本号的历史版本内容。
     */
    ContentDtos.VersionView version(CurrentUser actor, String moduleKey, int versionNo);
    /**
     * 将指定历史版本原地恢复为当前草稿，原草稿转为归档版本。
     */
    ContentDtos.ModuleView restore(CurrentUser actor, String moduleKey, int versionNo);
    /**
     * 软删除指定历史版本并解除其资源引用；线上发布态版本不可删除。
     */
    void deleteVersion(CurrentUser actor, String moduleKey, int versionNo);
    /**
     * 放弃当前草稿。
     */
    void deleteDraft(CurrentUser actor, String moduleKey);
}
