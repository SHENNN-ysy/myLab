package com.myblog.application.model.command.file;

import java.io.InputStream;

public record UploadFile(
        String originalName,
        String contentType,
        long size,
        InputStream content) {

    public boolean empty() {
        return size <= 0;
    }
}
