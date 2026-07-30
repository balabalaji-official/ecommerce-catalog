package com.example.catalog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Dedicated thread pool for background / write-side-effect work (cache
 * invalidation, cache warming, bulk seeding) so those tasks never block the
 * request-handling threads. Kept separate from Tomcat's request thread pool
 * on purpose - a slow cache write should never slow down an HTTP response.
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "cacheTaskExecutor")
    public Executor cacheTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("cache-async-");
        executor.initialize();
        return executor;
    }
}
