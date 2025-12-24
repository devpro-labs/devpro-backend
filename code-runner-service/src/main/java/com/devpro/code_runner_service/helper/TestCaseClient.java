package com.devpro.code_runner_service.helper;

import com.devpro.code_runner_service.models.TestCase;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "problem-service")
public interface TestCaseClient {
    @GetMapping("/api/problems/test-cases/problem/{id}/raw")
    List<TestCase> getTestCases(
            @PathVariable("id") String problemId
    );
}
