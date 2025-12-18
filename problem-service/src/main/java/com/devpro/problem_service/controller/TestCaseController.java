package com.devpro.problem_service.controller;

import java.util.List;
import java.util.UUID;

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
@RequestMapping("/admin/test-cases")
public class TestCaseController {

    private final TestCaseService service;

    public TestCaseController(TestCaseService service) {
        this.service = service;
    }

    // CREATE
    @PostMapping
    public ResponseEntity<TestCase> create(
            @RequestBody TestCaseRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    // READ BY ID
    @GetMapping("/{id}")
    public ResponseEntity<TestCase> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    // READ BY PROBLEM
    @GetMapping("/problem/{problemId}")
    public ResponseEntity<List<TestCase>> getByProblem(
            @PathVariable UUID problemId) {
        return ResponseEntity.ok(service.getByProblem(problemId));
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<TestCase> update(
            @PathVariable UUID id,
            @RequestBody TestCaseRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

