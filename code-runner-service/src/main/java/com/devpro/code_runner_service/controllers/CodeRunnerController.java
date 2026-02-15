package com.devpro.code_runner_service.controllers;

import com.devpro.code_runner_service.DTO.CustomResponse;
import com.devpro.code_runner_service.DTO.DockerRunner;
import com.devpro.code_runner_service.config.ExecutionRegistry;
import com.devpro.code_runner_service.service.ICodeRunner;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/code-runner")
public class CodeRunnerController {
    private final ICodeRunner codeRunnerService;
    private final ExecutionRegistry executionRegistry;

    public CodeRunnerController(ICodeRunner codeRunnerService, ExecutionRegistry executionRegistry){
        this.codeRunnerService = codeRunnerService;
        this.executionRegistry = executionRegistry;
    }


    @PostMapping("/run/{id}")
    public CustomResponse codeRunHelper(@PathVariable String id, @RequestBody DockerRunner dockerRunner){
        // Generate executionId (you already use projectId maybe)
        String executionId = UUID.randomUUID().toString();

        // Start async execution
        executionRegistry.store(executionId, id, dockerRunner);
        return codeRunnerService.runCode(id, dockerRunner, executionId);
    }

    @PostMapping("/submit/{id}")
    public CustomResponse codeSubmitHelper(@PathVariable String id, @RequestBody DockerRunner dockerRunner, HttpServletRequest request){
        return codeRunnerService.submitCode(id, dockerRunner, request);
    }


}
