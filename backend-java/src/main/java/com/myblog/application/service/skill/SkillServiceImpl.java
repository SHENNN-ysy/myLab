package com.myblog.application.service.skill;

import com.myblog.application.model.entity.Skill;
import com.myblog.common.exception.NotFoundException;
import com.myblog.common.exception.ValidationException;
import com.myblog.application.repository.SkillRepository;
import com.myblog.application.model.command.skill.SkillUpsert;
import com.myblog.common.security.CurrentUser;
import com.myblog.common.result.PageResult;
import com.myblog.common.security.Authorization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class SkillServiceImpl implements SkillService {

    private final SkillRepository skills;

    public SkillServiceImpl(SkillRepository skills) {
        this.skills = skills;
    }

    @Override
    public PageResult<Skill> list(CurrentUser actor, long page, long size) {
        return skills.findPage(page, size);
    }

    @Override
    @Transactional
    public Skill create(CurrentUser actor, SkillUpsert command) {
        Authorization.requireAdmin(actor);
        validate(command, true);
        Skill skill = new Skill();
        apply(skill, command);
        initialize(skill);
        skills.add(skill);
        return skill;
    }

    @Override
    @Transactional
    public Skill update(CurrentUser actor, UUID id, SkillUpsert command) {
        Authorization.requireAdmin(actor);
        validate(command, false);
        Skill skill = require(id);
        apply(skill, command);
        skill.setUpdatedAt(OffsetDateTime.now());
        skills.save(skill);
        return skill;
    }

    @Override
    @Transactional
    public void delete(CurrentUser actor, UUID id) {
        Authorization.requireAdmin(actor);
        if (!skills.remove(id)) throw new NotFoundException("skill not found");
    }

    private static void validate(SkillUpsert command, boolean creating) {
        if (creating && command.name() == null) throw new ValidationException("name is required");
        if (command.percentage() != null) {
            int percentage = command.percentage();
            if (percentage < 0 || percentage > 100) {
                throw new ValidationException("percentage out of range");
            }
        }
    }

    private Skill require(UUID id) {
        Skill skill = skills.findById(id);
        if (skill == null) throw new NotFoundException("skill not found");
        return skill;
    }

    private static void initialize(Skill skill) {
        skill.setId(UUID.randomUUID());
        OffsetDateTime now = OffsetDateTime.now();
        skill.setCreatedAt(now);
        skill.setUpdatedAt(now);
    }

    private static void apply(Skill skill, SkillUpsert command) {
        if (command.name() != null) skill.setName(command.name());
        if (command.category() != null) skill.setCategory(command.category());
        if (command.percentage() != null) skill.setPercentage(command.percentage());
        if (command.level() != null) skill.setLevel(command.level());
        if (command.icon() != null) skill.setIcon(command.icon());
        if (command.orderNum() != null) skill.setOrderNum(command.orderNum());
        if (command.barStyle() != null) skill.setBarStyle(command.barStyle());
    }
}
