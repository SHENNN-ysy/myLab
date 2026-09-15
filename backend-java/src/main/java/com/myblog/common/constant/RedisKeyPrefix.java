package com.myblog.common.constant;

/** Redis Key 统一命名空间，确保 RedisInsight 只展示 MyLab 一个顶层业务分组。 */
public final class RedisKeyPrefix {
    public static final String ROOT = "mylab:";
    public static final String AUTH = ROOT + "auth:";
    public static final String BLOG = ROOT + "blog:";
    public static final String CONTENT = BLOG + "content:";
    public static final String RATE = ROOT + "rate:";

    private RedisKeyPrefix() {
    }
}
