package com.myblog.application.service.footprint;

import com.myblog.application.model.entity.Footprint;
import com.myblog.application.model.command.footprint.FootprintUpsert;
import com.myblog.common.security.CurrentUser;

import java.util.List;
import java.util.UUID;

public interface FootprintService {

    List<Footprint> list();

    Footprint create(CurrentUser actor, FootprintUpsert command);

    Footprint update(CurrentUser actor, UUID id, FootprintUpsert command);

    void delete(CurrentUser actor, UUID id);
}
