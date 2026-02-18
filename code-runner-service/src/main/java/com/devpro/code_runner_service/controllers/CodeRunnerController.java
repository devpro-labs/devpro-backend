package com.devpro.code_runner_service.controllers;

import com.devpro.code_runner_service.DTO.CustomResponse;
import com.devpro.code_runner_service.DTO.DockerRunner;
import com.devpro.code_runner_service.models.ExecutionData;
import com.devpro.code_runner_service.service.ICodeRunner;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/code-runner")
public class CodeRunnerController {
    private final ICodeRunner codeRunnerService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${headers.user.id}")
    private String header_name;

    public CodeRunnerController(ICodeRunner codeRunnerService, RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper){
        this.codeRunnerService = codeRunnerService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }


    @PostMapping("/run/{problemId}")
    public CustomResponse codeRunHelper(HttpServletRequest request ,@PathVariable String problemId, @RequestBody DockerRunner dockerRunner) throws JsonProcessingException {
        //get userId
        String HEADER_NAME = header_name;
        String userId = request.getHeader(HEADER_NAME);
        if(userId==null){
            return new CustomResponse(null, "UnAuthenticated User", 401, null
            );
        }

        // Generate executionId - to connect with socket
        String executionId = UUID.randomUUID().toString();

        //convert data to string
        String data = objectMapper.writeValueAsString(new ExecutionData(executionId, problemId, dockerRunner, userId));

        // add code run into queue
        String key = "queue:code-runs";
        redisTemplate.opsForList()
                .leftPush(key, data);

        //get back executionId
        return codeRunnerService.runCode(problemId, dockerRunner, executionId);
    }

    @PostMapping("/submit/{id}")
    public CustomResponse codeSubmitHelper(@PathVariable String id, @RequestBody DockerRunner dockerRunner, HttpServletRequest request){
        return codeRunnerService.submitCode(id, dockerRunner, request);
    }


}
