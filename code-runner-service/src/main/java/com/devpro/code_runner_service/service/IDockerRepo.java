package com.devpro.code_runner_service.service;

import com.devpro.code_runner_service.DTO.CustomResponse;
import com.devpro.code_runner_service.DTO.DockerRunner;
import com.devpro.code_runner_service.DTO.PreviewURL;

public interface IDockerRepo {
    CustomResponse getPreviewURL(DockerRunner dockerRunner) throws  Exception;
    CustomResponse deleteContainer(String containerId, String fileId, String fileName);
}
