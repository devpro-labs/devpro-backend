package com.devpro.problem_service.controller;

import java.util.UUID;

import com.devpro.problem_service.model.CustomResponse;
import com.devpro.problem_service.model.Problem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.devpro.problem_service.dto.ProblemRequest;
import com.devpro.problem_service.service.ProblemService;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemService service;
    private static final String HEADER_NAME = "X-User-Id";

    public ProblemController(ProblemService service) {
        this.service = service;
    }

    // CREATE
    @PostMapping
    public CustomResponse create(
            @RequestPart("problem") String problemJson
    ) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        ProblemRequest problem = mapper.readValue(problemJson, ProblemRequest.class);
        return service.create(problem);
    }

    // READ ALL
    @GetMapping
    public CustomResponse getAll(HttpServletRequest request) {
        String userId = request.getHeader(HEADER_NAME);
        if(userId == null){
            return new CustomResponse(null, "UnAuthenticated User", 401, null);
        }
        return service.getAll(userId);
    }

    // READ BY ID
    @GetMapping("/{id}")
    public CustomResponse getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    // UPDATE
    @PutMapping("/{id}")
    public CustomResponse update(
            @PathVariable UUID id,
            @RequestBody ProblemRequest request) throws JsonProcessingException {

        return service.update(id, request);
    }


    // DELETE (soft)
    @DeleteMapping("/{id}")
    public ResponseEntity<CustomResponse> delete(@PathVariable UUID id) {

        CustomResponse response = service.delete(id);
        return ResponseEntity.ok(response);
    }

    //get raw
    @GetMapping("/{id}/raw")
    public Problem getByIdRaw(@PathVariable UUID id) {
        return service.getByIdRaw(id);
    }


    // UPDATE
//    @PutMapping(value = "/{id}")
//    public ResponseEntity<CustomResponse> update(
//            @PathVariable UUID id,
//            @RequestPart("problem") String problemJson
//    ) throws Exception {
//
//        ObjectMapper mapper = new ObjectMapper();
//        ProblemRequest problem = mapper.readValue(problemJson, ProblemRequest.class);
//
//        CustomResponse response = service.update(id, problem);
//
//        return ResponseEntity.ok(response);
//    }

}
