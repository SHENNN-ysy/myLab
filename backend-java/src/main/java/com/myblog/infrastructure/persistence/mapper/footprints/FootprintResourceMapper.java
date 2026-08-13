package com.myblog.infrastructure.persistence.mapper.footprints;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myblog.application.model.entity.FootprintResource;

/**
 * 足迹-资源关联表 Mapper：仅继承 MyBatis-Plus {@link BaseMapper} 提供基础 CRUD，
 * 按足迹整包读写的编排由 {@code JdbcContentReleaseRepository} 完成。
 */
public interface FootprintResourceMapper extends BaseMapper<FootprintResource> { }
