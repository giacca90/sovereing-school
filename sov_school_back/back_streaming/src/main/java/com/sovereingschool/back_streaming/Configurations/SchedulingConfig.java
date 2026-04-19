package com.sovereingschool.back_streaming.Configurations;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SchedulingConfig {

    @Bean
    public ScheduledExecutorService pingScheduler() {
        return Executors.newScheduledThreadPool(1);
    }
}
