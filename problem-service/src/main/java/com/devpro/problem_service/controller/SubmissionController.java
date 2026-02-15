package com.devpro.problem_service.controller;


import com.devpro.problem_service.dto.SubmissionRequest;
import com.devpro.problem_service.model.CustomResponse;
import com.devpro.problem_service.service.SubmissionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/submissions")
@Slf4j
public class SubmissionController {
    private final SubmissionService service;

    public SubmissionController(SubmissionService service) {
        this.service = service;
    }

    @PostMapping
    public CustomResponse createSubmission(@RequestBody SubmissionRequest request){
        return service.saveSubmission(request);
    }

    @GetMapping("/{submissionId}")
    public CustomResponse getSubmission(HttpServletRequest request,@PathVariable String submissionId){
        log.info("Getting submission by id {}", submissionId);
        return service.getSubmission(request, submissionId);
    }
}
