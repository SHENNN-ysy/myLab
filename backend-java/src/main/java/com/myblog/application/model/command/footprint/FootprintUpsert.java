package com.myblog.application.model.command.footprint;

public record FootprintUpsert(
        String name, String slug, String tag, Double positionX,
        Double positionY, Boolean isSelf, Object tipData, Integer orderNum) {
}
