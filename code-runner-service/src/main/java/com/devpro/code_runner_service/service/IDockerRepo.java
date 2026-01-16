package com.devpro.code_runner_service.service;

import com.devpro.code_runner_service.DTO.CustomResponse;
import com.devpro.code_runner_service.DTO.DockerRunner;

public interface IDockerRepo {

    CustomResponse getPreviewURL(DockerRunner dockerRunner);

    CustomResponse deleteContainer(String containerId, String fileId, String fileName);
}
