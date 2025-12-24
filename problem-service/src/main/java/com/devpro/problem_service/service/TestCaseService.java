package com.devpro.problem_service.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.devpro.problem_service.model.CustomResponse;
import com.devpro.problem_service.model.Problem;
import com.devpro.problem_service.repository.ProblemRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import com.devpro.problem_service.dto.TestCaseRequest;
import com.devpro.problem_service.model.StorageType;
import com.devpro.problem_service.model.TestCase;
import com.devpro.problem_service.repository.TestCaseRepository;

@Service
public class TestCaseService {

    private static final int DB_LIMIT_KB = 1;

    private final TestCaseRepository repository;
    private final FileStorageService fileStorageService;
    private final ProblemRepository problemRepository;
    ObjectMapper objectMapper = new ObjectMapper();

    public TestCaseService(
            TestCaseRepository repository,
            FileStorageService fileStorageService,
            ProblemRepository problemRepository) {
        this.repository = repository;
        this.fileStorageService = fileStorageService;
        this.problemRepository = problemRepository;
    }

    // CREATE
    public CustomResponse create(TestCaseRequest request) throws JsonProcessingException {

        int sizeKb = calculateSize(request);

        TestCase tc = new TestCase();
        tc.setExpectedStatus(request.getExpectedStatus());
        tc.setIsHidden(request.getIsHidden());
        tc.setSizeKb(sizeKb);
        tc.setProblemId(request.getProblemId());
        tc.setEndpoint(request.getEndpoint());
        tc.setMethod(request.getMethod());

        if (sizeKb <= DB_LIMIT_KB) {
            tc.setStorageType(StorageType.DB);
            tc.setInputJson(objectMapper.readTree(request.getInput()));
            tc.setExpectedOutputJson(objectMapper.readTree(request.getExpectedOutput()));
        } else {
            tc.setStorageType(StorageType.FILE);
            tc.setInputFileUrl(
                    fileStorageService.upload(request.getInput(), "input"));
            tc.setExpectedOutputFileUrl(
                    fileStorageService.upload(request.getExpectedOutput(), "expected"));
        }

        TestCase saved = repository.save(tc);
        return  new CustomResponse(
            Map.of("Testcase", saved),
            "Test case created successfully.",
            201,
            ""
        );

    }

    // READ BY ID
    public CustomResponse getById(UUID id) {
        TestCase tc = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Test case not found"));
        return  new CustomResponse(
                Map.of("Testcase", tc),
                "Testcase fetched successfully",
                200,
                ""
        );
    }

    // READ BY PROBLEM
    public CustomResponse getByProblem(UUID problemId) {
        List<TestCase> testCases = repository.findByProblemId(problemId);
        return  new CustomResponse(
                Map.of("Testcases", testCases),
                "Testcases fetched successfully",
                200,
                ""
        );
    }

    // UPDATE
    public CustomResponse update(UUID id, TestCaseRequest request) throws JsonProcessingException {

        CustomResponse response = getById(id);
        TestCase tc = (TestCase) response.getData().get("Testcase");

        // simplest approach: recreate storage if content changed
        repository.delete(tc);
        return create(request);
    }

    // DELETE
    public CustomResponse delete(UUID id) {
        repository.deleteById(id);
        return  new CustomResponse(
                Map.of(),
                "Test case deleted successfully",
                200,
                ""
        );
    }

    private int calculateSize(TestCaseRequest r) {
        return (r.getInput().length() + r.getExpectedOutput().length()) / 1024;
    }

    //get sample testcases
    public CustomResponse getSampleTestCases(UUID problemId) {
        List<TestCase> testCases = repository.findByProblemId(problemId)
                .stream().filter((tc) -> !tc.getIsHidden()).toList();
        Problem p = problemRepository.findById(problemId).get();

        Map<String, Object> data = new HashMap<>();
        data.put("testCases", testCases);
        data.put("problem", p);
        return  new CustomResponse(
                data,
                "Sample testcases fetched successfully",
                200,
                ""
        );

    }

    //raw
    public List<TestCase> getTestCasesRaw(UUID problemId) {
        return repository.findByProblemId(problemId);
    }

}
