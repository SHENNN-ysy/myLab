package com.myblog;

import com.myblog.common.properties.AppProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@MapperScan("com.myblog.infrastructure.persistence.mapper")
@EnableConfigurationProperties(AppProperties.class)
@SpringBootApplication
public class ApplicationLoader {

    public static void main(String[] args) {
        SpringApplication.run(ApplicationLoader.class, args);
    }
}
