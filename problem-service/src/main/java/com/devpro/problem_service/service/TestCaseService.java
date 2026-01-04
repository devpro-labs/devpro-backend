package com.devpro.problem_service.service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.devpro.problem_service.dto.TestCaseRequest;
import com.devpro.problem_service.model.CustomResponse;
import com.devpro.problem_service.model.Problem;
import com.devpro.problem_service.model.TestCase;
import com.devpro.problem_service.repository.ProblemRepository;
import com.devpro.problem_service.repository.TestCaseRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class TestCaseService {


    private final TestCaseRepository repository;
    private final ProblemRepository problemRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TestCaseService(
            TestCaseRepository repository,
            ProblemRepository problemRepository) {
        this.repository = repository;
        this.problemRepository = problemRepository;
    }

    // ================= CREATE =================
    public CustomResponse create(TestCaseRequest request) throws JsonProcessingException {


        double sizeKb = calculateSize(request);

        TestCase tc = new TestCase();
        tc.setProblemId(request.getProblemId());
        tc.setExpectedStatus(request.getExpectedStatus());
        tc.setIsHidden(request.getIsHidden());
        tc.setSizeKb(sizeKb);
        tc.setEndpoint(request.getEndpoint());
        tc.setMethod(request.getMethod());


        tc.setInputJson(objectMapper.readTree(request.getInput().toString()));
        tc.setExpectedOutputJson(objectMapper.readTree(request.getExpectedOutput().toString()));


        TestCase saved = repository.save(tc);

        return new CustomResponse(
                Map.of("testcase", saved),
                "Test case created successfully",
                201,
                ""
        );
    }

    // ================= READ =================
    public CustomResponse getById(UUID id) {
        TestCase tc = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Test case not found"));

        return new CustomResponse(
                Map.of("testcase", tc),
                "Testcase fetched successfully",
                200,
                ""
        );
    }

    public CustomResponse getByProblem(UUID problemId) {
        List<TestCase> testCases = repository.findByProblemId(problemId);

        return new CustomResponse(
                Map.of("testcases", testCases),
                "Testcases fetched successfully",
                200,
                ""
        );
    }

    // ================= UPDATE =================
    public CustomResponse update(UUID id, TestCaseRequest request) {

        TestCase tc = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Test case not found"));

        double sizeKb = calculateSize(request);

        tc.setExpectedStatus(request.getExpectedStatus());
        tc.setIsHidden(request.getIsHidden());
        tc.setSizeKb(sizeKb);
        tc.setEndpoint(request.getEndpoint());
        tc.setMethod(request.getMethod());


        tc.setInputJson(request.getInput());
        tc.setExpectedOutputJson(request.getExpectedOutput());


        repository.save(tc);

        return new CustomResponse(
                Map.of("testcase", tc),
                "Test case updated successfully",
                200,
                ""
        );
    }

    // ================= DELETE =================
    public CustomResponse delete(UUID id) {
        TestCase tc = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Test case not found"));


        repository.delete(tc);

        return new CustomResponse(
                Map.of(),
                "Test case deleted successfully",
                200,
                ""
        );
    }

    // ================= HELPERS =================


    private double calculateSize(TestCaseRequest r) {
        double bytes =
                r.getInput().toString().getBytes(StandardCharsets.UTF_8).length +
                        r.getExpectedOutput().toString().getBytes(StandardCharsets.UTF_8).length;

        return Math.round(bytes / 1024);
    }


    // ================= SAMPLE =================
    public CustomResponse getSampleTestCases(UUID problemId) {
        List<TestCase> testCases = repository.findByProblemId(problemId)
                .stream()
                .filter(tc -> !tc.getIsHidden())
                .toList();

        Problem p = problemRepository.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Problem not found"));

        Map<String, Object> data = new HashMap<>();
        data.put("testCases", testCases);
        data.put("problem", p);

        return new CustomResponse(
                data,
                "Sample testcases fetched successfully",
                200,
                ""
        );
    }

    public List<TestCase> getTestCasesRaw(UUID problemId) {
        return repository.findByProblemId(problemId);
    }
}
