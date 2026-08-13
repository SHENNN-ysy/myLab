package com.myblog.application.repository;

import com.myblog.application.model.entity.FileRecord;
import com.myblog.application.model.vo.FileReferenceVO;
import com.myblog.common.result.PageResult;

import java.util.List;
import java.util.UUID;

/**
 * 文件仓储接口：文件元数据记录（FileRecord）的持久化抽象，由基础设施层实现。
 */
public interface FileRepository {
    /**
     * 按对象键前缀分页查询文件记录。
     */
    PageResult<FileRecord> findPage(long page, long size, String objectKeyPrefix);
    /**
     * 按 ID 查询文件记录，不存在时返回 null。
     */
    FileRecord findById(UUID id);
    /**
     * 新增文件记录。
     */
    void add(FileRecord record);
    /**
     * 更新已有文件记录。
     */
    void save(FileRecord record);
    /**
     * 文件是否仍被业务内容引用，用于删除前的安全性校验。
     */
    boolean hasReferences(UUID id);
    /**
     * 查询文件被哪些内容版本引用的明细（模块、版本号、版本状态、用途），无引用返回空列表。
     */
    List<FileReferenceVO> findReferences(UUID id);
}
