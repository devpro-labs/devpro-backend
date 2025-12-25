package com.devpro.problem_service.service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.devpro.problem_service.dto.TestCaseRequest;
import com.devpro.problem_service.model.CustomResponse;
import com.devpro.problem_service.model.Problem;
import com.devpro.problem_service.model.StorageType;
import com.devpro.problem_service.model.TestCase;
import com.devpro.problem_service.repository.ProblemRepository;
import com.devpro.problem_service.repository.TestCaseRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class TestCaseService {


    private static final int DB_LIMIT_KB = 5;

    private final TestCaseRepository repository;
    private final FileStorageService fileStorageService;
    private final ProblemRepository problemRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TestCaseService(
            TestCaseRepository repository,
            FileStorageService fileStorageService,
            ProblemRepository problemRepository) {
        this.repository = repository;
        this.fileStorageService = fileStorageService;
        this.problemRepository = problemRepository;
    }

    // ================= CREATE =================
    public CustomResponse create(TestCaseRequest request) throws JsonProcessingException {

        validateJson(request);

        int sizeKb = calculateSize(request);

        TestCase tc = new TestCase();
        tc.setProblemId(request.getProblemId());
        tc.setExpectedStatus(request.getExpectedStatus());
        tc.setIsHidden(request.getIsHidden());
        tc.setSizeKb(sizeKb);
        tc.setEndpoint(request.getEndpoint());
        tc.setMethod(request.getMethod());

        if (request.getIsHidden()) {

            // 🔒 Hidden testcases → always FILE
            tc.setStorageType(StorageType.FILE);
            tc.setInputFileUrl(
                    fileStorageService.upload(request.getInput(), "input"));
            tc.setExpectedOutputFileUrl(
                    fileStorageService.upload(request.getExpectedOutput(), "expected"));

            tc.setInputJson(null);
            tc.setExpectedOutputJson(null);

        }
        else if (sizeKb <= DB_LIMIT_KB) {

            // 👀 Small visible testcases → DB
            tc.setStorageType(StorageType.DB);
            tc.setInputJson(objectMapper.readTree(request.getInput()));
            tc.setExpectedOutputJson(objectMapper.readTree(request.getExpectedOutput()));

            tc.setInputFileUrl(null);
            tc.setExpectedOutputFileUrl(null);

        }
        else {

            // 📦 Large visible testcases → FILE
            tc.setStorageType(StorageType.FILE);
            tc.setInputFileUrl(
                    fileStorageService.upload(request.getInput(), "input"));
            tc.setExpectedOutputFileUrl(
                    fileStorageService.upload(request.getExpectedOutput(), "expected"));

            tc.setInputJson(null);
            tc.setExpectedOutputJson(null);
        }

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

        validateJson(request);

        TestCase tc = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Test case not found"));

        int sizeKb = calculateSize(request);

        tc.setExpectedStatus(request.getExpectedStatus());
        tc.setIsHidden(request.getIsHidden());
        tc.setSizeKb(sizeKb);
        tc.setEndpoint(request.getEndpoint());
        tc.setMethod(request.getMethod());

        // cleanup old files
        if (tc.getStorageType() == StorageType.FILE) {
            fileStorageService.delete(tc.getInputFileUrl());
            fileStorageService.delete(tc.getExpectedOutputFileUrl());
        }

        if (sizeKb <= DB_LIMIT_KB) {
            tc.setStorageType(StorageType.DB);
            tc.setInputJson(readTree(request.getInput()));
            tc.setExpectedOutputJson(readTree(request.getExpectedOutput()));
            tc.setInputFileUrl(null);
            tc.setExpectedOutputFileUrl(null);
        } else {
            tc.setStorageType(StorageType.FILE);
            tc.setInputFileUrl(fileStorageService.upload(request.getInput(), "input"));
            tc.setExpectedOutputFileUrl(fileStorageService.upload(request.getExpectedOutput(), "expected"));
            tc.setInputJson(null);
            tc.setExpectedOutputJson(null);
        }

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

        if (tc.getStorageType() == StorageType.FILE) {
            fileStorageService.delete(tc.getInputFileUrl());
            fileStorageService.delete(tc.getExpectedOutputFileUrl());
        }

        repository.delete(tc);

        return new CustomResponse(
                Map.of(),
                "Test case deleted successfully",
                200,
                ""
        );
    }

    // ================= HELPERS =================
    private void validateJson(TestCaseRequest r) {
        readTree(r.getInput());
        readTree(r.getExpectedOutput());
    }

    private int calculateSize(TestCaseRequest r) {
        int bytes =
                r.getInput().getBytes(StandardCharsets.UTF_8).length +
                        r.getExpectedOutput().getBytes(StandardCharsets.UTF_8).length;
        return bytes / 1024;
    }

    private com.fasterxml.jackson.databind.JsonNode readTree(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON", e);
        }
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
