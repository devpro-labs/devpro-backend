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

import com.devpro.problem_service.dto.ProblemRequest;
import com.devpro.problem_service.model.Problem;
import com.devpro.problem_service.service.ProblemService;

@RestController
@RequestMapping("/admin/problems")
public class ProblemController {

    private final ProblemService service;

    public ProblemController(ProblemService service) {
        this.service = service;
    }

    // CREATE
    @PostMapping
    public ResponseEntity<Problem> create(
            @RequestBody ProblemRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<Problem>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    // READ BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Problem> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<Problem> update(
            @PathVariable UUID id,
            @RequestBody ProblemRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    // DELETE (soft)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
