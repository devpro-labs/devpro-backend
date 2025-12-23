package com.devpro.problem_service.service;
import java.util.List;
import java.util.UUID;

import com.devpro.problem_service.dto.TestCaseRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Service;

import com.devpro.problem_service.dto.ProblemRequest;
import com.devpro.problem_service.model.Problem;
import com.devpro.problem_service.repository.ProblemRepository;

@Service
public class ProblemService {

    private final ProblemRepository repository;
    private final TestCaseService testCaseService;

    private void map(ProblemRequest request, Problem p) {

        UUID uuid = UUID.randomUUID();

        p.setTitle(request.getTitle());
        p.setDescription(request.getDescription());
        p.setDifficulty(request.getDifficulty());
        p.setCategory(request.getCategory());
        p.setIsActive(true);
        p.setSlug(uuid.toString());

    }

    public ProblemService(ProblemRepository repository,  TestCaseService testCaseService) {
        this.repository = repository;
        this.testCaseService = testCaseService;
    }

 // CREATE
    public Problem create(ProblemRequest request) throws JsonProcessingException {
        Problem p = new Problem();
        map(request, p);
        p = repository.save(p);

        for (TestCaseRequest testCaseRequest : request.getTestCases()) {
            TestCaseRequest request1 = new TestCaseRequest();
            request1.setProblemId(p.getId());
            request1.setInput(testCaseRequest.getInput());
            request1.setExpectedOutput(testCaseRequest.getExpectedOutput());
            request1.setIsHidden(testCaseRequest.getIsHidden());
            request1.setExpectedStatus(testCaseRequest.getExpectedStatus());
            request1.setEndpoint(testCaseRequest.getEndpoint());
            request1.setMethod(testCaseRequest.getMethod());
            testCaseService.create(request1);
        }

        return p;
    }
    
    // READ ALL
    public List<Problem> getAll() {
        return repository.findAll();
    }
    
    // READ BY ID
    public Problem getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Problem not found"));
    }

//    // UPDATE
//    public Problem update(UUID id, ProblemRequest request) {
//        Problem p = getById(id);
//        map(request, p);
//        return repository.save(p);
//    }
//
//    // DELETE (soft delete)
//    public void delete(UUID id) {
//        Problem p = getById(id);
//        p.setIsActive(false);
//        repository.save(p);
//    }
//
//    private void map(ProblemRequest r, Problem p) {
//        p.setTitle(r.getTitle());
//        p.setSlug(r.getSlug());
//        p.setDescription(r.getDescription());
//        p.setDifficulty(r.getDifficulty());
//        p.setCategory(r.getCategory());
//
//        if (r.getIsActive() != null) {
//            p.setIsActive(r.getIsActive());
//        }
//    }
}
