package com.sovereingschool.back_streaming.Configurations;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor defaultTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10); // núcleos mínimos
        executor.setMaxPoolSize(30); // máximo de hilos
        executor.setQueueCapacity(100); // cola de tareas pendientes
        executor.setThreadNamePrefix("AsyncTask-"); // nombre para debug
        executor.initialize();
        return executor;
    }
}
