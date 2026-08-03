package com.myblog.application.model.command.project;

public record ProjectUpsert(
        String title, String slug, String description, String content,
        String tag, Integer year, String imageUrl, String projectUrl,
        String repoUrl, Object tech, Integer orderNum) {
}
