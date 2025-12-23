package com.devpro.code_runner_service.service;

import com.devpro.code_runner_service.DTO.CustomResponse;
import com.devpro.code_runner_service.DTO.DockerRunner;

public interface ICodeRunner {
    CustomResponse runCode(String uuid, DockerRunner dockerRunner);
    CustomResponse submitCode(String uuid, DockerRunner dockerRunner);
}
