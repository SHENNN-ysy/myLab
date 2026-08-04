package com.myblog.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myblog.application.model.entity.VisitLog;
import com.myblog.application.repository.VisitRepository;
import com.myblog.common.result.PageResult;
import com.myblog.infrastructure.persistence.mapper.VisitLogMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public class MybatisVisitRepository implements VisitRepository {

    private final VisitLogMapper mapper;
    private final JdbcTemplate jdbc;

    public MybatisVisitRepository(VisitLogMapper mapper, JdbcTemplate jdbc) {
        this.mapper = mapper;
        this.jdbc = jdbc;
    }

    @Override public void add(VisitLog visit) { mapper.insert(visit); }

    @Override
    public PageResult<VisitLog> findPage(long page, long size, OffsetDateTime start, OffsetDateTime end) {
        LambdaQueryWrapper<VisitLog> query = new LambdaQueryWrapper<VisitLog>()
                .ge(start != null, VisitLog::getVisitedAt, start)
                .le(end != null, VisitLog::getVisitedAt, end)
                .orderByDesc(VisitLog::getVisitedAt);
        Page<VisitLog> result = mapper.selectPage(new Page<>(page, size), query);
        return PageResult.of(result.getRecords(), page, size, result.getTotal());
    }

    @Override public int remove(UUID id) { return mapper.deleteById(id); }

    @Override
    public int removeBefore(OffsetDateTime cutoff) {
        return mapper.delete(new LambdaQueryWrapper<VisitLog>().lt(VisitLog::getVisitedAt, cutoff));
    }

    @Override public int removeAll() { return jdbc.update("delete from visit_logs"); }

    @Override
    public long countAll() {
        Long count = jdbc.queryForObject("select count(*) from visit_logs", Long.class);
        return count == null ? 0L : count;
    }

    @Override
    public long countDistinctVisitors() {
        Long count = jdbc.queryForObject("select count(distinct ip) from visit_logs", Long.class);
        return count == null ? 0L : count;
    }

    @Override
    public long countSessions() {
        Long count = jdbc.queryForObject(
                "select count(*) from (select ip, date_trunc('day', visited_at) from visit_logs group by ip, date_trunc('day', visited_at)) sessions",
                Long.class);
        return count == null ? 0L : count;
    }
}
