package com.devpro.code_runner_service.controllers;

import com.devpro.code_runner_service.DTO.CustomResponse;
import com.devpro.code_runner_service.DTO.DockerRunner;
import com.devpro.code_runner_service.repository.IDockerRepo;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/docker/api")
public class DockerController {
    private final IDockerRepo dockerRepo;
    public DockerController(IDockerRepo dockerRepo){
        this.dockerRepo = dockerRepo;
    }

    @PostMapping("/preview")
    public CustomResponse getPreviewURL(@RequestBody DockerRunner dockerRunner) throws Exception{
        return dockerRepo.getPreviewURL(dockerRunner);
    }

    @DeleteMapping("/preview/{containerId}/{fileId}/{fileName}")
    public CustomResponse deleteContainer(@PathVariable String containerId, @PathVariable String fileId, @PathVariable String fileName){
        return dockerRepo.deleteContainer(containerId, fileId, fileName);
    }
}
