package com.myblog.common.result;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 分页结果包装器。注意 {@code pageSize} 序列化为 {@code page_size}，
 * 以兼容前端的蛇形命名约定。
 */
@Schema(name = "PageResult", description = "分页结果")
public record PageResult<T>(
        @Schema(description = "当前页记录") List<T> records,
        @Schema(description = "总记录数", example = "100") long total,
        @Schema(description = "当前页码，从 1 开始", example = "1") long page,
        @JsonProperty("page_size") @Schema(description = "每页数量", example = "20") long pageSize) {

    /**
     * 构造分页结果。
     *
     * @param page 当前页码，从 1 开始
     */
    public static <T> PageResult<T> of(List<T> records, long page, long pageSize, long total) {
        return new PageResult<>(records, total, page, pageSize);
    }
}
