package com.myblog.common.result;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Paged response wrapper.
 */
@Schema(name = "PageResult", description = "分页结果")
public record PageResult<T>(
        @Schema(description = "当前页记录") List<T> records,
        @Schema(description = "总记录数", example = "100") long total,
        @Schema(description = "当前页码，从 1 开始", example = "1") long page,
        @JsonProperty("page_size") @Schema(description = "每页数量", example = "20") long pageSize) {

    public static <T> PageResult<T> of(List<T> records, long page, long pageSize, long total) {
        return new PageResult<>(records, total, page, pageSize);
    }
}
