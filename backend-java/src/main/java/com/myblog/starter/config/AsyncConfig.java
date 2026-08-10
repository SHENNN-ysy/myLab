package com.myblog.starter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步线程池配置：为 OSS 资源清理等存储类后台任务提供专用线程池。
 */
@Configuration
public class AsyncConfig {

    /**
     * 存储任务线程池：以单线程为主、最多 2 线程；关闭时等待已提交任务完成，避免清理任务被中断。
     *
     * @return 名为 storageTaskExecutor 的线程池执行器
     */
    @Bean(name = "storageTaskExecutor")
    public Executor storageTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("oss-cleanup-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
        executor.initialize();
        return executor;
    }
}
