package com.myblog.application.model.command.skill;

public record SkillUpsert(
        String name, String category, Integer percentage, String level,
        String icon, Integer orderNum, String barStyle) {
}
