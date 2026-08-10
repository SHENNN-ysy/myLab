package com.myblog.starter.config;

import com.myblog.common.properties.AppProperties;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * 外部服务客户端装配：目前仅提供阿里云 OSS 客户端。
 */
@Configuration
public class ExternalClients {

    /**
     * 构建阿里云 OSS 客户端。使用 @Lazy 延迟初始化，避免未配置 OSS 时应用启动失败。
     *
     * @param props 应用配置（OSS endpoint 与访问密钥）
     * @return OSS 客户端
     */
    @Bean
    @Lazy
    public OSS ossClient(AppProperties props) {
        return new OSSClientBuilder().build(
                props.ossEndpoint(),
                props.ossAccessKeyId(),
                props.ossAccessKeySecret());
    }
}
