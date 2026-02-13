package com.devpro.code_runner_service.config;

import com.devpro.code_runner_service.DTO.DockerRunner;
import com.devpro.code_runner_service.models.ExecutionData;
import com.devpro.code_runner_service.service.ICodeRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ExecutionRegistry {

    private  final Map<String, ExecutionData> pending = new ConcurrentHashMap<>();

    private final ICodeRunner codeRunnerService;

    @Autowired
    public ExecutionRegistry(ICodeRunner service) {
        codeRunnerService = service;
    }

    public void store(String executionId,
                             String uuid,
                             DockerRunner runner) {

        pending.put(executionId,
                new ExecutionData(uuid, runner));
    }

    public void startExecution(String executionId) {

        ExecutionData data = pending.remove(executionId);

        if (data != null) {
            codeRunnerService.executeAsync(
                    executionId,
                    data.uuid(),
                    data.runner()
            );
        }
    }
}


