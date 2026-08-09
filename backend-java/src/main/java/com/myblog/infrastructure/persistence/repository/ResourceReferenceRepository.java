package com.myblog.infrastructure.persistence.repository;

import com.myblog.application.model.entity.FileRecord;
import com.myblog.application.repository.FileRepository;
import com.myblog.common.result.PageResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myblog.infrastructure.persistence.mapper.FileRecordMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class ResourceReferenceRepository implements FileRepository {
    private final FileRecordMapper mapper;
    private final JdbcTemplate jdbc;

    public ResourceReferenceRepository(FileRecordMapper mapper, JdbcTemplate jdbc) {
        this.mapper = mapper;
        this.jdbc = jdbc;
    }

    @Override public PageResult<FileRecord> findPage(long page, long size) {
        Page<FileRecord> result = mapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<FileRecord>().isNull(FileRecord::getDeletedAt)
                        .orderByDesc(FileRecord::getCreatedAt));
        return PageResult.of(result.getRecords(), page, size, result.getTotal());
    }
    @Override public FileRecord findById(UUID id) { return mapper.selectById(id); }
    @Override public void add(FileRecord record) { mapper.insert(record); }
    @Override public void save(FileRecord record) { mapper.updateById(record); }

    @Override
    public boolean hasReferences(UUID id) {
        Long count = jdbc.queryForObject("""
                SELECT
                    (SELECT COUNT(*) FROM home_images WHERE image_resource_id = ? AND deleted_at IS NULL)
                  + (SELECT COUNT(*) FROM about_contents WHERE avatar_resource_id = ? AND deleted_at IS NULL)
                  + (SELECT COUNT(*) FROM skills WHERE icon_resource_id = ? AND deleted_at IS NULL)
                  + (SELECT COUNT(*) FROM footprint_resources WHERE resource_id = ? AND deleted_at IS NULL)
                  + (SELECT COUNT(*) FROM hobby_resources WHERE resource_id = ? AND deleted_at IS NULL)
                  + (SELECT COUNT(*) FROM mylab_resources
                     WHERE (image_resource_id = ? OR content_resource_id = ?) AND deleted_at IS NULL)
                """, Long.class, id, id, id, id, id, id, id);
        return count != null && count > 0;
    }
}
