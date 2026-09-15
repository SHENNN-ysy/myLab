package com.myblog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.myblog.application.service.engagement.VisitorIdentityService;
import com.myblog.application.service.engagement.EngagementPersistenceService;
import com.myblog.common.constant.RedisKeyPrefix;
import com.myblog.infrastructure.engagement.RedisEngagementEventStream;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 公开互动接口集成测试：浏览/点赞/取消点赞与全站访问统计。
 * 每次写操作后直查 Redis（实时计数的唯一存储）验证计数变化，
 * 再经统计接口读回验证一致性；访客身份按控制器实际机制走签名 Cookie。
 * 基类每条用例前 flushDb，计数从零开始断言。
 */
class PublicEngagementApiIT extends AbstractApiIntegrationTest {

    private static final String ENGAGEMENT_URL = "/api/v1/public/mylab/engagement";
    private static final String PAGE_VIEWS_URL = "/api/v1/public/analytics/page-views";
    private static final String SUMMARY_URL = "/api/v1/public/analytics/summary";
    private static final String SITE_METRICS_KEY = RedisKeyPrefix.BLOG + "site:metrics";

    @Autowired
    private EngagementPersistenceService persistence;

    @Autowired
    private VisitorIdentityService identities;

    /** 浏览/点赞/取消点赞全流程：Redis 实时计数与统计接口读回一致。 */
    @Test
    void viewLikeUnlikeFullFlow() {
        String postKey = uniqueKey("apitest-eng-");
        ensurePublishedMylabCard(postKey, "互动测试文章", true);

        // 首次浏览：无 Cookie 时签发访客身份，浏览计数 1
        ResponseEntity<JsonNode> firstView = pageView("mylab_detail", postKey, null);
        JsonNode view = assertStatusAndCode(firstView, HttpStatus.OK, 0);
        JsonNode content = view.path("data");
        assertThat(content.path("view_count").asLong()).isEqualTo(1);
        assertThat(content.path("like_count").asLong()).isZero();
        assertThat(content.path("liked").asBoolean()).isFalse();
        assertThat(view.path("data").path("site_statistics").path("visit_count").asLong()).isEqualTo(1);
        String visitor = visitorCookie(firstView);
        String visitorKey = redis.keys(RedisKeyPrefix.BLOG + "visitor:v2:*").iterator().next();
        assertThat(redisHash(engagementKey(postKey), "view_count")).isEqualTo("1");
        assertThat(redisHash(SITE_METRICS_KEY, "total_view_count")).isEqualTo("1");
        assertThat(streamSize()).isOne();
        assertThat(streamContainsVisitorField()).isFalse();

        // 24 小时滑动凭证内，同一详情页重复浏览不计数
        JsonNode repeatedView = assertStatusAndCode(
                pageView("mylab_detail", postKey, visitor), HttpStatus.OK, 0);
        assertThat(repeatedView.path("data").path("view_count").asLong()).isEqualTo(1);
        assertThat(streamSize()).isOne();

        // 点赞；重复点赞幂等不重复计数
        redis.expire(visitorKey, Duration.ofSeconds(5));
        ResponseEntity<JsonNode> likeResponse = exchange(likesUrl(postKey), HttpMethod.PUT, visitor);
        JsonNode like = assertStatusAndCode(likeResponse, HttpStatus.OK, 0);
        assertThat(like.path("data").path("like_count").asLong()).isEqualTo(1);
        assertThat(like.path("data").path("liked").asBoolean()).isTrue();
        assertThat(redis.getExpire(visitorKey)).isGreaterThan(Duration.ofHours(23).toSeconds());
        assertThat(likeResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("Max-Age=86400");
        JsonNode repeatedLike = assertStatusAndCode(
                exchange(likesUrl(postKey), HttpMethod.PUT, visitor), HttpStatus.OK, 0);
        assertThat(repeatedLike.path("data").path("like_count").asLong()).isEqualTo(1);
        assertThat(streamSize()).isEqualTo(2);
        assertThat(redisHash(engagementKey(postKey), "like_count")).isEqualTo("1");
        assertThat(redisHash(SITE_METRICS_KEY, "total_like_count")).isEqualTo("1");
        assertThat(redisHash(visitorKey, "like:" + postKey)).isNotBlank();
        assertThat(redis.keys(RedisKeyPrefix.BLOG + "visitor:likes:*")).isEmpty();
        assertThat(redis.keys(RedisKeyPrefix.BLOG + "site:session:*")).isEmpty();
        assertThat(redis.keys(RedisKeyPrefix.BLOG + "view:*")).isEmpty();

        // 统计接口读回：与 Redis 实时值一致
        JsonNode summary = engagementSummary(postKey);
        assertThat(summary.path("view_count").asLong()).isEqualTo(1);
        assertThat(summary.path("like_count").asLong()).isEqualTo(1);
        JsonNode site = assertStatusAndCode(
                rest.getForEntity(SUMMARY_URL, JsonNode.class), HttpStatus.OK, 0);
        assertThat(site.path("data").path("visit_count").asLong()).isEqualTo(1);
        assertThat(site.path("data").path("total_view_count").asLong()).isEqualTo(1);
        assertThat(site.path("data").path("total_like_count").asLong()).isEqualTo(1);

        // 取消点赞：计数回退并再次读回验证
        redis.expire(visitorKey, Duration.ofSeconds(5));
        ResponseEntity<JsonNode> unlikeResponse = exchange(likesUrl(postKey), HttpMethod.DELETE, visitor);
        JsonNode unlike = assertStatusAndCode(unlikeResponse, HttpStatus.OK, 0);
        assertThat(unlike.path("data").path("like_count").asLong()).isZero();
        assertThat(unlike.path("data").path("liked").asBoolean()).isFalse();
        assertThat(redis.getExpire(visitorKey)).isGreaterThan(Duration.ofHours(23).toSeconds());
        assertThat(unlikeResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("Max-Age=86400");
        assertThat(redisHash(engagementKey(postKey), "like_count")).isEqualTo("0");
        assertThat(redisHash(visitorKey, "like:" + postKey)).isNull();
        assertThat(engagementSummary(postKey).path("like_count").asLong()).isZero();
        assertThat(streamSize()).isEqualTo(3);
    }

    /** 一份凭证只计一次访问，不同页面分别计浏览，重复页面只刷新 24 小时 TTL。 */
    @Test
    void pageViewsDeduplicateByVisitorAndTargetWithSlidingTtl() {
        ResponseEntity<JsonNode> first = pageView("home", null, null);
        assertStatusAndCode(first, HttpStatus.OK, 0);
        assertThat(first.getBody().path("data").path("site_statistics")
                .path("visit_count").asLong()).isEqualTo(1);
        String visitor = visitorCookie(first);
        String visitorKey = redis.keys(RedisKeyPrefix.BLOG + "visitor:v2:*").iterator().next();
        redis.expire(visitorKey, Duration.ofSeconds(5));
        redis.opsForHash().put(visitorKey, "last_seen_at", "0");

        JsonNode second = assertStatusAndCode(
                pageView("home", null, visitor), HttpStatus.OK, 0);
        assertThat(second.path("data").has("post_key")).isFalse();
        assertThat(second.path("data").path("site_statistics").path("visit_count").asLong()).isEqualTo(1);
        assertThat(second.path("data").path("site_statistics").path("total_view_count").asLong()).isEqualTo(1);
        assertThat(redis.getExpire(visitorKey)).isGreaterThan(Duration.ofHours(23).toSeconds());
        assertThat(Long.parseLong(redisHash(visitorKey, "last_seen_at"))).isPositive();

        ResponseEntity<JsonNode> mylab = pageView("mylab", null, visitor);
        assertStatusAndCode(mylab, HttpStatus.OK, 0);
        assertThat(mylab.getBody().path("data").path("site_statistics")
                .path("total_view_count").asLong()).isEqualTo(2);
        assertThat(redisHash(SITE_METRICS_KEY, "visit_count")).isEqualTo("1");
        assertThat(redisHash(SITE_METRICS_KEY, "total_view_count")).isEqualTo("2");
        assertThat(redisHash(visitorKey, "visit")).isNotBlank();
        assertThat(redisHash(visitorKey, "view:home")).isNotBlank();
        assertThat(redisHash(visitorKey, "view:mylab")).isNotBlank();
        assertThat(streamSize()).isEqualTo(2);
        assertThat(mylab.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("Max-Age=86400");

        for (int index = 0; index < 3; index++) {
            String postKey = uniqueKey("apitest-page-target-");
            ensurePublishedMylabCard(postKey, "独立浏览目标 " + index, true);
            JsonNode detail = assertStatusAndCode(
                    pageView("mylab_detail", postKey, visitor), HttpStatus.OK, 0);
            assertThat(detail.path("data").path("post_key").asText()).isEqualTo(postKey);
            assertThat(detail.path("data").path("view_count").asLong()).isOne();
            assertThat(redisHash(visitorKey, "view:post:" + postKey)).isNotBlank();
        }

        JsonNode summary = assertStatusAndCode(
                rest.getForEntity(SUMMARY_URL, JsonNode.class), HttpStatus.OK, 0);
        assertThat(summary.path("data").path("visit_count").asLong()).isEqualTo(1);
        assertThat(summary.path("data").path("total_view_count").asLong()).isEqualTo(5);
    }

    /** 旧版 Cookie 没有对应 v2 Hash 时会轮换身份，不会复用旧访客状态。 */
    @Test
    void legacyCookieRotatesWhenV2VisitorDoesNotExist() {
        String legacyToken = "A".repeat(43);

        ResponseEntity<JsonNode> response = pageView("home", null, legacyToken);

        assertStatusAndCode(response, HttpStatus.OK, 0);
        assertThat(visitorCookie(response)).isNotEqualTo(legacyToken);
        assertThat(redis.keys(RedisKeyPrefix.BLOG + "visitor:v2:*")).hasSize(1);
    }

    /** 同一凭证并发浏览、点赞和取消点赞时，Lua 仍只累计一次访问与详情浏览。 */
    @Test
    void concurrentDetailInteractionsDoNotDuplicateVisitOrView() throws Exception {
        String postKey = uniqueKey("apitest-concurrent-");
        ensurePublishedMylabCard(postKey, "并发互动测试", true);
        String visitor = identities.resolve(null).token();
        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch ready = new CountDownLatch(3);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ResponseEntity<JsonNode>>> futures = new ArrayList<>();
        try {
            futures.add(executor.submit(() -> runTogether(ready, start,
                    () -> pageView("mylab_detail", postKey, visitor))));
            futures.add(executor.submit(() -> runTogether(ready, start,
                    () -> exchange(likesUrl(postKey), HttpMethod.PUT, visitor))));
            futures.add(executor.submit(() -> runTogether(ready, start,
                    () -> exchange(likesUrl(postKey), HttpMethod.DELETE, visitor))));
            ready.await();
            start.countDown();
            for (Future<ResponseEntity<JsonNode>> future : futures) {
                assertStatusAndCode(future.get(), HttpStatus.OK, 0);
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(redisHash(SITE_METRICS_KEY, "visit_count")).isEqualTo("1");
        assertThat(redisHash(SITE_METRICS_KEY, "total_view_count")).isEqualTo("1");
        assertThat(redisHash(engagementKey(postKey), "view_count")).isEqualTo("1");
    }

    /** 点赞接口可独立建立访客身份，并原子补记该详情页的访问和浏览。 */
    @Test
    void directLikeAlsoRegistersVisitAndDetailView() {
        String postKey = uniqueKey("apitest-direct-like-");
        ensurePublishedMylabCard(postKey, "直接点赞测试", true);

        ResponseEntity<JsonNode> response = exchange(likesUrl(postKey), HttpMethod.PUT, null);
        JsonNode body = assertStatusAndCode(response, HttpStatus.OK, 0);

        assertThat(body.path("data").path("view_count").asLong()).isOne();
        assertThat(body.path("data").path("like_count").asLong()).isOne();
        assertThat(body.path("data").path("site_statistics").path("visit_count").asLong()).isOne();
        assertThat(body.path("data").path("site_statistics").path("total_view_count").asLong()).isOne();
    }

    /** Stream 消费者合并通知、写入 Redis 最新绝对值，并在 PG 成功后清空 Pending。 */
    @Test
    void streamConsumerPersistsAbsoluteSnapshotAndAcknowledges() {
        String postKey = uniqueKey("apitest-stream-");
        ensurePublishedMylabCard(postKey, "Stream 落库测试", true);
        ResponseEntity<JsonNode> viewResponse = pageView("mylab_detail", postKey, null);
        assertStatusAndCode(viewResponse, HttpStatus.OK, 0);
        String visitor = visitorCookie(viewResponse);
        assertStatusAndCode(exchange(likesUrl(postKey), HttpMethod.PUT, visitor), HttpStatus.OK, 0);
        assertThat(streamSize()).isEqualTo(2);

        assertThat(persistence.synchronize("api-it-consumer")).isEqualTo(2);

        Map<String, Object> persisted = jdbc.queryForMap(
                "SELECT view_count, like_count FROM mylab_engagement_stats WHERE post_key = ?", postKey);
        assertThat(((Number) persisted.get("view_count")).longValue()).isOne();
        assertThat(((Number) persisted.get("like_count")).longValue()).isOne();
        assertThat(redis.opsForStream().pending(
                RedisEngagementEventStream.STREAM_KEY,
                RedisEngagementEventStream.GROUP_NAME).getTotalPendingMessages()).isZero();
    }

    /** 消费者取走消息后异常退出，超过空闲阈值的 Pending 能被新消费者接管并落库。 */
    @Test
    @SuppressWarnings("unchecked")
    void streamConsumerReclaimsStalePendingMessage() throws InterruptedException {
        String postKey = uniqueKey("apitest-reclaim-");
        ensurePublishedMylabCard(postKey, "Pending 接管测试", true);
        assertStatusAndCode(pageView("mylab_detail", postKey, null), HttpStatus.OK, 0);
        persistence.initialize();

        StreamOperations<String, String, String> stream = redis.opsForStream();
        var delivered = stream.read(
                Consumer.from(RedisEngagementEventStream.GROUP_NAME, "failed-consumer"),
                StreamReadOptions.empty().count(1),
                StreamOffset.create(RedisEngagementEventStream.STREAM_KEY, ReadOffset.lastConsumed()));
        assertThat(delivered).hasSize(1);
        assertThat(redis.opsForStream().pending(
                RedisEngagementEventStream.STREAM_KEY,
                RedisEngagementEventStream.GROUP_NAME).getTotalPendingMessages()).isOne();

        Thread.sleep(20);
        assertThat(persistence.synchronize("recovery-consumer")).isOne();

        Map<String, Object> persisted = jdbc.queryForMap(
                "SELECT view_count FROM mylab_engagement_stats WHERE post_key = ?", postKey);
        assertThat(((Number) persisted.get("view_count")).longValue()).isOne();
        assertThat(redis.opsForStream().pending(
                RedisEngagementEventStream.STREAM_KEY,
                RedisEngagementEventStream.GROUP_NAME).getTotalPendingMessages()).isZero();
    }

    /** 一次公开互动产生的限流、访客、计数和 Stream Key 只能位于 mylab 顶层。 */
    @Test
    void generatedRedisKeysUseSingleMylabRoot() {
        String postKey = uniqueKey("apitest-namespace-");
        ensurePublishedMylabCard(postKey, "Redis 命名空间测试", true);

        assertStatusAndCode(pageView("mylab_detail", postKey, null), HttpStatus.OK, 0);

        Set<String> keys = redis.keys("*");
        assertThat(keys).isNotEmpty().allMatch(key -> key.startsWith(RedisKeyPrefix.ROOT));
        assertThat(keys).anyMatch(key -> key.startsWith(RedisKeyPrefix.RATE));
        assertThat(keys).anyMatch(key -> key.startsWith(RedisKeyPrefix.BLOG));
    }

    /** 未产生互动的合法 post_key 批量查询按 0 兜底。 */
    @Test
    void engagementSummaryDefaultsToZeroForUnknownKey() {
        String postKey = uniqueKey("apitest-zero-");

        // 批量查询是只读接口：合法但未产生互动的 key 按 0 兜底
        JsonNode item = engagementSummary(postKey);
        assertThat(item.path("post_key").asText()).isEqualTo(postKey);
        assertThat(item.path("view_count").asLong()).isZero();
        assertThat(item.path("like_count").asLong()).isZero();
    }

    /** 不存在的文章浏览与点赞均返回 404。 */
    @Test
    void engagementOnUnknownPostReturns404() {
        String postKey = uniqueKey("apitest-missing-");

        assertStatusAndCode(pageView("mylab_detail", postKey, null),
                HttpStatus.NOT_FOUND, 10005);
        assertStatusAndCode(exchange(likesUrl(postKey), HttpMethod.PUT, null),
                HttpStatus.NOT_FOUND, 10005);
    }

    /** 已发布但停用的卡片不允许互动，浏览返回 404。 */
    @Test
    void engagementOnDisabledPostReturns404() {
        String postKey = uniqueKey("apitest-off-");
        ensurePublishedMylabCard(postKey, "停用卡片", false);

        // 已发布版本中存在但停用的卡片同样不允许刷互动
        assertStatusAndCode(pageView("mylab_detail", postKey, null),
                HttpStatus.NOT_FOUND, 10005);
    }

    /** 非法 post_key 走业务校验返回 422；缺少 post_keys 参数返回 400。 */
    @Test
    void invalidPostKeyReturns422AndMissingParamReturns400() {
        // post_key 格式非法走业务校验（10007）；缺少 post_keys 参数走 Spring 缺参处理（10012）
        assertStatusAndCode(pageView("mylab_detail", "bad+key", null),
                HttpStatus.UNPROCESSABLE_ENTITY, 10007);
        assertStatusAndCode(rest.getForEntity(ENGAGEMENT_URL, JsonNode.class),
                HttpStatus.BAD_REQUEST, 10012);
    }

    /** 页面类型和 post_key 组合必须严格匹配统一接口契约。 */
    @Test
    void invalidPageViewTargetReturns422() {
        assertStatusAndCode(pageView("unknown", null, null), HttpStatus.UNPROCESSABLE_ENTITY, 10007);
        assertStatusAndCode(pageView("mylab_detail", null, null), HttpStatus.UNPROCESSABLE_ENTITY, 10007);
        assertStatusAndCode(pageView("home", "first-post", null), HttpStatus.UNPROCESSABLE_ENTITY, 10007);
        assertStatusAndCode(pageView("home", "", null), HttpStatus.UNPROCESSABLE_ENTITY, 10007);
    }

    private String likesUrl(String postKey) {
        return "/api/v1/public/mylab/" + postKey + "/likes";
    }

    private String engagementKey(String postKey) {
        return RedisKeyPrefix.BLOG + "engagement:" + postKey;
    }

    /** 批量查询单篇文章的互动计数摘要（data[0]） */
    private JsonNode engagementSummary(String postKey) {
        JsonNode body = assertStatusAndCode(
                rest.getForEntity(ENGAGEMENT_URL + "?post_keys=" + postKey, JsonNode.class),
                HttpStatus.OK, 0);
        return body.path("data").get(0);
    }

    /** 携带访客 Cookie 发起无请求体的调用 */
    private ResponseEntity<JsonNode> exchange(String url, HttpMethod method, String visitorToken) {
        HttpHeaders headers = new HttpHeaders();
        if (visitorToken != null) {
            headers.add(HttpHeaders.COOKIE, VisitorIdentityService.COOKIE_NAME + "=" + visitorToken);
        }
        return rest.exchange(url, method, new HttpEntity<>(headers), JsonNode.class);
    }

    /** 从 Set-Cookie 中取出新签发的访客令牌，供后续请求保持同一访客身份 */
    private String visitorCookie(ResponseEntity<?> response) {
        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).as("首次访问应签发访客 Cookie")
                .contains(VisitorIdentityService.COOKIE_NAME + "=");
        String pair = setCookie.split(";", 2)[0];
        return pair.substring((VisitorIdentityService.COOKIE_NAME + "=").length());
    }

    /** 直读 Redis Hash 字段，未命中返回 null */
    private String redisHash(String key, String field) {
        Object value = redis.opsForHash().get(key, field);
        return value == null ? null : value.toString();
    }

    /** 调用统一页面浏览接口；postKey 只在 MyLab 详情页请求中出现。 */
    private ResponseEntity<JsonNode> pageView(String pageType, String postKey, String visitorToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (visitorToken != null) {
            headers.add(HttpHeaders.COOKIE, VisitorIdentityService.COOKIE_NAME + "=" + visitorToken);
        }
        Map<String, String> body = new LinkedHashMap<>();
        body.put("page_type", pageType);
        if (postKey != null) body.put("post_key", postKey);
        return rest.exchange(PAGE_VIEWS_URL, HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class);
    }

    private ResponseEntity<JsonNode> runTogether(
            CountDownLatch ready, CountDownLatch start, RequestCall request) throws Exception {
        ready.countDown();
        start.await();
        return request.execute();
    }

    private long streamSize() {
        Long size = redis.opsForStream().size(RedisEngagementEventStream.STREAM_KEY);
        return size == null ? 0 : size;
    }

    /** Stream 负载中不得出现任何 visitor 字段，避免访客级信息进入持久化链路。 */
    private boolean streamContainsVisitorField() {
        return redis.opsForStream().range(RedisEngagementEventStream.STREAM_KEY, Range.unbounded()).stream()
                .flatMap(record -> record.getValue().keySet().stream())
                .map(String::valueOf)
                .anyMatch(field -> field.toLowerCase().contains("visitor"));
    }

    @FunctionalInterface
    private interface RequestCall {
        ResponseEntity<JsonNode> execute();
    }
}
