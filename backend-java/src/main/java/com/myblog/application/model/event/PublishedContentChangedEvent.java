package com.myblog.application.model.event;

/**
 * 已发布内容发生变化事件，仅在发布或下线事务提交后用于清理公开缓存。
 */
public record PublishedContentChangedEvent(String moduleKey) {
}
