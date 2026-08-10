package com.myblog.starter.config;

import com.myblog.application.service.auth.AuthService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动初始化器：应用启动完成后执行一次性初始化逻辑。
 */
@Component
public class StartupInitializer implements ApplicationRunner {

    private final AuthService auth; // 认证服务

    public StartupInitializer(AuthService auth) {
        this.auth = auth;
    }

    /**
     * 应用启动后确保默认管理员账号存在（不存在则创建）。
     */
    @Override
    public void run(ApplicationArguments args) {
        auth.ensureAdmin();
    }
}
