package com.myblog.application.service.visit;

import com.myblog.application.model.entity.VisitLog;
import com.myblog.application.model.command.visit.RecordVisit;
import com.myblog.common.result.PageResult;
import com.myblog.common.security.CurrentUser;
import com.myblog.application.model.vo.VisitStatsVO;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public interface VisitService {

    void track(RecordVisit command);

    VisitStatsVO stats(CurrentUser actor, String date);

    PageResult<VisitLog> logs(CurrentUser actor, long page, long size, OffsetDateTime start, OffsetDateTime end);

    Map<String, Integer> delete(CurrentUser actor, UUID id);

    Map<String, Integer> batchDelete(CurrentUser actor, OffsetDateTime cutoff);

    Map<String, Integer> clear(CurrentUser actor);
}
