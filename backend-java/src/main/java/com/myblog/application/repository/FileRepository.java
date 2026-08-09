package com.myblog.application.repository;

import com.myblog.application.model.entity.FileRecord;
import com.myblog.common.result.PageResult;

import java.util.UUID;

public interface FileRepository {
    PageResult<FileRecord> findPage(long page, long size);
    FileRecord findById(UUID id);
    void add(FileRecord record);
    void save(FileRecord record);
    boolean hasReferences(UUID id);
}
