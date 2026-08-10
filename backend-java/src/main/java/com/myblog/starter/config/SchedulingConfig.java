package com.myblog.starter.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 开启互动聚合快照所需的定时任务。 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
