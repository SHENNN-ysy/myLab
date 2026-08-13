package com.myblog.infrastructure.persistence.codec;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JSON 快照字段级读取工具：新旧字段命名兼容、默认值填充、输出 Map 装配。
 * 原 {@code JdbcContentReleaseRepository} 的私有静态 helper 原样抽取，
 * 供各模块 codec 与主仓储复用（跨包使用故为 public；application 服务层另有自有副本，互不相关）。
 */
public final class JsonSnapshots {

    private JsonSnapshots() { }

    /** 取 JSON 数组字段，优先 preferred 名，不是数组则回退到 fallback 名（兼容新旧字段命名） */
    public static JsonNode array(JsonNode root, String preferred, String fallback) {
        JsonNode result = root.path(preferred);
        return result.isArray() ? result : root.path(fallback);
    }

    public static Iterable<JsonNode> iterable(JsonNode node) {
        return node != null && node.isArray() ? node : List.of();
    }

    /** 读取行 id；快照中缺失（新增行）时生成随机 UUID */
    public static UUID uuid(JsonNode node, String field) {
        UUID value = nullableUuid(node, field);
        return value == null ? UUID.randomUUID() : value;
    }

    public static UUID nullableUuid(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = node.path(field).asText("").trim();
            if (!value.isEmpty()) return UUID.fromString(value);
        }
        return null;
    }

    /** 读取必填文本字段（优先 preferred 名，回退 fallback 名），缺失或空白时抛异常 */
    public static String key(JsonNode node, String preferred, String fallback) {
        String result = firstText(node, preferred, fallback);
        if (result == null || result.isBlank()) throw new IllegalArgumentException(preferred + " is required");
        return result;
    }

    public static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    public static String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    public static int integer(JsonNode node, String field, int fallback) {
        return node.has(field) && node.path(field).canConvertToInt() ? node.path(field).asInt() : fallback;
    }

    public static double decimal(JsonNode node, String field, double fallback) {
        return node.has(field) && node.path(field).isNumber() ? node.path(field).asDouble() : fallback;
    }

    public static boolean bool(JsonNode node, String field, boolean fallback) {
        return node.has(field) ? node.path(field).asBoolean(fallback) : fallback;
    }

    public static LocalDate localDate(JsonNode node, String... fields) {
        String value = firstText(node, fields);
        return value == null ? null : LocalDate.parse(value);
    }

    /** 按空行把正文切分为段落列表 */
    public static List<String> splitParagraphs(String contents) {
        if (contents == null || contents.isBlank()) return List.of();
        return Arrays.stream(contents.split("(?:\\r?\\n){2,}"))
                .map(String::trim).filter(value -> !value.isEmpty()).toList();
    }

    /** 以键值对构建有序 Map，值为 null 的键跳过（保持输出 JSON 字段顺序稳定） */
    public static Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            if (pairs[index + 1] != null) result.put(String.valueOf(pairs[index]), pairs[index + 1]);
        }
        return result;
    }
}
