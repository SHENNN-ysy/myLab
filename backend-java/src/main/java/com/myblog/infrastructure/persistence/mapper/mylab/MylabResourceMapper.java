package com.myblog.infrastructure.persistence.mapper.mylab;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myblog.application.model.entity.MylabResource;

/**
 * 实验室卡片资源表 Mapper：仅继承 MyBatis-Plus {@link BaseMapper} 提供基础 CRUD，
 * 按 release 整包读写的编排由 {@code JdbcContentReleaseRepository} 完成。
 */
public interface MylabResourceMapper extends BaseMapper<MylabResource> { }
