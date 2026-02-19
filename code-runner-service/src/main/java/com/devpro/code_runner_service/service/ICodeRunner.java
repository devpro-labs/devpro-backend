package com.devpro.code_runner_service.service;

import com.devpro.code_runner_service.DTO.CustomResponse;
import com.devpro.code_runner_service.DTO.DockerRunner;
import jakarta.servlet.http.HttpServletRequest;

public interface ICodeRunner {
    CustomResponse runCode(String problemId, DockerRunner dockerRunner, String executionId);
    CustomResponse submitCode(String problemId, DockerRunner dockerRunner, HttpServletRequest request);

    void executeAsync(String executionId, String problemId, DockerRunner runner, String userId);
}
