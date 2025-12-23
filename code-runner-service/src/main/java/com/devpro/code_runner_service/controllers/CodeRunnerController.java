package com.devpro.code_runner_service.controllers;

import com.devpro.code_runner_service.DTO.CustomResponse;
import com.devpro.code_runner_service.DTO.DockerRunner;
import com.devpro.code_runner_service.service.Imp.CodeRunnerService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/code-runner")
public class CodeRunnerController {
    private final CodeRunnerService codeRunnerService;

    public CodeRunnerController( CodeRunnerService codeRunnerService){
        this.codeRunnerService = codeRunnerService;
    }


    @PostMapping("/run/{id}")
    public CustomResponse codeRunHelper(@PathVariable String id, @RequestBody DockerRunner dockerRunner){
        return codeRunnerService.runCode(id, dockerRunner);
    }

    @PostMapping("/submit/{id}")
    public CustomResponse codeSubmitHelper(@PathVariable String id, @RequestBody DockerRunner dockerRunner){
        return codeRunnerService.submitCode(id, dockerRunner);
    }


}
