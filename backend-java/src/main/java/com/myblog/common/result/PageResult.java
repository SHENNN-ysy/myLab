package com.myblog.common.result;

import java.util.List;

/**
 * Paged response wrapper.
 */
public record PageResult<T>(List<T> records, long total, long page, long pageSize) {

    public static <T> PageResult<T> of(List<T> records, long page, long pageSize, long total) {
        return new PageResult<>(records, total, page, pageSize);
    }
}
