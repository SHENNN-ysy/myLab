package com.myblog.application.model.command.file;

import java.io.InputStream;

/**
 * 文件上传命令：封装待上传文件的元数据与内容流，
 * 由应用层构造后交给文件存储端口处理，不直接暴露给 Web 层。
 */
public record UploadFile(
        // 存储目录前缀（如 posts、avatars），用于组织对象键
        String directory,
        String originalName,
        String contentType,
        long size,
        InputStream content) {

    /**
     * 判断上传内容是否为空（大小不合法即视为空文件）。
     *
     * @return 文件大小小于等于 0 时返回 true
     */
    public boolean empty() {
        return size <= 0;
    }
}
