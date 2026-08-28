package com.myblog.application.repository;

import java.util.Map;
import java.util.UUID;

/** MyLab 公开列表与详情的只读查询端口。 */
public interface MylabPublicRepository {

    /** 读取不含 Markdown 正文的卡片摘要集合。 */
    Map<String, Object> readSummary(UUID releaseId);

    /** 读取单张卡片详情；不存在时返回 null。 */
    Map<String, Object> readDetail(UUID releaseId, String postKey);
}
