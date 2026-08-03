package com.myblog.application.model.command.visit;

/** 与HTTP协议无关的访问记录命令。 */
public record RecordVisit(String ip, String userAgent, String path, String referer) {
}
