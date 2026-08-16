package com.myblog.infrastructure.persistence.mapper.home;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myblog.application.model.entity.HomeImage;

/**
 * 首页轮播图片表 Mapper：仅继承 MyBatis-Plus {@link BaseMapper} 提供基础 CRUD，
 * 按 release 整包读写的编排由 {@code JdbcContentReleaseRepository} 完成。
 */
public interface HomeImageMapper extends BaseMapper<HomeImage> { }
