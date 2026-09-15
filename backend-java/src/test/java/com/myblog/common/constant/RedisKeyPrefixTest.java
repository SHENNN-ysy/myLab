package com.myblog.common.constant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Redis 命名空间契约测试，防止后续模块重新产生并列顶层前缀。 */
class RedisKeyPrefixTest {

    @Test
    void exposesSingleMylabRootAndThreeBusinessGroups() {
        assertThat(RedisKeyPrefix.ROOT).isEqualTo("mylab:");
        assertThat(RedisKeyPrefix.AUTH).isEqualTo("mylab:auth:");
        assertThat(RedisKeyPrefix.BLOG).isEqualTo("mylab:blog:");
        assertThat(RedisKeyPrefix.CONTENT).isEqualTo("mylab:blog:content:");
        assertThat(RedisKeyPrefix.RATE).isEqualTo("mylab:rate:");
    }
}
