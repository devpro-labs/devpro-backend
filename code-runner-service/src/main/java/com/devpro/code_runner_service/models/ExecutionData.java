package com.devpro.code_runner_service.models;

import com.devpro.code_runner_service.DTO.DockerRunner;

public record ExecutionData(String uuid, DockerRunner runner) {}
