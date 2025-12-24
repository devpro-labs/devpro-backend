package com.devpro.code_runner_service.service.Imp;

import com.devpro.code_runner_service.DTO.CustomResponse;
import com.devpro.code_runner_service.DTO.DockerRunner;
import com.devpro.code_runner_service.DTO.PreviewURL;
import com.devpro.code_runner_service.helper.TestCaseHelper;
import com.devpro.code_runner_service.service.ICodeRunner;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CodeRunnerService implements ICodeRunner {

    private final DockerService dockerService;
    private final TestCaseHelper helper;

    public CodeRunnerService(DockerService dockerService, TestCaseHelper helper) {
        this.dockerService = dockerService;
        this.helper = helper;

    }

    @Override
    public CustomResponse runCode(String uuid, DockerRunner dockerRunner) {
        //docker container
        CustomResponse response = dockerService.getPreviewURL(dockerRunner);

        //docker - response
        Map<String, Object> data = response.getData();
        var cid  = data.get("containerId").toString();
        var fileId = data.get("fileId").toString();
        var fileName = data.get("fileName").toString();
        var url = (PreviewURL)data.get("url");


        //run code - sample testcases
        response = helper.codeRun(uuid, url);

        //delete code
        dockerService.deleteContainer(cid, fileId, fileName);

        return response;

    }

    @Override
    public CustomResponse submitCode(String uuid, DockerRunner dockerRunner) {
//        //docker container
        CustomResponse response = dockerService.getPreviewURL(dockerRunner);

        //docker - response
        Map<String, Object> data = response.getData();
        var cid  = data.get("containerId").toString();
        var fileId = data.get("fileId").toString();
        var fileName = data.get("fileName").toString();
        var url = (PreviewURL)data.get("url");


        //run code - sample and hidden testcases
        response = helper.codeSubmit(uuid, url);

        //delete code
        dockerService.deleteContainer(cid, fileId, fileName);

        return response;
    }

}
