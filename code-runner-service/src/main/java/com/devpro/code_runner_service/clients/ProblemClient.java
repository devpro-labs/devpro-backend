package com.devpro.code_runner_service.clients;

import com.devpro.code_runner_service.DTO.CustomResponse;
import com.devpro.code_runner_service.DTO.SubmissionRequest;
import com.devpro.code_runner_service.models.Problem;
import com.devpro.code_runner_service.models.TestCase;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "problem-service")
public interface ProblemClient {
    @GetMapping("/api/problems/test-cases/problem/{id}/raw")
    List<TestCase> getTestCases(
            @PathVariable("id") String problemId
    );

    @GetMapping("/api/problems/{id}/raw")
    Problem getProblem(
      @PathVariable("id") String problemId
    );

    @GetMapping("/api/problems/{publicId}/url")
    String getPublicUrl(
            @PathVariable("publicId") String publicId
    );

    @PostMapping("/api/submissions")
    CustomResponse saveSubmission(@RequestBody SubmissionRequest request);
}
