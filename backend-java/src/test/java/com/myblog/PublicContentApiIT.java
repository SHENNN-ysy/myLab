package com.myblog;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 公开内容接口集成测试：匿名读取已发布内容、停用卡片过滤、未知模块/文章的错误行为。
 * 前置数据全部经 JdbcTemplate 自建（apitest- 前缀），不依赖基线 dump 中的具体内容。
 */
class PublicContentApiIT extends AbstractApiIntegrationTest {

    private static final String CONTENT_URL = "/api/v1/public/content";
    private static final String MYLAB_URL = "/api/v1/public/mylab";

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

    @Test
    void mylabDetailReturnsPublishedCard() {
        String postKey = uniqueKey("apitest-detail-");
        ensurePublishedMylabCard(postKey, "详情测试文章", true);

        JsonNode body = assertStatusAndCode(
                rest.getForEntity(MYLAB_URL + "/" + postKey, JsonNode.class), HttpStatus.OK, 0);

        assertThat(body.path("data").path("post_key").asText()).isEqualTo(postKey);
        assertThat(body.path("data").path("title").asText()).isEqualTo("详情测试文章");
        assertThat(body.path("data").path("markdown_content").asText()).contains("API 集成测试正文");
    }

    @Test
    void mylabDetailUnknownPostKeyReturns404() {
        // 保证 mylab 存在已发布版本，使 404 来自"文章不存在"而非"模块未发布"
        ensurePublishedMylabCard(uniqueKey("apitest-fill-"), "占位卡片", true);

        assertStatusAndCode(
                rest.getForEntity(MYLAB_URL + "/" + uniqueKey("apitest-none-"), JsonNode.class),
                HttpStatus.NOT_FOUND, 10005);
    }

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
