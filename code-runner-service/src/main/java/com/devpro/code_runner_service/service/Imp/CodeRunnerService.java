package com.devpro.code_runner_service.service.Imp;

import com.devpro.code_runner_service.DTO.*;
import com.devpro.code_runner_service.config.socket_configs.LogWebSocketHandler;
import com.devpro.code_runner_service.helper.RateLimiting;
import com.devpro.code_runner_service.helper.TestCaseHelper;
import com.devpro.code_runner_service.models.Problem;
import com.devpro.code_runner_service.service.ICodeRunner;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class CodeRunnerService implements ICodeRunner {

    private final DockerService dockerService;
    private final TestCaseHelper helper;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final LogWebSocketHandler logWebSocketHandler;
    private final RateLimiting rateLimiting;

    @Value("${headers.user.id}")
    private String headerName;

    public CodeRunnerService(DockerService dockerService, TestCaseHelper helper, RedisTemplate<String,String> redisTemplate, ObjectMapper objectMapper, LogWebSocketHandler logWebSocketHandler, RateLimiting rateLimiting) {
        this.dockerService = dockerService;
        this.helper = helper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.logWebSocketHandler = logWebSocketHandler;
        this.rateLimiting = rateLimiting;
    }

    @Override
    public CustomResponse runCode(String problemId, DockerRunner dockerRunner, String executionId) {

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
                             String problemId,
                             DockerRunner dockerRunner, String userId) {

        try {

            String framework = dockerRunner.getLibOrFramework();

            //data key
            String dataKey = "container:data:" + userId + ":"  + problemId + ":" + framework;
            //ttl key
            String ttlKey = "container:ttl:" + userId + ":" + problemId + ":" + framework;

            //lock key
            String lockKey = "container:lock:" + userId + ":" + problemId + ":" + framework;

            //check if already locked then return
            if(redisTemplate.opsForValue().get(lockKey)!=null){
                logWebSocketHandler.sendEvent(executionId, "ERROR", new CustomResponse(null, "Execution already in progress", 400, "Execution already in progress"));
                return;
            }

            //check ratelimiting
            if(rateLimiting.isQuotaExceed(userId)){
                logWebSocketHandler.sendEvent(executionId, "ERROR", new CustomResponse(null, "Try after sometime.Your Quota is Exceed", 429, "Quota Exceed"));
                return;
            }

            // If container already exists → delete it
            String existingJson = redisTemplate.opsForValue().get(dataKey);

            //set container is not build yet to set lock
            redisTemplate.opsForValue().setIfAbsent(lockKey, "setlock");


            if (existingJson != null) {

                //get metadata
                ContainerDTO oldContainer =
                        objectMapper.readValue(existingJson, ContainerDTO.class);

                //remove container and volume
                dockerService.deleteContainer(
                        oldContainer.getProjectId(),
                        oldContainer.getFileId(),
                        oldContainer.getFileName()
                );

                //remove key
                redisTemplate.delete(dataKey);
                redisTemplate.delete(ttlKey);
            }


            // Get problem
            Problem problem = helper.getProblemById(problemId);

            //  Create Docker container
            CustomResponse response =
                    dockerService.getPreviewURL(dockerRunner, problem, executionId);

            Map<String, Object> data = response.getData();
            String projectId = data.get("projectId").toString();
            String fileId = data.get("fileId").toString();
            String fileName = data.get("fileName").toString();
            PreviewURL url = (PreviewURL) data.get("url");


            // Run testcases (this should stream logs via WS)
            helper.codeRun(problemId, url, executionId);

            //now remove lock
            redisTemplate.delete(lockKey);


            //prepare meta data
            ContainerDTO containerDTO = new ContainerDTO(projectId, fileId, fileName);
            String json = objectMapper.writeValueAsString(containerDTO);

            // Store metadata
            redisTemplate.opsForValue().set(dataKey, json);
            //store ttl
            redisTemplate.opsForValue().set(ttlKey, "active", Duration.ofMinutes(1));

        } catch (Exception e) {
            log.info("Error in executeAsync: {}", e.getMessage());
            logWebSocketHandler.sendEvent(executionId, "ERROR", new CustomResponse(null, e.getMessage(), 500, "Error in executeAsync"));
        }
    }


    @Override
    public CustomResponse submitCode(String problemId, DockerRunner dockerRunner, HttpServletRequest request) {

        String userId = request.getHeader(headerName);
        if (userId == null){
            return new CustomResponse(null, "UnAuthorized User", 401, null);
        }

        //check ratelimiting
        if(rateLimiting.isQuotaExceed(userId)){
            return new CustomResponse(null, "Try after sometime.Your Quota is Exceed", 429, "Quota Exceed");
        }

        Problem problem = helper.getProblemById(problemId);
//        //docker container
        CustomResponse response = dockerService.getPreviewURL(dockerRunner, problem, "");

        //docker - response
        Map<String, Object> data = response.getData();
        var cid  = data.get("containerId").toString();
        var fileId = data.get("fileId").toString();
        var fileName = data.get("fileName").toString();
        var url = (PreviewURL)data.get("url");


        //run code - sample and hidden testcases
        CustomResponse customResponse = helper.codeSubmit(problemId, url, " ");
        //delete code
        dockerService.deleteContainer(cid, fileId, fileName);

        log.info("response is {}", customResponse.toString());

        SubmissionRequest submissionRequest = new SubmissionRequest();
        submissionRequest.setProblemId(UUID.fromString(problemId));
        submissionRequest.setFramework(dockerRunner.getLibOrFramework());
        submissionRequest.setUserId(request.getHeader(headerName));
        submissionRequest.setTotalTestcases((Integer) customResponse.getData().get("TotalTestcases"));
        submissionRequest.setTestcasesPassed((Integer) customResponse.getData().get("PassedTestcases"));
        submissionRequest.setStatus((SubmissionStatus) customResponse.getData().get("Status"));
        log.info("submissionRequest is {}", submissionRequest.toString());
        log.info("SubmissionReq is ready ...................");

        //call submission
        helper.createSubmission(submissionRequest);

        return response;
    }

}
