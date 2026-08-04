package com.myblog.application.repository;

import com.myblog.application.model.entity.VisitLog;
import com.myblog.common.result.PageResult;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface VisitRepository {
    void add(VisitLog visit);
    PageResult<VisitLog> findPage(long page, long size, OffsetDateTime start, OffsetDateTime end);
    int remove(UUID id);
    int removeBefore(OffsetDateTime cutoff);
    int removeAll();
    long countAll();
    long countDistinctVisitors();
    long countSessions();
}
