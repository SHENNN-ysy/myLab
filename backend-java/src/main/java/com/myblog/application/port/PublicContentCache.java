package com.myblog.application.port;

import java.util.Map;
import java.util.Optional;

/**
 * 公开内容缓存端口：缓存前台全量摘要与 MyLab 单篇原始详情。
 */
public interface PublicContentCache {

    /** 读取全部模块的原始公开摘要。 */
    Optional<Map<String, Object>> getAll();

    /** 写入全部模块的原始公开摘要。 */
    void putAll(Map<String, Object> content);

    /** 按文章标识读取 MyLab 原始详情。 */
    Optional<Map<String, Object>> getMylabDetail(String postKey);

    /** 按文章标识写入 MyLab 原始详情。 */
    void putMylabDetail(String postKey, Map<String, Object> detail);

    /** 删除全部模块摘要缓存。 */
    void evictAll();

    /** 删除全部 MyLab 文章详情缓存。 */
    void evictMylabDetails();
}
