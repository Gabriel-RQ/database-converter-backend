package com.gabrielrq.database_converter.config;

import com.gabrielrq.database_converter.util.migration.MigrationTaskDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;


@EnableAsync
@Configuration
public class AsyncConfig {

    @Bean(name = "asyncEtlExecutor")
    public Executor asyncEtlExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("async-migration-etl-");

        executor.setTaskDecorator(new MigrationTaskDecorator());

        executor.initialize();

        return executor;
    }
}
