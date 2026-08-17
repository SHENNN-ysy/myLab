package com.myblog;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.myblog.application.service.engagement.VisitorIdentityService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 公开互动接口集成测试：浏览/点赞/取消点赞与全站访问统计。
 * 每次写操作后直查 Redis（实时计数的唯一存储）验证计数变化，
 * 再经统计接口读回验证一致性；访客身份按控制器实际机制走签名 Cookie。
 * 基类每条用例前 flushDb，计数从零开始断言。
 */
class PublicEngagementApiIT extends AbstractApiIntegrationTest {

    private static final String ENGAGEMENT_URL = "/api/v1/public/mylab/engagement";
    private static final String VISITS_URL = "/api/v1/public/analytics/visits";
    private static final String SUMMARY_URL = "/api/v1/public/analytics/summary";
    private static final String SITE_METRICS_KEY = "blog:site:metrics";

    @Test
    void viewLikeUnlikeFullFlow() {
        String postKey = uniqueKey("apitest-eng-");
        ensurePublishedMylabCard(postKey, "互动测试文章", true);

        // 首次浏览：无 Cookie 时签发访客身份，浏览计数 1
        ResponseEntity<JsonNode> firstView = rest.postForEntity(viewsUrl(postKey), null, JsonNode.class);
        JsonNode view = assertStatusAndCode(firstView, HttpStatus.OK, 0);
        assertThat(view.path("data").path("view_count").asLong()).isEqualTo(1);
        assertThat(view.path("data").path("like_count").asLong()).isZero();
        assertThat(view.path("data").path("liked").asBoolean()).isFalse();
        String visitor = visitorCookie(firstView);
        assertThat(redisHash(engagementKey(postKey), "view_count")).isEqualTo("1");
        assertThat(redisHash(SITE_METRICS_KEY, "total_view_count")).isEqualTo("1");

        // 30 分钟去重窗口内同一访客重复浏览不计数
        JsonNode repeatedView = assertStatusAndCode(
                exchange(viewsUrl(postKey), HttpMethod.POST, visitor), HttpStatus.OK, 0);
        assertThat(repeatedView.path("data").path("view_count").asLong()).isEqualTo(1);

        // 点赞；重复点赞幂等不重复计数
        JsonNode like = assertStatusAndCode(
                exchange(likesUrl(postKey), HttpMethod.PUT, visitor), HttpStatus.OK, 0);
        assertThat(like.path("data").path("like_count").asLong()).isEqualTo(1);
        assertThat(like.path("data").path("liked").asBoolean()).isTrue();
        JsonNode repeatedLike = assertStatusAndCode(
                exchange(likesUrl(postKey), HttpMethod.PUT, visitor), HttpStatus.OK, 0);
        assertThat(repeatedLike.path("data").path("like_count").asLong()).isEqualTo(1);
        assertThat(redisHash(engagementKey(postKey), "like_count")).isEqualTo("1");
        assertThat(redisHash(SITE_METRICS_KEY, "total_like_count")).isEqualTo("1");

        // 统计接口读回：与 Redis 实时值一致
        JsonNode summary = engagementSummary(postKey);
        assertThat(summary.path("view_count").asLong()).isEqualTo(1);
        assertThat(summary.path("like_count").asLong()).isEqualTo(1);
        JsonNode site = assertStatusAndCode(
                rest.getForEntity(SUMMARY_URL, JsonNode.class), HttpStatus.OK, 0);
        assertThat(site.path("data").path("visit_count").asLong()).isZero();
        assertThat(site.path("data").path("total_view_count").asLong()).isEqualTo(1);
        assertThat(site.path("data").path("total_like_count").asLong()).isEqualTo(1);

        // 取消点赞：计数回退并再次读回验证
        JsonNode unlike = assertStatusAndCode(
                exchange(likesUrl(postKey), HttpMethod.DELETE, visitor), HttpStatus.OK, 0);
        assertThat(unlike.path("data").path("like_count").asLong()).isZero();
        assertThat(unlike.path("data").path("liked").asBoolean()).isFalse();
        assertThat(redisHash(engagementKey(postKey), "like_count")).isEqualTo("0");
        assertThat(engagementSummary(postKey).path("like_count").asLong()).isZero();
    }

    @Test
    void registerVisitDedupesWithinSession() {
        // 首次访问签发访客并计数 1；同一会话窗口内重复访问只续期不计数
        ResponseEntity<JsonNode> first = rest.postForEntity(VISITS_URL, null, JsonNode.class);
        assertStatusAndCode(first, HttpStatus.OK, 0);
        assertThat(first.getBody().path("data").path("visit_count").asLong()).isEqualTo(1);
        String visitor = visitorCookie(first);

        JsonNode second = assertStatusAndCode(
                exchange(VISITS_URL, HttpMethod.POST, visitor), HttpStatus.OK, 0);
        assertThat(second.path("data").path("visit_count").asLong()).isEqualTo(1);
        assertThat(redisHash(SITE_METRICS_KEY, "visit_count")).isEqualTo("1");

        JsonNode summary = assertStatusAndCode(
                rest.getForEntity(SUMMARY_URL, JsonNode.class), HttpStatus.OK, 0);
        assertThat(summary.path("data").path("visit_count").asLong()).isEqualTo(1);
    }

    @Test
    void engagementSummaryDefaultsToZeroForUnknownKey() {
        String postKey = uniqueKey("apitest-zero-");

        // 批量查询是只读接口：合法但未产生互动的 key 按 0 兜底
        JsonNode item = engagementSummary(postKey);
        assertThat(item.path("post_key").asText()).isEqualTo(postKey);
        assertThat(item.path("view_count").asLong()).isZero();
        assertThat(item.path("like_count").asLong()).isZero();
    }

    @Test
    void engagementOnUnknownPostReturns404() {
        String postKey = uniqueKey("apitest-missing-");

        assertStatusAndCode(rest.postForEntity(viewsUrl(postKey), null, JsonNode.class),
                HttpStatus.NOT_FOUND, 10005);
        assertStatusAndCode(exchange(likesUrl(postKey), HttpMethod.PUT, null),
                HttpStatus.NOT_FOUND, 10005);
    }

    @Test
    void engagementOnDisabledPostReturns404() {
        String postKey = uniqueKey("apitest-off-");
        ensurePublishedMylabCard(postKey, "停用卡片", false);

        // 已发布版本中存在但停用的卡片同样不允许刷互动
        assertStatusAndCode(rest.postForEntity(viewsUrl(postKey), null, JsonNode.class),
                HttpStatus.NOT_FOUND, 10005);
    }

    @Test
    void invalidPostKeyReturns422AndMissingParamReturns400() {
        // post_key 格式非法走业务校验（10007）；缺少 post_keys 参数走 Spring 缺参处理（10012）
        assertStatusAndCode(rest.postForEntity(viewsUrl("bad+key"), null, JsonNode.class),
                HttpStatus.UNPROCESSABLE_ENTITY, 10007);
        assertStatusAndCode(rest.getForEntity(ENGAGEMENT_URL, JsonNode.class),
                HttpStatus.BAD_REQUEST, 10012);
    }

    private String viewsUrl(String postKey) {
        return "/api/v1/public/mylab/" + postKey + "/views";
    }

    private String likesUrl(String postKey) {
        return "/api/v1/public/mylab/" + postKey + "/likes";
    }

    private String engagementKey(String postKey) {
        return "blog:engagement:" + postKey;
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

    private String redisHash(String key, String field) {
        Object value = redis.opsForHash().get(key, field);
        return value == null ? null : value.toString();
    }
}
