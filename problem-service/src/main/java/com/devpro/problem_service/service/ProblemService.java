package com.devpro.problem_service.service;

import java.net.http.HttpRequest;
import java.util.*;

import com.devpro.problem_service.dto.ProblemResponse;
import com.devpro.problem_service.dto.TestCaseRequest;
import com.devpro.problem_service.model.*;
import com.devpro.problem_service.repository.SubmissionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import com.devpro.problem_service.dto.ProblemRequest;
import com.devpro.problem_service.repository.ProblemRepository;

@Service
public class ProblemService {

    private final ProblemRepository repository;
    private final TestCaseService testCaseService;
    private final SubmissionRepository submissionRepository;

    private void map(ProblemRequest request,Problem p) {

        p.setTitle(request.getTitle());
        p.setDescription(request.getDescription());
        p.setDifficulty(request.getDifficulty());
        p.setTags(request.getTags());
        p.setIsActive(true);
        p.setServices(request.getServices());
        p.setCpuLimit(request.getCpuLimit());
        p.setKeys(request.getKeys());
        p.setEntryFile(request.getEntryFile());
        p.setMemoryLimitMB(request.getMemoryLimitMB());
        p.setTimeLimitSeconds(request.getTimeLimitSeconds());
    }


    public ProblemService(ProblemRepository repository, TestCaseService testCaseService, SubmissionRepository submissionRepository) {
        this.repository = repository;
        this.testCaseService = testCaseService;
        this.submissionRepository = submissionRepository;
    }

    // CREATE
    public CustomResponse create(ProblemRequest request) throws JsonProcessingException {
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
        Map<String, Object> data = Map.of("problem", p);

        return new CustomResponse(
                data,
                "Successfully created problem",
                201,
                ""
        );
    }

    // READ ALL
    public CustomResponse getAll(String userId) {
        List<Problem> problems = repository.findAll();

        if(problems.isEmpty()){
            return new CustomResponse(
                    Map.of(),
                    "No problems found.",
                     200,
                    ""
            );
        }

        //get submissions and add to object
        List<UUID> submissions = submissionRepository.findAllProblemIdByUserIdAndStatus(userId, SubmissionStatus.ACCEPTED);

        List<ProblemResponse> data = new ArrayList<>();
        Map<UUID, Boolean> submissionMap = new HashMap<>();
        for (UUID submissionId : submissions) {
            submissionMap.put(submissionId, true);
        }

        for (Problem p : problems) {
            data.add(new ProblemResponse(p, submissionMap.getOrDefault(p.getId(), false)));
        }

        System.out.println(problems.size() + " " + problems.getFirst().toString());
        Map<String, Object> DATA = Map.of("problems", data);
        return new CustomResponse(
                DATA,
                "Problems fetched successfully.",
                200,
                ""
        );
    }

    // READ BY ID
    public CustomResponse getById(UUID id) {
        Problem p = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Problem not found"));
        List<TestCase> t = testCaseService.getTestcaseByProblem(id);
//        System.out.println(p);
        return new CustomResponse(
                Map.of("problem", p,"testcases", t),
                "Problem fetched successfully.",
                200,
                ""
        );
    }

    public Problem getByIdRaw(UUID id) {

        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Problem not found"));
    }

    private Problem findProblemById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Problem not found"));
    }

//    public CustomResponse update(UUID id, ProblemRequest request) {
//        Problem p = findProblemById(id);
//        map(request, p);
//        Problem updated = repository.save(p);
//
//        return new CustomResponse(
//                Map.of("problem", updated),
//                "Problem updated successfully.",
//                200,
//                ""
//        );
//    }



    //  Delete
    public CustomResponse delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Problem not found");
        }
        testCaseService.deleteByProblemId(id);
        repository.deleteById(id);

        return new CustomResponse(
                Map.of("problemId", id),
                "Problem deleted permanently",
                200,
                ""
        );
    }

    // UPDATE
    @Transactional
    public CustomResponse update(UUID id,ProblemRequest request)
            throws JsonProcessingException{

        Problem problem = findProblemById(id);

        // Reuse map()
        map(request, problem);

        // Replace testcases
        testCaseService.deleteByProblemId(id);

        for (TestCaseRequest testCaseRequest : request.getTestCases()) {

            TestCaseRequest newTestCase = new TestCaseRequest();
            newTestCase.setProblemId(problem.getId());
            newTestCase.setInput(testCaseRequest.getInput());
            newTestCase.setExpectedOutput(testCaseRequest.getExpectedOutput());
            newTestCase.setIsHidden(testCaseRequest.getIsHidden());
            newTestCase.setExpectedStatus(testCaseRequest.getExpectedStatus());
            newTestCase.setEndpoint(testCaseRequest.getEndpoint());
            newTestCase.setMethod(testCaseRequest.getMethod());

            testCaseService.create(newTestCase);
        }

        Problem updated = repository.save(problem);

        return new CustomResponse(
                Map.of("problem", updated),
                "Problem updated successfully",
                200,
                ""
        );
    }


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
