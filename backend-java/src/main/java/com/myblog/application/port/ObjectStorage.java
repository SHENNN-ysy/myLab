package com.myblog.application.port;

import java.io.InputStream;

public interface ObjectStorage {

    void upload(String objectKey, InputStream input, long size, String contentType);

    void deleteAsync(String objectKey);

    String publicUrl(String objectKey);

    String signedUrl(String objectKey, long expiresSeconds);

    boolean configured();
}
