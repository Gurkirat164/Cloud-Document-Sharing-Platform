package com.cloudvault.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configures the application-wide async task executor used by {@code @Async} methods.
 *
 * <p>The activity logging methods in {@code ActivityLogService} are annotated with
 * {@link org.springframework.scheduling.annotation.Async} and will be dispatched to
 * the thread pool defined here, keeping audit writes completely off the
 * request-handling thread.
 *
 * <p>Thread-pool sizing rationale:
 * <ul>
 *   <li><b>core threads (4)</b> — always alive; handle baseline audit traffic.</li>
 *   <li><b>max threads (20)</b> — burst capacity under heavy concurrent load.</li>
 *   <li><b>queue capacity (500)</b> — absorbs spikes without spawning new threads immediately.</li>
 * </ul>
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Defines the default executor used by {@code @Async} when no explicit qualifier
     * is provided.  Named {@code "taskExecutor"} so Spring's
     * {@link org.springframework.scheduling.annotation.AsyncConfigurer} auto-wires it.
     *
     * @return configured {@link ThreadPoolTaskExecutor}
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("async-activity-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
