package com.myblog.infrastructure.persistence.mapper.footprints;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myblog.application.model.entity.Footprint;

/**
 * 足迹条目表 Mapper：仅继承 MyBatis-Plus {@link BaseMapper} 提供基础 CRUD，
 * 按 release 整包读写的编排由 {@code JdbcContentReleaseRepository} 完成。
 */
public interface FootprintMapper extends BaseMapper<Footprint> { }
