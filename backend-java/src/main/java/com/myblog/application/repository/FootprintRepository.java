package com.myblog.application.repository;

import com.myblog.application.model.entity.Footprint;

import java.util.List;
import java.util.UUID;

public interface FootprintRepository {
    List<Footprint> findAll();
    Footprint findById(UUID id);
    void add(Footprint footprint);
    void save(Footprint footprint);
    boolean remove(UUID id);
}
