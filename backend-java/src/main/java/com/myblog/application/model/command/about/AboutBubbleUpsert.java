package com.myblog.application.model.command.about;

public record AboutBubbleUpsert(
        String label, String bgColor, String glowColor, String textColor,
        Double positionX, Double positionY, Double radius, String tier,
        Integer orderNum, Boolean enabled, String remark) {
}
