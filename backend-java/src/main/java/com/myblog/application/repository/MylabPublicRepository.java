package com.myblog.application.repository;

import java.util.Map;
import java.util.UUID;

/** MyLab 公开列表与详情的只读查询端口。 */
public interface MylabPublicRepository {

    /** 读取首页展示的项目卡片摘要，只返回各项目实际引用的标签名称，不返回全局标签字典与 Markdown 正文。 */
    Map<String, Object> readProjects(UUID releaseId);

    /** 读取最新的启用卡片摘要（文章与项目混合，按发布日期倒序），标签直接展开为名称，不含全局标签字典与 Markdown 正文。 */
    Map<String, Object> readLatest(UUID releaseId, int limit);

    /** 读取不含 Markdown 正文的卡片摘要集合。 */
    Map<String, Object> readSummary(UUID releaseId);

    /** 读取单张卡片详情（只含卡片本体，不含全局标签字典）；不存在时返回 null。 */
    Map<String, Object> readDetail(UUID releaseId, String postKey);
}
