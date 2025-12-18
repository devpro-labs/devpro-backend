package com.devpro.code_runner_service.repository;

import com.devpro.code_runner_service.DTO.CustomResponse;
import com.devpro.code_runner_service.DTO.DockerRunner;

public interface IDockerRepo {
    CustomResponse getPreviewURL(DockerRunner dockerRunner) throws  Exception;
    CustomResponse deleteContainer(String containerId, String fileId, String fileName);
}
