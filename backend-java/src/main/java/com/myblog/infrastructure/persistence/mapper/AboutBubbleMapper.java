package com.myblog.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.myblog.application.model.entity.AboutBubble;
import com.myblog.application.repository.AboutBubbleRepository;

import java.util.List;
import java.util.UUID;

public interface AboutBubbleMapper extends BaseMapper<AboutBubble>, AboutBubbleRepository {
    @Override default List<AboutBubble> findAll() {
        return selectList(new QueryWrapper<AboutBubble>().orderByAsc("order_num"));
    }
    @Override default AboutBubble findById(UUID id) { return selectById(id); }
    @Override default void add(AboutBubble bubble) { insert(bubble); }
    @Override default void save(AboutBubble bubble) { updateById(bubble); }
    @Override default boolean remove(UUID id) { return deleteById(id) > 0; }
}
