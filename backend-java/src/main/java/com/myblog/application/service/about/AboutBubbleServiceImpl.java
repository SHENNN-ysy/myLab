package com.myblog.application.service.about;

import com.myblog.application.model.entity.AboutBubble;
import com.myblog.common.exception.NotFoundException;
import com.myblog.common.exception.ValidationException;
import com.myblog.application.repository.AboutBubbleRepository;
import com.myblog.application.model.command.about.AboutBubbleUpsert;
import com.myblog.common.security.CurrentUser;
import com.myblog.common.security.Authorization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AboutBubbleServiceImpl implements AboutBubbleService {

    private final AboutBubbleRepository bubbles;

    public AboutBubbleServiceImpl(AboutBubbleRepository bubbles) {
        this.bubbles = bubbles;
    }

    @Override
    public List<AboutBubble> list() {
        return bubbles.findAll();
    }

    @Override
    @Transactional
    public AboutBubble create(CurrentUser actor, AboutBubbleUpsert command) {
        Authorization.requireAdmin(actor);
        if (command.label() == null) throw new ValidationException("label is required");
        AboutBubble bubble = new AboutBubble();
        apply(bubble, command);
        initialize(bubble);
        bubbles.add(bubble);
        return bubble;
    }

    @Override
    @Transactional
    public AboutBubble update(CurrentUser actor, UUID id, AboutBubbleUpsert command) {
        Authorization.requireAdmin(actor);
        AboutBubble bubble = require(id);
        apply(bubble, command);
        bubble.setUpdatedAt(OffsetDateTime.now());
        bubbles.save(bubble);
        return bubble;
    }

    @Override
    @Transactional
    public void delete(CurrentUser actor, UUID id) {
        Authorization.requireAdmin(actor);
        if (!bubbles.remove(id)) throw new NotFoundException("bubble not found");
    }

    private AboutBubble require(UUID id) {
        AboutBubble bubble = bubbles.findById(id);
        if (bubble == null) throw new NotFoundException("bubble not found");
        return bubble;
    }

    private static void initialize(AboutBubble bubble) {
        bubble.setId(UUID.randomUUID());
        OffsetDateTime now = OffsetDateTime.now();
        bubble.setCreatedAt(now);
        bubble.setUpdatedAt(now);
    }

    private static void apply(AboutBubble bubble, AboutBubbleUpsert command) {
        if (command.label() != null) bubble.setLabel(command.label());
        if (command.bgColor() != null) bubble.setBgColor(command.bgColor());
        if (command.glowColor() != null) bubble.setGlowColor(command.glowColor());
        if (command.textColor() != null) bubble.setTextColor(command.textColor());
        if (command.positionX() != null) bubble.setPositionX(command.positionX());
        if (command.positionY() != null) bubble.setPositionY(command.positionY());
        if (command.radius() != null) bubble.setRadius(command.radius());
        if (command.tier() != null) bubble.setTier(command.tier());
        if (command.orderNum() != null) bubble.setOrderNum(command.orderNum());
        if (command.enabled() != null) bubble.setEnabled(command.enabled());
        if (command.remark() != null) bubble.setRemark(command.remark());
    }
}
