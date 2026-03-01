package org.example.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Пул потоков для фоновых воркеров (SubmitWorker, ApproveWorker).
     * Каждый воркер работает в своём потоке, не блокируя основной.
     */
    @Bean(name = "workerTaskExecutor")
    public Executor workerTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);        // 2 потока — по одному на каждый воркер
        executor.setMaxPoolSize(4);         // максимум 4 при пиковой нагрузке
        executor.setQueueCapacity(10);      // очередь задач
        executor.setThreadNamePrefix("worker-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30); // ждём завершения задач при остановке
        executor.initialize();
        return executor;
    }

    /**
     * Пул потоков для теста конкурентного утверждения (ConcurrencyTestService).
     */
    @Bean(name = "concurrencyTestExecutor")
    public Executor concurrencyTestExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);       // до 20 параллельных потоков для теста
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("concurrency-test-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}