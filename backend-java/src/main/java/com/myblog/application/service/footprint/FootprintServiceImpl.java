package com.myblog.application.service.footprint;

import com.myblog.application.model.entity.Footprint;
import com.myblog.common.exception.NotFoundException;
import com.myblog.common.exception.ValidationException;
import com.myblog.application.repository.FootprintRepository;
import com.myblog.application.model.command.footprint.FootprintUpsert;
import com.myblog.common.security.CurrentUser;
import com.myblog.common.security.Authorization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class FootprintServiceImpl implements FootprintService {

    private final FootprintRepository footprints;

    public FootprintServiceImpl(FootprintRepository footprints) {
        this.footprints = footprints;
    }

    @Override
    public List<Footprint> list() {
        return footprints.findAll();
    }

    @Override
    @Transactional
    public Footprint create(CurrentUser actor, FootprintUpsert command) {
        Authorization.requireAdmin(actor);
        if (command.name() == null || command.slug() == null) {
            throw new ValidationException("name and slug are required");
        }
        Footprint footprint = new Footprint();
        apply(footprint, command);
        initialize(footprint);
        footprints.add(footprint);
        return footprint;
    }

    @Override
    @Transactional
    public Footprint update(CurrentUser actor, UUID id, FootprintUpsert command) {
        Authorization.requireAdmin(actor);
        Footprint footprint = require(id);
        apply(footprint, command);
        footprint.setUpdatedAt(OffsetDateTime.now());
        footprints.save(footprint);
        return footprint;
    }

    @Override
    @Transactional
    public void delete(CurrentUser actor, UUID id) {
        Authorization.requireAdmin(actor);
        if (!footprints.remove(id)) throw new NotFoundException("footprint not found");
    }

    private Footprint require(UUID id) {
        Footprint footprint = footprints.findById(id);
        if (footprint == null) throw new NotFoundException("footprint not found");
        return footprint;
    }

    private static void initialize(Footprint footprint) {
        footprint.setId(UUID.randomUUID());
        OffsetDateTime now = OffsetDateTime.now();
        footprint.setCreatedAt(now);
        footprint.setUpdatedAt(now);
    }

    private static void apply(Footprint footprint, FootprintUpsert command) {
        if (command.name() != null) footprint.setName(command.name());
        if (command.slug() != null) footprint.setSlug(command.slug());
        if (command.tag() != null) footprint.setTag(command.tag());
        if (command.positionX() != null) footprint.setPositionX(command.positionX());
        if (command.positionY() != null) footprint.setPositionY(command.positionY());
        if (command.isSelf() != null) footprint.setIsSelf(command.isSelf());
        if (command.tipData() != null) footprint.setTipData(command.tipData());
        if (command.orderNum() != null) footprint.setOrderNum(command.orderNum());
    }
}
