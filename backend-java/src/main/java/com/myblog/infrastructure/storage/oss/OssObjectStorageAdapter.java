package com.myblog.infrastructure.storage.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.myblog.common.properties.AppProperties;
import com.myblog.application.port.ObjectStorage;
import com.myblog.common.enumeration.ErrorCode;
import com.myblog.common.exception.InternalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.context.annotation.Lazy;

import java.io.InputStream;
import java.time.Instant;
import java.util.Date;

/**
 * 阿里云 OSS 对象存储适配器：实现应用层 {@link ObjectStorage} 端口，
 * 提供上传、异步删除、公开/签名 URL 生成；未配置 OSS 时上传与签名直接抛存储不可用异常。
 */
@Service
public class OssObjectStorageAdapter implements ObjectStorage {

    private static final Logger log = LoggerFactory.getLogger(OssObjectStorageAdapter.class);
    private final OSS oss;             // 阿里云 OSS 客户端（懒加载注入）
    private final AppProperties props; // OSS endpoint、密钥、bucket、CDN 域名等配置

    public OssObjectStorageAdapter(@Lazy OSS oss, AppProperties props) {
        this.oss = oss;
        this.props = props;
    }

    /**
     * 上传对象到 OSS，并设置 30 天 immutable 缓存头（objectKey 含内容哈希，内容不变）。
     *
     * @throws InternalException 存储未配置或上传失败时抛出
     */
    @Override
    public void upload(String objectKey, InputStream input, long size, String contentType) {
        requireConfigured();
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(size);
        metadata.setContentType(contentType);
        metadata.setCacheControl("public, max-age=2592000, immutable");
        try {
            oss.putObject(props.ossBucket(), objectKey, input, metadata);
        } catch (InternalException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new InternalException(ErrorCode.STORAGE_UNAVAILABLE, null);
        }
    }

    /** 异步删除 OSS 对象；删除失败只记日志不抛异常，避免影响主流程 */
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

    /**
     * 生成对象的公开访问 URL：配置了 CDN 域名时拼接 CDN 地址，否则回退为 1 小时有效的签名 URL。
     */
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

    /**
     * 生成带过期时间的预签名下载 URL。
     *
     * @param expiresSeconds 有效秒数
     * @throws InternalException 存储未配置或生成失败时抛出
     */
    @Override
    public String signedUrl(String objectKey, long expiresSeconds) {
        requireConfigured();
        Date expiresAt = Date.from(Instant.now().plusSeconds(expiresSeconds));
        try {
            return oss.generatePresignedUrl(props.ossBucket(), objectKey, expiresAt).toString();
        } catch (InternalException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new InternalException(ErrorCode.STORAGE_UNAVAILABLE, null);
        }
    }

    /** @return OSS 连接所需配置是否齐全 */
    @Override
    public boolean configured() {
        return StringUtils.hasText(props.ossEndpoint())
                && StringUtils.hasText(props.ossAccessKeyId())
                && StringUtils.hasText(props.ossAccessKeySecret())
                && StringUtils.hasText(props.ossBucket());
    }

    /** 校验 OSS 配置，未配置时抛存储不可用异常 */
    private void requireConfigured() {
        if (!configured()) {
            throw new InternalException(ErrorCode.STORAGE_UNAVAILABLE, "对象存储尚未配置");
        }
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String stripLeadingSlash(String value) {
        return value.startsWith("/") ? value.substring(1) : value;
    }
}
