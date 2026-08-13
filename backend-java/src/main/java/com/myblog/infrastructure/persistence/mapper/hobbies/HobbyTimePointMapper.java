package com.myblog.infrastructure.persistence.mapper.hobbies;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myblog.application.model.entity.HobbyTimePoint;

/**
 * 爱好时间分布数据点表 Mapper：仅继承 MyBatis-Plus {@link BaseMapper} 提供基础 CRUD，
 * 按 release 整包读写的编排由 {@code JdbcContentReleaseRepository} 完成。
 */
public interface HobbyTimePointMapper extends BaseMapper<HobbyTimePoint> { }
