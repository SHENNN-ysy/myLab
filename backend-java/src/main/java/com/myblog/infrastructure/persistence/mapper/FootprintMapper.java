package com.myblog.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.myblog.application.model.entity.Footprint;
import com.myblog.application.repository.FootprintRepository;

import java.util.List;
import java.util.UUID;

public interface FootprintMapper extends BaseMapper<Footprint>, FootprintRepository {
    @Override default List<Footprint> findAll() {
        return selectList(new QueryWrapper<Footprint>().orderByAsc("order_num"));
    }
    @Override default Footprint findById(UUID id) { return selectById(id); }
    @Override default void add(Footprint footprint) { insert(footprint); }
    @Override default void save(Footprint footprint) { updateById(footprint); }
    @Override default boolean remove(UUID id) { return deleteById(id) > 0; }
}
