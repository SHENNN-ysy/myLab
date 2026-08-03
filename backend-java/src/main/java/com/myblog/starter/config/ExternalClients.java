package com.myblog.starter.config;

import com.myblog.common.properties.AppProperties;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class ExternalClients {

    @Bean
    @Lazy
    public OSS ossClient(AppProperties props) {
        return new OSSClientBuilder().build(
                props.ossEndpoint(),
                props.ossAccessKeyId(),
                props.ossAccessKeySecret());
    }
}
