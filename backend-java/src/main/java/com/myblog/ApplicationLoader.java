package com.myblog;

import com.myblog.common.properties.AppProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 应用启动入口：开启异步支持、扫描 MyBatis Mapper、注册应用配置属性，并启动 Spring Boot 容器。
 */
@EnableAsync
@MapperScan("com.myblog.infrastructure.persistence.mapper")
@EnableConfigurationProperties(AppProperties.class)
@SpringBootApplication
public class ApplicationLoader {

    /**
     * 启动整个博客后端应用。
     */
    public static void main(String[] args) {
        SpringApplication.run(ApplicationLoader.class, args);
    }
}
