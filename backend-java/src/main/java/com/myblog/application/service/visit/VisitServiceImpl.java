package com.myblog.application.service.visit;

import com.myblog.application.model.entity.VisitLog;
import com.myblog.application.port.VisitCounter;
import com.myblog.application.repository.VisitRepository;
import com.myblog.application.model.command.visit.RecordVisit;
import com.myblog.common.result.PageResult;
import com.myblog.common.security.CurrentUser;
import com.myblog.common.security.Authorization;
import com.myblog.application.model.vo.VisitStatsVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class VisitServiceImpl implements VisitService {

    private final VisitRepository visits;
    private final VisitCounter counter;

    public VisitServiceImpl(VisitRepository visits, VisitCounter counter) {
        this.visits = visits;
        this.counter = counter;
    }

    @Override
    public void track(RecordVisit command) {
        VisitLog log = new VisitLog();
        log.setId(UUID.randomUUID());
        log.setIp(command.ip());
        log.setUserAgent(Objects.toString(command.userAgent(), ""));
        log.setPath(command.path());
        log.setReferer(command.referer());
        log.setVisitedAt(OffsetDateTime.now());
        visits.add(log);

        String date = LocalDate.now(ZoneOffset.UTC).toString();
        try {
            counter.record(date, log.getIp());
        } catch (Exception ignored) {
            // 访问记录已经入库，Redis统计失败不影响访客请求。
        }
    }

    @Override
    public VisitStatsVO stats(CurrentUser actor, String date) {
        Authorization.requireAdmin(actor);
        String day = date == null ? LocalDate.now(ZoneOffset.UTC).toString() : date;
        long pv = counter.pageViews(day);
        long uv = counter.uniqueVisitors(day);
        Long total = visits.countAll();
        Long totalUv = visits.countDistinctVisitors();
        return new VisitStatsVO(day, pv, uv, total, totalUv, total);
    }

    @Override
    public PageResult<VisitLog> logs(CurrentUser actor, long page, long size,
                                    OffsetDateTime start, OffsetDateTime end) {
        Authorization.requireAdmin(actor);
        return visits.findPage(page, size, start, end);
    }

    @Override
    public Map<String, Integer> delete(CurrentUser actor, UUID id) {
        Authorization.requireAdmin(actor);
        int n = visits.remove(id);
        return Map.of("deleted", n);
    }

    @Override
    public Map<String, Integer> batchDelete(CurrentUser actor, OffsetDateTime cutoff) {
        Authorization.requireSuperadmin(actor);
        int n = visits.removeBefore(cutoff);
        return Map.of("deleted", n);
    }

    @Override
    public Map<String, Integer> clear(CurrentUser actor) {
        Authorization.requireSuperadmin(actor);
        int n = visits.removeAll();
        return Map.of("deleted", n);
    }

}
