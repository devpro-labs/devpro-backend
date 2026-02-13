package com.devpro.code_runner_service.service.Imp;

import com.devpro.code_runner_service.DTO.CustomResponse;
import com.devpro.code_runner_service.DTO.DockerRunner;
import com.devpro.code_runner_service.DTO.PreviewURL;
import com.devpro.code_runner_service.config.LogWebSocketHandler;
import com.devpro.code_runner_service.helper.TestCaseHelper;
import com.devpro.code_runner_service.models.Problem;
import com.devpro.code_runner_service.service.ICodeRunner;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.HashMap;
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
    public CustomResponse runCode(String uuid, DockerRunner dockerRunner, String executionId) {

        // Immediately return executionId to frontend
        Map<String, Object> data = new HashMap<>();
        data.put("executionId", executionId);

        return new CustomResponse(
                data,
                "Execution Started",
                200,
                "Running in background"
        );
    }

    @Async
    @Override
    public void executeAsync(String executionId,
                             String uuid,
                             DockerRunner dockerRunner) {

        try {

            // 1️⃣ Get problem
            Problem problem = helper.getProblemById(uuid);
            System.out.println("problem got it brooooooooooooooooooooooooooooooooo " + problem);

            // 2️⃣ Create Docker container
            CustomResponse response =
                    dockerService.getPreviewURL(dockerRunner, problem, executionId);
            System.out.println("response got it brooooooooooooooooooooooooooooooooo " + response);

            Map<String, Object> data = response.getData();
            String containerId = data.get("containerId").toString();
            String fileId = data.get("fileId").toString();
            String fileName = data.get("fileName").toString();
            PreviewURL url = (PreviewURL) data.get("url");

            // Bind container to executionId (VERY IMPORTANT)


            // 3️⃣ Run testcases (this should stream logs via WS)
            helper.codeRun(uuid, url, executionId);


            // 4️⃣ Cleanup
//            dockerService.deleteContainer(containerId, fileId, fileName);

//            LogWebSocketHandler.cleanup(executionId);

        } catch (Exception e) {

            LogWebSocketHandler.sendError(executionId, e.getMessage());
        }
    }


    @Override
    public CustomResponse submitCode(String uuid, DockerRunner dockerRunner) {

        Problem problem = helper.getProblemById(uuid);
//        //docker container
        CustomResponse response = dockerService.getPreviewURL(dockerRunner, problem, "");

        //docker - response
        Map<String, Object> data = response.getData();
        var cid  = data.get("containerId").toString();
        var fileId = data.get("fileId").toString();
        var fileName = data.get("fileName").toString();
        var url = (PreviewURL)data.get("url");


        //run code - sample and hidden testcases
        helper.codeSubmit(uuid, url, " ");

        //delete code
        dockerService.deleteContainer(cid, fileId, fileName);

        return response;
    }

}
