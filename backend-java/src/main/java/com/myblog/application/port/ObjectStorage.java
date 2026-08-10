package com.myblog.application.port;

import java.io.InputStream;

/**
 * 对象存储端口：应用层对文件对象存取能力的抽象（如 OSS），由基础设施层实现。
 */
public interface ObjectStorage {

    /**
     * 上传文件对象。
     *
     * @param objectKey 对象键（存储路径）
     * @param input 文件内容流
     * @param size 文件大小（字节）
     * @param contentType 文件 MIME 类型
     */
    void upload(String objectKey, InputStream input, long size, String contentType);

    /**
     * 异步删除对象，不阻塞调用方。
     */
    void deleteAsync(String objectKey);

    /**
     * 返回对象的公开访问地址。
     */
    String publicUrl(String objectKey);

    /**
     * 生成带过期时间的签名访问地址，用于临时授权访问。
     *
     * @param expiresSeconds 签名有效期（秒）
     */
    String signedUrl(String objectKey, long expiresSeconds);

    /**
     * 对象存储是否已正确配置；未配置时相关功能应降级处理。
     */
    boolean configured();
}
