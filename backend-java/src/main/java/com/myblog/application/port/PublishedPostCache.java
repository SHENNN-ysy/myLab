package com.myblog.application.port;

import java.util.Collection;

/**
 * 已发布 MyLab 文章索引缓存：互动写接口的防刷校验优先命中 Redis 中的已发布 post_key 集合，
 * 未命中时由调用方回源 PostgreSQL 确认并补写。索引只作加速，校验结果始终以数据库为准；
 * Redis 故障时所有操作静默降级，不影响互动接口可用性。
 */
public interface PublishedPostCache {

    /** 索引命中返回 true；未命中或缓存不可用均返回 false，由调用方回源数据库确认。 */
    boolean contains(String postKey);

    /** 回源确认已发布后把 postKey 补写进索引；缓存不可用时静默忽略。 */
    void add(String postKey);

    /** 发布/下线后按数据库当前已发布集合整体重建索引；缓存不可用时静默忽略。 */
    void rebuild(Collection<String> postKeys);
}
