package com.myblog.application.service.skill;

import com.myblog.application.model.entity.Skill;
import com.myblog.application.model.command.skill.SkillUpsert;
import com.myblog.common.result.PageResult;
import com.myblog.common.security.CurrentUser;

import java.util.UUID;

public interface SkillService {

    PageResult<Skill> list(CurrentUser actor, long page, long size);

    Skill create(CurrentUser actor, SkillUpsert command);

    Skill update(CurrentUser actor, UUID id, SkillUpsert command);

    void delete(CurrentUser actor, UUID id);
}
