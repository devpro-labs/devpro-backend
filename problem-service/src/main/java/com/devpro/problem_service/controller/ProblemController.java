package com.devpro.problem_service.controller;

import java.util.List;
import java.util.UUID;

import com.devpro.problem_service.model.CustomResponse;
import com.devpro.problem_service.model.Problem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.*;

import com.devpro.problem_service.dto.ProblemRequest;
import com.devpro.problem_service.service.ProblemService;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemService service;

    public ProblemController(ProblemService service) {
        this.service = service;
    }

    // CREATE
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CustomResponse create(
            @RequestPart("problem") String problemJson,
            @RequestPart(value = "composeFiles", required = false) List<MultipartFile> composeFiles
    ) throws Exception {

        ObjectMapper mapper = new ObjectMapper();

        ProblemRequest problem = mapper.readValue(problemJson, ProblemRequest.class);

        System.out.println("TITLE: " + problem.getTitle());
        System.out.println("FILES: " + (composeFiles != null ? composeFiles.size() : 0));

        return service.create(problem, composeFiles);
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
    @PutMapping("/{id}")
    public ResponseEntity<CustomResponse> update(
            @PathVariable UUID id,
            @RequestBody ProblemRequest request) {

//        CustomResponse response = service.update(id, request);
        return ResponseEntity.ok(null);
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

    //get file url
    @GetMapping("/{folderName}/{publicId}/url")
    public String getPublicUrl(@PathVariable String publicId, @PathVariable String folderName){
        System.out.println(folderName);
        return service.getFileUrl(publicId);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<?> handleMediaTypeError(Exception e) {
        return ResponseEntity
                .badRequest()
                .body("Request must be multipart/form-data");
    }
}
