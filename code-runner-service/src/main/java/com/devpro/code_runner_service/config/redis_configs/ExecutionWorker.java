package com.devpro.code_runner_service.config.redis_configs;
import com.devpro.code_runner_service.models.ExecutionData;
import com.devpro.code_runner_service.service.ICodeRunner;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class ExecutionWorker {

    private final RedisTemplate<String, String> redisTemplate;
    private final ICodeRunner codeRunnerService;
    private final ObjectMapper objectMapper;
    private Boolean isRunning = true;
    private Thread workerThread;

    public ExecutionWorker(RedisTemplate<String, String > redisTemplate,
                           ICodeRunner codeRunnerService, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.codeRunnerService = codeRunnerService;
        this.objectMapper = objectMapper;
    }

    @PreDestroy
    public void stopWorker() {
        isRunning = false;
        if (workerThread != null) {
            workerThread.interrupt();
        }
        System.out.println("Worker stopped cleanly.");
    }

    @PostConstruct
    public void startWorker() {
        workerThread = new Thread(this::consumeQueue);
        workerThread.start();
    }

    private void consumeQueue() {
        String key = "queue:code-runs";

        while (isRunning && !Thread.currentThread().isInterrupted()) {
            try {
                String json = redisTemplate.opsForList()
                        .leftPop(key, Duration.ofSeconds(30));

                if (json != null) {

                    ExecutionData data =
                            objectMapper.readValue(json, ExecutionData.class);

                    codeRunnerService.executeAsync(
                            data.executionId(),
                            data.problemId(),
                            data.runner(),
                            data.userId()
                    );
                }

            } catch (Exception e) {
                System.err.println("Worker error: " + e.getMessage());
            }
        }
    }

}
