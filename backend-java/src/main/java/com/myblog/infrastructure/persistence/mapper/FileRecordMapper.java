package com.myblog.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myblog.application.model.entity.FileRecord;
/**
 * 文件资源记录表 Mapper：仅继承 MyBatis-Plus {@link BaseMapper} 提供基础 CRUD，
 * 业务查询由 {@code ResourceReferenceRepository} 组合实现。
 */
public interface FileRecordMapper extends BaseMapper<FileRecord> { }
