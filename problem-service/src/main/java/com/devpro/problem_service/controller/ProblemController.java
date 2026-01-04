package com.devpro.problem_service.controller;

import java.util.UUID;

import com.devpro.problem_service.model.CustomResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devpro.problem_service.dto.ProblemRequest;
import com.devpro.problem_service.service.ProblemService;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemService service;

    public ProblemController(ProblemService service) {
        this.service = service;
    }

    // CREATE
    @PostMapping
    public CustomResponse create(
            @RequestBody ProblemRequest request) throws JsonProcessingException {
        return service.create(request);
    }

    // READ ALL
    @GetMapping
    public CustomResponse getAll() {
        return service.getAll();
    }

    // READ BY ID
    @GetMapping("/{id}")
    public CustomResponse getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    // UPDATE
//    @PutMapping("/{id}")
//    public ResponseEntity<Problem> update(
//            @PathVariable UUID id,
//            @RequestBody ProblemRequest request) {
//        return ResponseEntity.ok(service.update(id, request));
//    }

    // DELETE (soft)
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> delete(@PathVariable UUID id) {
//        service.delete(id);
//        return ResponseEntity.noContent().build();
//    }
}
