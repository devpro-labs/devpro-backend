package com.devpro.code_runner_service.service;

import com.devpro.code_runner_service.DTO.CustomResponse;
import com.devpro.code_runner_service.DTO.DockerRunner;

public interface ICodeRunner {
    CustomResponse runCode(String uuid, DockerRunner dockerRunner, String executionId);
    CustomResponse submitCode(String uuid, DockerRunner dockerRunner);

    void executeAsync(String executionId, String uuid, DockerRunner runner);
}
