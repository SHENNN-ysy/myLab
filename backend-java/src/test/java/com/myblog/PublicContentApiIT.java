package com.myblog;

import com.myblog.application.port.DistributedLock;
import com.myblog.infrastructure.cache.RedisDistributedLock;
import com.myblog.infrastructure.cache.RedisPublicContentCache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 公开内容接口集成测试：匿名读取已发布内容、停用卡片过滤、未知模块/文章的错误行为。
 * 前置数据全部经 JdbcTemplate 自建（apitest- 前缀），不依赖基线 dump 中的具体内容。
 */
class PublicContentApiIT extends AbstractApiIntegrationTest {

    private static final String CONTENT_URL = "/api/v1/public/content";
    private static final String MYLAB_URL = "/api/v1/public/mylab";

    @Autowired
    private DistributedLock distributedLock;

    /** 全量公开内容包含已发布 mylab 卡片，列表不携带 Markdown 正文。 */
    @Test
    void publicContentIncludesPublishedMylabModule() {
        String postKey = uniqueKey("apitest-pub-");
        ensurePublishedMylabCard(postKey, "公开内容测试文章", true);

        // 匿名读取全量已发布内容，mylab 模块中应能看到自建卡片
        JsonNode body = assertStatusAndCode(
                rest.getForEntity(CONTENT_URL, JsonNode.class), HttpStatus.OK, 0);

        JsonNode card = findCard(body.path("data").path("mylab").path("cards"), postKey);
        assertThat(card).as("已发布 mylab 模块中应包含自建卡片").isNotNull();
        assertThat(card.has("markdown_content")).as("公开列表不应携带大正文").isFalse();
    }

    /** 全量内容在缓存被删除前一直命中 Redis；删除后回源数据库重建。 */
    @Test
    void publicContentUsesAllCacheUntilItIsEvicted() {
        String postKey = uniqueKey("apitest-cache-");
        ensurePublishedMylabCard(postKey, "缓存前标题", true);

        JsonNode first = assertStatusAndCode(
                rest.getForEntity(CONTENT_URL, JsonNode.class), HttpStatus.OK, 0);
        assertThat(findCard(first.path("data").path("mylab").path("cards"), postKey)
                .path("title").asText()).isEqualTo("缓存前标题");
        assertThat(redis.hasKey(RedisPublicContentCache.ALL_KEY)).isTrue();
        // 21_600 秒 = 缓存 TTL 上限 6 小时
        assertThat(redis.getExpire(RedisPublicContentCache.ALL_KEY)).isBetween(1L, 21_600L);

        jdbc.update("UPDATE mylab_cards SET card_title = ? WHERE post_key = ?", "数据库新标题", postKey);
        JsonNode cached = assertStatusAndCode(
                rest.getForEntity(CONTENT_URL, JsonNode.class), HttpStatus.OK, 0);
        assertThat(findCard(cached.path("data").path("mylab").path("cards"), postKey)
                .path("title").asText()).isEqualTo("缓存前标题");

        redis.delete(RedisPublicContentCache.ALL_KEY);
        JsonNode refreshed = assertStatusAndCode(
                rest.getForEntity(CONTENT_URL, JsonNode.class), HttpStatus.OK, 0);
        assertThat(findCard(refreshed.path("data").path("mylab").path("cards"), postKey)
                .path("title").asText()).isEqualTo("数据库新标题");
    }

    /** 单模块公开列表过滤停用卡片。 */
    @Test
    void publicModuleFiltersDisabledCards() {
        String visibleKey = uniqueKey("apitest-visible-");
        String hiddenKey = uniqueKey("apitest-hidden-");
        ensurePublishedMylabCard(visibleKey, "启用卡片", true);
        ensurePublishedMylabCard(hiddenKey, "停用卡片", false);

        JsonNode body = assertStatusAndCode(
                rest.getForEntity(CONTENT_URL + "/mylab", JsonNode.class), HttpStatus.OK, 0);

        JsonNode cards = body.path("data").path("cards");
        assertThat(findCard(cards, visibleKey)).isNotNull();
        assertThat(findCard(cards, hiddenKey)).as("停用卡片不应出现在公开输出").isNull();
    }

    /** 单模块接口直查数据库，不受全量缓存内容影响。 */
    @Test
    void publicModuleReadsDatabaseInsteadOfAllCache() {
        String postKey = uniqueKey("apitest-module-");
        ensurePublishedMylabCard(postKey, "单模块直查数据库", true);
        // 预置一个不含该卡片的全量缓存作为干扰，验证单模块接口不读它
        redis.opsForValue().set(RedisPublicContentCache.ALL_KEY,
                "{\"mylab\":{\"tags\":[],\"cards\":[]}}", Duration.ofHours(6));

        JsonNode body = assertStatusAndCode(
                rest.getForEntity(CONTENT_URL + "/mylab", JsonNode.class), HttpStatus.OK, 0);

        assertThat(findCard(body.path("data").path("cards"), postKey)).isNotNull();
    }

    /** 单篇详情返回 Markdown 正文并写入详情缓存。 */
    @Test
    void mylabDetailReturnsPublishedCard() {
        String postKey = uniqueKey("apitest-detail-");
        ensurePublishedMylabCard(postKey, "详情测试文章", true);

        JsonNode body = assertStatusAndCode(
                rest.getForEntity(MYLAB_URL + "/" + postKey, JsonNode.class), HttpStatus.OK, 0);

        assertThat(body.path("data").path("post_key").asText()).isEqualTo(postKey);
        assertThat(body.path("data").path("title").asText()).isEqualTo("详情测试文章");
        assertThat(body.path("data").path("markdown_content").asText()).contains("API 集成测试正文");
        Object cached = redis.opsForHash().get(RedisPublicContentCache.MYLAB_DETAILS_KEY, postKey);
        assertThat(String.valueOf(cached)).contains("markdown_content", "API 集成测试正文");
        // 21_600 秒 = 详情缓存 TTL 上限 6 小时
        assertThat(redis.getExpire(RedisPublicContentCache.MYLAB_DETAILS_KEY)).isBetween(1L, 21_600L);
    }

    /** 损坏的全量缓存被识别删除并回源数据库重建。 */
    @Test
    void corruptedAllCacheIsDeletedAndRebuilt() {
        String postKey = uniqueKey("apitest-corrupt-");
        ensurePublishedMylabCard(postKey, "损坏缓存回源", true);
        // 预置一段非法 JSON 作为损坏缓存
        redis.opsForValue().set(RedisPublicContentCache.ALL_KEY, "{broken-json", Duration.ofHours(6));

        JsonNode body = assertStatusAndCode(
                rest.getForEntity(CONTENT_URL, JsonNode.class), HttpStatus.OK, 0);

        assertThat(findCard(body.path("data").path("mylab").path("cards"), postKey)).isNotNull();
        assertThat(redis.opsForValue().get(RedisPublicContentCache.ALL_KEY)).startsWith("{");
        assertThat(redis.opsForValue().get(RedisPublicContentCache.ALL_KEY)).doesNotContain("broken-json");
    }

    /** 分布式锁只能由持有者本人释放，他人释放不生效。 */
    @Test
    void distributedLockCanOnlyBeReleasedByItsOwner() {
        String lockName = uniqueKey("apitest-lock-");
        assertThat(distributedLock.tryAcquire(lockName, "owner-a", Duration.ofSeconds(5))).isTrue();

        distributedLock.release(lockName, "owner-b");
        assertThat(redis.hasKey(RedisDistributedLock.LOCK_PREFIX + lockName)).isTrue();

        distributedLock.release(lockName, "owner-a");
        assertThat(redis.hasKey(RedisDistributedLock.LOCK_PREFIX + lockName)).isFalse();
    }

    /** 锁租约到期后自动释放，其他持有者可立即获取。 */
    @Test
    void distributedLockExpiresAfterLease() throws Exception {
        String lockName = uniqueKey("apitest-expiring-lock-");
        assertThat(distributedLock.tryAcquire(lockName, "owner-a", Duration.ofMillis(100))).isTrue();

        // 越过 100ms 租约，等待锁自动过期
        Thread.sleep(200);

        assertThat(distributedLock.tryAcquire(lockName, "owner-b", Duration.ofSeconds(5))).isTrue();
        distributedLock.release(lockName, "owner-b");
    }

    /** 未知 post_key 返回 404（模块已发布，排除"模块未发布"的干扰）。 */
    @Test
    void mylabDetailUnknownPostKeyReturns404() {
        // 保证 mylab 存在已发布版本，使 404 来自"文章不存在"而非"模块未发布"
        ensurePublishedMylabCard(uniqueKey("apitest-fill-"), "占位卡片", true);

        assertStatusAndCode(
                rest.getForEntity(MYLAB_URL + "/" + uniqueKey("apitest-none-"), JsonNode.class),
                HttpStatus.NOT_FOUND, 10005);
    }

    /** 白名单外的模块 key 按"模块不存在"返回 404。 */
    @Test
    void unknownModuleKeyReturns404() {
        // 模块 key 不在白名单内按"模块不存在"处理，而非通用参数错误
        assertStatusAndCode(
                rest.getForEntity(CONTENT_URL + "/notamodule", JsonNode.class),
                HttpStatus.NOT_FOUND, 12001);
    }

    /** 在 cards 数组中按 post_key 查找卡片，未找到返回 null */
    private JsonNode findCard(JsonNode cards, String postKey) {
        if (!cards.isArray()) {
            return null;
        }
        for (JsonNode card : cards) {
            if (postKey.equals(card.path("post_key").asText())) {
                return card;
            }
        }
        return null;
    }
}
