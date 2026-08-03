package com.myblog.application.repository;

import com.myblog.application.model.entity.AboutBubble;

import java.util.List;
import java.util.UUID;

public interface AboutBubbleRepository {
    List<AboutBubble> findAll();
    AboutBubble findById(UUID id);
    void add(AboutBubble bubble);
    void save(AboutBubble bubble);
    boolean remove(UUID id);
}
