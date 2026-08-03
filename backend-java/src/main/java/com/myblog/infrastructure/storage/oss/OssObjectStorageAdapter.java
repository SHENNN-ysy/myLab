package com.myblog.infrastructure.storage.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.myblog.common.properties.AppProperties;
import com.myblog.application.port.ObjectStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.context.annotation.Lazy;

import java.io.InputStream;
import java.time.Instant;
import java.util.Date;

@Service
public class OssObjectStorageAdapter implements ObjectStorage {

    private static final Logger log = LoggerFactory.getLogger(OssObjectStorageAdapter.class);

    private final OSS oss;
    private final AppProperties props;

    public OssObjectStorageAdapter(@Lazy OSS oss, AppProperties props) {
        this.oss = oss;
        this.props = props;
    }

    @Override
    public void upload(String objectKey, InputStream input, long size, String contentType) {
        requireConfigured();
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(size);
        metadata.setContentType(contentType);
        metadata.setCacheControl("public, max-age=2592000, immutable");
        oss.putObject(props.ossBucket(), objectKey, input, metadata);
    }

    @Override
    @Async("storageTaskExecutor")
    public void deleteAsync(String objectKey) {
        if (!configured()) {
            log.error("OSS delete skipped because storage is not configured: objectKey={}", objectKey);
            return;
        }
        try {
            oss.deleteObject(props.ossBucket(), objectKey);
        } catch (Exception exception) {
            log.error("OSS object deletion failed: bucket={}, objectKey={}",
                    props.ossBucket(), objectKey, exception);
        }
    }

    @Override
    public String publicUrl(String objectKey) {
        if (!StringUtils.hasText(props.ossCdnDomain())) {
            return signedUrl(objectKey, 3600);
        }
        String domain = props.ossCdnDomain().trim();
        if (!domain.startsWith("http://") && !domain.startsWith("https://")) {
            domain = "https://" + domain;
        }
        return stripTrailingSlash(domain) + "/" + stripLeadingSlash(objectKey);
    }

    @Override
    public String signedUrl(String objectKey, long expiresSeconds) {
        requireConfigured();
        Date expiresAt = Date.from(Instant.now().plusSeconds(expiresSeconds));
        return oss.generatePresignedUrl(props.ossBucket(), objectKey, expiresAt).toString();
    }

    @Override
    public boolean configured() {
        return StringUtils.hasText(props.ossEndpoint())
                && StringUtils.hasText(props.ossAccessKeyId())
                && StringUtils.hasText(props.ossAccessKeySecret())
                && StringUtils.hasText(props.ossBucket());
    }

    private void requireConfigured() {
        if (!configured()) {
            throw new IllegalStateException("OSS is not configured");
        }
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String stripLeadingSlash(String value) {
        return value.startsWith("/") ? value.substring(1) : value;
    }
}
