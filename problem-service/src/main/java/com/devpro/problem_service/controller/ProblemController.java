package com.devpro.problem_service.controller;

import java.util.UUID;

import com.devpro.problem_service.model.CustomResponse;
import com.devpro.problem_service.model.Problem;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<CustomResponse> update(
            @PathVariable UUID id,
            @RequestBody ProblemRequest request) {

        CustomResponse response = service.update(id, request);
        return ResponseEntity.ok(response);
    }


    // DELETE (soft)
    @DeleteMapping("/{id}")
    public ResponseEntity<CustomResponse> delete(@PathVariable UUID id) {

        CustomResponse response = service.delete(id);
        return ResponseEntity.ok(response);
    }
}
