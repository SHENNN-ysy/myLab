package com.myblog.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myblog.application.model.entity.ContentPublication;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ContentPublicationMapper extends BaseMapper<ContentPublication> {
}

