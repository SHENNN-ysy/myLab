package com.myblog.starter.config;

import com.myblog.application.service.auth.AuthService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupInitializer implements ApplicationRunner {

    private final AuthService auth;

    public StartupInitializer(AuthService auth) {
        this.auth = auth;
    }

    @Override
    public void run(ApplicationArguments args) {
        auth.ensureAdmin();
    }
}
