package com.myblog.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myblog.application.model.entity.FileRecord;
import com.myblog.application.repository.FileRepository;
import com.myblog.common.result.PageResult;

import java.util.UUID;

public interface FileRecordMapper extends BaseMapper<FileRecord>, FileRepository {
    @Override default PageResult<FileRecord> findPage(long page, long size) {
        Page<FileRecord> result = selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<FileRecord>()
                        .eq(FileRecord::getIsDeleted, false)
                        .orderByDesc(FileRecord::getCreatedAt));
        return PageResult.of(result.getRecords(), page, size, result.getTotal());
    }
    @Override default FileRecord findById(UUID id) { return selectById(id); }
    @Override default void add(FileRecord record) { insert(record); }
    @Override default void save(FileRecord record) { updateById(record); }
}
