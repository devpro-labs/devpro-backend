package com.devpro.problem_service.controller;

import java.util.List;
import java.util.UUID;

import com.devpro.problem_service.model.CustomResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devpro.problem_service.dto.TestCaseRequest;
import com.devpro.problem_service.model.TestCase;
import com.devpro.problem_service.service.TestCaseService;

@RestController
@RequestMapping("/api/problems/test-cases")
public class TestCaseController {

    private final TestCaseService service;

    public TestCaseController(TestCaseService service) {
        this.service = service;
    }

    // CREATE
    @PostMapping
    public CustomResponse create(
            @RequestBody TestCaseRequest request) throws JsonProcessingException {
        return service.create(request);
    }

    // READ BY ID
    @GetMapping("/{id}")
    public CustomResponse getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    // READ BY PROBLEM
    @GetMapping("/problem/{problemId}")
    public CustomResponse getByProblem(
            @PathVariable UUID problemId) {
        return service.getByProblem(problemId);
    }

    // UPDATE
    @PutMapping("/{id}")
    public CustomResponse update(
            @PathVariable UUID id,
            @RequestBody TestCaseRequest request) throws JsonProcessingException {
        return service.update(id, request);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public CustomResponse delete(@PathVariable UUID id) {
        return service.delete(id);
    }

    @GetMapping("/sample/{id}")
    public CustomResponse getSampleTestCases(@PathVariable UUID id){
        return service.getSampleTestCases(id);
    }

    //raw
    @GetMapping("/problem/{problemId}/raw")
    public List<TestCase> getByProblemRaw(@PathVariable UUID problemId) {
        return service.getTestCasesRaw(problemId);
    }
}

